package com.studyflow.controller;

import com.studyflow.dto.DisciplineRequest;
import com.studyflow.model.Discipline;
import com.studyflow.service.StudyFlowService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/api/disciplines")
public class DisciplineController {
    private final StudyFlowService service;
    public DisciplineController(StudyFlowService service) { this.service = service; }

    @GetMapping public List<Discipline> all() { return service.disciplines(); }
    @GetMapping("/{id}") public Discipline one(@PathVariable long id) { return service.discipline(id); }
    @PostMapping public Discipline create(@Valid @RequestBody DisciplineRequest request) { return service.createDiscipline(request); }
    @PutMapping("/{id}") public Discipline update(@PathVariable long id, @Valid @RequestBody DisciplineRequest request) { return service.updateDiscipline(id, request); }
    @DeleteMapping("/{id}") public void delete(@PathVariable long id) { service.deleteDiscipline(id); }
}
