/** "What should I do today?" The first screen after signing in. */

import { api } from "../api.js";
import { html, mount, raw } from "../dom.js";
import {
  addDays,
  deadline,
  duration,
  greeting,
  longDate,
  plural,
  time,
  WEEKDAYS,
} from "../format.js";
import { disciplineIcon, icon } from "../icons.js";
import { advisorCard, generateButton, staleBanner, warningsCard } from "../plan-ui.js";
import { firstName, isEmpty, loadAll } from "../store.js";

/** Timer that keeps the "starts in" label fresh; replaced on every render. */
let ticker;

const minutesOfDay = (value) => {
  const [hour, minute] = value.split(":").map(Number);
  return hour * 60 + minute;
};

export function renderToday(main, context) {
  clearInterval(ticker);
  const { store } = context;
  const plan = store.plan;
  if (!plan) {
    mount(main, html`<p class="empty__text">Carregando…</p>`);
    return;
  }

  const today = plan.today;
  const isCurrentWeek = today >= plan.weekStart && today <= plan.weekEnd;
  const blocks = isCurrentWeek ? plan.blocks.filter((block) => block.date === today) : [];
  const active = blocks.filter((block) => block.status !== "SKIPPED");
  const total = active.reduce((sum, block) => sum + block.minutes, 0);
  const done = active
    .filter((block) => block.status === "DONE")
    .reduce((sum, block) => sum + block.minutes, 0);
  const doneCount = active.filter((block) => block.status === "DONE").length;
  const upcoming = store.events.filter((event) => event.dueDate >= today);
  const ready = store.disciplines.length && store.events.length && store.windows.length;
  const showPlan = ready && plan.generated && isCurrentWeek;
  const focus = showPlan ? focusBlock(blocks) : null;

  mount(
    main,
    html`
      <header class="hero rise">
        <div class="hero__text">
          <p class="eyebrow">${longDate(today)}</p>
          <h1>${greeting()}, ${firstName()}.</h1>
          <p class="hero__lead">${headline({ showPlan, ready, blocks, active, total, done })}</p>
          <div class="hero__actions">
            ${ready && isCurrentWeek ? generateButton(plan) : ""}
            ${
              showPlan
                ? html`<a class="button button--ghost" href="#/semana">
                    ${raw(icon("week", 18))} Ver a semana
                  </a>`
                : ""
            }
          </div>
        </div>
        ${showPlan ? ring(done, total) : ""}
        <span class="hero__glow" aria-hidden="true"></span>
      </header>

      ${isCurrentWeek ? staleBanner(plan) : ""}
      ${
        !isCurrentWeek
          ? html`<div class="banner rise">
              <p>Você está vendo outra semana na aba Semana. O dia de hoje fica na semana atual.</p>
              <button class="button button--ghost button--small" type="button" data-week-reset>
                Voltar para esta semana
              </button>
            </div>`
          : ""
      }
      ${
        showPlan
          ? html`
              <section class="metrics" aria-label="Resumo do dia">
                ${metric("check", "Blocos", `${doneCount} de ${active.length}`, "concluídos hoje", 1)}
                ${metric("availability", "Foco", duration(total - done), "pela frente", 2)}
                ${metric(
                  "week",
                  "Semana",
                  duration(plan.stats.doneMinutes),
                  `de ${duration(plan.stats.doneMinutes + plan.stats.plannedMinutes)} planejados`,
                  3,
                )}
                ${
                  upcoming.length
                    ? metric(
                        "events",
                        "Próximo prazo",
                        deadline(today, upcoming[0].dueDate),
                        upcoming[0].title,
                        4,
                      )
                    : ""
                }
              </section>
            `
          : ""
      }

      <div class="today">
        <section class="today__main" aria-label="Seu dia">
          ${
            !ready
              ? onboarding(store)
              : !plan.generated
                ? emptyState({
                    image: "empty-plan",
                    title: "Tudo pronto para montar sua semana",
                    text: "Você já cadastrou prazos e horários. Gere o plano e veja o que fazer hoje.",
                    action: generateButton(plan),
                  })
                : html`
                    ${focus ? focusCard(focus, today) : ""}
                    ${
                      blocks.length
                        ? html`
                            <div class="section-title rise" style="--i:3">
                              <h2>Linha do dia</h2>
                              <span>${plural(blocks.length, "bloco", "blocos")}</span>
                            </div>
                            <ol class="timeline">
                              ${blocks.map((block, index) =>
                                timelineItem(block, index, focus?.block.id === block.id),
                              )}
                            </ol>
                          `
                        : emptyState({
                            image: "empty-today",
                            title: "Dia livre",
                            text: "Seu plano não tem blocos hoje. Aproveite o descanso ou adiante algo da semana.",
                            action: html`<a class="button button--ghost" href="#/semana"
                              >Ver a semana</a
                            >`,
                          })
                    }
                    ${weekStrip(plan, today)}
                  `
          }
        </section>

        <aside class="today__side">
          ${plan.generated ? advisorCard(plan.note, 2) : ""}
          ${plan.generated ? warningsCard(plan.warnings, today, 3) : ""}
          ${upcoming.length ? deadlinesCard(upcoming.slice(0, 4), today) : ""}
        </aside>
      </div>
    `,
  );

  main.querySelector("[data-week-reset]")?.addEventListener("click", () => context.setWeek(null));
  main.querySelector("[data-seed]")?.addEventListener("click", (event) => {
    event.currentTarget.disabled = true;
    event.currentTarget.classList.add("is-busy");
    context.run(() => seedExample(today), "Exemplo carregado. Este é o seu plano.");
  });

  countUp(main.querySelector("[data-count]"), main.classList.contains("main--still"));
  if (focus) {
    const label = main.querySelector("[data-when]");
    ticker = setInterval(() => {
      if (!document.body.contains(label)) return clearInterval(ticker);
      label.textContent = whenLabel(focus.block);
    }, 30_000);
  }
}

