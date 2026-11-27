package com.studyflow.discipline;

/**
 * A subject the student is taking.
 *
 * @param weight priority from 1 (light) to 3 (heavy), used to break ties when planning
 */
public record Discipline(long id, String name, String color, String icon, int weight) {}
