package com.studyflow.auth;

/** Wrong email or password (HTTP 401). */
public class InvalidCredentialsException extends RuntimeException {

  public InvalidCredentialsException() {
    super("Email or password is incorrect");
  }
}
