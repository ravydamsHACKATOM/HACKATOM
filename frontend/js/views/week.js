/** The whole week as a simple calendar: seven columns of blocks. */

import { api } from "../api.js";
import { dueOn } from "../day.js";
import { html, mount, raw, toast } from "../dom.js";
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
import { canSplit, moveTargets, openMoveDialog, openSplitDialog } from "../plan-dialogs.js";
import { acceptPlan } from "../store.js";
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
    const due = dueOn(store.events, date);
    const minutes = blocks
      .filter((block) => block.status === "PLANNED" || block.status === "DONE")
      .reduce((sum, block) => sum + block.minutes, 0);
    return { weekday, date, blocks, minutes, due };
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
          ${
            plan.generated && ready && !isPast
              ? html`<button
                  class="button button--ghost button--small spread-toggle"
                  type="button"
                  data-spread
                  aria-pressed="${String(plan.settings.spread)}"
                  title="Espalhar as sessões para estudar um pouco em cada dia"
                >
                  <span class="spread-toggle__track" aria-hidden="true">
                    <span class="spread-toggle__thumb"></span>
                  </span>
                  Um pouco por dia
                </button>`
              : ""
          }
          ${
            plan.generated && ready && !isPast && canSplit(plan, store.events)
              ? html`<button
                  class="button button--ghost button--small"
                  type="button"
                  data-split-week
                >
                  ${raw(icon("split", 16))} Dividir o tempo
                </button>`
              : ""
          }
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
                      data-day="${day.date}"
                    >
                      <header class="day__header">
                        <span class="day__name">${day.weekday.short}</span>
                        <span class="day__date">${dayMonth(day.date)}</span>
                        ${day.minutes ? html`<span class="day__total">${duration(day.minutes)}</span>` : ""}
                      </header>
                      ${
                        day.due.length
                          ? html`<ul class="day__due" aria-label="Prazos do dia">
                              ${day.due.map(
                                (event) => html`
                                  <li
                                    class="day__deadline day__deadline--${event.type.toLowerCase()}"
                                    style="--tone:${event.disciplineColor}"
                                    title="${event.type === "EXAM" ? "Prova" : "Entrega"}: ${event.title}"
                                  >
                                    ${raw(icon(event.type === "EXAM" ? "exam" : "assignment", 14))}
                                    <span>${event.title}</span>
                                  </li>
                                `,
                              )}
                            </ul>`
                          : ""
                      }
                      ${
                        day.blocks.length
                          ? html`<ul class="day__blocks">
                              ${day.blocks.map((block, index) =>
                                blockCard(block, {
                                  compact: true,
                                  index,
                                  movable: canMove(block, plan),
                                }),
                              )}
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

  wireMoves(main, context, plan);
  main
    .querySelector("[data-split-week]")
    ?.addEventListener("click", () =>
      openSplitDialog(plan, store.events, () => context.draw({ animate: false })),
    );
  main.querySelector("[data-spread]")?.addEventListener("click", (event) => {
    const button = event.currentTarget;
    button.disabled = true;
    button.classList.add("is-busy");
    const spread = !plan.settings.spread;
    context.run(
      async () =>
        acceptPlan(
          await api.plan.settings(store.weekDate ?? undefined, {
            spread,
            allocations: plan.settings.allocations,
          }),
        ),
      spread ? "Sessões espalhadas pela semana." : "Sessões concentradas nos primeiros horários.",
    );
  });

  main.querySelectorAll("[data-week]").forEach((button) =>
    button.addEventListener("click", () => {
      const step = Number(button.dataset.week);
      context.setWeek(step === 0 ? null : addDays(plan.weekStart, step));
    }),
  );
}

function canMove(block, plan) {
  return block.status === "PLANNED" && moveTargets(block, plan).length > 0;
}

/**
 * Drag a session onto another day to move it there at the same time. The move dialog does the
 * same with the keyboard or on a phone, where dragging is not available.
 */
function wireMoves(main, context, plan) {
  const redraw = () => context.draw({ animate: false });
  const blockById = (id) => plan.blocks.find((block) => block.id === Number(id));

  main.querySelectorAll("[data-move]").forEach((button) =>
    button.addEventListener("click", () => {
      openMoveDialog(blockById(button.dataset.move), plan, redraw);
    }),
  );

  let dragged = null;
  const days = [...main.querySelectorAll("[data-day]")];
  const clear = () =>
    days.forEach((day) => day.classList.remove("day--target", "day--closed", "day--over"));

  main.querySelectorAll(".block--movable").forEach((card) => {
    card.addEventListener("dragstart", (event) => {
      dragged = blockById(card.dataset.block);
      event.dataTransfer.effectAllowed = "move";
      event.dataTransfer.setData("text/plain", String(dragged.id));
      const allowed = new Set(moveTargets(dragged, plan).map((day) => day.date));
      days.forEach((day) =>
        day.classList.add(allowed.has(day.dataset.day) ? "day--target" : "day--closed"),
      );
      card.classList.add("block--dragging");
    });
    card.addEventListener("dragend", () => {
      card.classList.remove("block--dragging");
      clear();
      dragged = null;
    });
  });

  days.forEach((day) => {
    day.addEventListener("dragover", (event) => {
      if (!dragged || !day.classList.contains("day--target")) return;
      event.preventDefault();
      day.classList.add("day--over");
    });
    day.addEventListener("dragleave", (event) => {
      if (!day.contains(event.relatedTarget)) day.classList.remove("day--over");
    });
    day.addEventListener("drop", async (event) => {
      event.preventDefault();
      const block = dragged;
      clear();
      if (!block || day.dataset.day === block.date) return;
      try {
        await acceptPlan(await api.blocks.move(block.id, day.dataset.day, block.startTime));
        toast("Sessão movida. O prazo continua o mesmo.", "success");
      } catch (error) {
        toast(`${error.message} Use o botão mover para escolher outro horário.`, "error");
      }
      redraw();
    });
  });
}

function stat(label, value, tone) {
  return html`
    <div class="stat stat--${tone}">
      <span class="stat__label">${label}</span>
      <strong class="stat__value">${value}</strong>
    </div>
  `;
}
