package com.studyflow.plan;

import com.studyflow.advisor.AdvisorContext;
import com.studyflow.advisor.AdvisorContext.DayLoad;
import com.studyflow.advisor.AdvisorContext.EventProgress;
import com.studyflow.advisor.AdvisorNote;
import com.studyflow.advisor.AdvisorService;
import com.studyflow.availability.AvailabilityRepository;
import com.studyflow.common.BusinessRuleException;
import com.studyflow.common.DataRevision;
import com.studyflow.common.NotFoundException;
import com.studyflow.discipline.Discipline;
import com.studyflow.discipline.DisciplineRepository;
import com.studyflow.event.EventType;
import com.studyflow.event.StudyEvent;
import com.studyflow.event.StudyEventRepository;
import com.studyflow.plan.PlanNoteRepository.StoredNote;
import com.studyflow.plan.PlanView.Changes;
import com.studyflow.plan.PlanView.EventDelta;
import com.studyflow.plan.PlanView.Stats;
import com.studyflow.plan.PlanView.Warning;
import com.studyflow.plan.engine.PlannerModels.PlannedBlock;
import com.studyflow.plan.engine.PlannerModels.PlannerEvent;
import com.studyflow.plan.engine.PlannerModels.PlannerInput;
import com.studyflow.plan.engine.PlannerModels.PlannerResult;
import com.studyflow.plan.engine.PlannerModels.TimeRange;
import com.studyflow.plan.engine.PlannerModels.WeeklyWindow;
import com.studyflow.plan.engine.SchedulePlanner;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Builds, rebuilds and reads the weekly plan. The scheduling rules live in SchedulePlanner. */
@Service
public class PlanService {

  private final StudyBlockRepository blocks;
  private final StudyEventRepository events;
  private final DisciplineRepository disciplines;
  private final AvailabilityRepository availability;
  private final PlanNoteRepository notes;
  private final DataRevision revision;
  private final AdvisorService advisor;
  private final Clock clock;
  private final SchedulePlanner planner = new SchedulePlanner();

  public PlanService(
      StudyBlockRepository blocks,
      StudyEventRepository events,
      DisciplineRepository disciplines,
      AvailabilityRepository availability,
      PlanNoteRepository notes,
      DataRevision revision,
      AdvisorService advisor,
      Clock clock) {
    this.blocks = blocks;
    this.events = events;
    this.disciplines = disciplines;
    this.availability = availability;
    this.notes = notes;
    this.revision = revision;
    this.advisor = advisor;
    this.clock = clock;
  }

  /** Reads the plan of the week that contains the given day. */
  public PlanView view(LocalDate anyDayOfWeek) {
    LocalDate weekStart = mondayOf(anyDayOfWeek);
    return buildView(weekStart, LocalDateTime.now(clock), null, false);
  }

  /**
   * Generates the plan of a week, or regenerates it keeping what is already settled: done blocks,
   * locked blocks and postponed slots stay where they are; everything else is redistributed.
   */
  @Transactional
  public PlanView generate(LocalDate anyDayOfWeek) {
    LocalDate weekStart = mondayOf(anyDayOfWeek);
    LocalDate weekEnd = weekStart.plusDays(6);
    LocalDateTime now = LocalDateTime.now(clock);
    if (weekEnd.isBefore(now.toLocalDate())) {
      throw new BusinessRuleException(
          "WEEK_IN_PAST", "A week that already ended cannot be planned");
    }

    blocks.markMissedBefore(now.toLocalDate(), now.toLocalTime());
    LocalDateTime from = now.isAfter(weekStart.atStartOfDay()) ? now : weekStart.atStartOfDay();

    List<StudyBlock> current = blocks.findBetween(weekStart, weekEnd);
    List<StudyBlock> replaceable =
        current.stream().filter(block -> isReplaceable(block, from)).toList();
    blocks.deleteAll(replaceable.stream().map(StudyBlock::id).toList());

    List<StudyBlock> kept = current.stream().filter(block -> !replaceable.contains(block)).toList();
    PlannerResult result = planner.plan(plannerInput(weekStart, from, kept));
    blocks.insertAll(result.blocks());

    boolean replan = notes.find(weekStart).isPresent();
    Changes changes = diff(replaceable, result.blocks());
    // The note is not stored yet, so the draft has to be told the week is already generated.
    PlanView draft = buildView(weekStart, now, changes, true);
    AdvisorNote note = advisor.advise(advisorContext(draft, replan));
    notes.save(weekStart, note, revision.current(), now);
    return buildView(weekStart, now, changes, false);
  }

