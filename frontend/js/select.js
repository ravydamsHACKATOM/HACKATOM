/**
 * Custom select: a button that opens a list of rich options (icon, label, hint).
 *
 * It keeps the behavior people expect from a native select: it is a form control (the value
 * travels in a hidden input), it opens with click, Enter, Space or the arrow keys, typing jumps
 * to the matching option, Escape closes it and focus returns to the button. Roles and
 * aria attributes follow the WAI-ARIA listbox pattern.
 */

import { html, raw } from "./dom.js";
import { icon } from "./icons.js";

/**
 * @param {object} config
 * @param {string} config.name form field name
 * @param {string} config.id id of the trigger, so a label can point at it
 * @param {Array<{value: string|number, label: string, hint?: string, art?: string, tone?: string}>}
 *     config.options `art` is trusted markup shown in the option tile
 * @param {string|number} [config.value] selected value
 * @param {string} [config.placeholder]
 */
export function richSelect({ name, id, options, value, placeholder = "Escolha" }) {
  const selected = options.find((option) => String(option.value) === String(value));
  return html`
    <div class="select" data-select>
      <input type="hidden" name="${name}" value="${selected?.value ?? ""}" />
      <button
        class="select__trigger"
        id="${id}"
        type="button"
        role="combobox"
        aria-haspopup="listbox"
        aria-expanded="false"
        aria-controls="${id}-menu"
      >
        <span class="select__value">${selected ? optionContent(selected) : placeholder}</span>
        <span class="select__chevron" aria-hidden="true">${raw(icon("chevron", 18))}</span>
      </button>
      <ul class="select__menu" id="${id}-menu" role="listbox" tabindex="-1" hidden>
        ${options.map(
          (option, index) => html`
            <li
              class="select__option"
              id="${id}-option-${index}"
              role="option"
              data-value="${option.value}"
              aria-selected="${String(option === selected)}"
              style="--i:${index}; --tone:${option.tone ?? "var(--accent)"}"
            >
              ${optionContent(option)}
              <span class="select__check" aria-hidden="true">${raw(icon("check", 16))}</span>
            </li>
          `,
        )}
      </ul>
    </div>
  `;
}

function optionContent(option) {
  return html`
    <span class="select__tile" style="--tone:${option.tone ?? "var(--accent)"}" aria-hidden="true">
      ${option.art ? raw(option.art) : ""}
    </span>
    <span class="select__text">
      <span class="select__label">${option.label}</span>
      ${option.hint ? html`<span class="select__hint">${option.hint}</span>` : ""}
    </span>
  `;
}

/** Wires every custom select inside `root`. Call it after mounting markup built by richSelect. */
export function enhanceSelects(root) {
  root.querySelectorAll("[data-select]").forEach(setup);
}

