package com.studyflow.controller;

import com.studyflow.dto.AvailabilityRequest;
import com.studyflow.model.Availability;
import com.studyflow.service.StudyFlowService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/availabilities")
public class AvailabilityController {
    private final StudyFlowService service;
    public AvailabilityController(StudyFlowService service) { this.service = service; }

    @GetMapping public List<Availability> all() { return service.availabilities(); }
    @PostMapping public Availability create(@Valid @RequestBody AvailabilityRequest request) { return service.createAvailability(request); }
    @PutMapping("/{id}") public Availability update(@PathVariable long id, @Valid @RequestBody AvailabilityRequest request) { return service.updateAvailability(id, request); }
    @DeleteMapping("/{id}") public void delete(@PathVariable long id) { service.deleteAvailability(id); }
}