/** One sentence that sums up the day. */
function headline({ showPlan, ready, blocks, active, total, done }) {
  if (!ready) return "Vamos deixar sua semana de estudos organizada em poucos minutos.";
  if (!showPlan) return "Seus prazos e horários estão prontos. Falta só gerar o plano.";
  if (!blocks.length) return "Nada previsto para hoje. Um bom dia para descansar a cabeça.";
  if (total > 0 && done === total) return "Tudo feito por hoje. Pode fechar o caderno sem culpa.";
  const left = active.filter((block) => block.status !== "DONE").length;
  return `Faltam ${plural(left, "bloco", "blocos")} hoje, ${duration(total - done)} de foco.`;
}

/** The block happening now, or the next one that is still planned. */
function focusBlock(blocks) {
  const now = new Date().getHours() * 60 + new Date().getMinutes();
  const planned = blocks.filter((block) => block.status === "PLANNED");
  const current = planned.find(
    (block) => minutesOfDay(block.startTime) <= now && now < minutesOfDay(block.endTime),
  );
  if (current) return { block: current, current: true };
  const next = planned.find((block) => minutesOfDay(block.startTime) > now) ?? planned[0];
  return next ? { block: next, current: false } : null;
}

function whenLabel(block) {
  const now = new Date().getHours() * 60 + new Date().getMinutes();
  const start = minutesOfDay(block.startTime);
  const end = minutesOfDay(block.endTime);
  if (now >= start && now < end) return `Acontecendo agora · termina em ${duration(end - now)}`;
  if (now < start) return `A seguir · começa em ${duration(start - now)}`;
  return "O horário passou · ainda dá para fazer";
}

function focusCard({ block, current }, today) {
  return html`
    <article
      class="focus ${current ? "focus--now" : ""} rise"
      style="--i:2; --tone:${block.disciplineColor}"
    >
      <div class="focus__art">${raw(disciplineIcon(block.disciplineIcon, 64))}</div>
      <div class="focus__body">
        <p class="focus__when">
          <span class="pulse" aria-hidden="true"></span>
          <span data-when>${whenLabel(block)}</span>
        </p>
        <h2 class="focus__title">${block.eventTitle}</h2>
        <p class="focus__meta">
          <span class="focus__discipline">
            <span class="dot" aria-hidden="true"></span>${block.disciplineName}
          </span>
          <span>${time(block.startTime)} – ${time(block.endTime)}</span>
          <span>${duration(block.minutes)}</span>
          ${block.kind === "REVIEW" ? html`<span class="tag tag--mustard">Revisão</span>` : ""}
          <span class="tag tag--quiet">
            ${block.eventType === "EXAM" ? "Prova" : "Entrega"} ${deadline(today, block.dueDate)}
          </span>
        </p>
      </div>
      <div class="focus__actions">
        <button
          class="button button--primary"
          type="button"
          data-action="done"
          data-id="${block.id}"
        >
          ${raw(icon("check", 18))} Concluir
        </button>
        <button
          class="button button--ghost"
          type="button"
          data-action="postpone"
          data-id="${block.id}"
          title="Não vou conseguir: adiar e refazer o plano"
        >
          ${raw(icon("later", 18))} Adiar
        </button>
      </div>
    </article>
  `;
}