  @Transactional
  public StudyBlock setDone(long blockId, boolean done) {
    StudyBlock block = require(blockId);
    if (block.status() == BlockStatus.SKIPPED) {
      throw new BusinessRuleException("BLOCK_SKIPPED", "A postponed block cannot be completed");
    }
    blocks.updateStatus(blockId, done ? BlockStatus.DONE : BlockStatus.PLANNED);
    return require(blockId);
  }

  @Transactional
  public StudyBlock setLocked(long blockId, boolean locked) {
    StudyBlock block = require(blockId);
    if (block.status() != BlockStatus.PLANNED) {
      throw new BusinessRuleException("BLOCK_NOT_PLANNED", "Only planned blocks can be locked");
    }
    blocks.updateLocked(blockId, locked);
    return require(blockId);
  }

  /**
   * The student cannot study in this slot after all: the slot stays taken and the week is
   * regenerated so the effort lands somewhere else.
   */
  @Transactional
  public PlanView postpone(long blockId) {
    StudyBlock block = require(blockId);
    if (block.status() != BlockStatus.PLANNED) {
      throw new BusinessRuleException("BLOCK_NOT_PLANNED", "Only planned blocks can be postponed");
    }
    blocks.updateLocked(blockId, false);
    blocks.updateStatus(blockId, BlockStatus.SKIPPED);
    return generate(block.date());
  }

  private StudyBlock require(long blockId) {
    return blocks.findById(blockId).orElseThrow(() -> new NotFoundException("Block", blockId));
  }

  private static boolean isReplaceable(StudyBlock block, LocalDateTime from) {
    boolean inFuture = !block.date().atTime(block.startTime()).isBefore(from);
    return block.status() == BlockStatus.PLANNED && !block.locked() && inFuture;
  }

  private PlannerInput plannerInput(
      LocalDate weekStart, LocalDateTime from, List<StudyBlock> kept) {
    Map<Long, Integer> weights =
        disciplines.findAll().stream()
            .collect(Collectors.toMap(Discipline::id, Discipline::weight));
    Map<Long, Integer> keptPlanned =
        kept.stream()
            .filter(block -> block.status() == BlockStatus.PLANNED)
            .collect(
                Collectors.groupingBy(
                    StudyBlock::eventId, Collectors.summingInt(StudyBlock::getMinutes)));

    List<PlannerEvent> pending =
        events.findAll().stream()
            .filter(event -> !event.lastStudyDay().isBefore(from.toLocalDate()))
            .map(
                event ->
                    new PlannerEvent(
                        event.id(),
                        event.lastStudyDay(),
                        event.remainingMinutes() - keptPlanned.getOrDefault(event.id(), 0),
                        weights.getOrDefault(event.disciplineId(), 2),
                        event.type() == EventType.EXAM))
            .filter(event -> event.remainingMinutes() > 0)
            .toList();

    List<TimeRange> busy =
        kept.stream()
            .filter(block -> block.status() != BlockStatus.MISSED)
            .map(block -> new TimeRange(block.date(), block.startTime(), block.endTime()))
            .toList();
    return new PlannerInput(weekStart, from, windows(), busy, pending);
  }

  private List<WeeklyWindow> windows() {
    return availability.findAll().stream()
        .map(window -> new WeeklyWindow(window.dayOfWeek(), window.startTime(), window.endTime()))
        .toList();
  }

  private PlanView buildView(
      LocalDate weekStart, LocalDateTime now, Changes changes, boolean justGenerated) {
    LocalDate weekEnd = weekStart.plusDays(6);
    LocalDate today = now.toLocalDate();
    List<StudyBlock> weekBlocks = blocks.findBetween(weekStart, weekEnd);
    Optional<StoredNote> stored = notes.find(weekStart);
    boolean generated = justGenerated || stored.isPresent();

    List<Warning> warnings = generated ? warnings(weekEnd, today) : List.of();
    LocalDateTime from = now.isAfter(weekStart.atStartOfDay()) ? now : weekStart.atStartOfDay();
    int available =
        weekEnd.isBefore(today)
            ? 0
            : planner
                .freeRanges(new PlannerInput(weekStart, from, windows(), List.of(), List.of()))
                .stream()
                .mapToInt(TimeRange::minutes)
                .sum();
    Stats stats =
        new Stats(
            available,
            minutes(weekBlocks, BlockStatus.PLANNED),
            minutes(weekBlocks, BlockStatus.DONE),
            warnings.stream().mapToInt(Warning::missingMinutes).sum());

    return new PlanView(
        weekStart,
        weekEnd,
        today,
        generated,
        stored.isPresent() && stored.get().dataRevision() != revision.current(),
        weekBlocks,
        warnings,
        stats,
        stored.map(StoredNote::note).orElse(null),
        changes);
  }

