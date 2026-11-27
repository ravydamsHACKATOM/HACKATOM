/** Exams and assignments: what the plan is built from. */

import { api, ApiError } from "../api.js";
import {
  confirmDialog,
  formValues,
  html,
  modal,
  mount,
  raw,
  showFieldErrors,
  toast,
} from "../dom.js";
import { deadline, duration, longDate } from "../format.js";
import { disciplineIcon, icon } from "../icons.js";
import { emptyState, progressBar } from "./today.js";

export function renderEvents(main, context) {
  const { store } = context;
  const today = store.plan.today;
  const upcoming = store.events.filter((event) => event.dueDate >= today);
  const past = store.events.filter((event) => event.dueDate < today);
  const canCreate = store.disciplines.length > 0;
  // Events the current plan could not fit before their deadline.
  const late = new Set(store.plan.warnings.map((warning) => warning.eventId));

  mount(
    main,
    html`
      <header class="page-header rise">
        <div>
          <p class="eyebrow">O que vem aí</p>
          <h1>Provas e entregas</h1>
        </div>
        ${
          canCreate
            ? html`<button class="button button--primary" type="button" data-new>
                ${raw(icon("plus", 18))} Novo prazo
              </button>`
            : ""
        }
      </header>

      ${
        !canCreate
          ? emptyState({
              image: "empty-events",
              title: "Primeiro, as disciplinas",
              text: "Cada prova ou entrega pertence a uma disciplina. Cadastre ao menos uma para continuar.",
              action: html`<a class="button button--primary" href="#/disciplinas"
                >Cadastrar disciplina</a
              >`,
            })
          : !store.events.length
            ? emptyState({
                image: "empty-events",
                title: "Nenhum prazo por enquanto",
                text: "Adicione a próxima prova ou entrega e diga quantas horas de estudo ela pede.",
                action: html`<button class="button button--primary" type="button" data-new>
                  ${raw(icon("plus", 18))} Novo prazo
                </button>`,
              })
            : html`
                <ul class="event-list">
                  ${upcoming.map((event, index) => eventCard(event, today, index + 1, late))}
                </ul>
                ${
                  past.length
                    ? html`
                        <details class="past rise" style="--i:${upcoming.length + 1}">
                          <summary>Encerrados (${past.length})</summary>
                          <ul class="event-list event-list--past">
                            ${past.map((event) => eventCard(event, today, 0))}
                          </ul>
                        </details>
                      `
                    : ""
                }
              `
      }
    `,
  );

  main
    .querySelectorAll("[data-new]")
    .forEach((button) => button.addEventListener("click", () => openForm(context)));
  main.querySelectorAll("[data-edit]").forEach((button) =>
    button.addEventListener("click", () =>
      openForm(
        context,
        store.events.find((event) => event.id === Number(button.dataset.edit)),
      ),
    ),
  );
  main.querySelectorAll("[data-delete]").forEach((button) =>
    button.addEventListener("click", async () => {
      const event = store.events.find((item) => item.id === Number(button.dataset.delete));
      const confirmed = await confirmDialog({
        title: "Excluir prazo?",
        message: `"${event.title}" e os blocos de estudo dele serão removidos.`,
        confirmLabel: "Excluir",
        danger: true,
      });
      if (confirmed) {
        await context.run(async () => {
          await api.events.remove(event.id);
          await context.refresh();
        }, "Prazo excluído.");
      }
    }),
  );
}

function eventCard(event, today, index, late = new Set()) {
  const missing = Math.max(0, event.effortMinutes - event.doneMinutes - event.plannedMinutes);
  return html`
    <li class="card event rise" style="--i:${index}; --tone:${event.disciplineColor}">
      ${raw(disciplineIcon(event.disciplineIcon, 52))}
      <div class="event__body">
        <p class="event__meta">
          <span class="dot" aria-hidden="true"></span>${event.disciplineName}
          <span class="tag ${event.type === "EXAM" ? "tag--terracotta" : "tag--blue"}"
            >${event.type === "EXAM" ? "Prova" : "Entrega"}</span
          >
        </p>
        <h2 class="event__title">${event.title}</h2>
        <p class="event__date">${longDate(event.dueDate)} · ${deadline(today, event.dueDate)}</p>
        ${progressBar(event)}
        <p class="event__progress">
          ${duration(event.doneMinutes)} feitos · ${duration(event.plannedMinutes)} planejados
          ${
            missing > 0 && event.dueDate >= today
              ? late.has(event.id)
                ? html`·
                    <span class="event__missing event__missing--late"
                      >${duration(missing)} sem espaço</span
                    >`
                : html`· <span class="event__missing">${duration(missing)} a planejar</span>`
              : ""
          }
          · estimativa de ${duration(event.effortMinutes)}
        </p>
        ${event.notes ? html`<p class="event__notes">${event.notes}</p>` : ""}
      </div>
      <div class="event__actions">
        <button
          class="icon-button"
          type="button"
          data-edit="${event.id}"
          aria-label="Editar ${event.title}"
        >
          ${raw(icon("edit", 18))}
        </button>
        <button
          class="icon-button"
          type="button"
          data-delete="${event.id}"
          aria-label="Excluir ${event.title}"
        >
          ${raw(icon("trash", 18))}
        </button>
      </div>
    </li>
  `;
}