const STATUS_LABEL = { DONE: "Concluído", SKIPPED: "Adiado", MISSED: "Não realizado" };

function timelineItem(block, index, isFocus) {
  const planned = block.status === "PLANNED";
  const done = block.status === "DONE";
  const checkable = planned || done || block.status === "MISSED";
  return html`
    <li
      class="moment moment--${block.status.toLowerCase()} ${isFocus ? "moment--focus" : ""} rise"
      style="--i:${index + 4}; --tone:${block.disciplineColor}"
    >
      <div class="moment__time">
        <strong>${time(block.startTime)}</strong>
        <span>${time(block.endTime)}</span>
      </div>
      <div class="moment__rail" aria-hidden="true"><span class="moment__dot"></span></div>
      <div class="moment__card ${block.locked ? "moment__card--locked" : ""}">
        ${
          checkable
            ? html`<button
                class="check ${done ? "check--on" : ""}"
                type="button"
                data-action="${done ? "undo" : "done"}"
                data-id="${block.id}"
                aria-pressed="${String(done)}"
                aria-label="${done ? "Desmarcar bloco" : "Marcar bloco como concluído"}"
              >
                ${raw(icon("check", 16))}
              </button>`
            : html`<span class="check check--off" aria-hidden="true"></span>`
        }
        <div class="moment__body">
          <p class="moment__title">${block.eventTitle}</p>
          <p class="moment__meta">
            <span class="dot" aria-hidden="true"></span>${block.disciplineName}
            <span class="moment__minutes">${duration(block.minutes)}</span>
            ${block.kind === "REVIEW" ? html`<span class="tag tag--mustard">Revisão</span>` : ""}
            ${block.locked ? html`<span class="tag">Travado</span>` : ""}
            ${
              STATUS_LABEL[block.status]
                ? html`<span class="tag tag--quiet">${STATUS_LABEL[block.status]}</span>`
                : ""
            }
          </p>
        </div>
        ${
          planned
            ? html`<div class="moment__actions">
                <button
                  class="icon-button"
                  type="button"
                  data-action="${block.locked ? "unlock" : "lock"}"
                  data-id="${block.id}"
                  title="${block.locked ? "Destravar" : "Travar neste horário"}"
                  aria-label="${block.locked ? "Destravar bloco" : "Travar bloco neste horário"}"
                >
                  ${raw(icon(block.locked ? "lock" : "unlock", 18))}
                </button>
                <button
                  class="icon-button"
                  type="button"
                  data-action="postpone"
                  data-id="${block.id}"
                  title="Não vou conseguir: adiar e refazer"
                  aria-label="Adiar bloco e refazer o plano"
                >
                  ${raw(icon("later", 18))}
                </button>
              </div>`
            : ""
        }
        <span class="moment__icon">${raw(disciplineIcon(block.disciplineIcon, 40))}</span>
      </div>
    </li>
  `;
}

/** Seven small bars, one per day, showing how the study time is spread over the week. */
function weekStrip(plan, today) {
  const days = WEEKDAYS.map((weekday, index) => {
    const date = addDays(plan.weekStart, index);
    const dayBlocks = plan.blocks.filter(
      (block) => block.date === date && (block.status === "PLANNED" || block.status === "DONE"),
    );
    const minutes = dayBlocks.reduce((sum, block) => sum + block.minutes, 0);
    const doneMinutes = dayBlocks
      .filter((block) => block.status === "DONE")
      .reduce((sum, block) => sum + block.minutes, 0);
    return { weekday, date, minutes, doneMinutes };
  });
  const peak = Math.max(...days.map((day) => day.minutes), 1);

  return html`
    <section class="card strip rise" style="--i:6" aria-label="Carga da semana">
      <header class="strip__header">
        <h3 class="card__title">Sua semana</h3>
        <a class="link" href="#/semana">Abrir ${raw(icon("arrow", 14))}</a>
      </header>
      <ol class="strip__days">
        ${days.map(
          (day, index) => html`
            <li
              class="strip__day ${day.date === today ? "strip__day--today" : ""} ${
                day.date < today ? "strip__day--past" : ""
              }"
              title="${day.weekday.long}: ${day.minutes ? duration(day.minutes) : "livre"}"
            >
              <div class="strip__track">
                <span
                  class="strip__bar"
                  style="--h:${Math.round((day.minutes / peak) * 100)}%; --d:${index}"
                >
                  <span
                    class="strip__done"
                    style="height:${
                      day.minutes ? Math.round((day.doneMinutes / day.minutes) * 100) : 0
                    }%"
                  ></span>
                </span>
              </div>
              <span class="strip__name">${day.weekday.short}</span>
              <span class="strip__total">${day.minutes ? duration(day.minutes) : "–"}</span>
            </li>
          `,
        )}
      </ol>
    </section>
  `;
}

