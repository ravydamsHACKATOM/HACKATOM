/** Application shell: session, routing, navigation and the actions shared by every view. */

import { api, setSessionLostHandler } from "./api.js";
import { html, mount, raw, toast } from "./dom.js";
import { icon } from "./icons.js";
import { acceptPlan, clear, loadAll, store, theme } from "./store.js";
import { renderAvailability } from "./views/availability.js";
import { renderDisciplines } from "./views/disciplines.js";
import { renderEvents } from "./views/events.js";
import { renderToday } from "./views/today.js";
import { renderWeek } from "./views/week.js";
import { renderWelcome } from "./views/welcome.js";

const ROUTES = [
  { path: "hoje", label: "Hoje", icon: "today", render: renderToday },
  { path: "semana", label: "Semana", icon: "week", render: renderWeek },
  {
    path: "provas",
    label: "Provas e entregas",
    short: "Prazos",
    icon: "events",
    render: renderEvents,
  },
  { path: "disciplinas", label: "Disciplinas", icon: "disciplines", render: renderDisciplines },
  {
    path: "horarios",
    label: "Horários livres",
    short: "Horários",
    icon: "availability",
    render: renderAvailability,
  },
];

const root = document.getElementById("app");

function currentRoute() {
  const path = location.hash.replace(/^#\//, "");
  return ROUTES.find((route) => route.path === path) ?? ROUTES[0];
}

function initials(name) {
  const parts = name.trim().split(/\s+/);
  return (parts[0][0] + (parts.length > 1 ? parts[parts.length - 1][0] : "")).toUpperCase();
}

function shell(route) {
  const dark = theme.current === "dark";
  return html`
    <div class="shell">
      <aside class="sidebar">
        <a class="brand" href="#/hoje" aria-label="StudyFlow, ir para hoje">
          <img src="assets/favicon.svg" width="32" height="32" alt="" />
          <span>StudyFlow</span>
        </a>
        <nav class="nav" aria-label="Principal">
          ${ROUTES.map(
            (item) => html`
              <a
                class="nav__item ${item === route ? "nav__item--active" : ""}"
                href="#/${item.path}"
                aria-current="${item === route ? "page" : "false"}"
              >
                ${raw(icon(item.icon))}
                <span class="nav__label">${item.label}</span>
                <span class="nav__label nav__label--short">${item.short ?? item.label}</span>
              </a>
            `,
          )}
        </nav>
        <div class="sidebar__footer">
          <button class="nav__item" type="button" data-action="theme">
            ${raw(icon(dark ? "sun" : "moon"))}
            <span class="nav__label">${dark ? "Tema claro" : "Tema escuro"}</span>
          </button>
          <button class="nav__item" type="button" data-action="logout">
            ${raw(icon("logout"))}
            <span class="nav__label">Sair</span>
          </button>
          <div class="account">
            <span class="account__avatar" aria-hidden="true">${initials(store.user.name)}</span>
            <div class="account__text">
              <strong>${store.user.name}</strong>
              <span>${store.user.email}</span>
            </div>
          </div>
        </div>
      </aside>
      <main id="main" class="main" tabindex="-1"></main>
    </div>
  `;
}

/** Draws the current route. `animate` is false on data refreshes so the screen does not flicker. */
function draw({ animate = true } = {}) {
  if (!store.user) {
    document.title = "StudyFlow · sua semana de estudos, organizada";
    renderWelcome(root, { onSignedIn: enter });
    return;
  }
  const route = currentRoute();
  mount(root, shell(route));
  const main = root.querySelector("#main");
  main.classList.toggle("main--still", !animate);
  route.render(main, context);
  document.title = `${route.label} · StudyFlow`;
}

async function refresh({ animate = false } = {}) {
  await loadAll();
  draw({ animate });
}

/** Runs an action, shows any failure as a toast and redraws with fresh data. */
async function run(task, successMessage) {
  try {
    await task();
    if (successMessage) toast(successMessage, "success");
  } catch (error) {
    toast(error.message, "error");
  }
  if (store.user) draw({ animate: false });
}

const planActions = {
  generate: () =>
    run(
      async () => acceptPlan(await api.plan.generate(store.weekDate ?? undefined)),
      "Plano atualizado.",
    ),
  done: (id) =>
    run(async () => api.blocks.setDone(id, true).then(reloadPlan), "Bloco concluído. Boa!"),
  undo: (id) => run(async () => api.blocks.setDone(id, false).then(reloadPlan)),
  lock: (id) =>
    run(
      async () => api.blocks.setLocked(id, true).then(reloadPlan),
      "Bloco travado neste horário.",
    ),
  unlock: (id) => run(async () => api.blocks.setLocked(id, false).then(reloadPlan)),
  postpone: (id) =>
    run(
      async () => acceptPlan(await api.blocks.postpone(id)),
      "Bloco adiado. Refiz o resto da semana.",
    ),
};

async function reloadPlan() {
  await acceptPlan(await api.plan.get(store.weekDate ?? undefined));
}

/** What each view receives: the data plus ways to change it. */
const context = {
  store,
  refresh,
  run,
  draw,
  setWeek(date) {
    store.weekDate = date;
    return run(reloadPlan);
  },
};

/** Leaves the signed-in area and shows the landing screen again. */
function leave() {
  clear();
  document.getElementById("modal-root").innerHTML = "";
  document.body.classList.remove("has-modal");
  history.replaceState(null, "", location.pathname);
  draw();
}

/** Called after a successful sign-in or sign-up. */
async function enter(user) {
  store.user = user;
  try {
    await loadAll();
  } catch (error) {
    toast(error.message, "error");
    return;
  }
  if (!location.hash) history.replaceState(null, "", "#/hoje");
  draw();
}

root.addEventListener("click", async (event) => {
  const target = event.target.closest("[data-action]");
  if (!target) return;
  const { action, id } = target.dataset;
  if (action === "theme") {
    theme.toggle();
    draw({ animate: false });
  } else if (action === "logout") {
    await api.auth.logout().catch(() => {});
    leave();
  } else if (planActions[action]) {
    target.disabled = true;
    target.classList.add("is-busy");
    planActions[action](id);
  }
});

window.addEventListener("hashchange", () => {
  if (!store.user) return;
  draw();
  document.getElementById("main")?.focus({ preventScroll: true });
  window.scrollTo({ top: 0 });
});

setSessionLostHandler(() => {
  if (store.user) {
    leave();
    toast("Sua sessão terminou. Entre de novo.", "error");
  }
});

/** On load: if the session cookie is still valid go straight in, otherwise show the landing. */
async function boot() {
  theme.apply();
  try {
    await enter(await api.auth.me());
  } catch {
    draw();
  }
}

boot();
