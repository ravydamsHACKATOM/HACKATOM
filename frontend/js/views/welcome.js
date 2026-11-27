/** Landing screen with the sign-in and sign-up card. */

import { api, ApiError } from "../api.js";
import { html, mount, raw, withBusy } from "../dom.js";
import { icon } from "../icons.js";

const STEPS = [
  {
    icon: "events",
    title: "Conte o que vem aí",
    text: "Provas, entregas e quantas horas você acha que cada uma pede.",
  },
  {
    icon: "availability",
    title: "Diga quando você pode",
    text: "Só os horários em que dá para estudar de verdade.",
  },
  {
    icon: "week",
    title: "Receba a semana pronta",
    text: "Blocos de foco distribuídos até cada prazo. Mudou algo? O plano se refaz.",
  },
];

const MODES = {
  login: {
    title: "Bom te ver de novo",
    text: "Entre para continuar de onde parou.",
    submit: "Entrar",
  },
  register: {
    title: "Crie sua conta",
    text: "Leva menos de um minuto e é gratuito.",
    submit: "Criar conta",
  },
};

const STRENGTH = ["Muito curta", "Fraca", "Razoável", "Boa", "Forte"];

/** A rough 0-4 score: length first, then variety. It guides the student; the server only enforces length. */
function passwordScore(password) {
  if (password.length < 8) return 0;
  const variety = [/[a-z]/, /[A-Z]/, /\d/, /[^\w\s]/].filter((rule) => rule.test(password)).length;
  return Math.min(
    4,
    1 + (password.length >= 12 ? 1 : 0) + (variety >= 3 ? 1 : 0) + (variety >= 4 ? 1 : 0),
  );
}

