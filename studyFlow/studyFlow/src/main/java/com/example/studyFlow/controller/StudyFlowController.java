
package com.example.studyFlow.controller;

import com.example.studyFlow.service.StudyFlowService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class StudyFlowController {

    private final StudyFlowService studyFlowService;

    public StudyFlowController(StudyFlowService studyFlowService) {
        this.studyFlowService = studyFlowService;
    }

    // =========================
    // MATÉRIAS
    // =========================

    @GetMapping("/subjects")
    public ResponseEntity<List<SubjectDTO>> getSubjects() {
        return ResponseEntity.ok(studyFlowService.getSubjects());
    }

    @PostMapping("/subjects")
    public ResponseEntity<SubjectDTO> createSubject(
            @RequestBody SubjectDTO subject) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(studyFlowService.createSubject(subject));
    }

    @PutMapping("/subjects/{id}")
    public ResponseEntity<SubjectDTO> updateSubject(
            @PathVariable Long id,
            @RequestBody SubjectDTO subject) {

        return ResponseEntity.ok(
                studyFlowService.updateSubject(id, subject));
    }

    @DeleteMapping("/subjects/{id}")
    public ResponseEntity<Void> deleteSubject(@PathVariable Long id) {
        studyFlowService.deleteSubject(id);
        return ResponseEntity.noContent().build();
    }

    // =========================
    // PROVAS E ENTREGAS
    // =========================

    @GetMapping("/events")
    public ResponseEntity<List<EventDTO>> getEvents() {
        return ResponseEntity.ok(studyFlowService.getEvents());
    }

    @PostMapping("/events")
    public ResponseEntity<EventDTO> createEvent(
            @RequestBody EventDTO event) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(studyFlowService.createEvent(event));
    }

    @PutMapping("/events/{id}")
    public ResponseEntity<EventDTO> updateEvent(
            @PathVariable Long id,
            @RequestBody EventDTO event) {

        return ResponseEntity.ok(
                studyFlowService.updateEvent(id, event));
    }

    @DeleteMapping("/events/{id}")
    public ResponseEntity<Void> deleteEvent(@PathVariable Long id) {
        studyFlowService.deleteEvent(id);
        return ResponseEntity.noContent().build();
    }

    // =========================
    // DISPONIBILIDADE
    // =========================

    @GetMapping("/availability")
    public ResponseEntity<List<AvailabilityDTO>> getAvailability() {
        return ResponseEntity.ok(studyFlowService.getAvailability());
    }

    @PostMapping("/availability")
    public ResponseEntity<AvailabilityDTO> createAvailability(
            @RequestBody AvailabilityDTO availability) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(studyFlowService.createAvailability(availability));
    }

    @PutMapping("/availability/{id}")
    public ResponseEntity<AvailabilityDTO> updateAvailability(
            @PathVariable Long id,
            @RequestBody AvailabilityDTO availability) {

        return ResponseEntity.ok(
                studyFlowService.updateAvailability(id, availability));
    }

    @DeleteMapping("/availability/{id}")
    public ResponseEntity<Void> deleteAvailability(@PathVariable Long id) {
        studyFlowService.deleteAvailability(id);
        return ResponseEntity.noContent().build();
    }

    // =========================
    // CRONOGRAMA
    // =========================

    @GetMapping("/blocks")
    public ResponseEntity<List<StudyBlockDTO>> getStudyBlocks() {
        return ResponseEntity.ok(studyFlowService.getStudyBlocks());
    }

    @PostMapping("/schedule/generate")
    public ResponseEntity<List<StudyBlockDTO>> generateSchedule() {
        return ResponseEntity.ok(studyFlowService.generateSchedule());
    }

    @PatchMapping("/blocks/{id}/complete")
    public ResponseEntity<StudyBlockDTO> completeStudyBlock(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                studyFlowService.completeStudyBlock(id));
    }

    @GetMapping("/today")
    public ResponseEntity<List<StudyBlockDTO>> getTodayTasks() {
        return ResponseEntity.ok(studyFlowService.getTodayTasks());
    }

    // =========================
    // DTOs
    // =========================

    public record SubjectDTO(
            Long id,
            String name,
            String color
    ) {}

    public record EventDTO(
            Long id,
            String title,
            String type,
            Long subjectId,
            LocalDate dueDate,
            Double estimatedHours
    ) {}

    public record AvailabilityDTO(
            Long id,
            List<Integer> daysOfWeek,
            LocalTime startTime,
            LocalTime endTime
    ) {}

    public record StudyBlockDTO(
            Long id,
            Long eventId,
            String title,
            LocalDateTime startAt,
            LocalDateTime endAt,
            Boolean completed
    ) {}
}
