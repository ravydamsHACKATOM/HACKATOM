import assert from "node:assert/strict";
import { test } from "node:test";

import { addDays, daysBetween, deadline, duration, plural, time, weekRange } from "../js/format.js";

test("duration reads like a student would say it", () => {
  assert.equal(duration(25), "25 min");
  assert.equal(duration(60), "1h");
  assert.equal(duration(90), "1h30");
  assert.equal(duration(125), "2h05");
});

test("time drops the seconds", () => {
  assert.equal(time("18:00:00"), "18:00");
});

test("addDays crosses month and year boundaries", () => {
  assert.equal(addDays("2026-11-27", 5), "2026-12-02");
  assert.equal(addDays("2026-12-31", 1), "2027-01-01");
  assert.equal(addDays("2026-11-27", -7), "2026-11-20");
});

test("daysBetween counts calendar days", () => {
  assert.equal(daysBetween("2026-11-27", "2026-11-30"), 3);
  assert.equal(daysBetween("2026-11-27", "2026-11-27"), 0);
});

test("deadline describes the distance to a due date", () => {
  assert.equal(deadline("2026-11-27", "2026-11-27"), "hoje");
  assert.equal(deadline("2026-11-27", "2026-11-28"), "amanhã");
  assert.equal(deadline("2026-11-27", "2026-12-01"), "em 4 dias");
  assert.equal(deadline("2026-11-27", "2026-11-25"), "há 2 dias");
});

test("weekRange names the month once when the week stays inside it", () => {
  assert.equal(weekRange("2026-11-23", "2026-11-29"), "23 a 29 de nov");
  assert.equal(weekRange("2026-11-30", "2026-12-06"), "30 de nov a 6 de dez");
});

test("plural picks the right word", () => {
  assert.equal(plural(1, "bloco", "blocos"), "1 bloco");
  assert.equal(plural(3, "bloco", "blocos"), "3 blocos");
});
