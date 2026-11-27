package com.studyflow.dto;

import com.studyflow.model.StudyBlock;

import java.util.List;

public record ScheduleGenerationResponse(List<StudyBlock> blocks, List<ScheduleWarning> warnings) {}
