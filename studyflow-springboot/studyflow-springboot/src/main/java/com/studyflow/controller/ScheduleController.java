package com.studyflow.controller;

import com.studyflow.model.StudyBlock;
import com.studyflow.service.StudyFlowService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/schedule")
public class ScheduleController {
    private final StudyFlowService service;
    public ScheduleController(StudyFlowService service) { this.service = service; }

    @PostMapping("/generate")
    public List<StudyBlock> generate(@RequestParam LocalDate weekStart) {
        return service.generateSchedule(weekStart);
    }
}
