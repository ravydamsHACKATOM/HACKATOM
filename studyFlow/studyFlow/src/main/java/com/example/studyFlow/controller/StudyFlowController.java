package com.example.studyFlow.controller;

import com.example.studyFlow.dto.*;
import com.example.studyFlow.service.StudyFlowService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@RestController
@RequestMapping("/api")
@Validated
public class StudyFlowController {
    private final StudyFlowService service;

    public StudyFlowController(StudyFlowService service) { this.service = service; }

    @GetMapping({"/", "/health"})
    public ResponseEntity<?> health() { return ResponseEntity.ok(java.util.Map.of("status", "ok", "application", "StudyFlow")); }

    @GetMapping("/subjects")
    public List<SubjectResponse> subjects() { return service.getSubjects(); }

    @PostMapping("/subjects")
    public ResponseEntity<SubjectResponse> createSubject(@Valid @RequestBody SubjectRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createSubject(request));
    }

    @PutMapping("/subjects/{id}")
    public SubjectResponse updateSubject(@PathVariable @Positive Long id, @Valid @RequestBody SubjectRequest request) {
        return service.updateSubject(id, request);
    }

    @DeleteMapping("/subjects/{id}")
    public ResponseEntity<Void> deleteSubject(@PathVariable @Positive Long id) { service.deleteSubject(id); return ResponseEntity.noContent().build(); }

    @GetMapping("/events")
    public List<EventResponse> events() { return service.getEvents(); }

    @PostMapping("/events")
    public ResponseEntity<EventResponse> createEvent(@Valid @RequestBody EventRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createEvent(request));
    }

    @PutMapping("/events/{id}")
    public EventResponse updateEvent(@PathVariable @Positive Long id, @Valid @RequestBody EventRequest request) {
        return service.updateEvent(id, request);
    }

    @DeleteMapping("/events/{id}")
    public ResponseEntity<Void> deleteEvent(@PathVariable @Positive Long id) { service.deleteEvent(id); return ResponseEntity.noContent().build(); }

    @GetMapping("/availability")
    public List<AvailabilityResponse> availability() { return service.getAvailability(); }

    @PutMapping("/availability")
    public List<AvailabilityResponse> replaceAvailability(@Valid @RequestBody AvailabilityRequest request) {
        return service.replaceAvailability(request);
    }

    @DeleteMapping("/availability")
    public ResponseEntity<Void> clearAvailability() { service.clearAvailability(); return ResponseEntity.noContent().build(); }

    @GetMapping("/blocks")
    public List<BlockResponse> blocks() { return service.getStudyBlocks(); }

    @PostMapping("/schedule/generate")
    public ScheduleResponse generateSchedule() { return service.generateSchedule(); }

    @PatchMapping("/blocks/{id}/completion")
    public BlockResponse setCompletion(@PathVariable @Positive Long id, @Valid @RequestBody CompletionRequest request) {
        return service.setCompletion(id, request.completed());
    }

    @GetMapping("/today")
    public List<BlockResponse> today() { return service.getTodayTasks(); }
}
