package com.studyflow.discipline;

import com.studyflow.auth.AuthInterceptor;
import com.studyflow.common.DataRevision;
import com.studyflow.common.NotFoundException;
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
@RequestMapping("/api/disciplines")
public class DisciplineController {

  private final DisciplineRepository disciplines;
  private final DataRevision revision;

  public DisciplineController(DisciplineRepository disciplines, DataRevision revision) {
    this.disciplines = disciplines;
    this.revision = revision;
  }

  @GetMapping
  List<Discipline> list(@RequestAttribute(AuthInterceptor.USER_ID) long userId) {
    return disciplines.findAll(userId);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  Discipline create(
      @RequestAttribute(AuthInterceptor.USER_ID) long userId,
      @Valid @RequestBody DisciplineRequest request) {
    return disciplines.insert(userId, request);
  }

  @PutMapping("/{id}")
  Discipline update(
      @RequestAttribute(AuthInterceptor.USER_ID) long userId,
      @PathVariable long id,
      @Valid @RequestBody DisciplineRequest request) {
    if (!disciplines.update(userId, id, request)) {
      throw new NotFoundException("Discipline", id);
    }
    // The weight feeds the planner, so an edit can make the current plan stale.
    revision.bump(userId);
    return disciplines.findById(userId, id).orElseThrow();
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  void delete(@RequestAttribute(AuthInterceptor.USER_ID) long userId, @PathVariable long id) {
    if (!disciplines.delete(userId, id)) {
      throw new NotFoundException("Discipline", id);
    }
    // Deleting a discipline cascades to its events and blocks.
    revision.bump(userId);
  }
}
