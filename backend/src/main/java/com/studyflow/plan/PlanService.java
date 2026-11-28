package com.studyflow.plan;

import com.studyflow.advisor.AdvisorContext;
import com.studyflow.advisor.AdvisorContext.DayLoad;
import com.studyflow.advisor.AdvisorContext.EventProgress;
import com.studyflow.advisor.AdvisorNote;
import com.studyflow.advisor.PlanAdvisor;
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
import com.studyflow.plan.PlanRequests.SettingsRequest;
import com.studyflow.plan.PlanView.Allocation;
import com.studyflow.plan.PlanView.Changes;
import com.studyflow.plan.PlanView.EventDelta;
import com.studyflow.plan.PlanView.Settings;
import com.studyflow.plan.PlanView.Stats;
import com.studyflow.plan.PlanView.Warning;
import com.studyflow.plan.WeekPreferencesRepository.WeekPreferences;
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
import java.time.LocalTime;
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
  private final WeekPreferencesRepository preferences;
  private final DataRevision revision;
  private final PlanAdvisor advisor;
  private final Clock clock;
  private final SchedulePlanner planner = new SchedulePlanner();

  public PlanService(
      StudyBlockRepository blocks,
      StudyEventRepository events,
      DisciplineRepository disciplines,
      AvailabilityRepository availability,
      PlanNoteRepository notes,
      WeekPreferencesRepository preferences,
      DataRevision revision,
      PlanAdvisor advisor,
      Clock clock) {
    this.blocks = blocks;
    this.events = events;
    this.disciplines = disciplines;
    this.availability = availability;
    this.notes = notes;
    this.preferences = preferences;
    this.revision = revision;
    this.advisor = advisor;
    this.clock = clock;
  }

  /** Reads the plan of the week that contains the given day. */
  public PlanView view(long userId, LocalDate anyDayOfWeek) {
    LocalDate weekStart = mondayOf(anyDayOfWeek);
    return buildView(userId, weekStart, LocalDateTime.now(clock), null, false);
  }

  /**
   * Generates the plan of a week, or regenerates it keeping what is already settled: done blocks,
   * locked blocks and postponed slots stay where they are; everything else is redistributed.
   */
  @Transactional
  public PlanView generate(long userId, LocalDate anyDayOfWeek) {
    LocalDate weekStart = mondayOf(anyDayOfWeek);
    LocalDate weekEnd = weekStart.plusDays(6);
    LocalDateTime now = LocalDateTime.now(clock);
    if (weekEnd.isBefore(now.toLocalDate())) {
      throw new BusinessRuleException(
          "WEEK_IN_PAST", "A week that already ended cannot be planned");
    }

    blocks.markMissedBefore(userId, now.toLocalDate(), now.toLocalTime());
    LocalDateTime from = now.isAfter(weekStart.atStartOfDay()) ? now : weekStart.atStartOfDay();

    List<StudyBlock> current = blocks.findBetween(userId, weekStart, weekEnd);
    List<StudyBlock> replaceable =
        current.stream().filter(block -> isReplaceable(block, from)).toList();
    blocks.deleteAll(replaceable.stream().map(StudyBlock::id).toList());

    List<StudyBlock> kept = current.stream().filter(block -> !replaceable.contains(block)).toList();
    PlannerResult result = planner.plan(plannerInput(userId, weekStart, from, kept));
    blocks.insertAll(result.blocks());

    boolean replan = notes.find(userId, weekStart).isPresent();
    Changes changes = diff(userId, replaceable, result.blocks());
    // The note is not stored yet, so the draft has to be told the week is already generated.
    PlanView draft = buildView(userId, weekStart, now, changes, true);
    AdvisorNote note = advisor.advise(advisorContext(userId, draft, replan));
    notes.save(userId, weekStart, note, revision.current(userId), now);
    return buildView(userId, weekStart, now, changes, false);
  }

  @Transactional
  public StudyBlock setDone(long userId, long blockId, boolean done) {
    StudyBlock block = require(userId, blockId);
    if (block.status() == BlockStatus.SKIPPED) {
      throw new BusinessRuleException("BLOCK_SKIPPED", "A postponed block cannot be completed");
    }
    blocks.updateStatus(blockId, done ? BlockStatus.DONE : BlockStatus.PLANNED);
    return require(userId, blockId);
  }

  @Transactional
  public StudyBlock setLocked(long userId, long blockId, boolean locked) {
    StudyBlock block = require(userId, blockId);
    if (block.status() != BlockStatus.PLANNED) {
      throw new BusinessRuleException("BLOCK_NOT_PLANNED", "Only planned blocks can be locked");
    }
    blocks.updateLocked(blockId, locked);
    return require(userId, blockId);
  }

  /**
   * The student cannot study in this slot after all: the slot stays taken and the week is
   * regenerated so the effort lands somewhere else.
   */
  @Transactional
  public PlanView postpone(long userId, long blockId) {
    StudyBlock block = require(userId, blockId);
    if (block.status() != BlockStatus.PLANNED) {
      throw new BusinessRuleException("BLOCK_NOT_PLANNED", "Only planned blocks can be postponed");
    }
    blocks.updateLocked(blockId, false);
    blocks.updateStatus(blockId, BlockStatus.SKIPPED);
    return generate(userId, block.date());
  }

  /**
   * Saves how the student wants the week organised (spread over the days, minutes per exam or
   * assignment) and regenerates it with those choices.
   */
  @Transactional
  public PlanView configure(long userId, LocalDate anyDayOfWeek, SettingsRequest request) {
    Set<Long> owned =
        events.findAll(userId).stream().map(StudyEvent::id).collect(Collectors.toSet());
    Map<Long, Integer> allocations = new LinkedHashMap<>();
    request
        .allocations()
        .forEach(
            allocation -> {
              if (!owned.contains(allocation.eventId())) {
                throw new NotFoundException("Event", allocation.eventId());
              }
              allocations.put(allocation.eventId(), allocation.minutes());
            });
    preferences.save(
        userId, mondayOf(anyDayOfWeek), new WeekPreferences(request.spread(), allocations));
    return generate(userId, anyDayOfWeek);
  }

  /**
   * Moves a planned block to another day or time of the same week. Only the study session moves:
   * the exam or assignment keeps its real date. The block is locked in its new place.
   */
  @Transactional
  public PlanView move(long userId, long blockId, LocalDate date, LocalTime start) {
    StudyBlock block = require(userId, blockId);
    if (block.status() != BlockStatus.PLANNED) {
      throw new BusinessRuleException("BLOCK_NOT_PLANNED", "Only planned blocks can be moved");
    }
    if (!mondayOf(date).equals(mondayOf(block.date()))) {
      throw new BusinessRuleException(
          "BLOCK_OTHER_WEEK", "A block can only move inside its own week");
    }
    LocalTime begin = start.withSecond(0).withNano(0);
    int minutes = block.getMinutes();
    if (begin.toSecondOfDay() / 60 + minutes > 24 * 60) {
      throw new BusinessRuleException("BLOCK_CROSSES_MIDNIGHT", "The block must end the same day");
    }
    LocalTime end = begin.plusMinutes(minutes);
    if (date.atTime(begin).isBefore(LocalDateTime.now(clock))) {
      throw new BusinessRuleException("BLOCK_IN_PAST", "A block cannot move into the past");
    }
    StudyEvent event =
        events
            .findById(userId, block.eventId())
            .orElseThrow(() -> new NotFoundException("Event", block.eventId()));
    if (date.isAfter(event.lastStudyDay())) {
      throw new BusinessRuleException(
          "BLOCK_AFTER_DEADLINE", "A study block must happen before its deadline");
    }
    boolean overlaps =
        blocks.findBetween(userId, date, date).stream()
            .filter(other -> other.id() != blockId)
            .filter(other -> other.status() != BlockStatus.MISSED)
            .anyMatch(other -> other.startTime().isBefore(end) && begin.isBefore(other.endTime()));
    if (overlaps) {
      throw new BusinessRuleException("BLOCK_OVERLAP", "Another block already uses that time");
    }
    blocks.moveTo(blockId, date, begin, end);
    return view(userId, date);
  }

  private StudyBlock require(long userId, long blockId) {
    return blocks
        .findById(userId, blockId)
        .orElseThrow(() -> new NotFoundException("Block", blockId));
  }

  private static boolean isReplaceable(StudyBlock block, LocalDateTime from) {
    boolean inFuture = !block.date().atTime(block.startTime()).isBefore(from);
    return block.status() == BlockStatus.PLANNED && !block.locked() && inFuture;
  }

  private PlannerInput plannerInput(
      long userId, LocalDate weekStart, LocalDateTime from, List<StudyBlock> kept) {
    Map<Long, Integer> weights =
        disciplines.findAll(userId).stream()
            .collect(Collectors.toMap(Discipline::id, Discipline::weight));
    Map<Long, Integer> keptPlanned =
        kept.stream()
            .filter(block -> block.status() == BlockStatus.PLANNED)
            .collect(
                Collectors.groupingBy(
                    StudyBlock::eventId, Collectors.summingInt(StudyBlock::getMinutes)));
    // Done and kept blocks of this week already count toward the minutes the student chose.
    Map<Long, Integer> keptThisWeek =
        kept.stream()
            .filter(
                block ->
                    block.status() == BlockStatus.PLANNED || block.status() == BlockStatus.DONE)
            .collect(
                Collectors.groupingBy(
                    StudyBlock::eventId, Collectors.summingInt(StudyBlock::getMinutes)));
    WeekPreferences chosen = preferences.find(userId, weekStart);

    List<PlannerEvent> pending =
        events.findAll(userId).stream()
            .filter(event -> !event.lastStudyDay().isBefore(from.toLocalDate()))
            .map(
                event ->
                    new PlannerEvent(
                        event.id(),
                        event.lastStudyDay(),
                        event.remainingMinutes() - keptPlanned.getOrDefault(event.id(), 0),
                        weights.getOrDefault(event.disciplineId(), 2),
                        event.type() == EventType.EXAM,
                        target(chosen, event.id(), keptThisWeek)))
            .filter(event -> event.remainingMinutes() > 0)
            .toList();

    List<TimeRange> busy =
        kept.stream()
            .filter(block -> block.status() != BlockStatus.MISSED)
            .map(block -> new TimeRange(block.date(), block.startTime(), block.endTime()))
            .toList();
    return new PlannerInput(weekStart, from, windows(userId), busy, pending, chosen.spread());
  }

  /** Minutes still to place for an event the student gave a share to; null when automatic. */
  private static Integer target(
      WeekPreferences chosen, long eventId, Map<Long, Integer> keptThisWeek) {
    Integer minutes = chosen.allocations().get(eventId);
    return minutes == null ? null : Math.max(0, minutes - keptThisWeek.getOrDefault(eventId, 0));
  }

  private List<WeeklyWindow> windows(long userId) {
    return availability.findAll(userId).stream()
        .map(window -> new WeeklyWindow(window.dayOfWeek(), window.startTime(), window.endTime()))
        .toList();
  }

  private PlanView buildView(
      long userId, LocalDate weekStart, LocalDateTime now, Changes changes, boolean justGenerated) {
    LocalDate weekEnd = weekStart.plusDays(6);
    LocalDate today = now.toLocalDate();
    List<StudyBlock> weekBlocks = blocks.findBetween(userId, weekStart, weekEnd);
    Optional<StoredNote> stored = notes.find(userId, weekStart);
    boolean generated = justGenerated || stored.isPresent();

    List<Warning> warnings = generated ? warnings(userId, weekEnd, today) : List.of();
    LocalDateTime from = now.isAfter(weekStart.atStartOfDay()) ? now : weekStart.atStartOfDay();
    int available =
        weekEnd.isBefore(today)
            ? 0
            : planner
                .freeRanges(
                    new PlannerInput(weekStart, from, windows(userId), List.of(), List.of()))
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
        stored.isPresent() && stored.get().dataRevision() != revision.current(userId),
        weekBlocks,
        warnings,
        stats,
        stored.map(StoredNote::note).orElse(null),
        changes,
        settings(userId, weekStart));
  }

  private Settings settings(long userId, LocalDate weekStart) {
    WeekPreferences chosen = preferences.find(userId, weekStart);
    return new Settings(
        chosen.spread(),
        chosen.allocations().entrySet().stream()
            .map(entry -> new Allocation(entry.getKey(), entry.getValue()))
            .toList());
  }

  /** Events that must be finished by the end of this week but still have uncovered effort. */
  private List<Warning> warnings(long userId, LocalDate weekEnd, LocalDate today) {
    return events.findAll(userId).stream()
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
  private Changes diff(long userId, List<StudyBlock> before, List<PlannedBlock> after) {
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
        events.findAll(userId).stream()
            .collect(Collectors.toMap(StudyEvent::id, StudyEvent::title));

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

  private AdvisorContext advisorContext(long userId, PlanView view, boolean replan) {
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
        events.findAll(userId).stream()
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
