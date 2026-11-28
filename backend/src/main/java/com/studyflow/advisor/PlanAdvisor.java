package com.studyflow.advisor;

/** Explains a plan to the student. It only describes facts from the context. */
public interface PlanAdvisor {

  AdvisorNote advise(AdvisorContext context);
}
