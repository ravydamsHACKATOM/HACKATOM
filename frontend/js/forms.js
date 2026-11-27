/** Building blocks of the forms shown inside dialogs: panels, choice cards, steppers and chips. */

import { html, raw } from "./dom.js";
import { icon } from "./icons.js";

/**
 * A framed section of a form with its own small header. Dialogs are made of a few of these
 * instead of a flat list of inputs, so each decision has a clear place.
 */
export function panel({ icon: iconName, title, hint, index = 0 }, body) {
  return html`
    <section class="panel" style="--i:${index}">
      <header class="panel__header">
        <span class="panel__icon" aria-hidden="true">${raw(icon(iconName, 16))}</span>
        <h3>${title}</h3>
        ${hint ? html`<span class="panel__hint">${hint}</span>` : ""}
      </header>
      ${body}
    </section>
  `;
}

/**
 * A group of selectable cards backed by radio inputs (or checkboxes when `multiple`).
 *
 * @param {Array<{value: string|number, label: string, hint?: string, icon?: string, art?: string,
 *     tone?: string}>} options
 */
export function choiceCards({ name, legend, options, value, multiple = false, layout = "row" }) {
  const selected = new Set([].concat(value ?? []).map(String));
  return html`
    <fieldset class="choices choices--${layout}">
      <legend class="visually-hidden">${legend}</legend>
      ${options.map(
        (option) => html`
          <label class="choice" style="--tone:${option.tone ?? "var(--accent)"}">
            <input
              type="${multiple ? "checkbox" : "radio"}"
              name="${name}"
              value="${option.value}"
              ${selected.has(String(option.value)) ? raw("checked") : ""}
            />
            <span class="choice__card">
              ${
                option.art || option.icon
                  ? html`<span class="choice__icon" aria-hidden="true">
                      ${option.art ? raw(option.art) : raw(icon(option.icon, 20))}
                    </span>`
                  : ""
              }
              <span class="choice__text">
                <span class="choice__label">${option.label}</span>
                ${option.hint ? html`<span class="choice__hint">${option.hint}</span>` : ""}
              </span>
              <span class="choice__check" aria-hidden="true">${raw(icon("check", 14))}</span>
            </span>
          </label>
        `,
      )}
    </fieldset>
  `;
}

/** Quick-pick buttons that fill another control. The caller wires `data-chip` clicks. */
export function chips(items, group) {
  return html`
    <div class="chips" data-chips="${group}">
      ${items.map(
        (item) =>
          html`<button class="chip-button" type="button" data-chip="${item.value}">
            ${item.label}
          </button>`,
      )}
    </div>
  `;
}

/** A number input with large minus and plus buttons. */
export function stepper({ id, name, value, min, max, step, unit }) {
  return html`
    <div class="stepper" data-stepper>
      <button class="stepper__button" type="button" data-step="-1" aria-label="Diminuir">
        ${raw(icon("minus", 18))}
      </button>
      <div class="stepper__value">
        <input
          id="${id}"
          name="${name}"
          type="number"
          inputmode="decimal"
          min="${min}"
          max="${max}"
          step="${step}"
          value="${value}"
          required
        />
        <span>${unit}</span>
      </div>
      <button class="stepper__button" type="button" data-step="1" aria-label="Aumentar">
        ${raw(icon("plus", 18))}
      </button>
    </div>
  `;
}

/** Wires the steppers inside `root`. `onChange` runs after every change. */
export function enhanceSteppers(root, onChange = () => {}) {
  root.querySelectorAll("[data-stepper]").forEach((stepperElement) => {
    const input = stepperElement.querySelector("input");
    const step = Number(input.step) || 1;
    const clamp = (number) =>
      Math.min(Number(input.max), Math.max(Number(input.min), Math.round(number / step) * step));

    stepperElement.addEventListener("click", (event) => {
      const button = event.target.closest("[data-step]");
      if (!button) return;
      input.value = clamp((Number(input.value) || 0) + Number(button.dataset.step) * step);
      bump(stepperElement);
      onChange();
    });
    input.addEventListener("input", onChange);
  });
}

/** Replays the small "value changed" animation of a control. */
export function bump(element) {
  element.classList.remove("is-bumped");
  void element.offsetWidth;
  element.classList.add("is-bumped");
}

/** The buttons at the bottom of a dialog form. */
export function formFooter(submitLabel) {
  return html`
    <footer class="modal__footer">
      <button class="button button--ghost" type="button" data-close>Cancelar</button>
      <button class="button button--primary" type="submit">
        <span class="button__label">${submitLabel}</span>
        <span class="button__spinner" aria-hidden="true"></span>
      </button>
    </footer>
  `;
}
