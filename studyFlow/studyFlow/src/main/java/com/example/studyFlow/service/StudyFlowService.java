package com.example.studyFlow.service;

import com.example.studyFlow.dto.*;
import com.example.studyFlow.entity.*;
import com.example.studyFlow.exception.ConflictException;
import com.example.studyFlow.exception.ResourceNotFoundException;
import com.example.studyFlow.repository.*;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class StudyFlowService {
    private static final Set<String> EVENT_TYPES = Set.of("PROVA", "ENTREGA");
    private static final int SESSION_MINUTES = 50;
    private static final int BREAK_MINUTES = 10;

    private final SubjectRepository subjects;
    private final StudyEventRepository events;
    private final AvailabilityRepository availabilities;
    private final StudyBlockRepository blocks;

    public StudyFlowService(SubjectRepository subjects, StudyEventRepository events,
                            AvailabilityRepository availabilities, StudyBlockRepository blocks) {
        this.subjects = subjects;
        this.events = events;
        this.availabilities = availabilities;
        this.blocks = blocks;
    }

    public List<SubjectResponse> getSubjects() {
        return subjects.findAll().stream().map(this::subjectResponse).toList();
    }

    public SubjectResponse createSubject(SubjectRequest request) {
        Subject subject = new Subject(request.name().trim(), request.color());
        return subjectResponse(subjects.save(subject));
    }

    public SubjectResponse updateSubject(Long id, SubjectRequest request) {
        Subject subject = subject(id);
        subject.setName(request.name().trim());
        subject.setColor(request.color());
        return subjectResponse(subjects.save(subject));
    }

    public void deleteSubject(Long id) {
        Subject subject = subject(id);
        List<StudyEvent> related = events.findBySubjectId(id);
        for (StudyEvent event : related) deleteEventData(event);
        events.deleteAll(related);
        subjects.delete(subject);
    }

    public List<EventResponse> getEvents() {
        return events.findAllByOrderByDueDateAsc().stream().map(this::eventResponse).toList();
    }

    public EventResponse createEvent(EventRequest request) {
        validateEvent(request);
        Subject subject = subject(request.subjectId());
        StudyEvent event = new StudyEvent(request.title().trim(), normalizeType(request.type()), subject,
                request.dueDate(), request.estimatedHours());
        return eventResponse(events.save(event));
    }

    public EventResponse updateEvent(Long id, EventRequest request) {
        validateEvent(request);
        StudyEvent event = event(id);
        Subject subject = subject(request.subjectId());
        event.setTitle(request.title().trim());
        event.setType(normalizeType(request.type()));
        event.setSubject(subject);
        event.setDueDate(request.dueDate());
        event.setEstimatedHours(request.estimatedHours());
        blocks.deleteAll(blocks.findByEventId(id).stream().filter(b -> !b.getCompleted()).toList());
        return eventResponse(events.save(event));
    }

    public void deleteEvent(Long id) {
        StudyEvent event = event(id);
        deleteEventData(event);
        events.delete(event);
    }

    public List<AvailabilityResponse> getAvailability() {
        Map<String, List<Integer>> daysByInterval = new LinkedHashMap<>();
        Map<String, Availability> intervalData = new HashMap<>();
        for (Availability availability : availabilities.findAllByOrderByDayOfWeekAsc()) {
            String key = availability.getStartTime() + "|" + availability.getEndTime();
            daysByInterval.computeIfAbsent(key, ignored -> new ArrayList<>()).add(availability.getDayOfWeek());
            intervalData.putIfAbsent(key, availability);
        }
        return daysByInterval.entrySet().stream().map(entry -> {
            Availability sample = intervalData.get(entry.getKey());
            return new AvailabilityResponse(entry.getValue(), sample.getStartTime(), sample.getEndTime());
        }).toList();
    }

    /** Replaces the full weekly configuration. A day can occur only once in this MVP. */
    public List<AvailabilityResponse> replaceAvailability(AvailabilityRequest request) {
        validateAvailability(request);
        availabilities.deleteAllInBatch();
        request.daysOfWeek().stream().distinct().sorted().forEach(day ->
                availabilities.save(new Availability(day, request.startTime(), request.endTime())));
        return getAvailability();
    }

    public void clearAvailability() { availabilities.deleteAllInBatch(); }

    public List<BlockResponse> getStudyBlocks() {
        return blocks.findAllByOrderByStartAtAsc().stream().map(this::blockResponse).toList();
    }

    public ScheduleResponse generateSchedule() {
        List<StudyBlock> allBlocks = blocks.findAllByOrderByStartAtAsc();
        List<StudyBlock> completed = allBlocks.stream().filter(StudyBlock::getCompleted).collect(Collectors.toCollection(ArrayList::new));
        blocks.deleteAllInBatch(allBlocks.stream().filter(b -> !b.getCompleted()).toList());
        allBlocks.clear();
        allBlocks.addAll(completed);
        List<StudyEvent> orderedEvents = events.findAllByOrderByDueDateAsc();
        List<Availability> weekly = availabilities.findAllByOrderByDayOfWeekAsc();
        Map<Long, Long> completedMinutes = completed.stream().collect(Collectors.groupingBy(
                b -> b.getEvent().getId(), Collectors.summingLong(b -> Duration.between(b.getStartAt(), b.getEndAt()).toMinutes())));
        List<String> warnings = new ArrayList<>();
        LocalDate today = LocalDate.now();

        for (StudyEvent event : orderedEvents) {
            long required = Math.round(event.getEstimatedHours() * 60.0);
            long remaining = Math.max(0, required - completedMinutes.getOrDefault(event.getId(), 0L));
            if (remaining == 0) continue;
            if (!event.getDueDate().isAfter(today)) {
                warnings.add(event.getTitle() + ": o prazo é hoje ou já passou; não há dias disponíveis antes do prazo.");
                continue;
            }
            for (LocalDate date = today; date.isBefore(event.getDueDate()) && remaining > 0; date = date.plusDays(1)) {
                final int isoDay = date.getDayOfWeek().getValue();
                for (Availability availability : weekly) {
                    if (availability.getDayOfWeek() != isoDay || remaining <= 0) continue;
                    LocalTime start = availability.getStartTime();
                    LocalTime end = availability.getEndTime();
                    while (start.isBefore(end) && remaining > 0) {
                        if (date.equals(today) && !start.isAfter(LocalTime.now())) {
                            start = start.plusMinutes(BREAK_MINUTES);
                            continue;
                        }
                        int duration = (int) Math.min(SESSION_MINUTES, remaining);
                        LocalTime finish = start.plusMinutes(duration);
                        if (finish.isAfter(end)) break;
                        LocalDateTime startAt = LocalDateTime.of(date, start);
                        LocalDateTime endAt = LocalDateTime.of(date, finish);
                        if (hasConflict(startAt, endAt, allBlocks)) {
                            start = start.plusMinutes(BREAK_MINUTES);
                            continue;
                        }
                        StudyBlock block = blocks.save(new StudyBlock(event, startAt, endAt, false));
                        allBlocks.add(block);
                        remaining -= duration;
                        start = start.plusMinutes(duration == SESSION_MINUTES ? SESSION_MINUTES + BREAK_MINUTES : duration);
                    }
                }
            }
            if (remaining > 0) warnings.add(event.getTitle() + ": " + formatHours(remaining) + "h não puderam ser distribuídas antes do prazo.");
        }
        return new ScheduleResponse(getStudyBlocks(), warnings);
    }

    public BlockResponse setCompletion(Long id, boolean completed) {
        StudyBlock block = blocks.findById(id).orElseThrow(() -> new ResourceNotFoundException("Bloco de estudo não encontrado."));
        block.setCompleted(completed);
        return blockResponse(blocks.save(block));
    }

    public List<BlockResponse> getTodayTasks() {
        LocalDateTime start = LocalDate.now().atStartOfDay();
        return blocks.findByStartAtBetween(start, start.plusDays(1).minusNanos(1)).stream()
                .sorted(Comparator.comparing(StudyBlock::getStartAt)).map(this::blockResponse).toList();
    }

    private void validateEvent(EventRequest request) {
        if (!EVENT_TYPES.contains(normalizeType(request.type()))) throw new IllegalArgumentException("Tipo de evento inválido.");
        if (request.dueDate().isBefore(LocalDate.now())) throw new IllegalArgumentException("O prazo não pode estar no passado.");
        if (request.estimatedHours() > 100) throw new IllegalArgumentException("A estimativa máxima é 100 horas.");
    }

    private void validateAvailability(AvailabilityRequest request) {
        if (request.daysOfWeek().stream().anyMatch(day -> day < 1 || day > 7)) throw new IllegalArgumentException("Os dias devem usar ISO: 1 (segunda) até 7 (domingo).");
        if (!request.startTime().isBefore(request.endTime())) throw new IllegalArgumentException("O horário inicial deve ser anterior ao final.");
        if (request.daysOfWeek().stream().distinct().count() != request.daysOfWeek().size()) throw new ConflictException("Um dia da semana foi selecionado mais de uma vez.");
    }

    private boolean hasConflict(LocalDateTime start, LocalDateTime end, List<StudyBlock> existing) {
        return existing.stream().anyMatch(block -> start.isBefore(block.getEndAt()) && end.isAfter(block.getStartAt()));
    }

    private void deleteEventData(StudyEvent event) { blocks.deleteAll(blocks.findByEventId(event.getId())); }
    private Subject subject(Long id) { return subjects.findById(id).orElseThrow(() -> new ResourceNotFoundException("Matéria não encontrada.")); }
    private StudyEvent event(Long id) { return events.findById(id).orElseThrow(() -> new ResourceNotFoundException("Evento não encontrado.")); }
    private String normalizeType(String type) { return type == null ? "" : type.trim().toUpperCase(Locale.ROOT); }
    private String formatHours(long minutes) { return String.format(Locale.ROOT, "%.1f", minutes / 60.0); }
    private SubjectResponse subjectResponse(Subject value) { return new SubjectResponse(value.getId(), value.getName(), value.getColor()); }
    private EventResponse eventResponse(StudyEvent value) { return new EventResponse(value.getId(), value.getTitle(), value.getType(), value.getSubject().getId(), value.getDueDate(), value.getEstimatedHours()); }
    private BlockResponse blockResponse(StudyBlock value) { return new BlockResponse(value.getId(), value.getEvent().getId(), value.getEvent().getTitle(), value.getStartAt(), value.getEndAt(), Boolean.TRUE.equals(value.getCompleted())); }
}
