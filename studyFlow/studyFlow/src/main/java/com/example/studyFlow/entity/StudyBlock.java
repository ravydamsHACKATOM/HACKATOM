
package com.example.studyFlow.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.LocalDateTime;

@Entity
@Table(name = "study_blocks", indexes = {
        @Index(name = "idx_blocks_event_id", columnList = "event_id"),
        @Index(name = "idx_blocks_start_at", columnList = "start_at")
})
public class StudyBlock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JdbcTypeCode(SqlTypes.INTEGER)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false)
    private StudyEvent event;

    @Column(nullable = false)
    @Convert(converter = LocalDateTimeConverter.class)
    private LocalDateTime startAt;

    @Column(nullable = false)
    @Convert(converter = LocalDateTimeConverter.class)
    private LocalDateTime endAt;

    @Column(nullable = false)
    private Boolean completed = false;

    public StudyBlock() {
    }

    public StudyBlock(
            StudyEvent event,
            LocalDateTime startAt,
            LocalDateTime endAt,
            Boolean completed
    ) {
        this.event = event;
        this.startAt = startAt;
        this.endAt = endAt;
        this.completed = completed;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public StudyEvent getEvent() {
        return event;
    }

    public void setEvent(StudyEvent event) {
        this.event = event;
    }

    public LocalDateTime getStartAt() {
        return startAt;
    }

    public void setStartAt(LocalDateTime startAt) {
        this.startAt = startAt;
    }

    public LocalDateTime getEndAt() {
        return endAt;
    }

    public void setEndAt(LocalDateTime endAt) {
        this.endAt = endAt;
    }

    public Boolean getCompleted() {
        return completed;
    }

    public void setCompleted(Boolean completed) {
        this.completed = completed;
    }
}
