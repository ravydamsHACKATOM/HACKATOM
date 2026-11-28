/**
 * Dialogs that reorganise a week: share the study time between exams and assignments, choose the
 * rhythm of the week and move one session to another day or time.
 */

import { api } from "./api.js";
import { formValues, html, modal, raw, toast, withBusy } from "./dom.js";
import {
  evenSplit,
  currentShares,
  lastStudyDay,
  maxShare,
  minutesOfDay,
  proportionalSplit,
  splitCandidates,
} from "./day.js";
import { addDays, dayMonth, deadline, duration, time, WEEKDAYS } from "./format.js";
import { bump, choiceCards, chips, enhanceSteppers, formFooter, panel, stepper } from "./forms.js";
import { disciplineIcon, icon } from "./icons.js";
import { acceptPlan, store } from "./store.js";

/** Shares are chosen in half hours, the unit students think in. */
const STEP = 30;

/** True when the week has more than one exam or assignment to share the time between. */
export function canSplit(plan, events) {
  return splitCandidates(events, plan).length > 1;
}

/** "Organizar a semana": minutes per exam or assignment and the rhythm of the week. */
export function openSplitDialog(plan, events, onSaved) {
  const today = plan.today;
  const candidates = splitCandidates(events, plan);
  const shares = currentShares(candidates, plan);
  const manual = plan.settings.allocations.length > 0;
  const capacity = plan.stats.availableMinutes + plan.stats.doneMinutes;

  modal({
    title: "Organizar a semana",
    subtitle: "Divida o tempo entre as provas e entregas. O plano se refaz com a sua escolha.",
    icon: "split",
    tone: "blue",
    body: html`
      <form class="form split" novalidate>
        ${panel(
          { icon: "sparkle", title: "Como dividir", index: 0 },
          choiceCards({
            name: "mode",
            legend: "Como dividir o tempo",
            value: manual ? "manual" : "auto",
            options: [
              {
                value: "auto",
                label: "Automático",
                hint: "O prazo mais próximo vem primeiro",
                icon: "sparkle",
              },
              {
                value: "manual",
                label: "Eu divido",
                hint: "Você escolhe as horas de cada uma",
                icon: "split",
                tone: "var(--blue)",
              },
            ],
          }),
        )}
        ${panel(
          {
            icon: "availability",
            title: "Horas desta semana",
            hint: html`<span data-total></span>`,
            index: 1,
          },
          html`
            <div class="split__meter" aria-hidden="true">
              <span class="split__fill" data-fill></span>
            </div>
            <p class="split__note" data-note role="status"></p>
            <div class="split__tools">
              <button class="chip-button" type="button" data-split="even">
                ${raw(icon("split", 14))} Dividir igualmente
              </button>
              <button class="chip-button" type="button" data-split="need">
                ${raw(icon("weightMid", 14))} Pelo que falta
              </button>
            </div>
            <ul class="split__list">
              ${candidates.map((event, index) => {
                const share = shares[index].minutes;
                const left = Math.max(0, event.effortMinutes - event.doneMinutes);
                return html`
                  <li class="split__row" style="--tone:${event.disciplineColor}; --i:${index}">
                    <span class="split__art" aria-hidden="true">
                      ${raw(disciplineIcon(event.disciplineIcon, 34))}
                    </span>
                    <div class="split__text">
                      <label class="split__title" for="share-${event.id}">${event.title}</label>
                      <span class="split__meta">
                        ${event.type === "EXAM" ? "Prova" : "Entrega"}
                        ${deadline(today, event.dueDate)} · faltam ${duration(left)}
                      </span>
                    </div>
                    ${stepper({
                      id: `share-${event.id}`,
                      name: `share-${event.id}`,
                      value: Math.round(share / STEP) * (STEP / 60),
                      min: 0,
                      max: maxShare(event, STEP) / 60,
                      step: STEP / 60,
                      unit: "h",
                    })}
                  </li>
                `;
              })}
            </ul>
          `,
        )}
        ${panel(
          { icon: "week", title: "Ritmo da semana", index: 2 },
          choiceCards({
            name: "spread",
            legend: "Ritmo da semana",
            value: plan.settings.spread ? "yes" : "no",
            options: [
              {
                value: "no",
                label: "Concentrar",
                hint: "Usa os primeiros horários livres",
                icon: "weightHigh",
              },
              {
                value: "yes",
                label: "Um pouco por dia",
                hint: "Espalha as sessões pelos dias",
                icon: "week",
                tone: "var(--mustard)",
              },
            ],
          }),
        )}
        ${formFooter("Refazer o plano")}
      </form>
    `,
    onMount(dialog, close) {
      const form = dialog.querySelector("form");
      const inputs = candidates.map((event) => form.elements[`share-${event.id}`]);
      const caps = candidates.map((event) => maxShare(event, STEP));
      const list = dialog.querySelector(".split__list");
      const tools = dialog.querySelector(".split__tools");
      const total = dialog.querySelector("[data-total]");
      const fill = dialog.querySelector("[data-fill]");
      const note = dialog.querySelector("[data-note]");

      const chosen = () => inputs.map((input) => Math.round((Number(input.value) || 0) * 60));
      const isManual = () => form.elements.mode.value === "manual";

      const describe = () => {
        const manualMode = isManual();
        list.classList.toggle("split__list--off", !manualMode);
        tools.hidden = !manualMode;
        inputs.forEach((input) => {
          input.disabled = !manualMode;
          input
            .closest("[data-stepper]")
            .querySelectorAll("button")
            .forEach((button) => (button.disabled = !manualMode));
        });
        const sum = chosen().reduce((a, b) => a + b, 0);
        total.textContent = manualMode
          ? `${duration(sum)} de ${duration(capacity)}`
          : `${duration(capacity)} livres`;
        const ratio = capacity ? Math.min(1, sum / capacity) : 0;
        fill.style.width = `${manualMode ? ratio * 100 : 100}%`;
        fill.classList.toggle("split__fill--over", manualMode && sum > capacity);
        note.textContent = !manualMode
          ? "O plano reparte o tempo pelos prazos, do mais próximo ao mais distante."
          : sum > capacity
            ? `Passou ${duration(sum - capacity)} do tempo livre. O que não couber aparece como aviso.`
            : `Sobram ${duration(capacity - sum)} livres na semana.`;
      };

      const apply = (minutes) => {
        inputs.forEach((input, index) => {
          input.value = minutes[index] / 60;
          bump(input.closest("[data-stepper]"));
        });
        describe();
      };

      enhanceSteppers(dialog, describe);
      form.addEventListener("change", describe);
      dialog.querySelector('[data-split="even"]').addEventListener("click", () => {
        apply(evenSplit(capacity, caps, STEP));
      });
      dialog.querySelector('[data-split="need"]').addEventListener("click", () => {
        apply(proportionalSplit(capacity, caps, STEP));
      });
      describe();

      form.addEventListener("submit", async (event) => {
        event.preventDefault();
        const values = formValues(form);
        const minutes = chosen();
        const body = {
          spread: values.spread === "yes",
          allocations: isManual()
            ? candidates.map((item, index) => ({ eventId: item.id, minutes: minutes[index] }))
            : [],
        };
        const button = form.querySelector('[type="submit"]');
        try {
          await withBusy(button, async () =>
            acceptPlan(await api.plan.settings(store.weekDate ?? undefined, body)),
          );
          close();
          toast(
            body.allocations.length ? "Tempo dividido. Plano refeito." : "Plano refeito.",
            "success",
          );
          onSaved();
        } catch (error) {
          toast(error.message, "error");
        }
      });
    },
  });
}

