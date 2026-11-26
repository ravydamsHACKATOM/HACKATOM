package com.example.studyFlow.dto;

import java.util.List;

public record ScheduleResponse(List<BlockResponse> blocks, List<String> warnings) {}
