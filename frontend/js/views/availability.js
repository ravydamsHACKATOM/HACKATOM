/** Weekly windows in which the student can really study. */

import { api } from "../api.js";
import { formValues, html, modal, mount, raw, toast, withBusy } from "../dom.js";
import { duration, plural, time, WEEKDAYS } from "../format.js";
import { bump, chips, choiceCards, formFooter, panel } from "../forms.js";
import { icon } from "../icons.js";
import { emptyState } from "./today.js";

const toMinutes = (value) => {
  const [hour, minute] = value.split(":").map(Number);
  return hour * 60 + minute;
};

const minutesOf = (window) => toMinutes(window.endTime) - toMinutes(window.startTime);

const PRESETS = [
  { value: "08:00-12:00", label: "Manhã" },
  { value: "14:00-18:00", label: "Tarde" },
  { value: "19:00-22:00", label: "Noite" },
];

export function renderAvailability(main, context) {
  const { store } = context;
  const total = store.windows.reduce((sum, window) => sum + minutesOf(window), 0);

  mount(
    main,
    html`
      <header class="page-header rise">
        <div>
          <p class="eyebrow">Quando você pode</p>
          <h1>Horários livres</h1>
        </div>
        <button class="button button--primary" type="button" data-new>
          ${raw(icon("plus", 18))} Novo horário
        </button>
      </header>

      ${
        !store.windows.length
          ? emptyState({
              image: "empty-availability",
              title: "Quando dá para estudar?",
              text: "Informe só o tempo que é seu de verdade. Um plano honesto começa por aqui.",
              action: html`<button class="button button--ghost" type="button" data-preset>
                Usar noites de segunda a sexta, das 19h às 22h
              </button>`,
            })
          : html`
              <p class="availability__total rise" style="--i:1">
                Você tem <strong>${duration(total)}</strong> por semana para estudar.
              </p>
              <ol class="availability rise" style="--i:2">
                ${WEEKDAYS.map((weekday) => {
                  const windows = store.windows.filter(
                    (window) => window.dayOfWeek === weekday.key,
                  );
                  const minutes = windows.reduce((sum, window) => sum + minutesOf(window), 0);
                  return html`
                    <li class="availability__day">
                      <div class="availability__name">
                        <strong>${weekday.long}</strong>
                        <span>${minutes ? duration(minutes) : "sem horário"}</span>
                      </div>
                      <ul class="availability__windows">
                        ${windows.map(
                          (window) => html`
                            <li class="chip">
                              ${time(window.startTime)} – ${time(window.endTime)}
                              <button
                                type="button"
                                data-delete="${window.id}"
                                aria-label="Remover horário de ${weekday.long}, ${time(
                                  window.startTime,
                                )} às ${time(window.endTime)}"
                              >
                                ${raw(icon("close", 14))}
                              </button>
                            </li>
                          `,
                        )}
                        <li>
                          <button
                            class="chip chip--add"
                            type="button"
                            data-new="${weekday.key}"
                            aria-label="Adicionar horário em ${weekday.long}"
                          >
                            ${raw(icon("plus", 14))} Adicionar
                          </button>
                        </li>
                      </ul>
                    </li>
                  `;
                })}
              </ol>
            `
      }
    `,
  );

  main
    .querySelectorAll("[data-new]")
    .forEach((button) =>
      button.addEventListener("click", () => openForm(context, button.dataset.new)),
    );
  main.querySelector("[data-preset]")?.addEventListener("click", () =>
    context.run(async () => {
      await Promise.all(
        WEEKDAYS.slice(0, 5).map((weekday) =>
          api.availability.create({ dayOfWeek: weekday.key, startTime: "19:00", endTime: "22:00" }),
        ),
      );
      await context.refresh();
    }, "Horários adicionados."),
  );
  main.querySelectorAll("[data-delete]").forEach((button) =>
    button.addEventListener("click", () =>
      context.run(async () => {
        await api.availability.remove(Number(button.dataset.delete));
        await context.refresh();
      }, "Horário removido."),
    ),
  );
}

