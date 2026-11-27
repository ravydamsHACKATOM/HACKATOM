/** "What should I do today?" The first screen after entering. */

import { api } from "../api.js";
import { html, mount, raw } from "../dom.js";
import { addDays, deadline, duration, greeting, longDate } from "../format.js";
import { disciplineIcon, icon } from "../icons.js";
import {
  advisorCard,
  blockCard,
  generateButton,
  progressRing,
  staleBanner,
  warningsCard,
} from "../plan-ui.js";
import { isEmpty, loadAll, profile } from "../store.js";

export function renderToday(main, context) {
  const { store } = context;
  const plan = store.plan;
  if (!plan) {
    mount(main, html`<p class="empty__text">Carregando…</p>`);
    return;
  }

  const today = plan.today;
  const isCurrentWeek = today >= plan.weekStart && today <= plan.weekEnd;
  const todayBlocks = isCurrentWeek ? plan.blocks.filter((block) => block.date === today) : [];
  const active = todayBlocks.filter((block) => block.status !== "SKIPPED");
  const total = active.reduce((sum, block) => sum + block.minutes, 0);
  const done = active
    .filter((block) => block.status === "DONE")
    .reduce((sum, block) => sum + block.minutes, 0);
  const upcoming = store.events.filter((event) => event.dueDate >= today).slice(0, 4);
  const ready = store.disciplines.length && store.events.length && store.windows.length;

  mount(
    main,
    html`
      <header class="page-header rise">
        <div>
          <p class="eyebrow">${longDate(today)}</p>
          <h1>${greeting()}, ${profile.name}.</h1>
        </div>
        ${ready && isCurrentWeek ? generateButton(plan) : ""}
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

      <div class="today">
        <section class="today__main" aria-labelledby="today-title">
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
                    <div class="today__summary card rise" style="--i:1">
                      ${progressRing(done, total)}
                      <div>
                        <h2 id="today-title">
                          ${
                            todayBlocks.length === 0
                              ? "Dia livre"
                              : done === total && total > 0
                                ? "Tudo feito por hoje"
                                : `${duration(total - done)} pela frente`
                          }
                        </h2>
                        <p class="today__hint">
                          ${
                            todayBlocks.length === 0
                              ? "Nenhum bloco previsto para hoje. Aproveite o descanso."
                              : `${duration(done)} de ${duration(total)} concluídos em ${
                                  active.length
                                } ${active.length === 1 ? "bloco" : "blocos"}.`
                          }
                        </p>
                      </div>
                    </div>
                    ${
                      todayBlocks.length
                        ? html`<ul class="block-list">
                            ${todayBlocks.map((block, index) => blockCard(block, { index: index + 2 }))}
                          </ul>`
                        : emptyState({
                            image: "empty-today",
                            title: "Nada para hoje",
                            text: "Seu plano não tem blocos neste dia. Veja a semana completa para se adiantar.",
                            action: html`<a class="button button--ghost" href="#/semana"
                              >Ver a semana</a
                            >`,
                          })
                    }
                  `
          }
        </section>

        <aside class="today__side">
          ${plan.generated ? advisorCard(plan.note, 2) : ""}
          ${plan.generated ? warningsCard(plan.warnings, today, 3) : ""}
          ${
            upcoming.length
              ? html`
                  <section class="card rise" style="--i:4">
                    <h3 class="card__title">Próximos prazos</h3>
                    <ul class="deadline-list">
                      ${upcoming.map(
                        (event) => html`
                          <li style="--tone:${event.disciplineColor}">
                            ${raw(disciplineIcon(event.disciplineIcon, 36))}
                            <div>
                              <p class="deadline__title">${event.title}</p>
                              <p class="deadline__meta">
                                ${event.type === "EXAM" ? "Prova" : "Entrega"}
                                ${deadline(today, event.dueDate)} · ${duration(event.effortMinutes)}
                              </p>
                              ${progressBar(event)}
                            </div>
                          </li>
                        `,
                      )}
                    </ul>
                    <a class="link" href="#/provas">Ver todos ${raw(icon("arrow", 14))}</a>
                  </section>
                `
              : ""
          }
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
      title: "Cadastre suas disciplinas",
      text: "As matérias que você está cursando.",
      href: "#/disciplinas",
    },
    {
      done: store.events.length > 0,
      title: "Adicione provas e entregas",
      text: "Com a data e as horas de estudo que você estima.",
      href: "#/provas",
    },
    {
      done: store.windows.length > 0,
      title: "Informe seus horários livres",
      text: "Os dias e horas em que dá para estudar.",
      href: "#/horarios",
    },
  ];
  const next = steps.findIndex((step) => !step.done);

  return html`
    <div class="card onboarding rise" style="--i:1">
      <h2 id="today-title">Vamos montar sua primeira semana</h2>
      <p class="today__hint">Três passos rápidos e o plano sai sozinho.</p>
      <ol class="steps">
        ${steps.map(
          (step, index) => html`
            <li class="step ${step.done ? "step--done" : ""} ${index === next ? "step--next" : ""}">
              <span class="step__mark">${step.done ? raw(icon("check", 16)) : index + 1}</span>
              <div>
                <p class="step__title">${step.title}</p>
                <p class="step__text">${step.text}</p>
              </div>
              ${
                index === next
                  ? html`<a class="button button--primary button--small" href="${step.href}"
                      >Começar</a
                    >`
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

/** Fills the app with a believable week so a first-time visitor sees the whole flow at once. */
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