  /** Events that must be finished by the end of this week but still have uncovered effort. */
  private List<Warning> warnings(LocalDate weekEnd, LocalDate today) {
    return events.findAll().stream()
        .filter(event -> !event.dueDate().isBefore(today))
        .filter(event -> !event.lastStudyDay().isAfter(weekEnd))
        .filter(event -> missing(event) > 0)
        .map(
            event ->
                new Warning(
                    event.id(),
                    event.title(),
                    event.disciplineName(),
                    event.type(),
                    event.dueDate(),
                    missing(event)))
        .toList();
  }

  private static int missing(StudyEvent event) {
    return event.effortMinutes() - event.doneMinutes() - event.plannedMinutes();
  }

  private static int minutes(List<StudyBlock> blocks, BlockStatus status) {
    return blocks.stream()
        .filter(block -> block.status() == status)
        .mapToInt(StudyBlock::getMinutes)
        .sum();
  }

  /** Compares the blocks that were replaced with the freshly planned ones. */
  private Changes diff(List<StudyBlock> before, List<PlannedBlock> after) {
    Set<String> beforeKeys = before.stream().map(PlanService::key).collect(Collectors.toSet());
    Set<String> afterKeys = after.stream().map(PlanService::key).collect(Collectors.toSet());
    Set<String> keptKeys = new HashSet<>(beforeKeys);
    keptKeys.retainAll(afterKeys);

    Map<Long, Integer> beforeMinutes =
        before.stream()
            .collect(
                Collectors.groupingBy(
                    StudyBlock::eventId, Collectors.summingInt(StudyBlock::getMinutes)));
    Map<Long, Integer> afterMinutes =
        after.stream()
            .collect(
                Collectors.groupingBy(
                    PlannedBlock::eventId, Collectors.summingInt(PlannedBlock::minutes)));
    Map<Long, String> titles =
        events.findAll().stream().collect(Collectors.toMap(StudyEvent::id, StudyEvent::title));

    Map<Long, EventDelta> deltas = new LinkedHashMap<>();
    Set<Long> ids = new HashSet<>(beforeMinutes.keySet());
    ids.addAll(afterMinutes.keySet());
    for (Long id : ids) {
      int was = beforeMinutes.getOrDefault(id, 0);
      int is = afterMinutes.getOrDefault(id, 0);
      if (was != is) {
        deltas.put(id, new EventDelta(id, titles.getOrDefault(id, "?"), was, is));
      }
    }
    return new Changes(
        afterKeys.size() - keptKeys.size(),
        beforeKeys.size() - keptKeys.size(),
        keptKeys.size(),
        new ArrayList<>(deltas.values()));
  }

  private static String key(StudyBlock block) {
    return block.eventId() + "|" + block.date() + "|" + block.startTime() + "|" + block.endTime();
  }

  private static String key(PlannedBlock block) {
    return block.eventId() + "|" + block.date() + "|" + block.start() + "|" + block.end();
  }

  private AdvisorContext advisorContext(PlanView view, boolean replan) {
    Map<LocalDate, List<StudyBlock>> byDay =
        view.blocks().stream()
            .filter(block -> block.status() == BlockStatus.PLANNED)
            .collect(Collectors.groupingBy(StudyBlock::date));
    List<DayLoad> days = new ArrayList<>();
    for (LocalDate day = view.weekStart(); !day.isAfter(view.weekEnd()); day = day.plusDays(1)) {
      List<StudyBlock> dayBlocks = byDay.getOrDefault(day, List.of());
      days.add(
          new DayLoad(
              day, dayBlocks.stream().mapToInt(StudyBlock::getMinutes).sum(), dayBlocks.size()));
    }

    Map<Long, Integer> plannedThisWeek =
        view.blocks().stream()
            .filter(block -> block.status() == BlockStatus.PLANNED)
            .collect(
                Collectors.groupingBy(
                    StudyBlock::eventId, Collectors.summingInt(StudyBlock::getMinutes)));
    List<EventProgress> progress =
        events.findAll().stream()
            .filter(event -> !event.dueDate().isBefore(view.today()))
            .filter(event -> ChronoUnit.DAYS.between(view.today(), event.dueDate()) <= 21)
            .map(
                event ->
                    new EventProgress(
                        event.title(),
                        event.disciplineName(),
                        event.type().name(),
                        event.dueDate(),
                        event.effortMinutes(),
                        event.doneMinutes(),
                        plannedThisWeek.getOrDefault(event.id(), 0)))
            .toList();

    return new AdvisorContext(
        view.weekStart(),
        view.today(),
        replan,
        view.stats(),
        view.warnings(),
        view.changes(),
        days,
        progress);
  }

  private static LocalDate mondayOf(LocalDate day) {
    return day.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
  }
}