export function renderWelcome(root, { onSignedIn }) {
  let mode = "login";

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
            <ol class="welcome__steps">
              ${STEPS.map(
                (step, index) => html`
                  <li class="rise" style="--i:${index + 4}">
                    <span class="welcome__number">${raw(icon(step.icon, 18))}</span>
                    <div>
                      <h2>${step.title}</h2>
                      <p>${step.text}</p>
                    </div>
                  </li>
                `,
              )}
            </ol>
          </div>

          <section class="auth rise" style="--i:3" aria-labelledby="auth-title">
            <div class="auth__art" aria-hidden="true">
              <img src="assets/img/hero.webp" width="1600" height="1000" alt="" />
            </div>
            <div class="auth__tabs" role="tablist" aria-label="Acesso">
              <button class="auth__tab" type="button" role="tab" data-mode="login">Entrar</button>
              <button class="auth__tab" type="button" role="tab" data-mode="register">
                Criar conta
              </button>
              <span class="auth__indicator" aria-hidden="true"></span>
            </div>

            <form class="auth__form" novalidate>
              <div class="auth__heading">
                <h2 id="auth-title"></h2>
                <p data-text></p>
              </div>

              <div class="field field--icon" data-only="register">
                <label for="auth-name">Seu nome</label>
                <div class="input-wrap">
                  ${raw(icon("user", 18))}
                  <input
                    id="auth-name"
                    class="input"
                    name="name"
                    type="text"
                    maxlength="60"
                    autocomplete="name"
                    placeholder="Como podemos te chamar?"
                  />
                </div>
              </div>

              <div class="field field--icon">
                <label for="auth-email">E-mail</label>
                <div class="input-wrap">
                  ${raw(icon("mail", 18))}
                  <input
                    id="auth-email"
                    class="input"
                    name="email"
                    type="email"
                    maxlength="120"
                    autocomplete="email"
                    placeholder="voce@exemplo.com"
                  />
                </div>
              </div>

              <div class="field field--icon">
                <label for="auth-password">Senha</label>
                <div class="input-wrap">
                  ${raw(icon("lock", 18))}
                  <input
                    id="auth-password"
                    class="input"
                    name="password"
                    type="password"
                    maxlength="72"
                    placeholder="Sua senha"
                  />
                  <button
                    class="input-wrap__action"
                    type="button"
                    data-reveal
                    aria-label="Mostrar senha"
                    aria-pressed="false"
                  >
                    ${raw(icon("eye", 18))}
                  </button>
                </div>
                <div class="strength" data-only="register" data-score="0" aria-live="polite">
                  <div class="strength__bars" aria-hidden="true">
                    <span></span><span></span><span></span><span></span>
                  </div>
                  <span class="strength__label">Use pelo menos 8 caracteres.</span>
                </div>
              </div>

              <p class="form__error" data-error role="alert" hidden></p>

              <button class="button button--primary button--large auth__submit" type="submit">
                <span class="button__label"></span>
                <span class="button__spinner" aria-hidden="true"></span>
              </button>

              <p class="auth__switch">
                <span data-switch-text></span>
                <button class="link" type="button" data-switch></button>
              </p>
            </form>
          </section>
        </main>
      </div>
    `,
  );

  const form = root.querySelector(".auth__form");
  const tabs = [...root.querySelectorAll(".auth__tab")];
  const password = form.elements.password;
  const strength = root.querySelector(".strength");
  const errorLabel = root.querySelector("[data-error]");
  const submit = form.querySelector('[type="submit"]');

  const fail = (message) => {
    errorLabel.textContent = message;
    errorLabel.hidden = !message;
    if (message) {
      form.classList.remove("auth__form--shake");
      void form.offsetWidth;
      form.classList.add("auth__form--shake");
    }
  };

  function setMode(next, { focus = true } = {}) {
    mode = next;
    const copy = MODES[mode];
    root.querySelector(".auth").dataset.mode = mode;
    root.querySelector("#auth-title").textContent = copy.title;
    root.querySelector("[data-text]").textContent = copy.text;
    submit.querySelector(".button__label").textContent = copy.submit;
    root.querySelector("[data-switch-text]").textContent =
      mode === "login" ? "Ainda não tem conta?" : "Já tem conta?";
    root.querySelector("[data-switch]").textContent = mode === "login" ? "Criar conta" : "Entrar";
    tabs.forEach((tab) => {
      const selected = tab.dataset.mode === mode;
      tab.setAttribute("aria-selected", String(selected));
      tab.tabIndex = selected ? 0 : -1;
    });
    root.querySelectorAll("[data-only]").forEach((node) => {
      node.hidden = node.dataset.only !== mode;
    });
    password.autocomplete = mode === "login" ? "current-password" : "new-password";
    password.placeholder = mode === "login" ? "Sua senha" : "Crie uma senha";
    fail("");
    if (focus) (mode === "login" ? form.elements.email : form.elements.name).focus();
  }

  tabs.forEach((tab) => tab.addEventListener("click", () => setMode(tab.dataset.mode)));
  root
    .querySelector("[data-switch]")
    .addEventListener("click", () => setMode(mode === "login" ? "register" : "login"));

  root.querySelector("[data-reveal]").addEventListener("click", (event) => {
    const button = event.currentTarget;
    const visible = password.type === "password";
    password.type = visible ? "text" : "password";
    button.setAttribute("aria-pressed", String(visible));
    button.setAttribute("aria-label", visible ? "Ocultar senha" : "Mostrar senha");
    button.innerHTML = icon(visible ? "eyeOff" : "eye", 18);
  });

  password.addEventListener("input", () => {
    const score = passwordScore(password.value);
    strength.dataset.score = String(score);
    strength.querySelector(".strength__label").textContent = password.value
      ? STRENGTH[score]
      : "Use pelo menos 8 caracteres.";
  });

  form.addEventListener("input", () => fail(""));

  form.addEventListener("submit", async (event) => {
    event.preventDefault();
    const name = form.elements.name.value.trim();
    const email = form.elements.email.value.trim();

    if (mode === "register" && !name) return fail("Diga como podemos te chamar.");
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) return fail("Informe um e-mail válido.");
    if (!password.value) return fail("Digite sua senha.");
    if (mode === "register" && password.value.length < 8) {
      return fail("A senha precisa ter pelo menos 8 caracteres.");
    }

    await withBusy(submit, async () => {
      try {
        const user =
          mode === "login"
            ? await api.auth.login({ email, password: password.value })
            : await api.auth.register({ name, email, password: password.value });
        onSignedIn(user);
      } catch (error) {
        fail(
          error instanceof ApiError && error.code === "VALIDATION_FAILED"
            ? "Confira o e-mail e use uma senha de 8 a 72 caracteres."
            : error.message,
        );
      }
    });
  });

  setMode("login", { focus: false });
}
