import assert from "node:assert/strict";
import { test } from "node:test";

import {
  currentShares,
  dueOn,
  dueReminder,
  evenSplit,
  lastStudyDay,
  maxShare,
  msUntilNextStart,
  nextDeadlines,
  proportionalSplit,
  reminderKey,
  splitCandidates,
  splitDay,
} from "../js/day.js";

const event = (id, dueDate, type = "EXAM", title = `Prazo ${id}`, extra = {}) => ({
  id,
  dueDate,
  type,
  title,
  effortMinutes: 120,
  doneMinutes: 0,
  ...extra,
});

const block = (id, startTime, endTime, status = "PLANNED", extra = {}) => ({
  id,
  eventId: 1,
  date: "2026-11-27",
  startTime,
  endTime,
  status,
  minutes: 50,
  ...extra,
});

test("dueOn lists every deadline of the day, however many there are", () => {
  const events = [
    event(1, "2026-11-27", "ASSIGNMENT", "Relatório"),
    event(2, "2026-11-27", "EXAM", "Prova B"),
    event(3, "2026-11-28"),
    event(4, "2026-11-27", "EXAM", "Prova A"),
    event(5, "2026-11-27", "ASSIGNMENT", "Lista"),
  ];

  assert.deepEqual(
    dueOn(events, "2026-11-27").map((item) => item.title),
    ["Prova A", "Prova B", "Lista", "Relatório"],
  );
});

test("nextDeadlines never cuts a day in half", () => {
  const events = [
    event(1, "2026-11-28"),
    event(2, "2026-11-29"),
    event(3, "2026-11-30"),
    event(4, "2026-12-01"),
    event(5, "2026-12-01"),
    event(6, "2026-12-01"),
    event(7, "2026-12-05"),
    event(8, "2026-11-20"),
  ];

  assert.deepEqual(
    nextDeadlines(events, "2026-11-27").map((item) => item.id),
    [1, 2, 3, 4, 5, 6],
  );
});

test("splitDay takes done sessions out of the pending list and keeps them as history", () => {
  const day = splitDay([
    block(3, "20:00", "20:50"),
    block(1, "18:00", "18:50", "DONE"),
    block(2, "19:00", "19:50", "MISSED"),
    block(4, "21:00", "21:50", "SKIPPED"),
  ]);

  assert.deepEqual(
    day.pending.map((item) => item.id),
    [2, 3],
  );
  assert.deepEqual(
    day.done.map((item) => item.id),
    [1],
  );
  assert.deepEqual(
    day.skipped.map((item) => item.id),
    [4],
  );
});

test("exams are studied until the day before, assignments until the due date", () => {
  assert.equal(lastStudyDay(event(1, "2026-12-01", "EXAM")), "2026-11-30");
  assert.equal(lastStudyDay(event(1, "2026-12-01", "ASSIGNMENT")), "2026-12-01");
});

test("splitCandidates keeps what can still get study time this week", () => {
  const plan = { today: "2026-11-25", weekStart: "2026-11-23" };
  const events = [
    event(1, "2026-11-25", "EXAM"),
    event(2, "2026-11-25", "ASSIGNMENT"),
    event(3, "2026-12-03", "EXAM", "Prova", { doneMinutes: 120 }),
    event(4, "2026-12-10"),
  ];

  assert.deepEqual(
    splitCandidates(events, plan).map((item) => item.id),
    [2, 4],
  );
});

test("currentShares prefers the saved choice and falls back to the week's sessions", () => {
  const plan = {
    settings: { allocations: [{ eventId: 2, minutes: 90 }] },
    blocks: [
      block(1, "18:00", "18:50", "PLANNED", { eventId: 1 }),
      block(2, "19:00", "19:50", "DONE", { eventId: 1 }),
      block(3, "20:00", "20:50", "SKIPPED", { eventId: 1 }),
    ],
  };

  assert.deepEqual(currentShares([event(1, "2026-11-27"), event(2, "2026-11-27")], plan), [
    { eventId: 1, minutes: 100 },
    { eventId: 2, minutes: 90 },
  ]);
});

test("evenSplit shares the time and hands over what one event cannot use", () => {
  assert.deepEqual(evenSplit(240, [300, 300], 30), [120, 120]);
  assert.deepEqual(evenSplit(240, [60, 300], 30), [60, 180]);
  assert.deepEqual(evenSplit(100, [0, 300], 30), [0, 90]);
});

test("proportionalSplit follows what each event still needs", () => {
  assert.deepEqual(proportionalSplit(300, [100, 50], 30), [100, 50]);
  assert.deepEqual(proportionalSplit(180, [240, 120], 30), [120, 60]);
});

test("maxShare is the effort still left, in whole steps", () => {
  assert.equal(maxShare(event(1, "2026-11-27", "EXAM", "x", { doneMinutes: 50 }), 30), 90);
});

test("a reminder shows once the session starts and not before", () => {
  const blocks = [block(1, "18:00", "18:50"), block(2, "19:00", "19:50", "DONE")];
  const seen = new Set();

  assert.equal(dueReminder(blocks, new Date(2026, 10, 27, 17, 59), seen), null);
  assert.equal(dueReminder(blocks, new Date(2026, 10, 27, 18, 0), seen).id, 1);
  assert.equal(dueReminder(blocks, new Date(2026, 10, 27, 18, 49), seen).id, 1);
  assert.equal(dueReminder(blocks, new Date(2026, 10, 27, 18, 50), seen), null);
  assert.equal(dueReminder(blocks, new Date(2026, 10, 27, 19, 10), seen), null);
  assert.equal(dueReminder(blocks, new Date(2026, 10, 28, 18, 10), seen), null);
});

test("a session is reminded only once, until it moves to a new time", () => {
  const session = block(1, "18:00", "18:50");
  const seen = new Set([reminderKey(session)]);

  assert.equal(dueReminder([session], new Date(2026, 10, 27, 18, 5), seen), null);
  const moved = { ...session, startTime: "18:05:00", endTime: "18:55:00" };
  assert.equal(dueReminder([moved], new Date(2026, 10, 27, 18, 5), seen).id, 1);
});

test("msUntilNextStart aims at the next planned start of today", () => {
  const blocks = [block(1, "18:00", "18:50"), block(2, "20:00", "20:50")];

  assert.equal(msUntilNextStart(blocks, new Date(2026, 10, 27, 17, 59, 30)), 30_000);
  assert.equal(msUntilNextStart(blocks, new Date(2026, 10, 27, 18, 30)), 90 * 60_000);
  assert.equal(msUntilNextStart(blocks, new Date(2026, 10, 27, 21, 0)), null);
});
