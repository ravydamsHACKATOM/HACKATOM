package com.studyflow.event;

import com.studyflow.auth.AuthInterceptor;
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
import org.springframework.web.bind.annotation.RequestAttribute;
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
  List<StudyEvent> list(@RequestAttribute(AuthInterceptor.USER_ID) long userId) {
    return events.findAll(userId);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  StudyEvent create(
      @RequestAttribute(AuthInterceptor.USER_ID) long userId,
      @Valid @RequestBody StudyEventRequest request) {
    requireDiscipline(userId, request.disciplineId());
    StudyEvent created = events.insert(userId, request);
    revision.bump(userId);
    return created;
  }

  @PutMapping("/{id}")
  StudyEvent update(
      @RequestAttribute(AuthInterceptor.USER_ID) long userId,
      @PathVariable long id,
      @Valid @RequestBody StudyEventRequest request) {
    requireDiscipline(userId, request.disciplineId());
    if (!events.update(userId, id, request)) {
      throw new NotFoundException("Event", id);
    }
    revision.bump(userId);
    return events.findById(userId, id).orElseThrow();
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  void delete(@RequestAttribute(AuthInterceptor.USER_ID) long userId, @PathVariable long id) {
    if (!events.delete(userId, id)) {
      throw new NotFoundException("Event", id);
    }
    revision.bump(userId);
  }

  /** Also stops a user from attaching an event to somebody else's discipline. */
  private void requireDiscipline(long userId, long disciplineId) {
    if (disciplines.findById(userId, disciplineId).isEmpty()) {
      throw new BusinessRuleException(
          "DISCIPLINE_NOT_FOUND", "Discipline " + disciplineId + " does not exist");
    }
  }
}
