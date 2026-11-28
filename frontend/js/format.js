/** Date, time and duration helpers. All dates travel as ISO strings (YYYY-MM-DD). */

const LOCALE = "pt-BR";

export const WEEKDAYS = [
  { key: "MONDAY", short: "Seg", long: "Segunda" },
  { key: "TUESDAY", short: "Ter", long: "Terça" },
  { key: "WEDNESDAY", short: "Qua", long: "Quarta" },
  { key: "THURSDAY", short: "Qui", long: "Quinta" },
  { key: "FRIDAY", short: "Sex", long: "Sexta" },
  { key: "SATURDAY", short: "Sáb", long: "Sábado" },
  { key: "SUNDAY", short: "Dom", long: "Domingo" },
];

/** Parses an ISO date at local noon, which keeps it on the same day in any time zone. */
export function parseDate(iso) {
  return new Date(`${iso}T12:00:00`);
}

export function toIso(date) {
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  return `${date.getFullYear()}-${month}-${day}`;
}

export function addDays(iso, days) {
  const date = parseDate(iso);
  date.setDate(date.getDate() + days);
  return toIso(date);
}

export function daysBetween(fromIso, toIsoDate) {
  return Math.round((parseDate(toIsoDate) - parseDate(fromIso)) / 86_400_000);
}

/** "50 min", "2h", "1h30". */
export function duration(minutes) {
  if (minutes < 60) {
    return `${minutes} min`;
  }
  const hours = Math.floor(minutes / 60);
  const rest = minutes % 60;
  return rest === 0 ? `${hours}h` : `${hours}h${String(rest).padStart(2, "0")}`;
}

/** "18:00:00" becomes "18:00". */
export function time(value) {
  return value.slice(0, 5);
}

export function dayMonth(iso) {
  return parseDate(iso).toLocaleDateString(LOCALE, { day: "2-digit", month: "2-digit" });
}

export function longDate(iso) {
  const text = parseDate(iso).toLocaleDateString(LOCALE, {
    weekday: "long",
    day: "numeric",
    month: "long",
  });
  return text.charAt(0).toUpperCase() + text.slice(1);
}

export function weekRange(startIso, endIso) {
  const start = parseDate(startIso);
  const end = parseDate(endIso);
  const month = (date) => date.toLocaleDateString(LOCALE, { month: "short" }).replace(".", "");
  return start.getMonth() === end.getMonth()
    ? `${start.getDate()} a ${end.getDate()} de ${month(end)}`
    : `${start.getDate()} de ${month(start)} a ${end.getDate()} de ${month(end)}`;
}

/** Human distance to a deadline: "hoje", "amanhã", "em 4 dias", "há 2 dias". */
export function deadline(todayIso, dueIso) {
  const days = daysBetween(todayIso, dueIso);
  if (days === 0) return "hoje";
  if (days === 1) return "amanhã";
  if (days === -1) return "ontem";
  return days > 0 ? `em ${days} dias` : `há ${-days} dias`;
}

export function greeting(date = new Date()) {
  const hour = date.getHours();
  if (hour < 5) return "Boa madrugada";
  if (hour < 12) return "Bom dia";
  if (hour < 18) return "Boa tarde";
  return "Boa noite";
}

export function plural(count, one, many) {
  return `${count} ${count === 1 ? one : many}`;
}