function deadlinesCard(events, today) {
  return html`
    <section class="card rise" style="--i:4">
      <h3 class="card__title">Próximos prazos</h3>
      <ul class="deadline-list">
        ${events.map((event) => {
          const total = Math.max(event.effortMinutes, 1);
          const covered = Math.min(
            100,
            Math.round(((event.doneMinutes + event.plannedMinutes) / total) * 100),
          );
          return html`
            <li style="--tone:${event.disciplineColor}">
              <span class="deadline__art">${raw(disciplineIcon(event.disciplineIcon, 30))}</span>
              <div>
                <p class="deadline__title">${event.title}</p>
                <p class="deadline__meta">
                  ${event.type === "EXAM" ? "Prova" : "Entrega"} ${deadline(today, event.dueDate)} ·
                  ${duration(event.effortMinutes)}
                </p>
                ${progressBar(event)}
              </div>
              <span class="deadline__cover" title="Parte do esforço já feita ou agendada"
                >${covered}%</span
              >
            </li>
          `;
        })}
      </ul>
      <a class="link" href="#/provas">Ver todos ${raw(icon("arrow", 14))}</a>
    </section>
  `;
}

function metric(iconName, label, value, hint, index) {
  return html`
    <div class="metric rise" style="--i:${index}">
      <span class="metric__icon" aria-hidden="true">${raw(icon(iconName, 18))}</span>
      <div>
        <p class="metric__label">${label}</p>
        <p class="metric__value">${value}</p>
        <p class="metric__hint">${hint}</p>
      </div>
    </div>
  `;
}

/** Large progress ring of the day. The number counts up when the screen opens. */
function ring(done, total) {
  const ratio = total === 0 ? 0 : Math.min(1, done / total);
  const circumference = 2 * Math.PI * 58;
  const percent = Math.round(ratio * 100);
  return html`
    <div
      class="dial"
      role="img"
      aria-label="${duration(done)} de ${duration(total)} concluídos hoje"
    >
      <svg viewBox="0 0 136 136" width="136" height="136">
        <defs>
          <linearGradient id="dial-gradient" x1="0" y1="0" x2="1" y2="1">
            <stop offset="0" stop-color="var(--accent)" />
            <stop offset="1" stop-color="var(--mustard)" />
          </linearGradient>
        </defs>
        <circle class="dial__track" cx="68" cy="68" r="58" />
        <circle
          class="dial__value"
          cx="68"
          cy="68"
          r="58"
          stroke-dasharray="${circumference}"
          style="--offset:${circumference * (1 - ratio)}; --full:${circumference}"
        />
      </svg>
      <div class="dial__label">
        <strong><span data-count="${percent}">${percent}</span>%</strong>
        <span>do dia</span>
      </div>
    </div>
  `;
}

/** Animates a number from zero to its value, unless motion is reduced or the view is refreshing. */
function countUp(node, still) {
  if (!node) return;
  const target = Number(node.dataset.count);
  if (still || target === 0 || window.matchMedia("(prefers-reduced-motion: reduce)").matches) {
    return;
  }
  const start = performance.now();
  const step = (now) => {
    const progress = Math.min(1, (now - start) / 900);
    node.textContent = Math.round(target * (1 - (1 - progress) ** 3));
    if (progress < 1) requestAnimationFrame(step);
  };
  node.textContent = "0";
  requestAnimationFrame(step);
}

/** Share of an event that is done, planned and still uncovered. */
export function progressBar(event) {
  const total = Math.max(event.effortMinutes, 1);
  const donePct = Math.min(100, (event.doneMinutes / total) * 100);
  const plannedPct = Math.min(100 - donePct, (event.plannedMinutes / total) * 100);
  return html`
    <div
      class="progress"
      role="img"
      aria-label="${duration(event.doneMinutes)} concluídos e ${duration(
        event.plannedMinutes,
      )} planejados de ${duration(event.effortMinutes)}"
    >
      <span class="progress__done" style="width:${donePct}%"></span>
      <span class="progress__planned" style="width:${plannedPct}%"></span>
    </div>
  `;
}

