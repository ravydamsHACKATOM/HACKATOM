
package com.example.studyFlow.entity;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "study_events")
public class StudyEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(nullable = false, length = 30)
    private String type;

    @ManyToOne
    @JoinColumn(name = "subject_id", nullable = false)
    private Subject subject;

    @Column(nullable = false)
    private LocalDate dueDate;

    @Column(nullable = false)
    private Double estimatedHours;

    public StudyEvent() {
    }

    public StudyEvent(
            String title,
            String type,
            Subject subject,
            LocalDate dueDate,
            Double estimatedHours
    ) {
        this.title = title;
        this.type = type;
        this.subject = subject;
        this.dueDate = dueDate;
        this.estimatedHours = estimatedHours;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Subject getSubject() {
        return subject;
    }

    public void setSubject(Subject subject) {
        this.subject = subject;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public Double getEstimatedHours() {
        return estimatedHours;
    }

    public void setEstimatedHours(Double estimatedHours) {
        this.estimatedHours = estimatedHours;
    }
}
