package com.studyflow.controller;

import com.studyflow.model.StudyBlock;
import com.studyflow.service.StudyFlowService;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/blocks")
public class StudyBlockController {
    private final StudyFlowService service;
    public StudyBlockController(StudyFlowService service) { this.service = service; }

    @GetMapping
    public List<StudyBlock> between(@RequestParam LocalDate from, @RequestParam LocalDate to) {
        return service.blocks(from, to);
    }

    @GetMapping("/today")
    public List<StudyBlock> today(@RequestParam(required = false) LocalDate date) {
        return service.today(date == null ? LocalDate.now() : date);
    }

    @PatchMapping("/{id}/completion")
    public StudyBlock completion(@PathVariable long id, @RequestBody Map<String, Boolean> body) {
        Boolean completed = body.get("completed");
        if (completed == null) throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.BAD_REQUEST, "The 'completed' field is required");
        return service.completeBlock(id, completed);
    }
}
