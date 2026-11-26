package com.example.studyFlow.dto;

import java.time.LocalDateTime;

public record BlockResponse(Long id, Long eventId, String title, LocalDateTime startAt, LocalDateTime endAt, boolean completed) {}
