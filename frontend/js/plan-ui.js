/** Pieces of the plan shared by the "today" and "week" views. */

import { html, raw } from "./dom.js";
import { deadline, duration, plural, time } from "./format.js";
import { disciplineIcon, icon } from "./icons.js";

const STATUS_LABEL = {
  DONE: "Concluído",
  SKIPPED: "Adiado",
  MISSED: "Não realizado",
};

/** One study block with its actions. `compact` is the denser variant used in the week grid. */
export function blockCard(block, { compact = false, index = 0 } = {}) {
  const planned = block.status === "PLANNED";
  const done = block.status === "DONE";
  const classes = [
    "block",
    compact && "block--compact",
    `block--${block.status.toLowerCase()}`,
    block.locked && "block--locked",
  ]
    .filter(Boolean)
    .join(" ");

  return html`
    <li
      class="${classes} rise"
      style="--i:${index}; --tone:${block.disciplineColor}"
      data-block="${block.id}"
    >
      ${
        planned || done || block.status === "MISSED"
          ? html`<button
              class="check ${done ? "check--on" : ""}"
              type="button"
              data-action="${done ? "undo" : "done"}"
              data-id="${block.id}"
              aria-pressed="${String(done)}"
              aria-label="${done ? "Desmarcar bloco" : "Marcar bloco como concluído"}"
            >
              ${raw(icon("check", 16))}
            </button>`
          : html`<span class="check check--off" aria-hidden="true"></span>`
      }
      <div class="block__body">
        <p class="block__time">
          ${time(block.startTime)} – ${time(block.endTime)}
          <span class="block__minutes">${duration(block.minutes)}</span>
        </p>
        <p class="block__title">${block.eventTitle}</p>
        <p class="block__meta">
          <span class="dot" aria-hidden="true"></span>${block.disciplineName}
          ${block.kind === "REVIEW" ? html`<span class="tag tag--mustard">Revisão</span>` : ""}
          ${block.locked ? html`<span class="tag">Travado</span>` : ""}
          ${
            STATUS_LABEL[block.status]
              ? html`<span class="tag tag--quiet">${STATUS_LABEL[block.status]}</span>`
              : ""
          }
        </p>
      </div>
      ${
        planned
          ? html`<div class="block__actions">
              <button
                class="icon-button"
                type="button"
                data-action="${block.locked ? "unlock" : "lock"}"
                data-id="${block.id}"
                title="${block.locked ? "Destravar" : "Travar neste horário"}"
                aria-label="${block.locked ? "Destravar bloco" : "Travar bloco neste horário"}"
              >
                ${raw(icon(block.locked ? "lock" : "unlock", 18))}
              </button>
              <button
                class="icon-button"
                type="button"
                data-action="postpone"
                data-id="${block.id}"
                title="Não vou conseguir: adiar e refazer"
                aria-label="Adiar bloco e refazer o plano"
              >
                ${raw(icon("later", 18))}
              </button>
            </div>`
          : ""
      }
      ${compact ? "" : raw(disciplineIcon(block.disciplineIcon, 44))}
    </li>
  `;
}

/** The explanation written by the AI (or by the rules when no key is configured). */
export function advisorCard(note, index = 0) {
  if (!note) return "";
  const fromAi = note.source === "AI";
  return html`
    <section class="card advisor rise" style="--i:${index}" aria-label="Orientador de estudos">
      <header class="advisor__header">
        <span class="advisor__badge">${raw(icon("sparkle", 16))} Orientador</span>
        <span class="tag ${fromAi ? "tag--accent" : "tag--quiet"}"
          >${fromAi ? "Escrito por IA" : "Regras automáticas"}</span
        >
      </header>
      <p class="advisor__summary">${note.summary}</p>
      ${
        note.tips.length
          ? html`<ul class="advisor__tips">
              ${note.tips.map((tip) => html`<li>${tip}</li>`)}
            </ul>`
          : ""
      }
    </section>
  `;
}

/** Effort that does not fit before its deadline. Never hidden: the student decides what to cut. */
export function warningsCard(warnings, today, index = 0) {
  if (!warnings.length) return "";
  return html`
    <section class="card card--warning rise" style="--i:${index}" role="alert">
      <h3 class="card__title">${raw(icon("alert", 18))} Não coube tudo</h3>
      <ul class="warning-list">
        ${warnings.map(
          (warning) => html`
            <li>
              <strong>${warning.title}</strong>
              <span>
                faltam ${duration(warning.missingMinutes)} ·
                ${warning.type === "EXAM" ? "prova" : "entrega"} ${deadline(today, warning.dueDate)}
              </span>
            </li>
          `,
        )}
      </ul>
      <p class="card__hint">
        Abra mais horários, reveja a estimativa ou antecipe o estudo. Depois é só refazer o plano.
      </p>
      <a class="button button--ghost button--small" href="#/horarios">Ajustar horários</a>
    </section>
  `;
}

/** What the last regeneration moved, in plain language. */
export function changesCard(changes, index = 0) {
  if (!changes || (changes.added === 0 && changes.removed === 0)) return "";
  return html`
    <section class="card card--changes rise" style="--i:${index}">
      <h3 class="card__title">${raw(icon("refresh", 18))} O que mudou</h3>
      <p class="card__hint">
        ${plural(changes.added, "bloco novo", "blocos novos")},
        ${plural(changes.removed, "removido", "removidos")} e
        ${plural(changes.kept, "mantido", "mantidos")}.
      </p>
      ${
        changes.events.length
          ? html`<ul class="delta-list">
              ${changes.events.map(
                (delta) => html`
                  <li>
                    <span>${delta.title}</span>
                    <span
                      class="delta ${delta.afterMinutes > delta.beforeMinutes ? "delta--up" : "delta--down"}"
                    >
                      ${duration(delta.beforeMinutes)} → ${duration(delta.afterMinutes)}
                    </span>
                  </li>
                `,
              )}
            </ul>`
          : ""
      }
    </section>
  `;
}

export function staleBanner(plan) {
  if (!plan.generated || !plan.stale) return "";
  return html`
    <div class="banner rise" role="status">
      <p>
        <strong>O plano está desatualizado.</strong> Você mudou prazos, disciplinas ou horários
        depois de gerá-lo.
      </p>
      <button class="button button--primary button--small" type="button" data-action="generate">
        ${raw(icon("refresh", 16))} Refazer plano
      </button>
    </div>
  `;
}

export function generateButton(plan, { small = false } = {}) {
  return html`
    <button
      class="button button--primary ${small ? "button--small" : ""}"
      type="button"
      data-action="generate"
    >
      ${raw(icon(plan.generated ? "refresh" : "sparkle", 18))}
      ${plan.generated ? "Refazer plano" : "Gerar plano"}
    </button>
  `;
}

/** Circular progress used on the "today" header. */
export function progressRing(done, total) {
  const ratio = total === 0 ? 0 : Math.min(1, done / total);
  const circumference = 2 * Math.PI * 52;
  return html`
    <div
      class="ring"
      role="img"
      aria-label="${duration(done)} de ${duration(total)} concluídos hoje"
    >
      <svg viewBox="0 0 120 120" width="120" height="120">
        <circle class="ring__track" cx="60" cy="60" r="52" />
        <circle
          class="ring__value"
          cx="60"
          cy="60"
          r="52"
          stroke-dasharray="${circumference}"
          style="--offset:${circumference * (1 - ratio)}; --full:${circumference}"
        />
      </svg>
      <div class="ring__label">
        <strong>${Math.round(ratio * 100)}%</strong>
        <span>do dia</span>
      </div>
    </div>
  `;
}
