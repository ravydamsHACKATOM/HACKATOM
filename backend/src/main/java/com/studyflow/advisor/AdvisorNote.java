package com.studyflow.advisor;

import java.util.List;

/**
 * Short explanation of a plan shown to the student.
 *
 * @param source who wrote it: {@code AI} or {@code RULES}
 */
public record AdvisorNote(String summary, List<String> tips, String source) {

  public static final String SOURCE_AI = "AI";
  public static final String SOURCE_RULES = "RULES";
}
