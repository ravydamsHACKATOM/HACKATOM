/** Small DOM toolkit: safe templating, toasts, modals and confirmation dialogs. */

import { icon } from "./icons.js";

const ESCAPES = { "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" };

class Raw {
  constructor(value) {
    this.value = value;
  }
}

/** Marks a string as trusted markup (icons, nested templates). */
export const raw = (value) => new Raw(value);

function render(value) {
  if (value === null || value === undefined || value === false) return "";
  if (value instanceof Raw) return value.value;
  if (Array.isArray(value)) return value.map(render).join("");
  return String(value).replace(/[&<>"']/g, (char) => ESCAPES[char]);
}

/**
 * Tagged template that escapes every interpolated value. User text (titles, names, notes) can
 * never inject markup; only values wrapped in raw() or produced by another html`` are trusted.
 */
export function html(strings, ...values) {
  let out = strings[0];
  values.forEach((value, index) => {
    out += render(value) + strings[index + 1];
  });
  return new Raw(out);
}

export function mount(element, template) {
  element.innerHTML = render(template);
}

let toastTimer;

export function toast(message, kind = "info") {
  const root = document.getElementById("toast-root");
  mount(root, html`<div class="toast toast--${kind}">${message}</div>`);
  clearTimeout(toastTimer);
  toastTimer = setTimeout(() => {
    root.firstElementChild?.classList.add("toast--leaving");
    setTimeout(() => (root.innerHTML = ""), 260);
  }, 3600);
}

/**
 * Opens a modal dialog. `onMount` receives the dialog element and a close function.
 * Focus is moved in and restored on close; Escape and the backdrop close it.
 */
export function modal({ title, body, onMount }) {
  const root = document.getElementById("modal-root");
  const previousFocus = document.activeElement;
  mount(
    root,
    html`
      <div class="modal-backdrop" data-close>
        <div class="modal" role="dialog" aria-modal="true" aria-labelledby="modal-title">
          <header class="modal__header">
            <h2 id="modal-title">${title}</h2>
            <button class="icon-button" type="button" data-close aria-label="Fechar">
              ${raw(icon("close"))}
            </button>
          </header>
          <div class="modal__body">${body}</div>
        </div>
      </div>
    `,
  );

  const backdrop = root.firstElementChild;
  const dialog = backdrop.querySelector(".modal");

  function close() {
    document.removeEventListener("keydown", onKey);
    backdrop.classList.add("modal-backdrop--leaving");
    setTimeout(() => {
      root.innerHTML = "";
      previousFocus?.focus?.();
    }, 180);
  }

  function onKey(event) {
    if (event.key === "Escape") close();
  }

  backdrop.addEventListener("mousedown", (event) => {
    if (event.target === backdrop) close();
  });
  backdrop.addEventListener("click", (event) => {
    if (event.target.closest("button[data-close]")) close();
  });
  document.addEventListener("keydown", onKey);
  onMount?.(dialog, close);
  (dialog.querySelector("[autofocus]") ?? dialog.querySelector("input, select, button"))?.focus();
  return close;
}

export function confirmDialog({ title, message, confirmLabel = "Confirmar", danger = false }) {
  return new Promise((resolve) => {
    let answered = false;
    modal({
      title,
      body: html`
        <p class="modal__text">${message}</p>
        <div class="form-actions">
          <button class="button button--ghost" type="button" data-answer="no">Cancelar</button>
          <button
            class="button ${danger ? "button--danger" : "button--primary"}"
            type="button"
            data-answer="yes"
            autofocus
          >
            ${confirmLabel}
          </button>
        </div>
      `,
      onMount(dialog, close) {
        dialog.addEventListener("click", (event) => {
          const answer = event.target.closest("[data-answer]")?.dataset.answer;
          if (!answer) return;
          answered = true;
          resolve(answer === "yes");
          close();
        });
        const observer = new MutationObserver(() => {
          if (!document.body.contains(dialog)) {
            observer.disconnect();
            if (!answered) resolve(false);
          }
        });
        observer.observe(document.getElementById("modal-root"), { childList: true });
      },
    });
  });
}

/** Reads a form into a plain object and shows server-side field errors next to the inputs. */
export function formValues(form) {
  return Object.fromEntries(new FormData(form).entries());
}

export function showFieldErrors(form, fields) {
  form.querySelectorAll(".field__error").forEach((node) => node.remove());
  form
    .querySelectorAll(".field--invalid")
    .forEach((node) => node.classList.remove("field--invalid"));
  Object.keys(fields).forEach((name) => {
    const input = form.querySelector(`[name="${name}"]`);
    const field = input?.closest(".field");
    if (!field) return;
    field.classList.add("field--invalid");
    const note = document.createElement("p");
    note.className = "field__error";
    note.textContent = "Confira este campo.";
    field.append(note);
  });
}
