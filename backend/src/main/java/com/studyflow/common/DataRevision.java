package com.studyflow.common;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/**
 * Per-user counter bumped whenever exams, assignments or availability change. A generated plan
 * stores the revision it was built from, which lets the UI tell the student the plan is stale.
 */
@Component
public class DataRevision {

  private final JdbcClient jdbc;

  public DataRevision(JdbcClient jdbc) {
    this.jdbc = jdbc;
  }

  public long current(long userId) {
    return jdbc.sql("SELECT revision FROM data_revision WHERE user_id = :userId")
        .param("userId", userId)
        .query(Long.class)
        .optional()
        .orElse(0L);
  }

  public void bump(long userId) {
    jdbc.sql(
            "INSERT INTO data_revision (user_id, revision) VALUES (:userId, 1)"
                + " ON CONFLICT (user_id) DO UPDATE SET revision = revision + 1")
        .param("userId", userId)
        .update();
  }
}
