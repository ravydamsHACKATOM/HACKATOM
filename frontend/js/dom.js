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
  const mark = kind === "error" ? "alert" : kind === "success" ? "check" : "sparkle";
  mount(
    root,
    html`<div class="toast toast--${kind}">
      <span class="toast__mark">${raw(icon(mark, 14))}</span>${message}
    </div>`,
  );
  clearTimeout(toastTimer);
  toastTimer = setTimeout(() => {
    root.firstElementChild?.classList.add("toast--leaving");
    setTimeout(() => (root.innerHTML = ""), 260);
  }, 3600);
}

const FOCUSABLE =
  'button:not([disabled]), [href], input:not([type="hidden"]):not([disabled]), select, textarea, [tabindex]:not([tabindex="-1"])';

/**
 * Opens a modal dialog.
 *
 * @param {object} options
 * @param {string} options.title
 * @param {string} [options.subtitle] one line under the title
 * @param {string} [options.icon] interface icon shown in the header badge
 * @param {string} [options.art] trusted markup for the badge instead of an icon
 * @param {string} [options.tone] accent | terracotta | blue | mustard
 * @param {boolean} [options.compact] narrower dialog, for confirmations
 * @param {Function} [options.onMount] receives the dialog element and a close function
 *
 * Focus moves in, stays trapped while the dialog is open and returns to where it was on close.
 * Escape and a click on the backdrop close it.
 */
export function modal({
  title,
  subtitle,
  icon: iconName,
  art,
  tone = "accent",
  compact,
  body,
  onMount,
}) {
  const root = document.getElementById("modal-root");
  const previousFocus = document.activeElement;
  mount(
    root,
    html`
      <div class="modal-backdrop">
        <div
          class="modal modal--${tone} ${compact ? "modal--compact" : ""}"
          role="dialog"
          aria-modal="true"
          aria-labelledby="modal-title"
        >
          <header class="modal__header">
            <span class="modal__badge" aria-hidden="true">
              ${art ? raw(art) : raw(icon(iconName ?? "sparkle", 24))}
            </span>
            <div class="modal__heading">
              <h2 id="modal-title">${title}</h2>
              ${subtitle ? html`<p>${subtitle}</p>` : ""}
            </div>
            <button class="icon-button modal__close" type="button" data-close aria-label="Fechar">
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
  document.body.classList.add("has-modal");

  function close() {
    document.removeEventListener("keydown", onKey, true);
    document.body.classList.remove("has-modal");
    // An open dropdown renders at page level; take it away with the dialog.
    document.querySelectorAll("body > .select__menu").forEach((menu) => menu.remove());
    backdrop.classList.add("modal-backdrop--leaving");
    setTimeout(() => {
      if (root.firstElementChild === backdrop) root.innerHTML = "";
      previousFocus?.focus?.();
    }, 200);
  }

  function onKey(event) {
    if (event.key === "Escape" && !event.defaultPrevented) {
      // An open dropdown inside the dialog handles Escape first.
      if (dialog.querySelector('.select__trigger[aria-expanded="true"]')) return;
      close();
      return;
    }
    if (event.key !== "Tab") return;
    const focusable = [...dialog.querySelectorAll(FOCUSABLE)].filter(
      (node) => node.offsetParent !== null,
    );
    if (!focusable.length) return;
    const first = focusable[0];
    const last = focusable[focusable.length - 1];
    if (event.shiftKey && document.activeElement === first) {
      event.preventDefault();
      last.focus();
    } else if (!event.shiftKey && document.activeElement === last) {
      event.preventDefault();
      first.focus();
    }
  }

  backdrop.addEventListener("mousedown", (event) => {
    if (event.target === backdrop) close();
  });
  backdrop.addEventListener("click", (event) => {
    if (event.target.closest("button[data-close]")) close();
  });
  document.addEventListener("keydown", onKey, true);
  onMount?.(dialog, close);
  (dialog.querySelector("[autofocus]") ?? dialog.querySelector(FOCUSABLE))?.focus();
  return close;
}

export function confirmDialog({ title, message, confirmLabel = "Confirmar", danger = false }) {
  return new Promise((resolve) => {
    let answered = false;
    modal({
      title,
      icon: danger ? "trash" : "alert",
      tone: danger ? "terracotta" : "accent",
      compact: true,
      body: html`
        <p class="modal__text">${message}</p>
        <footer class="modal__footer">
          <button class="button button--ghost" type="button" data-answer="no">Cancelar</button>
          <button
            class="button ${danger ? "button--danger" : "button--primary"}"
            type="button"
            data-answer="yes"
            autofocus
          >
            ${confirmLabel}
          </button>
        </footer>
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

/** Reads a form into a plain object. */
export function formValues(form) {
  return Object.fromEntries(new FormData(form).entries());
}

/** Shows messages next to the inputs they refer to. `messages` maps a field name to a text. */
export function showFieldErrors(form, messages) {
  form.querySelectorAll(".field__error").forEach((node) => node.remove());
  form
    .querySelectorAll(".field--invalid")
    .forEach((node) => node.classList.remove("field--invalid"));
  Object.entries(messages).forEach(([name, message]) => {
    const input = form.querySelector(`[name="${name}"]`);
    const field = input?.closest(".field");
    if (!field) return;
    field.classList.add("field--invalid");
    const note = document.createElement("p");
    note.className = "field__error";
    note.textContent = message || "Confira este campo.";
    field.append(note);
  });
  form.querySelector(".field--invalid input, .field--invalid textarea")?.focus();
}

/** Puts a button in its busy state while an async task runs. */
export async function withBusy(button, task) {
  button.disabled = true;
  button.classList.add("is-loading");
  try {
    return await task();
  } finally {
    button.disabled = false;
    button.classList.remove("is-loading");
  }
}
