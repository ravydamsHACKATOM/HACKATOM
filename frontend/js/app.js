/** Application shell: routing, navigation and the actions shared by every view. */

import { api } from "./api.js";
import { html, mount, raw, toast } from "./dom.js";
import { icon } from "./icons.js";
import { acceptPlan, loadAll, profile, store, theme } from "./store.js";
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
          <p class="sidebar__user">${profile.name}</p>
        </div>
      </aside>
      <main id="main" class="main" tabindex="-1"></main>
    </div>
  `;
}

/** Draws the current route. `animate` is false on data refreshes so the screen does not flicker. */
function draw({ animate = true } = {}) {
  if (!profile.name) {
    renderWelcome(root, { onEnter: start });
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
  draw({ animate: false });
}

const planActions = {
  generate: () =>
    run(async () => {
      const wasGenerated = store.plan.generated;
      await acceptPlan(await api.plan.generate(store.weekDate ?? undefined));
      return wasGenerated;
    }, "Plano atualizado."),
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

root.addEventListener("click", (event) => {
  const target = event.target.closest("[data-action]");
  if (!target) return;
  const { action, id } = target.dataset;
  if (action === "theme") {
    theme.toggle();
    draw({ animate: false });
  } else if (action === "logout") {
    profile.name = null;
    location.hash = "";
    draw();
  } else if (planActions[action]) {
    target.disabled = true;
    target.classList.add("is-busy");
    planActions[action](id);
  }
});

window.addEventListener("hashchange", () => {
  if (profile.name) draw();
  document.getElementById("main")?.focus({ preventScroll: true });
  window.scrollTo({ top: 0 });
});

async function start() {
  try {
    await loadAll();
  } catch (error) {
    toast(error.message, "error");
  }
  if (!location.hash) history.replaceState(null, "", "#/hoje");
  draw();
}

theme.apply();
if (profile.name) start();
else draw();
