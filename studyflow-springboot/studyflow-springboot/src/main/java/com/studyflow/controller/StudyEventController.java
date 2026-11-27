package com.studyflow.controller;

import com.studyflow.dto.StudyEventRequest;
import com.studyflow.model.StudyEvent;
import com.studyflow.service.StudyFlowService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/events")
public class StudyEventController {
    private final StudyFlowService service;
    public StudyEventController(StudyFlowService service) { this.service = service; }

    @GetMapping public List<StudyEvent> all() { return service.events(); }
    @GetMapping("/{id}") public StudyEvent one(@PathVariable long id) { return service.event(id); }
    @PostMapping public StudyEvent create(@Valid @RequestBody StudyEventRequest request) { return service.createEvent(request); }
    @PutMapping("/{id}") public StudyEvent update(@PathVariable long id, @Valid @RequestBody StudyEventRequest request) { return service.updateEvent(id, request); }
    @DeleteMapping("/{id}") public void delete(@PathVariable long id) { service.deleteEvent(id); }
}
