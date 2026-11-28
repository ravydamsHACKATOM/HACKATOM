/**
 * Pure rules behind the "today" and "week" screens: which deadlines and sessions to show, how to
 * split the week's time and when a session reminder is due. No DOM here, so all of it is tested.
 */

/** "18:05:00" or "18:05" as minutes since midnight. */
export function minutesOfDay(value) {
  const [hour, minute] = value.split(":").map(Number);
  return hour * 60 + minute;
}

/** Every exam and assignment due on `date`, exams first, then by title. Never truncated. */
export function dueOn(events, date) {
  return events
    .filter((event) => event.dueDate === date)
    .sort(
      (a, b) =>
        (a.type === "EXAM" ? 0 : 1) - (b.type === "EXAM" ? 0 : 1) ||
        a.title.localeCompare(b.title, "pt-BR"),
    );
}

/**
 * The next deadlines for a short list: at least `min` of them, but never cutting a day in half.
 * If the fourth one shares its date with a fifth, both show.
 */
export function nextDeadlines(events, today, min = 4) {
  const upcoming = events
    .filter((event) => event.dueDate >= today)
    .sort((a, b) => a.dueDate.localeCompare(b.dueDate));
  if (upcoming.length <= min) return upcoming;
  const lastDate = upcoming[min - 1].dueDate;
  return upcoming.filter((event) => event.dueDate <= lastDate);
}

/**
 * Splits the sessions of a day. Pending ones are still to do; done ones leave the pending list but
 * stay listed as history, together with the postponed ones.
 */
export function splitDay(blocks) {
  const byTime = [...blocks].sort((a, b) => a.startTime.localeCompare(b.startTime));
  return {
    pending: byTime.filter((block) => block.status === "PLANNED" || block.status === "MISSED"),
    done: byTime.filter((block) => block.status === "DONE"),
    skipped: byTime.filter((block) => block.status === "SKIPPED"),
  };
}

/** Last day on which a study session for this event still makes sense. */
export function lastStudyDay(event) {
  if (event.type !== "EXAM") return event.dueDate;
  const date = new Date(`${event.dueDate}T12:00:00`);
  date.setDate(date.getDate() - 1);
  return [
    date.getFullYear(),
    String(date.getMonth() + 1).padStart(2, "0"),
    String(date.getDate()).padStart(2, "0"),
  ].join("-");
}

/** Exams and assignments that can still get study time in the shown week. */
export function splitCandidates(events, plan) {
  const firstDay = plan.today > plan.weekStart ? plan.today : plan.weekStart;
  return events
    .filter((event) => lastStudyDay(event) >= firstDay)
    .filter((event) => event.effortMinutes - event.doneMinutes > 0)
    .sort((a, b) => a.dueDate.localeCompare(b.dueDate));
}

/**
 * Minutes each event has in the shown week right now: the student's saved choice when there is
 * one, otherwise what the plan already holds (planned and done).
 */
export function currentShares(candidates, plan) {
  const saved = new Map(plan.settings.allocations.map((item) => [item.eventId, item.minutes]));
  return candidates.map((event) => {
    const inWeek = plan.blocks
      .filter((block) => block.eventId === event.id)
      .filter((block) => block.status === "PLANNED" || block.status === "DONE")
      .reduce((sum, block) => sum + block.minutes, 0);
    return { eventId: event.id, minutes: saved.get(event.id) ?? inWeek };
  });
}

/** Largest share that still makes sense for an event: the effort not done yet. */
export function maxShare(event, step = 25) {
  return Math.ceil(Math.max(0, event.effortMinutes - event.doneMinutes) / step) * step;
}

/**
 * Shares `total` minutes out evenly, in steps, never giving an event more than it still needs.
 * What one event cannot use goes to the others.
 */
export function evenSplit(total, caps, step = 25) {
  const shares = caps.map(() => 0);
  let left = Math.floor(total / step) * step;
  let open = caps.map((cap, index) => (cap > 0 ? index : -1)).filter((index) => index >= 0);
  while (left >= step && open.length) {
    for (const index of open) {
      if (left < step) break;
      shares[index] += step;
      left -= step;
    }
    open = open.filter((index) => shares[index] < caps[index]);
  }
  return shares.map((share, index) => Math.min(share, caps[index]));
}

/** Shares `total` minutes in proportion to what each event still needs. */
export function proportionalSplit(total, caps, step = 25) {
  const need = caps.reduce((sum, cap) => sum + cap, 0);
  if (need === 0) return caps.map(() => 0);
  if (need <= total) return [...caps];
  return caps.map((cap) => Math.min(cap, Math.floor((total * cap) / need / step) * step));
}

/**
 * The session whose reminder should show now: today, started, not finished, still planned and
 * not reminded yet. `now` is a Date in the student's own time.
 */
export function dueReminder(blocks, now, seen) {
  const date = [
    now.getFullYear(),
    String(now.getMonth() + 1).padStart(2, "0"),
    String(now.getDate()).padStart(2, "0"),
  ].join("-");
  const minute = now.getHours() * 60 + now.getMinutes();
  return (
    blocks
      .filter((block) => block.date === date && block.status === "PLANNED")
      .filter((block) => !seen.has(reminderKey(block)))
      .filter(
        (block) => minutesOfDay(block.startTime) <= minute && minute < minutesOfDay(block.endTime),
      )
      .sort((a, b) => a.startTime.localeCompare(b.startTime))[0] ?? null
  );
}

/** Milliseconds until the next planned session of today starts, or null when none is left. */
export function msUntilNextStart(blocks, now) {
  const date = [
    now.getFullYear(),
    String(now.getMonth() + 1).padStart(2, "0"),
    String(now.getDate()).padStart(2, "0"),
  ].join("-");
  const nowMs = now.getHours() * 3_600_000 + now.getMinutes() * 60_000 + now.getSeconds() * 1000;
  const starts = blocks
    .filter((block) => block.date === date && block.status === "PLANNED")
    .map((block) => minutesOfDay(block.startTime) * 60_000 - nowMs)
    .filter((ms) => ms > 0);
  return starts.length ? Math.min(...starts) : null;
}

/** A moved session gets a new key, so it can remind again at its new time. */
export function reminderKey(block) {
  return `${block.id}@${block.date}T${block.startTime}`;
}
