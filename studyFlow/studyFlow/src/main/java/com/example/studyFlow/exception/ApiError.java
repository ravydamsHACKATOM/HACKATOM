package com.example.studyFlow.exception;

import java.time.Instant;
import java.util.Map;

public record ApiError(int status, String message, String code, Map<String, String> fields, Instant timestamp) {}