function setup(select) {
  const input = select.querySelector('input[type="hidden"]');
  const trigger = select.querySelector(".select__trigger");
  const menu = select.querySelector(".select__menu");
  const options = [...menu.querySelectorAll(".select__option")];
  let active = Math.max(
    0,
    options.findIndex((option) => option.getAttribute("aria-selected") === "true"),
  );
  let typed = "";
  let typedTimer;

  const isOpen = () => trigger.getAttribute("aria-expanded") === "true";

  function place() {
    // Fixed positioning keeps the list out of the dialog's scroll area, so it is never clipped.
    const rect = trigger.getBoundingClientRect();
    const below = window.innerHeight - rect.bottom;
    const height = Math.min(menu.scrollHeight, 288);
    const above = below < height + 16 && rect.top > below;
    menu.style.left = `${rect.left}px`;
    menu.style.width = `${rect.width}px`;
    menu.style.top = above ? "auto" : `${rect.bottom + 6}px`;
    menu.style.bottom = above ? `${window.innerHeight - rect.top + 6}px` : "auto";
    menu.dataset.side = above ? "top" : "bottom";
  }

  function setActive(index) {
    active = (index + options.length) % options.length;
    options.forEach((option, i) => option.classList.toggle("is-active", i === active));
    trigger.setAttribute("aria-activedescendant", options[active].id);
    // Scroll only the list itself: scrollIntoView would also move the dialog behind it.
    const option = options[active];
    if (option.offsetTop < menu.scrollTop) {
      menu.scrollTop = option.offsetTop - 6;
    } else if (option.offsetTop + option.offsetHeight > menu.scrollTop + menu.clientHeight) {
      menu.scrollTop = option.offsetTop + option.offsetHeight - menu.clientHeight + 6;
    }
  }

  function open() {
    if (isOpen()) return;
    // The list is moved to the end of the page while open. Inside the dialog, ancestors with
    // filters or transforms would become its containing block and misplace or clip it.
    document.body.append(menu);
    menu.hidden = false;
    place();
    trigger.setAttribute("aria-expanded", "true");
    select.classList.add("is-open");
    setActive(active);
    document.addEventListener("mousedown", onOutside, true);
    window.addEventListener("resize", close);
    window.addEventListener("scroll", onScroll, true);
  }

  function close() {
    if (!isOpen()) return;
    trigger.setAttribute("aria-expanded", "false");
    trigger.removeAttribute("aria-activedescendant");
    select.classList.remove("is-open");
    menu.hidden = true;
    select.append(menu);
    document.removeEventListener("mousedown", onOutside, true);
    window.removeEventListener("resize", close);
    window.removeEventListener("scroll", onScroll, true);
  }

  function choose(index) {
    const option = options[index];
    options.forEach((item) => item.setAttribute("aria-selected", String(item === option)));
    input.value = option.dataset.value;
    const value = trigger.querySelector(".select__value");
    value.innerHTML = "";
    option
      .querySelectorAll(":scope > .select__tile, :scope > .select__text")
      .forEach((node) => value.append(node.cloneNode(true)));
    value.classList.remove("is-changed");
    void value.offsetWidth; // restart the animation
    value.classList.add("is-changed");
    active = index;
    close();
    trigger.focus();
    input.dispatchEvent(new Event("change", { bubbles: true }));
  }

  function onOutside(event) {
    if (!select.contains(event.target) && !menu.contains(event.target)) close();
  }

  function onScroll(event) {
    if (!menu.contains(event.target)) close();
  }

  trigger.addEventListener("click", () => (isOpen() ? close() : open()));

  trigger.addEventListener("keydown", (event) => {
    const { key } = event;
    if (key === "ArrowDown" || key === "ArrowUp") {
      event.preventDefault();
      if (!isOpen()) open();
      else setActive(active + (key === "ArrowDown" ? 1 : -1));
    } else if (key === "Home" && isOpen()) {
      event.preventDefault();
      setActive(0);
    } else if (key === "End" && isOpen()) {
      event.preventDefault();
      setActive(options.length - 1);
    } else if ((key === "Enter" || key === " ") && isOpen()) {
      event.preventDefault();
      choose(active);
    } else if (key === "Escape" && isOpen()) {
      event.preventDefault();
      close();
    } else if (key === "Tab") {
      close();
    } else if (key.length === 1 && /\S/.test(key)) {
      // Type-ahead: jump to the first option starting with what was typed.
      clearTimeout(typedTimer);
      typed += key.toLowerCase();
      typedTimer = setTimeout(() => (typed = ""), 600);
      const match = options.findIndex((option) =>
        option.querySelector(".select__label").textContent.trim().toLowerCase().startsWith(typed),
      );
      if (match >= 0) {
        if (!isOpen()) open();
        setActive(match);
      }
    }
  });

  options.forEach((option, index) => {
    option.addEventListener("click", () => choose(index));
    option.addEventListener("mousemove", () => {
      if (active !== index) setActive(index);
    });
  });
}
