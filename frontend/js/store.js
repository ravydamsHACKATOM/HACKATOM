/** In-memory copy of what the server knows. Views read from here and never fetch on their own. */

import { api } from "./api.js";

export const store = {
  disciplines: [],
  events: [],
  windows: [],
  plan: null,
  /** Any day of the week being shown; null means the current week. */
  weekDate: null,
  aiEnabled: false,
};

export async function loadAll() {
  const [disciplines, events, windows, plan, advisor] = await Promise.all([
    api.disciplines.list(),
    api.events.list(),
    api.availability.list(),
    api.plan.get(store.weekDate),
    api.advisor(),
  ]);
  Object.assign(store, { disciplines, events, windows, plan, aiEnabled: advisor.ai });
}

/** Replaces the plan with a fresh server answer and refreshes event progress alongside it. */
export async function acceptPlan(plan) {
  store.plan = plan;
  store.events = await api.events.list();
}

export function isEmpty() {
  return !store.disciplines.length && !store.events.length && !store.windows.length;
}

const PROFILE_KEY = "studyflow.profile";
const THEME_KEY = "studyflow.theme";

export const profile = {
  get name() {
    return localStorage.getItem(PROFILE_KEY);
  },
  set name(value) {
    if (value) localStorage.setItem(PROFILE_KEY, value);
    else localStorage.removeItem(PROFILE_KEY);
  },
};

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
