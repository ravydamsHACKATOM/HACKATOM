/** Thin client for the StudyFlow REST API. Every call returns parsed JSON or throws ApiError. */

/** Messages for the error codes the back-end can answer with. */
const MESSAGES = {
  VALIDATION_FAILED: "Confira os campos destacados.",
  AVAILABILITY_OVERLAP: "Esse horário se sobrepõe a outro no mesmo dia.",
  DISCIPLINE_NOT_FOUND: "Essa disciplina não existe mais.",
  WEEK_IN_PAST: "Essa semana já terminou e não pode ser planejada.",
  BLOCK_NOT_PLANNED: "Esse bloco não está mais planejado.",
  BLOCK_SKIPPED: "Esse bloco foi adiado e não pode ser concluído.",
  NOT_FOUND: "Esse item não existe mais. Atualize a página.",
  NETWORK: "Não consegui falar com o servidor. Verifique se ele está rodando.",
};

export class ApiError extends Error {
  constructor(code, fields) {
    super(MESSAGES[code] ?? "Algo deu errado. Tente de novo.");
    this.code = code;
    this.fields = fields ?? {};
  }
}

async function request(method, path, body) {
  let response;
  try {
    response = await fetch(`/api${path}`, {
      method,
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
    throw new ApiError(payload?.code ?? "UNKNOWN", payload?.fields);
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
  disciplines: crud("/disciplines"),
  events: crud("/events"),
  availability: crud("/availability"),
  plan: {
    get: (date) => request("GET", date ? `/plan?date=${date}` : "/plan"),
    generate: (date) => request("POST", date ? `/plan/generate?date=${date}` : "/plan/generate"),
  },
  blocks: {
    setDone: (id, value) => request("PATCH", `/blocks/${id}/done`, { value }),
    setLocked: (id, value) => request("PATCH", `/blocks/${id}/locked`, { value }),
    postpone: (id) => request("POST", `/blocks/${id}/postpone`),
  },
  advisor: () => request("GET", "/advisor"),
};
