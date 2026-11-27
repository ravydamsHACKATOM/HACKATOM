/** The whole week as a simple calendar: seven columns of blocks. */

import { html, mount, raw } from "../dom.js";
import { addDays, dayMonth, duration, WEEKDAYS, weekRange } from "../format.js";
import { icon } from "../icons.js";
import {
  advisorCard,
  blockCard,
  changesCard,
  generateButton,
  staleBanner,
  warningsCard,
} from "../plan-ui.js";
import { emptyState } from "./today.js";

export function renderWeek(main, context) {
  const { store } = context;
  const plan = store.plan;
  const today = plan.today;
  const isCurrentWeek = today >= plan.weekStart && today <= plan.weekEnd;
  const isPast = plan.weekEnd < today;
  const ready = store.events.length && store.windows.length;

  const days = WEEKDAYS.map((weekday, index) => {
    const date = addDays(plan.weekStart, index);
    const blocks = plan.blocks.filter((block) => block.date === date);
    const minutes = blocks
      .filter((block) => block.status === "PLANNED" || block.status === "DONE")
      .reduce((sum, block) => sum + block.minutes, 0);
    return { weekday, date, blocks, minutes };
  });

  mount(
    main,
    html`
      <header class="page-header rise">
        <div>
          <p class="eyebrow">
            ${isCurrentWeek ? "Esta semana" : isPast ? "Semana passada" : "Semana futura"}
          </p>
          <h1>${weekRange(plan.weekStart, plan.weekEnd)}</h1>
        </div>
        <div class="page-header__actions">
          <div class="pager" role="group" aria-label="Trocar de semana">
            <button class="icon-button" type="button" data-week="-7" aria-label="Semana anterior">
              ${raw(icon("left", 18))}
            </button>
            <button
              class="button button--ghost button--small"
              type="button"
              data-week="0"
              ${isCurrentWeek ? raw("disabled") : ""}
            >
              Hoje
            </button>
            <button class="icon-button" type="button" data-week="7" aria-label="Próxima semana">
              ${raw(icon("right", 18))}
            </button>
          </div>
          ${ready && !isPast ? generateButton(plan) : ""}
        </div>
      </header>

      ${staleBanner(plan)}

      <section class="stats rise" style="--i:1" aria-label="Resumo da semana">
        ${stat("Planejado", duration(plan.stats.plannedMinutes), "blue")}
        ${stat("Concluído", duration(plan.stats.doneMinutes), "accent")}
        ${stat("Tempo livre restante", duration(plan.stats.availableMinutes), "mustard")}
        ${stat(
          "Sem espaço",
          duration(plan.stats.missingMinutes),
          plan.stats.missingMinutes > 0 ? "terracotta" : "quiet",
        )}
      </section>

      ${
        plan.generated
          ? html`<div class="week-notes">
              ${changesCard(plan.changes, 2)} ${warningsCard(plan.warnings, today, 2)}
              ${advisorCard(plan.note, 3)}
            </div>`
          : ""
      }
      ${
        !plan.generated && !plan.blocks.length
          ? emptyState({
              image: "empty-plan",
              title: isPast ? "Nenhum plano nessa semana" : "Esta semana ainda não tem plano",
              text: ready
                ? "Gere o plano para distribuir os blocos de estudo até cada prazo."
                : "Cadastre ao menos uma prova ou entrega e um horário livre para gerar o plano.",
              action:
                ready && !isPast
                  ? generateButton(plan)
                  : html`<a class="button button--ghost" href="#/provas">Cadastrar prazos</a>`,
            })
          : html`
              <ol class="week rise" style="--i:3">
                ${days.map(
                  (day) => html`
                    <li
                      class="day ${day.date === today ? "day--today" : ""} ${
                        day.date < today ? "day--past" : ""
                      }"
                    >
                      <header class="day__header">
                        <span class="day__name">${day.weekday.short}</span>
                        <span class="day__date">${dayMonth(day.date)}</span>
                        ${day.minutes ? html`<span class="day__total">${duration(day.minutes)}</span>` : ""}
                      </header>
                      ${
                        day.blocks.length
                          ? html`<ul class="day__blocks">
                              ${day.blocks.map((block, index) => blockCard(block, { compact: true, index }))}
                            </ul>`
                          : html`<p class="day__empty">Livre</p>`
                      }
                    </li>
                  `,
                )}
              </ol>
            `
      }
    `,
  );

  main.querySelectorAll("[data-week]").forEach((button) =>
    button.addEventListener("click", () => {
      const step = Number(button.dataset.week);
      context.setWeek(step === 0 ? null : addDays(plan.weekStart, step));
    }),
  );
}

function stat(label, value, tone) {
  return html`
    <div class="stat stat--${tone}">
      <span class="stat__label">${label}</span>
      <strong class="stat__value">${value}</strong>
    </div>
  `;
}
