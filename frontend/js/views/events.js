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
  withBusy,
} from "../dom.js";
import { addDays, deadline, duration, longDate, plural } from "../format.js";
import { bump, chips, choiceCards, enhanceSteppers, formFooter, panel, stepper } from "../forms.js";
import { disciplineIcon, icon } from "../icons.js";
import { enhanceSelects, richSelect } from "../select.js";
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

const TYPES = [
  {
    value: "EXAM",
    label: "Prova",
    hint: "O estudo termina na véspera.",
    icon: "exam",
    tone: "var(--terracotta)",
  },
  {
    value: "ASSIGNMENT",
    label: "Entrega",
    hint: "Dá para trabalhar até o próprio dia.",
    icon: "assignment",
    tone: "var(--blue)",
  },
];

function openForm(context, event) {
  const { store } = context;
  const today = store.plan.today;
  const editing = Boolean(event);
  const disciplineOptions = store.disciplines.map((discipline) => {
    const count = store.events.filter((item) => item.disciplineId === discipline.id).length;
    return {
      value: discipline.id,
      label: discipline.name,
      hint: count ? plural(count, "prazo cadastrado", "prazos cadastrados") : "Nenhum prazo ainda",
      art: disciplineIcon(discipline.icon, 30),
      tone: discipline.color,
    };
  });

  modal({
    title: editing ? "Editar prazo" : "Novo prazo",
    subtitle: editing
      ? "Mudou a data ou a estimativa? O plano se ajusta depois."
      : "Uma prova ou entrega que você precisa se preparar.",
    icon: "events",
    tone: "terracotta",
    body: html`
      <form class="form" novalidate>
        ${panel(
          { icon: "edit", title: "O que é", index: 0 },
          html`
            <div class="field">
              <label for="event-title">Nome</label>
              <input
                id="event-title"
                class="input"
                name="title"
                type="text"
                maxlength="80"
                required
                autofocus
                autocomplete="off"
                placeholder="Ex.: Prova de SQL"
                value="${event?.title ?? ""}"
              />
            </div>
            <div class="field">
              <label for="event-discipline">Disciplina</label>
              ${richSelect({
                name: "disciplineId",
                id: "event-discipline",
                options: disciplineOptions,
                value: event?.disciplineId ?? disciplineOptions[0].value,
              })}
            </div>
          `,
        )}
        ${panel(
          { icon: "events", title: "Tipo", index: 1 },
          choiceCards({
            name: "type",
            legend: "Tipo de prazo",
            options: TYPES,
            value: event?.type ?? "EXAM",
          }),
        )}
        ${panel(
          { icon: "week", title: "Quando", hint: html`<span data-when></span>`, index: 2 },
          html`
            <div class="field">
              <label class="visually-hidden" for="event-date">Data</label>
              <input
                id="event-date"
                class="input"
                name="dueDate"
                type="date"
                required
                value="${event?.dueDate ?? addDays(today, 7)}"
              />
            </div>
            ${chips(
              [
                { value: 1, label: "Amanhã" },
                { value: 3, label: "Em 3 dias" },
                { value: 7, label: "Em 1 semana" },
                { value: 14, label: "Em 2 semanas" },
              ],
              "date",
            )}
          `,
        )}
        ${panel(
          {
            icon: "availability",
            title: "Quanto estudo ela pede",
            hint: html`<span data-sessions></span>`,
            index: 3,
          },
          html`
            <div class="field">
              <label class="visually-hidden" for="event-effort">Horas de estudo</label>
              ${stepper({
                id: "event-effort",
                name: "effortHours",
                value: event ? event.effortMinutes / 60 : 2,
                min: 0.5,
                max: 80,
                step: 0.5,
                unit: "horas",
              })}
            </div>
            ${chips(
              [
                { value: 1, label: "1h" },
                { value: 2, label: "2h" },
                { value: 4, label: "4h" },
                { value: 8, label: "8h" },
              ],
              "effort",
            )}
          `,
        )}
        ${panel(
          { icon: "note", title: "Anotações", hint: "opcional", index: 4 },
          html`
            <div class="field">
              <label class="visually-hidden" for="event-notes">Anotações</label>
              <textarea
                id="event-notes"
                class="input"
                name="notes"
                rows="2"
                maxlength="500"
                placeholder="Capítulos, sala, o que não pode esquecer…"
              >
${event?.notes ?? ""}</textarea>
            </div>
          `,
        )}
        ${formFooter(editing ? "Salvar" : "Adicionar prazo")}
      </form>
    `,
    onMount(dialog, close) {
      const form = dialog.querySelector("form");
      const date = form.elements.dueDate;
      const effort = form.elements.effortHours;
      const when = dialog.querySelector("[data-when]");
      const sessions = dialog.querySelector("[data-sessions]");

      const describe = () => {
        when.textContent = date.value ? deadline(today, date.value) : "";
        const minutes = Math.round((Number(effort.value) || 0) * 60);
        sessions.textContent = minutes
          ? `cerca de ${plural(Math.ceil(minutes / 50), "sessão", "sessões")}`
          : "";
      };

      enhanceSelects(dialog);
      enhanceSteppers(dialog, describe);
      date.addEventListener("input", describe);
      dialog.addEventListener("click", (clickEvent) => {
        const chip = clickEvent.target.closest("[data-chip]");
        if (!chip) return;
        const group = chip.closest("[data-chips]").dataset.chips;
        if (group === "date") {
          date.value = addDays(today, Number(chip.dataset.chip));
          bump(date);
        } else {
          effort.value = chip.dataset.chip;
          bump(effort.closest(".stepper"));
        }
        describe();
      });
      describe();

      form.addEventListener("submit", async (submitEvent) => {
        submitEvent.preventDefault();
        const values = formValues(form);
        const problems = {};
        if (!values.title.trim()) problems.title = "Dê um nome para o prazo.";
        if (!values.dueDate) problems.dueDate = "Escolha a data.";
        if (!(Number(values.effortHours) >= 0.5)) {
          problems.effortHours = "Informe pelo menos meia hora.";
        }
        showFieldErrors(form, problems);
        if (Object.keys(problems).length) return;

        const payload = {
          title: values.title,
          disciplineId: Number(values.disciplineId),
          type: values.type,
          dueDate: values.dueDate,
          effortHours: Number(values.effortHours),
          notes: values.notes.trim() || null,
        };
        await withBusy(form.querySelector('[type="submit"]'), async () => {
          try {
            if (editing) await api.events.update(event.id, payload);
            else await api.events.create(payload);
            close();
            toast(editing ? "Prazo atualizado." : "Prazo adicionado.", "success");
            await context.refresh();
          } catch (error) {
            if (error instanceof ApiError && error.code === "VALIDATION_FAILED") {
              showFieldErrors(
                form,
                Object.fromEntries(Object.keys(error.fields).map((name) => [name, ""])),
              );
            }
            toast(error.message, "error");
          }
        });
      });
    },
  });
}
