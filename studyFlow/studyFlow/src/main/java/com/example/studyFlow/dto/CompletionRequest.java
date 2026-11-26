package com.example.studyFlow.dto;

import jakarta.validation.constraints.NotNull;

public record CompletionRequest(@NotNull Boolean completed) {}
