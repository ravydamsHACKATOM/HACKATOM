/** Weekly windows in which the student can really study. */

import { api } from "../api.js";
import { formValues, html, modal, mount, raw, toast } from "../dom.js";
import { duration, time, WEEKDAYS } from "../format.js";
import { icon } from "../icons.js";
import { emptyState } from "./today.js";

const minutesOf = (window) => {
  const [startHour, startMinute] = window.startTime.split(":").map(Number);
  const [endHour, endMinute] = window.endTime.split(":").map(Number);
  return endHour * 60 + endMinute - (startHour * 60 + startMinute);
};

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
      button.addEventListener("click", () => openForm(context, button.dataset.new || "MONDAY")),
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
    body: html`
      <form class="form" novalidate>
        <div class="field">
          <label for="window-day">Dia da semana</label>
          <select id="window-day" class="input" name="dayOfWeek" autofocus>
            ${WEEKDAYS.map(
              (weekday) =>
                html`<option value="${weekday.key}" ${weekday.key === day ? raw("selected") : ""}>
                  ${weekday.long}
                </option>`,
            )}
          </select>
        </div>
        <div class="field-row">
          <div class="field">
            <label for="window-start">Das</label>
            <input
              id="window-start"
              class="input"
              name="startTime"
              type="time"
              value="19:00"
              required
            />
          </div>
          <div class="field">
            <label for="window-end">Até</label>
            <input
              id="window-end"
              class="input"
              name="endTime"
              type="time"
              value="22:00"
              required
            />
          </div>
        </div>
        <p class="field__hint">
          O plano usa sessões de até 50 minutos com 10 de pausa. O mínimo é 25 minutos.
        </p>
        <div class="form-actions">
          <button class="button button--ghost" type="button" data-close>Cancelar</button>
          <button class="button button--primary" type="submit">Adicionar</button>
        </div>
      </form>
    `,
    onMount(dialog, close) {
      const form = dialog.querySelector("form");
      form.addEventListener("submit", async (event) => {
        event.preventDefault();
        if (!form.reportValidity()) return;
        const values = formValues(form);
        if (values.endTime <= values.startTime) {
          toast("O horário final precisa ser depois do inicial.", "error");
          return;
        }
        try {
          await api.availability.create(values);
          close();
          toast("Horário adicionado.", "success");
          await context.refresh();
        } catch (error) {
          toast(
            error.code === "VALIDATION_FAILED"
              ? "O horário precisa ter pelo menos 25 minutos."
              : error.message,
            "error",
          );
        }
      });
    },
  });
}
