package com.studyflow.common;

import java.util.Map;

/**
 * Error payload returned by every failing endpoint.
 *
 * @param code stable machine-readable code the front-end can translate
 * @param message human-readable explanation
 * @param fields per-field validation messages, when the request body was invalid
 */
public record ApiError(String code, String message, Map<String, String> fields) {

  public static ApiError of(String code, String message) {
    return new ApiError(code, message, null);
  }
}
