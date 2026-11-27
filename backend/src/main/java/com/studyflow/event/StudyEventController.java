package com.studyflow.event;

import com.studyflow.common.BusinessRuleException;
import com.studyflow.common.DataRevision;
import com.studyflow.common.NotFoundException;
import com.studyflow.discipline.DisciplineRepository;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/events")
public class StudyEventController {

  private final StudyEventRepository events;
  private final DisciplineRepository disciplines;
  private final DataRevision revision;

  public StudyEventController(
      StudyEventRepository events, DisciplineRepository disciplines, DataRevision revision) {
    this.events = events;
    this.disciplines = disciplines;
    this.revision = revision;
  }

  @GetMapping
  List<StudyEvent> list() {
    return events.findAll();
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  StudyEvent create(@Valid @RequestBody StudyEventRequest request) {
    requireDiscipline(request.disciplineId());
    StudyEvent created = events.insert(request);
    revision.bump();
    return created;
  }

  @PutMapping("/{id}")
  StudyEvent update(@PathVariable long id, @Valid @RequestBody StudyEventRequest request) {
    requireDiscipline(request.disciplineId());
    if (!events.update(id, request)) {
      throw new NotFoundException("Event", id);
    }
    revision.bump();
    return events.findById(id).orElseThrow();
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  void delete(@PathVariable long id) {
    if (!events.delete(id)) {
      throw new NotFoundException("Event", id);
    }
    revision.bump();
  }

  private void requireDiscipline(long disciplineId) {
    if (disciplines.findById(disciplineId).isEmpty()) {
      throw new BusinessRuleException(
          "DISCIPLINE_NOT_FOUND", "Discipline " + disciplineId + " does not exist");
    }
  }
}
