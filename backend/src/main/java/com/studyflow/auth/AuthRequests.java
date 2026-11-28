package com.studyflow.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Request bodies of the authentication endpoints. */
public final class AuthRequests {

  private AuthRequests() {}

  public record Register(
      @NotBlank @Size(max = 60) String name,
      @NotBlank @Email @Size(max = 120) String email,
      @NotBlank @Size(min = 8, max = 72, message = "must have between 8 and 72 characters")
          String password) {}

  public record Login(@NotBlank String email, @NotBlank String password) {}
}