function openForm(context, event) {
  const { store } = context;
  const editing = Boolean(event);
  modal({
    title: editing ? "Editar prazo" : "Novo prazo",
    body: html`
      <form class="form" novalidate>
        <div class="field">
          <label for="event-title">O que é?</label>
          <input
            id="event-title"
            class="input"
            name="title"
            type="text"
            maxlength="80"
            required
            autofocus
            placeholder="Ex.: Prova de SQL"
            value="${event?.title ?? ""}"
          />
        </div>
        <div class="field">
          <label for="event-discipline">Disciplina</label>
          <select id="event-discipline" class="input" name="disciplineId" required>
            ${store.disciplines.map(
              (discipline) =>
                html`<option
                  value="${discipline.id}"
                  ${event?.disciplineId === discipline.id ? raw("selected") : ""}
                >
                  ${discipline.name}
                </option>`,
            )}
          </select>
        </div>
        <fieldset class="field">
          <legend>Tipo</legend>
          <div class="segmented">
            <label>
              <input
                type="radio"
                name="type"
                value="EXAM"
                ${event?.type !== "ASSIGNMENT" ? raw("checked") : ""}
              />
              <span>Prova</span>
            </label>
            <label>
              <input
                type="radio"
                name="type"
                value="ASSIGNMENT"
                ${event?.type === "ASSIGNMENT" ? raw("checked") : ""}
              />
              <span>Entrega</span>
            </label>
          </div>
          <p class="field__hint">
            Para provas o estudo termina na véspera. Entregas podem usar o próprio dia.
          </p>
        </fieldset>
        <div class="field-row">
          <div class="field">
            <label for="event-date">Data</label>
            <input
              id="event-date"
              class="input"
              name="dueDate"
              type="date"
              required
              value="${event?.dueDate ?? ""}"
            />
          </div>
          <div class="field">
            <label for="event-effort">Horas de estudo</label>
            <input
              id="event-effort"
              class="input"
              name="effortHours"
              type="number"
              min="0.5"
              max="80"
              step="0.5"
              required
              inputmode="decimal"
              value="${event ? event.effortMinutes / 60 : 2}"
            />
          </div>
        </div>
        <div class="field">
          <label for="event-notes">Anotações <span class="field__optional">opcional</span></label>
          <textarea id="event-notes" class="input" name="notes" rows="2" maxlength="500">
${event?.notes ?? ""}</textarea>
        </div>
        <div class="form-actions">
          <button class="button button--ghost" type="button" data-close>Cancelar</button>
          <button class="button button--primary" type="submit">
            ${editing ? "Salvar" : "Adicionar"}
          </button>
        </div>
      </form>
    `,
    onMount(dialog, close) {
      const form = dialog.querySelector("form");
      form.addEventListener("submit", async (submitEvent) => {
        submitEvent.preventDefault();
        if (!form.reportValidity()) return;
        const values = formValues(form);
        const payload = {
          title: values.title,
          disciplineId: Number(values.disciplineId),
          type: values.type,
          dueDate: values.dueDate,
          effortHours: Number(values.effortHours),
          notes: values.notes || null,
        };
        try {
          if (editing) await api.events.update(event.id, payload);
          else await api.events.create(payload);
          close();
          toast(editing ? "Prazo atualizado." : "Prazo adicionado.", "success");
          await context.refresh();
        } catch (error) {
          if (error instanceof ApiError && error.code === "VALIDATION_FAILED") {
            showFieldErrors(form, error.fields);
          }
          toast(error.message, "error");
        }
      });
    },
  });
}
