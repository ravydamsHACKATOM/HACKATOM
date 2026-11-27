package com.studyflow.service;

import com.studyflow.model.Availability;
import com.studyflow.model.StudyBlock;
import com.studyflow.model.StudyEvent;
import com.studyflow.repository.StudyFlowRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StudyFlowServiceTest {
    @Mock
    private StudyFlowRepository repository;

    @InjectMocks
    private StudyFlowService service;

    @Test
    void generateScheduleDoesNotCreateBlocksAfterSelectedWeek() {
        LocalDate monday = LocalDate.of(2026, 11, 23);
        LocalDate sunday = monday.plusDays(6);
        List<Availability> availability = java.util.stream.IntStream.rangeClosed(1, 7)
                .mapToObj(day -> new Availability((long) day, day, "09:00", "12:00"))
                .toList();
        StudyEvent event = new StudyEvent(1L, "Final exam", "EXAM",
                monday.plusDays(13).toString(), 30, 1L, "");

        when(repository.findAllAvailabilities()).thenReturn(availability);
        when(repository.findAllEvents()).thenReturn(List.of(event));
        when(repository.hasOverlap(any(LocalDate.class), any(LocalTime.class), any(LocalTime.class)))
                .thenReturn(false);
        when(repository.createBlock(anyLong(), anyLong(), any(LocalDate.class),
                any(LocalTime.class), any(LocalTime.class), anyInt()))
                .thenAnswer(invocation -> {
                    LocalDate date = invocation.getArgument(2);
                    LocalTime start = invocation.getArgument(3);
                    LocalTime end = invocation.getArgument(4);
                    int minutes = invocation.getArgument(5);
                    return new StudyBlock(1L, 1L, 1L, date.toString(), start.toString(),
                            end.toString(), minutes, false);
                });

        List<StudyBlock> blocks = service.generateSchedule(monday);

        assertEquals(21, blocks.size());
        assertTrue(blocks.stream().allMatch(block -> {
            LocalDate date = LocalDate.parse(block.studyDate());
            return !date.isBefore(monday) && !date.isAfter(sunday);
        }));
    }
}
