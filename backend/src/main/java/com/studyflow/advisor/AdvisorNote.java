package com.studyflow.advisor;

import java.util.List;

/** Short explanation of a plan shown to the student: a summary and a few practical tips. */
public record AdvisorNote(String summary, List<String> tips) {}
