package br.com.studyflow.subject;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/subjects")
public class SubjectController {
    private final SubjectRepository repository;

    public SubjectController(SubjectRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<Subject> findAll() {
        return repository.findAll();
    }

    @GetMapping("/{id}")
    public Subject findById(@PathVariable Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Matéria não encontrada: " + id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Subject create(@Valid @RequestBody Subject subject) {
        subject.setId(null);
        return repository.save(subject);
    }

    @PutMapping("/{id}")
    public Subject update(@PathVariable Long id, @Valid @RequestBody Subject data) {
        Subject subject = findById(id);
        subject.setName(data.getName());
        subject.setDescription(data.getDescription());
        subject.setColor(data.getColor());
        return repository.save(subject);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        repository.delete(findById(id));
    }
}
