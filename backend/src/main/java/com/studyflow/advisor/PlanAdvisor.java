package com.studyflow.advisor;

/** Explains a plan to the student. Implementations must not invent facts outside the context. */
public interface PlanAdvisor {

  AdvisorNote advise(AdvisorContext context);
}
