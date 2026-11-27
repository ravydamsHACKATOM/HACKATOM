package com.studyflow.controller;

import com.studyflow.dto.StudyRecommendation;
import com.studyflow.service.AiRecommendationService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai")
public class AiRecommendationController {
    private final AiRecommendationService service;

    public AiRecommendationController(AiRecommendationService service) {
        this.service = service;
    }

    @PostMapping("/study-recommendation")
    public StudyRecommendation recommend() {
        return service.recommend();
    }
}
