package com.studyflow.availability;

import com.studyflow.auth.AuthInterceptor;
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
import org.springframework.web.bind.annotation.RequestAttribute;
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
  List<AvailabilityWindow> list(@RequestAttribute(AuthInterceptor.USER_ID) long userId) {
    return windows.findAll(userId);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  AvailabilityWindow create(
      @RequestAttribute(AuthInterceptor.USER_ID) long userId,
      @Valid @RequestBody AvailabilityRequest request) {
    rejectOverlap(userId, request, null);
    AvailabilityWindow created = windows.insert(userId, request);
    revision.bump(userId);
    return created;
  }

  @PutMapping("/{id}")
  AvailabilityWindow update(
      @RequestAttribute(AuthInterceptor.USER_ID) long userId,
      @PathVariable long id,
      @Valid @RequestBody AvailabilityRequest request) {
    rejectOverlap(userId, request, id);
    if (!windows.update(userId, id, request)) {
      throw new NotFoundException("Availability window", id);
    }
    revision.bump(userId);
    return windows.findById(userId, id).orElseThrow();
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  void delete(@RequestAttribute(AuthInterceptor.USER_ID) long userId, @PathVariable long id) {
    if (!windows.delete(userId, id)) {
      throw new NotFoundException("Availability window", id);
    }
    revision.bump(userId);
  }

  private void rejectOverlap(long userId, AvailabilityRequest request, Long ignoreId) {
    if (windows.overlaps(userId, request, ignoreId)) {
      throw new BusinessRuleException(
          "AVAILABILITY_OVERLAP", "This window overlaps another one on the same day");
    }
  }
}
