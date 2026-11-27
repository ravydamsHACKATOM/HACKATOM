package com.studyflow.service;

import com.studyflow.dto.AvailabilityRequest;
import com.studyflow.dto.DisciplineRequest;
import com.studyflow.dto.ScheduleGenerationResponse;
import com.studyflow.dto.ScheduleWarning;
import com.studyflow.dto.StudyEventRequest;
import com.studyflow.model.Availability;
import com.studyflow.model.Discipline;
import com.studyflow.model.StudyBlock;
import com.studyflow.model.StudyEvent;
import com.studyflow.repository.StudyFlowRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class StudyFlowService {
    private static final int SESSION_MINUTES = 50;
    private final StudyFlowRepository repository;

    public StudyFlowService(StudyFlowRepository repository) {
        this.repository = repository;
    }

    public List<Discipline> disciplines() { return repository.findAllDisciplines(); }
    public Discipline discipline(long id) { return repository.findDiscipline(id).orElseThrow(() -> notFound("Discipline not found")); }
    public Discipline createDiscipline(DisciplineRequest r) { return repository.createDiscipline(r); }
    public Discipline updateDiscipline(long id, DisciplineRequest r) { return repository.updateDiscipline(id, r).orElseThrow(() -> notFound("Discipline not found")); }
    public void deleteDiscipline(long id) { if (!repository.deleteDiscipline(id)) throw notFound("Discipline not found"); }

    public List<StudyEvent> events() { return repository.findAllEvents(); }
    public StudyEvent event(long id) { return repository.findEvent(id).orElseThrow(() -> notFound("Event not found")); }
    public StudyEvent createEvent(StudyEventRequest r) {
        discipline(r.disciplineId());
        return repository.createEvent(r);
    }
    public StudyEvent updateEvent(long id, StudyEventRequest r) {
        discipline(r.disciplineId());
        return repository.updateEvent(id, r).orElseThrow(() -> notFound("Event not found"));
    }
    public void deleteEvent(long id) { if (!repository.deleteEvent(id)) throw notFound("Event not found"); }

    public List<Availability> availabilities() { return repository.findAllAvailabilities(); }
    public Availability createAvailability(AvailabilityRequest r) {
        validateAvailability(r);
        return repository.createAvailability(r);
    }
    public Availability updateAvailability(long id, AvailabilityRequest r) {
        validateAvailability(r);
        return repository.updateAvailability(id, r).orElseThrow(() -> notFound("Availability not found"));
    }
    public void deleteAvailability(long id) { if (!repository.deleteAvailability(id)) throw notFound("Availability not found"); }

    public List<StudyBlock> blocks(LocalDate from, LocalDate to) {
        if (to.isBefore(from)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "End date must be on or after start date");
        return repository.findBlocks(from, to);
    }
    public List<StudyBlock> today(LocalDate date) { return repository.findBlocksForDay(date); }
    public StudyBlock completeBlock(long id, boolean completed) {
        return repository.setBlockCompleted(id, completed).orElseThrow(() -> notFound("Study block not found"));
    }

    @Transactional
    public ScheduleGenerationResponse generateSchedule(LocalDate weekStart) {
        LocalDate monday = weekStart.with(DayOfWeek.MONDAY);
        LocalDate sunday = monday.plusDays(6);
        List<Availability> availabilities = repository.findAllAvailabilities();
        if (availabilities.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Register your study availability before generating a schedule");
        }

        repository.deletePendingBlocksBetween(monday, sunday);
        List<StudyEvent> events = repository.findAllEvents().stream()
                .filter(e -> !LocalDate.parse(e.dueDate()).isBefore(monday))
                .sorted(Comparator.comparing(e -> LocalDate.parse(e.dueDate())))
                .toList();

        List<StudyBlock> created = new ArrayList<>();
        List<ScheduleWarning> warnings = new ArrayList<>();
        for (StudyEvent event : events) {
            int remainingMinutes = (int) Math.ceil(event.estimatedHours() * 60);
            LocalDate dueDate = LocalDate.parse(event.dueDate());

            for (LocalDate day = monday;
                 !day.isAfter(sunday) && !day.isAfter(dueDate) && remainingMinutes > 0;
                 day = day.plusDays(1)) {
                int dayOfWeek = day.getDayOfWeek().getValue();
                List<Availability> daySlots = availabilities.stream()
                        .filter(a -> a.dayOfWeek() == dayOfWeek)
                        .sorted(Comparator.comparing(Availability::startTime))
                        .toList();

                for (Availability availability : daySlots) {
                    LocalTime cursor = LocalTime.parse(availability.startTime());
                    LocalTime endOfAvailability = LocalTime.parse(availability.endTime());
                    while (!cursor.plusMinutes(SESSION_MINUTES).isAfter(endOfAvailability)
                            && remainingMinutes > 0) {
                        LocalTime sessionEnd = cursor.plusMinutes(SESSION_MINUTES);
                        if (!repository.hasOverlap(day, cursor, sessionEnd)) {
                            StudyBlock block = repository.createBlock(event.id(), event.disciplineId(),
                                    day, cursor, sessionEnd, SESSION_MINUTES);
                            created.add(block);
                            remainingMinutes -= SESSION_MINUTES;
                        }
                        cursor = cursor.plusMinutes(SESSION_MINUTES + 10);
                    }
                }
            }

            if (remainingMinutes > 0) {
                warnings.add(new ScheduleWarning(event.id(), remainingMinutes));
            }
        }
        return new ScheduleGenerationResponse(created, warnings);
    }

    private void validateAvailability(AvailabilityRequest r) {
        if (!r.endTime().isAfter(r.startTime())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "End time must be after start time");
        }
    }

    private ResponseStatusException notFound(String message) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, message);
    }
}
