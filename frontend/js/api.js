/** Thin client for the StudyFlow REST API. Every call returns parsed JSON or throws ApiError. */

/** Messages for the error codes the back-end can answer with. */
const MESSAGES = {
  VALIDATION_FAILED: "Confira os campos destacados.",
  AVAILABILITY_OVERLAP: "Esse horário se sobrepõe a outro no mesmo dia.",
  DISCIPLINE_NOT_FOUND: "Essa disciplina não existe mais.",
  WEEK_IN_PAST: "Essa semana já terminou e não pode ser planejada.",
  BLOCK_NOT_PLANNED: "Esse bloco não está mais planejado.",
  BLOCK_SKIPPED: "Esse bloco foi adiado e não pode ser concluído.",
  BLOCK_IN_PAST: "Esse horário já passou. Escolha um horário a partir de agora.",
  BLOCK_AFTER_DEADLINE: "Esse dia fica depois do prazo. Estude antes da prova ou da entrega.",
  BLOCK_OVERLAP: "Já existe outra sessão nesse horário.",
  BLOCK_OTHER_WEEK: "A sessão só pode mudar de dia dentro da mesma semana.",
  BLOCK_CROSSES_MIDNIGHT: "A sessão precisa terminar no mesmo dia.",
  NOT_FOUND: "Esse item não existe mais. Atualize a página.",
  EMAIL_TAKEN: "Já existe uma conta com esse e-mail. Tente entrar.",
  INVALID_CREDENTIALS: "E-mail ou senha incorretos.",
  UNAUTHENTICATED: "Sua sessão terminou. Entre de novo.",
  NETWORK: "Não consegui falar com o servidor. Verifique se ele está rodando.",
};

export class ApiError extends Error {
  constructor(code, fields) {
    super(MESSAGES[code] ?? "Algo deu errado. Tente de novo.");
    this.code = code;
    this.fields = fields ?? {};
  }
}

/** Called when the server says the session is gone, so the app can go back to the sign-in screen. */
let onSessionLost = () => {};

export function setSessionLostHandler(handler) {
  onSessionLost = handler;
}

async function request(method, path, body) {
  let response;
  try {
    response = await fetch(`/api${path}`, {
      method,
      credentials: "same-origin",
      headers: body === undefined ? undefined : { "Content-Type": "application/json" },
      body: body === undefined ? undefined : JSON.stringify(body),
    });
  } catch {
    throw new ApiError("NETWORK");
  }
  if (response.status === 204) {
    return null;
  }
  const payload = await response.json().catch(() => null);
  if (!response.ok) {
    const error = new ApiError(payload?.code ?? "UNKNOWN", payload?.fields);
    if (error.code === "UNAUTHENTICATED") onSessionLost();
    throw error;
  }
  return payload;
}

const crud = (path) => ({
  list: () => request("GET", path),
  create: (data) => request("POST", path, data),
  update: (id, data) => request("PUT", `${path}/${id}`, data),
  remove: (id) => request("DELETE", `${path}/${id}`),
});

export const api = {
  auth: {
    me: () => request("GET", "/auth/me"),
    register: (data) => request("POST", "/auth/register", data),
    login: (data) => request("POST", "/auth/login", data),
    logout: () => request("POST", "/auth/logout"),
  },
  disciplines: crud("/disciplines"),
  events: crud("/events"),
  availability: crud("/availability"),
  plan: {
    get: (date) => request("GET", date ? `/plan?date=${date}` : "/plan"),
    generate: (date) => request("POST", date ? `/plan/generate?date=${date}` : "/plan/generate"),
    settings: (date, body) =>
      request("PUT", date ? `/plan/settings?date=${date}` : "/plan/settings", body),
  },
  blocks: {
    setDone: (id, value) => request("PATCH", `/blocks/${id}/done`, { value }),
    setLocked: (id, value) => request("PATCH", `/blocks/${id}/locked`, { value }),
    postpone: (id) => request("POST", `/blocks/${id}/postpone`),
    move: (id, date, startTime) => request("POST", `/blocks/${id}/move`, { date, startTime }),
  },
};