/** Days of the shown week a session can move to: not in the past and not after its deadline. */
export function moveTargets(block, plan) {
  const last = lastStudyDay({ type: block.eventType, dueDate: block.dueDate });
  return WEEKDAYS.map((weekday, index) => ({
    weekday,
    date: addDays(plan.weekStart, index),
  })).filter((day) => day.date >= plan.today && day.date <= last);
}

/** Moves one study session. The exam or assignment keeps its real date. */
export function openMoveDialog(block, plan, onSaved) {
  const days = moveTargets(block, plan);
  const end = (start) => {
    const minutes = minutesOfDay(start) + block.minutes;
    return `${String(Math.floor(minutes / 60)).padStart(2, "0")}:${String(minutes % 60).padStart(2, "0")}`;
  };

  modal({
    title: "Mover sessão",
    subtitle: `${block.eventTitle} · ${duration(block.minutes)}`,
    art: disciplineIcon(block.disciplineIcon, 30),
    tone: "mustard",
    body: html`
      <form class="form" novalidate>
        ${panel(
          { icon: "week", title: "Dia", index: 0 },
          days.length
            ? choiceCards({
                name: "date",
                legend: "Dia da sessão",
                layout: "days",
                value: days.some((day) => day.date === block.date) ? block.date : days[0].date,
                options: days.map((day) => ({
                  value: day.date,
                  label: day.weekday.short,
                  hint: dayMonth(day.date),
                })),
              })
            : html`<p class="card__hint">Não há outro dia possível antes do prazo.</p>`,
        )}
        ${panel(
          {
            icon: "availability",
            title: "Começa às",
            hint: html`<span data-end></span>`,
            index: 1,
          },
          html`
            <div class="field">
              <label class="visually-hidden" for="move-start">Horário de início</label>
              <input
                id="move-start"
                class="input"
                name="startTime"
                type="time"
                step="300"
                required
                value="${time(block.startTime)}"
              />
            </div>
            ${chips(
              [
                { value: "08:00", label: "Manhã" },
                { value: "14:00", label: "Tarde" },
                { value: "19:00", label: "Noite" },
              ],
              "start",
            )}
          `,
        )}
        <p class="move__note">
          ${raw(icon("events", 16))}
          <span>
            ${block.eventType === "EXAM" ? "A prova" : "A entrega"} continua em
            <strong>${dayMonth(block.dueDate)}</strong>. Só a sessão de estudo muda de lugar e fica
            travada no novo horário.
          </span>
        </p>
        ${formFooter("Mover sessão")}
      </form>
    `,
    onMount(dialog, close) {
      const form = dialog.querySelector("form");
      const start = form.elements.startTime;
      const endLabel = dialog.querySelector("[data-end]");
      const describe = () => {
        endLabel.textContent = start.value ? `termina às ${end(start.value)}` : "";
      };
      dialog.querySelector('[data-chips="start"]').addEventListener("click", (event) => {
        const chip = event.target.closest("[data-chip]");
        if (!chip) return;
        start.value = chip.dataset.chip;
        bump(start);
        describe();
      });
      start.addEventListener("input", describe);
      describe();
      if (!days.length) form.querySelector('[type="submit"]').disabled = true;

      form.addEventListener("submit", async (event) => {
        event.preventDefault();
        const values = formValues(form);
        const button = form.querySelector('[type="submit"]');
        try {
          await withBusy(button, async () =>
            acceptPlan(await api.blocks.move(block.id, values.date, values.startTime)),
          );
          close();
          toast("Sessão movida. O prazo continua o mesmo.", "success");
          onSaved();
        } catch (error) {
          toast(error.message, "error");
        }
      });
    },
  });
}
