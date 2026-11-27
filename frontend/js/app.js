/** Application shell: session, routing, navigation and the actions shared by every view. */

import { api, setSessionLostHandler } from "./api.js";
import { html, mount, raw, toast } from "./dom.js";
import { duration } from "./format.js";
import { icon } from "./icons.js";
import { acceptPlan, clear, loadAll, store, theme } from "./store.js";
import { renderAvailability } from "./views/availability.js";
import { renderDisciplines } from "./views/disciplines.js";
import { renderEvents } from "./views/events.js";
import { renderToday } from "./views/today.js";
import { renderWeek } from "./views/week.js";
import { renderWelcome } from "./views/welcome.js";

const ROUTES = [
  { path: "hoje", group: "Planejar", label: "Hoje", icon: "today", render: renderToday },
  { path: "semana", group: "Planejar", label: "Semana", icon: "week", render: renderWeek },
  {
    path: "provas",
    group: "Organizar",
    label: "Provas e entregas",
    short: "Prazos",
    icon: "events",
    render: renderEvents,
  },
  {
    path: "disciplinas",
    group: "Organizar",
    label: "Disciplinas",
    icon: "disciplines",
    render: renderDisciplines,
  },
  {
    path: "horarios",
    group: "Organizar",
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

/** Small numbers next to the navigation items: what is still open in each area. */
function badges() {
  const plan = store.plan;
  if (!plan) return {};
  const inCurrentWeek = plan.today >= plan.weekStart && plan.today <= plan.weekEnd;
  const pendingToday = inCurrentWeek
    ? plan.blocks.filter((block) => block.date === plan.today && block.status === "PLANNED").length
    : 0;
  return {
    hoje: pendingToday,
    provas: store.events.filter((event) => event.dueDate >= plan.today).length,
    disciplinas: store.disciplines.length,
  };
}

/** Compact progress of the week shown at the top of the sidebar. */
function weekCard() {
  const plan = store.plan;
  if (!plan?.generated) {
    return html`
      <a class="week-card week-card--empty" href="#/hoje">
        <span class="week-card__icon">${raw(icon("sparkle", 18))}</span>
        <span class="week-card__text">
          <strong>Sem plano ainda</strong>
          <span>Cadastre prazos e horários</span>
        </span>
      </a>
    `;
  }
  const done = plan.stats.doneMinutes;
  const total = done + plan.stats.plannedMinutes;
  const ratio = total ? done / total : 0;
  const circumference = 2 * Math.PI * 16;
  return html`
    <a class="week-card" href="#/semana" aria-label="Progresso da semana, abrir a semana">
      <svg class="week-card__ring" viewBox="0 0 40 40" width="40" height="40" aria-hidden="true">
        <circle class="week-card__track" cx="20" cy="20" r="16" />
        <circle
          class="week-card__value"
          cx="20"
          cy="20"
          r="16"
          stroke-dasharray="${circumference}"
          style="--offset:${circumference * (1 - ratio)}; --full:${circumference}"
        />
      </svg>
      <span class="week-card__text">
        <strong>${Math.round(ratio * 100)}% da semana</strong>
        <span>${duration(done)} de ${duration(total)}</span>
      </span>
      ${raw(icon("right", 16))}
    </a>
  `;
}

function shell(route) {
  const dark = theme.current === "dark";
  const counts = badges();
  const groups = [...new Set(ROUTES.map((item) => item.group))];
  const avatar = initials(store.user.name);

  return html`
    <div class="shell">
      <header class="topbar">
        <a class="brand" href="#/hoje" aria-label="StudyFlow, ir para hoje">
          <img src="assets/favicon.svg" width="28" height="28" alt="" />
          <span>StudyFlow</span>
        </a>
        <div class="topbar__actions">
          <button
            class="icon-button"
            type="button"
            data-action="theme"
            aria-label="${dark ? "Usar tema claro" : "Usar tema escuro"}"
          >
            ${raw(icon(dark ? "sun" : "moon", 18))}
          </button>
          <button class="icon-button" type="button" data-action="logout" aria-label="Sair">
            ${raw(icon("logout", 18))}
          </button>
          <span class="avatar avatar--small" aria-hidden="true">${avatar}</span>
        </div>
      </header>

      <aside class="sidebar">
        <a class="brand" href="#/hoje" aria-label="StudyFlow, ir para hoje">
          <img src="assets/favicon.svg" width="34" height="34" alt="" />
          <span class="brand__text">
            <strong>StudyFlow</strong>
            <small>Planejador de estudos</small>
          </span>
        </a>

        ${weekCard()}

        <nav class="nav" aria-label="Principal">
          ${groups.map(
            (group) => html`
              <div class="nav__group">
                <p class="nav__title">${group}</p>
                ${ROUTES.filter((item) => item.group === group).map(
                  (item) => html`
                    <a
                      class="nav__item ${item === route ? "nav__item--active" : ""}"
                      href="#/${item.path}"
                      aria-current="${item === route ? "page" : "false"}"
                    >
                      <span class="nav__icon">${raw(icon(item.icon))}</span>
                      <span class="nav__label">${item.label}</span>
                      <span class="nav__label nav__label--short">${item.short ?? item.label}</span>
                      ${
                        counts[item.path]
                          ? html`<span class="nav__badge">${counts[item.path]}</span>`
                          : ""
                      }
                    </a>
                  `,
                )}
              </div>
            `,
          )}
        </nav>

        <div class="sidebar__footer">
          <button
            class="theme-switch"
            type="button"
            role="switch"
            aria-checked="${String(dark)}"
            data-action="theme"
          >
            <span class="theme-switch__label">
              ${raw(icon(dark ? "moon" : "sun", 18))} Tema ${dark ? "escuro" : "claro"}
            </span>
            <span class="theme-switch__track" aria-hidden="true">
              <span class="theme-switch__thumb"></span>
            </span>
          </button>
          <div class="account">
            <span class="avatar" aria-hidden="true">${avatar}</span>
            <div class="account__text">
              <strong>${store.user.name}</strong>
              <span>${store.user.email}</span>
            </div>
            <button
              class="icon-button"
              type="button"
              data-action="logout"
              title="Sair"
              aria-label="Sair da conta"
            >
              ${raw(icon("logout", 18))}
            </button>
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
  root.firstElementChild.classList.toggle("shell--still", !animate);
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
