package com.studyflow.advisor;

/** Formats minutes the way a student would say them in Portuguese: 50 min, 2h, 1h30. */
final class Durations {

  private Durations() {}

  static String format(int minutes) {
    if (minutes < 60) {
      return minutes + " min";
    }
    int hours = minutes / 60;
    int rest = minutes % 60;
    return rest == 0 ? hours + "h" : "%dh%02d".formatted(hours, rest);
  }
}
