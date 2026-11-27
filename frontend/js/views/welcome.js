/** Landing screen shown before the student enters the app. */

import { html, mount, raw } from "../dom.js";
import { icon } from "../icons.js";
import { profile } from "../store.js";

const STEPS = [
  {
    title: "Conte o que vem aí",
    text: "Provas, entregas e quantas horas você acha que cada uma pede.",
  },
  {
    title: "Diga quando você pode",
    text: "Só os horários em que dá para estudar de verdade.",
  },
  {
    title: "Receba a semana pronta",
    text: "Blocos de foco distribuídos até cada prazo. Mudou algo? O plano se refaz.",
  },
];

export function renderWelcome(root, { onEnter }) {
  mount(
    root,
    html`
      <div class="welcome">
        <header class="welcome__bar rise">
          <span class="brand">
            <img src="assets/favicon.svg" width="32" height="32" alt="" />
            <span>StudyFlow</span>
          </span>
        </header>

        <main id="main" class="welcome__hero">
          <div class="welcome__copy">
            <p class="eyebrow rise" style="--i:1">Planejador de estudos</p>
            <h1 class="welcome__title rise" style="--i:2">
              Sua semana de estudos, <em>do tamanho do seu tempo.</em>
            </h1>
            <p class="welcome__lead rise" style="--i:3">
              O StudyFlow transforma provas e entregas em um cronograma realista, bloco a bloco, e
              avisa quando o volume não cabe, antes da véspera.
            </p>
            <form class="welcome__form rise" style="--i:4" novalidate>
              <label class="visually-hidden" for="welcome-name">Como podemos te chamar?</label>
              <input
                id="welcome-name"
                class="input input--large"
                name="name"
                type="text"
                maxlength="30"
                autocomplete="given-name"
                placeholder="Como podemos te chamar?"
                required
              />
              <button class="button button--primary button--large" type="submit">
                Começar ${raw(icon("arrow", 18))}
              </button>
            </form>
            <p class="welcome__note rise" style="--i:5">
              Sem cadastro e sem senha. Seus dados ficam só neste computador.
            </p>
          </div>
          <figure class="welcome__art rise" style="--i:3">
            <img src="assets/img/hero.webp" width="1600" height="1000" alt="" />
          </figure>
        </main>

        <ol class="welcome__steps">
          ${STEPS.map(
            (step, index) => html`
              <li class="rise" style="--i:${index + 6}">
                <span class="welcome__number">${index + 1}</span>
                <h2>${step.title}</h2>
                <p>${step.text}</p>
              </li>
            `,
          )}
        </ol>
      </div>
    `,
  );

  const form = root.querySelector("form");
  form.addEventListener("submit", (event) => {
    event.preventDefault();
    const name = form.elements.name.value.trim();
    if (!name) {
      form.elements.name.focus();
      form.classList.add("welcome__form--shake");
      setTimeout(() => form.classList.remove("welcome__form--shake"), 400);
      return;
    }
    profile.name = name;
    onEnter();
  });
}
