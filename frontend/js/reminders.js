/**
 * Session reminders: when a planned study session starts, a card appears in the bottom right
 * corner of the page. It stays until the student acts on it or closes it.
 *
 * The timer aims at the exact start of the next session and a slower check covers a laptop that
 * slept. Sessions already reminded are kept in sessionStorage so a reload does not repeat them.
 */

import { dueReminder, minutesOfDay, msUntilNextStart, reminderKey } from "./day.js";
import { html, mount, raw } from "./dom.js";
import { duration, time } from "./format.js";
import { disciplineIcon, icon } from "./icons.js";

const SEEN_KEY = "studyflow.reminded";
const CHECK_EVERY_MS = 20_000;
/** setTimeout cannot wait longer than this; the periodic check covers anything further. */
const LONGEST_WAIT_MS = 30 * 60_000;

let blocks = [];
let exactTimer;
let checkTimer;
let onAction = () => {};

function seen() {
  try {
    return new Set(JSON.parse(sessionStorage.getItem(SEEN_KEY) ?? "[]"));
  } catch {
    return new Set();
  }
}

function remember(block) {
  const keys = seen();
  keys.add(reminderKey(block));
  sessionStorage.setItem(SEEN_KEY, JSON.stringify([...keys].slice(-200)));
}

/**
 * Starts watching. `handler(action, blockId)` runs for "shown", "done" and "open".
 */
export function startReminders(handler) {
  onAction = handler;
  clearInterval(checkTimer);
  checkTimer = setInterval(check, CHECK_EVERY_MS);
  document.addEventListener("visibilitychange", () => {
    if (!document.hidden) check();
  });
}

/** Gives the reminders the latest sessions of the current week. */
export function updateReminders(weekBlocks) {
  blocks = weekBlocks;
  const card = root().firstElementChild;
  // A session that was done, moved or postponed elsewhere no longer needs its card.
  if (
    card &&
    !blocks.some((block) => block.status === "PLANNED" && reminderKey(block) === card.dataset.key)
  ) {
    hide();
  }
  check();
}

export function stopReminders() {
  clearInterval(checkTimer);
  clearTimeout(exactTimer);
  blocks = [];
  hide();
}

function root() {
  return document.getElementById("reminder-root");
}

function check() {
  clearTimeout(exactTimer);
  const now = new Date();
  const block = dueReminder(blocks, now, seen());
  if (block) show(block, now);
  const wait = msUntilNextStart(blocks, now);
  if (wait !== null) {
    exactTimer = setTimeout(check, Math.min(wait + 250, LONGEST_WAIT_MS));
  }
}

function show(block, now) {
  remember(block);
  const minute = now.getHours() * 60 + now.getMinutes();
  const left = minutesOfDay(block.endTime) - minute;
  mount(
    root(),
    html`
      <article
        class="reminder"
        style="--tone:${block.disciplineColor}"
        data-key="${reminderKey(block)}"
        aria-labelledby="reminder-title"
        aria-describedby="reminder-text"
      >
        <span class="reminder__art" aria-hidden="true">
          ${raw(disciplineIcon(block.disciplineIcon, 40))}
        </span>
        <div class="reminder__body">
          <p class="reminder__eyebrow">
            <span class="pulse" aria-hidden="true"></span> Hora de estudar
          </p>
          <h2 class="reminder__title" id="reminder-title">${block.eventTitle}</h2>
          <p class="reminder__text" id="reminder-text">
            ${block.disciplineName} · ${time(block.startTime)} – ${time(block.endTime)} ·
            ${left < block.minutes ? `faltam ${duration(left)}` : duration(block.minutes)}
          </p>
          <div class="reminder__actions">
            <button class="button button--primary button--small" type="button" data-reminder="open">
              ${raw(icon("today", 16))} Começar
            </button>
            <button class="button button--ghost button--small" type="button" data-reminder="done">
              ${raw(icon("check", 16))} Já concluí
            </button>
          </div>
        </div>
        <button
          class="icon-button reminder__close"
          type="button"
          data-reminder="close"
          aria-label="Fechar aviso"
        >
          ${raw(icon("close", 16))}
        </button>
      </article>
    `,
  );
  const card = root().firstElementChild;
  onAction("shown", block.id);
  card.addEventListener("click", (event) => {
    const action = event.target.closest("[data-reminder]")?.dataset.reminder;
    if (!action) return;
    hide();
    if (action !== "close") onAction(action, block.id);
  });
}

function hide() {
  const card = root()?.firstElementChild;
  if (!card) return;
  card.classList.add("reminder--leaving");
  const still = window.matchMedia("(prefers-reduced-motion: reduce)").matches;
  setTimeout(() => card.remove(), still ? 0 : 220);
}
