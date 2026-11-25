package br.com.studyflow.task;

import br.com.studyflow.subject.Subject;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;

@Entity
@Table(name = "study_tasks")
public class StudyTask {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false)
    private String title;

    private String description;
    private LocalDateTime dueAt;

    @Column(nullable = false)
    private boolean completed = false;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "subject_id")
    private Subject subject;

    public StudyTask() {}

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public LocalDateTime getDueAt() { return dueAt; }
    public boolean isCompleted() { return completed; }
    public Subject getSubject() { return subject; }

    public void setId(Long id) { this.id = id; }
    public void setTitle(String title) { this.title = title; }
    public void setDescription(String description) { this.description = description; }
    public void setDueAt(LocalDateTime dueAt) { this.dueAt = dueAt; }
    public void setCompleted(boolean completed) { this.completed = completed; }
    public void setSubject(Subject subject) { this.subject = subject; }
}
