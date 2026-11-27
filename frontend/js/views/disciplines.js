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
  withBusy,
} from "../dom.js";
import { plural } from "../format.js";
import { choiceCards, formFooter, panel } from "../forms.js";
import { DISCIPLINE_COLORS, DISCIPLINE_ICONS, disciplineIcon, icon } from "../icons.js";
import { emptyState } from "./today.js";

const WEIGHTS = [
  { value: 1, label: "Leve", hint: "Cede a vez no empate.", icon: "weightLow" },
  { value: 2, label: "Normal", hint: "Sem preferência.", icon: "weightMid" },
  { value: 3, label: "Pesada", hint: "Vem primeiro no empate.", icon: "weightHigh" },
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

  modal({
    title: editing ? "Editar disciplina" : "Nova disciplina",
    subtitle: "O ícone e a cor ajudam a reconhecer a matéria no plano.",
    art: html`<span class="modal__preview" data-preview style="--tone:${selectedColor}">
      ${raw(disciplineIcon(selectedIcon, 34))}
    </span>`.value,
    tone: "accent",
    body: html`
      <form class="form" novalidate>
        ${panel(
          { icon: "edit", title: "Nome", index: 0 },
          html`
            <div class="field">
              <label class="visually-hidden" for="discipline-name">Nome da disciplina</label>
              <input
                id="discipline-name"
                class="input"
                name="name"
                type="text"
                maxlength="60"
                required
                autofocus
                autocomplete="off"
                placeholder="Ex.: Banco de Dados"
                value="${discipline?.name ?? ""}"
              />
            </div>
          `,
        )}
        ${panel(
          { icon: "sparkle", title: "Ícone", index: 1 },
          html`
            <fieldset class="picker picker--icons">
              <legend class="visually-hidden">Ícone da disciplina</legend>
              ${DISCIPLINE_ICONS.map(
                (option) => html`
                  <label title="${option.label}">
                    <input
                      type="radio"
                      name="icon"
                      value="${option.key}"
                      ${option.key === selectedIcon ? raw("checked") : ""}
                    />
                    <span class="picker__tile">${raw(disciplineIcon(option.key, 38))}</span>
                    <span class="visually-hidden">${option.label}</span>
                  </label>
                `,
              )}
            </fieldset>
          `,
        )}
        ${panel(
          { icon: "palette", title: "Cor", index: 2 },
          html`
            <fieldset class="picker picker--colors">
              <legend class="visually-hidden">Cor da disciplina</legend>
              ${DISCIPLINE_COLORS.map(
                (color) => html`
                  <label>
                    <input
                      type="radio"
                      name="color"
                      value="${color}"
                      ${color === selectedColor ? raw("checked") : ""}
                    />
                    <span class="picker__swatch" style="--swatch:${color}">
                      ${raw(icon("check", 14))}
                    </span>
                    <span class="visually-hidden">Cor ${color}</span>
                  </label>
                `,
              )}
            </fieldset>
          `,
        )}
        ${panel(
          { icon: "weightHigh", title: "Peso", hint: "desempata prazos no mesmo dia", index: 3 },
          choiceCards({
            name: "weight",
            legend: "Peso da disciplina",
            options: WEIGHTS,
            value: discipline?.weight ?? 2,
            layout: "stack",
          }),
        )}
        ${formFooter(editing ? "Salvar" : "Adicionar disciplina")}
      </form>
    `,
    onMount(dialog, close) {
      const form = dialog.querySelector("form");
      const preview = dialog.querySelector("[data-preview]");

      // The header badge previews the chosen icon and color as the student picks them.
      form.addEventListener("change", (event) => {
        if (event.target.name === "icon") {
          preview.innerHTML = disciplineIcon(event.target.value, 34);
        } else if (event.target.name === "color") {
          preview.style.setProperty("--tone", event.target.value);
        } else {
          return;
        }
        preview.classList.remove("is-bumped");
        void preview.offsetWidth;
        preview.classList.add("is-bumped");
      });

      form.addEventListener("submit", async (event) => {
        event.preventDefault();
        const values = formValues(form);
        if (!values.name.trim()) {
          showFieldErrors(form, { name: "Dê um nome para a disciplina." });
          return;
        }
        showFieldErrors(form, {});
        const payload = {
          name: values.name,
          icon: values.icon,
          color: values.color,
          weight: Number(values.weight),
        };
        await withBusy(form.querySelector('[type="submit"]'), async () => {
          try {
            if (editing) await api.disciplines.update(discipline.id, payload);
            else await api.disciplines.create(payload);
            close();
            toast(editing ? "Disciplina atualizada." : "Disciplina adicionada.", "success");
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
