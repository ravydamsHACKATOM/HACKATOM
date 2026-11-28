/** In-memory copy of what the server knows. Views read from here and never fetch on their own. */

import { api } from "./api.js";

export const store = {
  /** The signed-in student, or null. The session itself lives in an HttpOnly cookie. */
  user: null,
  disciplines: [],
  events: [],
  windows: [],
  plan: null,
  /** Any day of the week being shown; null means the current week. */
  weekDate: null,
};

export async function loadAll() {
  const [disciplines, events, windows, plan] = await Promise.all([
    api.disciplines.list(),
    api.events.list(),
    api.availability.list(),
    api.plan.get(store.weekDate),
  ]);
  Object.assign(store, { disciplines, events, windows, plan });
}

/** Replaces the plan with a fresh server answer and refreshes event progress alongside it. */
export async function acceptPlan(plan) {
  store.plan = plan;
  store.events = await api.events.list();
}

export function clear() {
  Object.assign(store, {
    user: null,
    disciplines: [],
    events: [],
    windows: [],
    plan: null,
    weekDate: null,
  });
}

export function isEmpty() {
  return !store.disciplines.length && !store.events.length && !store.windows.length;
}

/** First name, for greetings. */
export function firstName() {
  return store.user?.name.trim().split(/\s+/)[0] ?? "";
}

const THEME_KEY = "studyflow.theme";

export const theme = {
  get current() {
    const saved = localStorage.getItem(THEME_KEY);
    if (saved) return saved;
    return window.matchMedia("(prefers-color-scheme: dark)").matches ? "dark" : "light";
  },
  apply() {
    document.documentElement.dataset.theme = this.current;
    document
      .querySelector('meta[name="theme-color"]')
      ?.setAttribute("content", this.current === "dark" ? "#1F1E1C" : "#F6F1E7");
  },
  toggle() {
    localStorage.setItem(THEME_KEY, this.current === "dark" ? "light" : "dark");
    this.apply();
  },
};
