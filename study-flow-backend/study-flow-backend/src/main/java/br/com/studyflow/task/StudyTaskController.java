package br.com.studyflow.task;

import br.com.studyflow.subject.Subject;
import br.com.studyflow.subject.SubjectRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/tasks")
public class StudyTaskController {
    private final StudyTaskRepository taskRepository;
    private final SubjectRepository subjectRepository;

    public StudyTaskController(StudyTaskRepository taskRepository, SubjectRepository subjectRepository) {
        this.taskRepository = taskRepository;
        this.subjectRepository = subjectRepository;
    }

    @GetMapping
    public List<StudyTask> findAll(@RequestParam(required = false) Boolean completed) {
        return completed == null
                ? taskRepository.findAll()
                : taskRepository.findByCompleted(completed);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public StudyTask create(@Valid @RequestBody TaskRequest request) {
        StudyTask task = new StudyTask();
        applyRequest(task, request);
        task.setCompleted(false);
        return taskRepository.save(task);
    }

    @PutMapping("/{id}")
    public StudyTask update(@PathVariable Long id, @Valid @RequestBody TaskRequest request) {
        StudyTask task = taskRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Tarefa não encontrada: " + id));
        applyRequest(task, request);
        return taskRepository.save(task);
    }

    @PatchMapping("/{id}/complete")
    public StudyTask markCompleted(@PathVariable Long id, @RequestParam(defaultValue = "true") boolean value) {
        StudyTask task = taskRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Tarefa não encontrada: " + id));
        task.setCompleted(value);
        return taskRepository.save(task);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        StudyTask task = taskRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Tarefa não encontrada: " + id));
        taskRepository.delete(task);
    }

    private void applyRequest(StudyTask task, TaskRequest request) {
        task.setTitle(request.title());
        task.setDescription(request.description());
        task.setDueAt(request.dueAt());
        if (request.subjectId() == null) {
            task.setSubject(null);
        } else {
            Subject subject = subjectRepository.findById(request.subjectId())
                    .orElseThrow(() -> new IllegalArgumentException("Matéria não encontrada: " + request.subjectId()));
            task.setSubject(subject);
        }
    }

    public record TaskRequest(
            @jakarta.validation.constraints.NotBlank String title,
            String description,
            java.time.LocalDateTime dueAt,
            Long subjectId
    ) {}
}
