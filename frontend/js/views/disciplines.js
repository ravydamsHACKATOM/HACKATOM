/** Subjects the student is taking, each with its own color, icon and weight. */

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
import { plural } from "../format.js";
import { DISCIPLINE_COLORS, DISCIPLINE_ICONS, disciplineIcon, icon } from "../icons.js";
import { emptyState } from "./today.js";

const WEIGHTS = [
  { value: 1, label: "Leve" },
  { value: 2, label: "Normal" },
  { value: 3, label: "Pesada" },
];

export function renderDisciplines(main, context) {
  const { store } = context;
  const newButton = html`<button class="button button--primary" type="button" data-new>
    ${raw(icon("plus", 18))} Nova disciplina
  </button>`;

  mount(
    main,
    html`
      <header class="page-header rise">
        <div>
          <p class="eyebrow">O que você estuda</p>
          <h1>Disciplinas</h1>
        </div>
        ${store.disciplines.length ? newButton : ""}
      </header>

      ${
        !store.disciplines.length
          ? emptyState({
              image: "empty-events",
              title: "Nenhuma disciplina ainda",
              text: "Comece pelas matérias deste módulo. Cada uma ganha uma cor para você reconhecer no plano.",
              action: newButton,
            })
          : html`
              <ul class="discipline-grid">
                ${store.disciplines.map((discipline, index) => {
                  const events = store.events.filter(
                    (event) => event.disciplineId === discipline.id,
                  );
                  return html`
                    <li
                      class="card discipline rise"
                      style="--i:${index + 1}; --tone:${discipline.color}"
                    >
                      <div class="discipline__art">${raw(disciplineIcon(discipline.icon, 72))}</div>
                      <h2>${discipline.name}</h2>
                      <p class="discipline__meta">
                        ${WEIGHTS.find((weight) => weight.value === discipline.weight).label} ·
                        ${plural(events.length, "prazo", "prazos")}
                      </p>
                      <div class="discipline__actions">
                        <button
                          class="icon-button"
                          type="button"
                          data-edit="${discipline.id}"
                          aria-label="Editar ${discipline.name}"
                        >
                          ${raw(icon("edit", 18))}
                        </button>
                        <button
                          class="icon-button"
                          type="button"
                          data-delete="${discipline.id}"
                          aria-label="Excluir ${discipline.name}"
                        >
                          ${raw(icon("trash", 18))}
                        </button>
                      </div>
                    </li>
                  `;
                })}
              </ul>
            `
      }
    `,
  );

  const find = (id) => store.disciplines.find((discipline) => discipline.id === Number(id));
  main
    .querySelectorAll("[data-new]")
    .forEach((button) => button.addEventListener("click", () => openForm(context)));
  main
    .querySelectorAll("[data-edit]")
    .forEach((button) =>
      button.addEventListener("click", () => openForm(context, find(button.dataset.edit))),
    );
  main.querySelectorAll("[data-delete]").forEach((button) =>
    button.addEventListener("click", async () => {
      const discipline = find(button.dataset.delete);
      const count = store.events.filter((event) => event.disciplineId === discipline.id).length;
      const confirmed = await confirmDialog({
        title: "Excluir disciplina?",
        message: count
          ? `"${discipline.name}" tem ${plural(count, "prazo", "prazos")}. Eles e os blocos de estudo serão removidos juntos.`
          : `"${discipline.name}" será removida.`,
        confirmLabel: "Excluir",
        danger: true,
      });
      if (confirmed) {
        await context.run(async () => {
          await api.disciplines.remove(discipline.id);
          await context.refresh();
        }, "Disciplina excluída.");
      }
    }),
  );
}

function openForm(context, discipline) {
  const editing = Boolean(discipline);
  const selectedIcon = discipline?.icon ?? "book";
  const selectedColor = (discipline?.color ?? DISCIPLINE_COLORS[0]).toUpperCase();
  const selectedWeight = discipline?.weight ?? 2;

  modal({
    title: editing ? "Editar disciplina" : "Nova disciplina",
    body: html`
      <form class="form" novalidate>
        <div class="field">
          <label for="discipline-name">Nome</label>
          <input
            id="discipline-name"
            class="input"
            name="name"
            type="text"
            maxlength="60"
            required
            autofocus
            placeholder="Ex.: Banco de Dados"
            value="${discipline?.name ?? ""}"
          />
        </div>
        <fieldset class="field">
          <legend>Ícone</legend>
          <div class="picker picker--icons">
            ${DISCIPLINE_ICONS.map(
              (option) => html`
                <label title="${option.label}">
                  <input
                    type="radio"
                    name="icon"
                    value="${option.key}"
                    ${option.key === selectedIcon ? raw("checked") : ""}
                  />
                  <span>${raw(disciplineIcon(option.key, 40))}</span>
                  <span class="visually-hidden">${option.label}</span>
                </label>
              `,
            )}
          </div>
        </fieldset>
        <fieldset class="field">
          <legend>Cor</legend>
          <div class="picker picker--colors">
            ${DISCIPLINE_COLORS.map(
              (color) => html`
                <label>
                  <input
                    type="radio"
                    name="color"
                    value="${color}"
                    ${color === selectedColor ? raw("checked") : ""}
                  />
                  <span style="--swatch:${color}"></span>
                  <span class="visually-hidden">Cor ${color}</span>
                </label>
              `,
            )}
          </div>
        </fieldset>
        <fieldset class="field">
          <legend>Peso</legend>
          <div class="segmented">
            ${WEIGHTS.map(
              (weight) => html`
                <label>
                  <input
                    type="radio"
                    name="weight"
                    value="${weight.value}"
                    ${weight.value === selectedWeight ? raw("checked") : ""}
                  />
                  <span>${weight.label}</span>
                </label>
              `,
            )}
          </div>
          <p class="field__hint">
            Quando dois prazos caem no mesmo dia, a mais pesada vem primeiro.
          </p>
        </fieldset>
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
      form.addEventListener("submit", async (event) => {
        event.preventDefault();
        if (!form.reportValidity()) return;
        const values = formValues(form);
        const payload = {
          name: values.name,
          icon: values.icon,
          color: values.color,
          weight: Number(values.weight),
        };
        try {
          if (editing) await api.disciplines.update(discipline.id, payload);
          else await api.disciplines.create(payload);
          close();
          toast(editing ? "Disciplina atualizada." : "Disciplina adicionada.", "success");
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