export function emptyState({ image, title, text, action }) {
  return html`
    <div class="empty rise" style="--i:1">
      <img src="assets/img/${image}.webp" width="400" height="300" alt="" loading="lazy" />
      <h2>${title}</h2>
      <p class="empty__text">${text}</p>
      ${action ?? ""}
    </div>
  `;
}

function onboarding(store) {
  const steps = [
    {
      done: store.disciplines.length > 0,
      icon: "disciplines",
      title: "Cadastre suas disciplinas",
      text: "As matérias que você está cursando.",
      href: "#/disciplinas",
    },
    {
      done: store.events.length > 0,
      icon: "events",
      title: "Adicione provas e entregas",
      text: "Com a data e as horas de estudo que você estima.",
      href: "#/provas",
    },
    {
      done: store.windows.length > 0,
      icon: "availability",
      title: "Informe seus horários livres",
      text: "Os dias e horas em que dá para estudar.",
      href: "#/horarios",
    },
  ];
  const next = steps.findIndex((step) => !step.done);
  const doneCount = steps.filter((step) => step.done).length;

  return html`
    <div class="card onboarding rise" style="--i:1">
      <header class="onboarding__header">
        <div>
          <h2>Vamos montar sua primeira semana</h2>
          <p class="today__hint">Três passos rápidos e o plano sai sozinho.</p>
        </div>
        <span class="onboarding__count">${doneCount} de 3</span>
      </header>
      <div class="onboarding__bar" aria-hidden="true">
        <span style="width:${(doneCount / 3) * 100}%"></span>
      </div>
      <ol class="steps">
        ${steps.map(
          (step, index) => html`
            <li class="step ${step.done ? "step--done" : ""} ${index === next ? "step--next" : ""}">
              <span class="step__mark">${raw(icon(step.done ? "check" : step.icon, 18))}</span>
              <div>
                <p class="step__title">${step.title}</p>
                <p class="step__text">${step.text}</p>
              </div>
              ${
                index === next
                  ? html`<a class="button button--primary button--small" href="${step.href}">
                      Começar ${raw(icon("arrow", 16))}
                    </a>`
                  : ""
              }
            </li>
          `,
        )}
      </ol>
      ${
        isEmpty()
          ? html`<div class="onboarding__seed">
              <p>Quer só ver funcionando?</p>
              <button class="button button--ghost button--small" type="button" data-seed>
                ${raw(icon("sparkle", 16))} Preencher com um exemplo
              </button>
            </div>`
          : ""
      }
    </div>
  `;
}

/** Fills the account with a believable week so a first-time visitor sees the whole flow at once. */
async function seedExample(today) {
  const disciplines = await Promise.all(
    [
      { name: "Banco de Dados", color: "#3F6B5C", icon: "code", weight: 3 },
      { name: "Desenvolvimento Web", color: "#C8643C", icon: "project", weight: 2 },
      { name: "Lógica de Programação", color: "#6C8AA6", icon: "math", weight: 2 },
      { name: "Inglês Técnico", color: "#D9A441", icon: "language", weight: 1 },
    ].map((discipline) => api.disciplines.create(discipline)),
  );
  const [database, web, logic, english] = disciplines.map((discipline) => discipline.id);

  const event = (disciplineId, title, type, inDays, effortHours) =>
    api.events.create({ disciplineId, title, type, dueDate: addDays(today, inDays), effortHours });
  await Promise.all([
    event(database, "Prova de SQL e modelagem", "EXAM", 3, 3),
    event(web, "Entrega do projeto integrador", "ASSIGNMENT", 5, 5),
    event(logic, "Lista de exercícios 4", "ASSIGNMENT", 2, 1.5),
    event(english, "Apresentação oral", "EXAM", 6, 2),
    event(web, "Protótipo navegável", "ASSIGNMENT", 12, 6),
  ]);

  const window = (dayOfWeek, startTime, endTime) =>
    api.availability.create({ dayOfWeek, startTime, endTime });
  await Promise.all([
    ...["MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY"].map((day) =>
      window(day, "19:00", "21:50"),
    ),
    window("SATURDAY", "09:00", "11:50"),
  ]);

  await api.plan.generate();
  await loadAll();
}
