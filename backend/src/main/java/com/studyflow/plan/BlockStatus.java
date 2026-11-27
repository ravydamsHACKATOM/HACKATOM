package com.studyflow.plan;

public enum BlockStatus {
  PLANNED,
  DONE,
  /** The student postponed it; the slot stays taken so the replan moves the work elsewhere. */
  SKIPPED,
  /** Its time passed without being marked as done; the effort returns to the pool. */
  MISSED
}
