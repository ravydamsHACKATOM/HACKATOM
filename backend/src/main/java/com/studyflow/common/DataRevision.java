package com.studyflow.common;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/**
 * Monotonic counter bumped whenever exams, assignments or availability change. A generated plan
 * stores the revision it was built from, which lets the UI tell the student the plan is stale.
 */
@Component
public class DataRevision {

  private final JdbcClient jdbc;

  public DataRevision(JdbcClient jdbc) {
    this.jdbc = jdbc;
  }

  public long current() {
    return jdbc.sql("SELECT revision FROM data_revision WHERE id = 1").query(Long.class).single();
  }

  public void bump() {
    jdbc.sql("UPDATE data_revision SET revision = revision + 1 WHERE id = 1").update();
  }
}
