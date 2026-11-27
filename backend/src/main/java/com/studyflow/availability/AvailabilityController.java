package com.studyflow.availability;

import com.studyflow.common.BusinessRuleException;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/availability")
public class AvailabilityController {

  private final AvailabilityRepository windows;
  private final DataRevision revision;

  public AvailabilityController(AvailabilityRepository windows, DataRevision revision) {
    this.windows = windows;
    this.revision = revision;
  }

  @GetMapping
  List<AvailabilityWindow> list() {
    return windows.findAll();
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  AvailabilityWindow create(@Valid @RequestBody AvailabilityRequest request) {
    rejectOverlap(request, null);
    AvailabilityWindow created = windows.insert(request);
    revision.bump();
    return created;
  }

  @PutMapping("/{id}")
  AvailabilityWindow update(
      @PathVariable long id, @Valid @RequestBody AvailabilityRequest request) {
    rejectOverlap(request, id);
    if (!windows.update(id, request)) {
      throw new NotFoundException("Availability window", id);
    }
    revision.bump();
    return windows.findById(id).orElseThrow();
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  void delete(@PathVariable long id) {
    if (!windows.delete(id)) {
      throw new NotFoundException("Availability window", id);
    }
    revision.bump();
  }

  private void rejectOverlap(AvailabilityRequest request, Long ignoreId) {
    if (windows.overlaps(request, ignoreId)) {
      throw new BusinessRuleException(
          "AVAILABILITY_OVERLAP", "This window overlaps another one on the same day");
    }
  }
}