function openForm(context, day) {
  modal({
    title: "Novo horário livre",
    subtitle: "Escolha um ou mais dias e o intervalo em que você consegue estudar.",
    icon: "availability",
    tone: "mustard",
    body: html`
      <form class="form" novalidate>
        ${panel(
          { icon: "week", title: "Dias", hint: html`<span data-days></span>`, index: 0 },
          html`
            ${choiceCards({
              name: "days",
              legend: "Dias da semana",
              options: WEEKDAYS.map((weekday) => ({ value: weekday.key, label: weekday.short })),
              value: day ? [day] : [],
              multiple: true,
              layout: "days",
            })}
            ${chips(
              [
                { value: "weekdays", label: "Segunda a sexta" },
                { value: "weekend", label: "Fim de semana" },
                { value: "all", label: "Todos" },
              ],
              "days",
            )}
          `,
        )}
        ${panel(
          {
            icon: "availability",
            title: "Intervalo",
            hint: html`<span data-length></span>`,
            index: 1,
          },
          html`
            <div class="time-range">
              <div class="field">
                <label for="window-start">Das</label>
                <input
                  id="window-start"
                  class="input input--time"
                  name="startTime"
                  type="time"
                  value="19:00"
                  required
                />
              </div>
              <span class="time-range__arrow" aria-hidden="true">${raw(icon("arrow", 18))}</span>
              <div class="field">
                <label for="window-end">Até</label>
                <input
                  id="window-end"
                  class="input input--time"
                  name="endTime"
                  type="time"
                  value="22:00"
                  required
                />
              </div>
            </div>
            ${chips(PRESETS, "range")}
            <p class="form__error" data-error role="alert" hidden></p>
          `,
        )}
        ${formFooter("Adicionar horário")}
      </form>
    `,
    onMount(dialog, close) {
      const form = dialog.querySelector("form");
      const start = form.elements.startTime;
      const end = form.elements.endTime;
      const dayInputs = [...form.querySelectorAll('input[name="days"]')];
      const daysLabel = dialog.querySelector("[data-days]");
      const lengthLabel = dialog.querySelector("[data-length]");
      const errorLabel = dialog.querySelector("[data-error]");

      const fail = (message) => {
        errorLabel.textContent = message;
        errorLabel.hidden = !message;
      };

      const describe = () => {
        const count = dayInputs.filter((input) => input.checked).length;
        daysLabel.textContent = count ? plural(count, "dia", "dias") : "nenhum dia";
        const minutes =
          start.value && end.value ? toMinutes(end.value) - toMinutes(start.value) : 0;
        if (minutes >= 25) {
          const sessions = Math.floor((minutes + 10) / 60) || 1;
          lengthLabel.textContent = `${duration(minutes)} · ${plural(sessions, "sessão", "sessões")}`;
        } else {
          lengthLabel.textContent = "mínimo de 25 min";
        }
        fail("");
      };

      form.addEventListener("input", describe);
      dialog.addEventListener("click", (event) => {
        const chip = event.target.closest("[data-chip]");
        if (!chip) return;
        const { chip: value } = chip.dataset;
        if (chip.closest("[data-chips]").dataset.chips === "days") {
          dayInputs.forEach((input, index) => {
            input.checked = value === "all" || (value === "weekdays" ? index < 5 : index >= 5);
          });
        } else {
          [start.value, end.value] = value.split("-");
          bump(dialog.querySelector(".time-range"));
        }
        describe();
      });
      describe();

      form.addEventListener("submit", async (event) => {
        event.preventDefault();
        const values = formValues(form);
        const days = dayInputs.filter((input) => input.checked).map((input) => input.value);
        if (!days.length) return fail("Escolha pelo menos um dia.");
        if (!values.startTime || !values.endTime) return fail("Informe o início e o fim.");
        if (toMinutes(values.endTime) - toMinutes(values.startTime) < 25) {
          return fail("O horário precisa ter pelo menos 25 minutos.");
        }

        await withBusy(form.querySelector('[type="submit"]'), async () => {
          // Each day is saved on its own, so one overlapping day does not block the others.
          const results = await Promise.allSettled(
            days.map((dayOfWeek) =>
              api.availability.create({
                dayOfWeek,
                startTime: values.startTime,
                endTime: values.endTime,
              }),
            ),
          );
          const failed = results.filter((result) => result.status === "rejected");
          const saved = results.length - failed.length;
          if (saved) await context.refresh();
          if (!failed.length) {
            close();
            toast(
              saved === 1 ? "Horário adicionado." : `${saved} horários adicionados.`,
              "success",
            );
            return;
          }
          const overlapping = days
            .filter((_, index) => results[index].status === "rejected")
            .map((key) => WEEKDAYS.find((weekday) => weekday.key === key).long.toLowerCase());
          fail(
            failed[0].reason.code === "AVAILABILITY_OVERLAP"
              ? `Já existe um horário nesse intervalo em: ${overlapping.join(", ")}.`
              : failed[0].reason.message,
          );
          // Keep only the days that still need fixing selected.
          dayInputs.forEach((input) => {
            input.checked =
              days.includes(input.value) && overlapping.length > 0
                ? results[days.indexOf(input.value)].status === "rejected"
                : false;
          });
        });
      });
    },
  });
}
