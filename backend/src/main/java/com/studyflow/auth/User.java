package com.studyflow.auth;

/** Public view of an account. The password hash never leaves the repository. */
public record User(long id, String name, String email) {}
