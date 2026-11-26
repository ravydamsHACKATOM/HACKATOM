const state = { subjects: [], events: [], availability: [], blocks: [], today: [], scheduleWarnings: [] };
const dayNames = ['Segunda-feira', 'Terça-feira', 'Quarta-feira', 'Quinta-feira', 'Sexta-feira', 'Sábado', 'Domingo'];
const dayShort = ['Seg', 'Ter', 'Qua', 'Qui', 'Sex', 'Sáb', 'Dom'];
const $ = (selector, root = document) => root.querySelector(selector);
const $$ = (selector, root = document) => [...root.querySelectorAll(selector)];
const escapeHtml = value => String(value ?? '').replace(/[&<>"']/g, char => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[char]));

async function api(path, options = {}) {
  const response = await fetch(`/api${path}`, {
    ...options,
    headers: { ...(options.body ? { 'Content-Type': 'application/json' } : {}), ...options.headers }
  });
  if (!response.ok) {
    let message = `A solicitação falhou (${response.status}).`;
    try { const error = await response.json(); message = error.message || message; }
    catch { /* A resposta pode não conter JSON. */ }
    throw new Error(message);
  }
  if (response.status === 204) return null;
  return response.json();
}

function showNotice(message, isError = false) {
  const notice = $('#notice');
  notice.textContent = message;
  notice.classList.toggle('notice-error', isError);
  notice.hidden = !message;
  if (message) window.setTimeout(() => { if (notice.textContent === message) notice.hidden = true; }, 6000);
}
function connection(online, message) {
  const badge = $('#connection');
  badge.textContent = message;
  badge.dataset.state = online ? 'ok' : 'error';
}
function dateOnly(value) {
  const [year, month, day] = value.slice(0, 10).split('-').map(Number);
  return new Date(year, month - 1, day);
}
function dateKey(date) {
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
}
function dateLabel(value, options = { day: '2-digit', month: 'short' }) {
  return dateOnly(value).toLocaleDateString('pt-BR', options);
}
function timeLabel(value) { return value.slice(0, 5); }
function duration(block) {
  return Math.max(0, Math.round((new Date(block.endAt) - new Date(block.startAt)) / 60000));
}
function eventForBlock(block) { return state.events.find(event => event.id === block.eventId); }
function subjectName(subjectId) { return state.subjects.find(subject => subject.id === subjectId)?.name || 'Matéria removida'; }

async function refresh() {
  const [subjects, events, availability, blocks, today] = await Promise.all([
    api('/subjects'), api('/events'), api('/availability'), api('/blocks'), api('/today')
  ]);
  Object.assign(state, { subjects, events, availability, blocks, today });
  renderAll();
  connection(true, 'Dados sincronizados');
}

function renderAll() {
  renderStats(); renderSubjects(); renderEvents(); renderAvailability(); renderToday(); renderSchedule();
}
function renderEmpty(container, text) { container.innerHTML = `<div class="empty">${escapeHtml(text)}</div>`; }
function renderStats() {
  const done = state.blocks.filter(block => block.completed).length;
  const dueSoon = state.events.filter(event => event.dueDate >= dateKey(new Date())).length;
  const total = state.events.length;
  $('#stats').innerHTML = [
    `<div class="stat"><strong>${state.subjects.length}</strong><span>matérias cadastradas</span></div>`,
    `<div class="stat"><strong>${dueSoon}</strong><span>prazos futuros</span></div>`,
    `<div class="stat"><strong>${done}/${state.blocks.length}</strong><span>blocos concluídos</span></div>`
  ].join('');
  $('#today-label').textContent = new Date().toLocaleDateString('pt-BR', { weekday: 'long', day: 'numeric', month: 'long' });
  $('#home-title').setAttribute('aria-label', `Visão geral de ${total} prazos`);
}
function renderSubjects() {
  const container = $('#subjects-list');
  if (!state.subjects.length) return renderEmpty(container, 'Você ainda não cadastrou matérias. Use “Nova matéria” para começar.');
  container.innerHTML = state.subjects.map(subject => `
    <article class="item"><div class="item-icon" aria-hidden="true">${escapeHtml(subject.name.slice(0, 1).toUpperCase())}</div>
      <div class="item-info"><strong>${escapeHtml(subject.name)}</strong><small>${state.events.filter(event => event.subjectId === subject.id).length} prova(s) ou entrega(s)</small></div>
      <div class="item-actions"><button class="button secondary" data-action="edit-subject" data-id="${subject.id}">Editar</button><button class="button danger" data-action="delete-subject" data-id="${subject.id}">Excluir</button></div>
    </article>`).join('');
}
function renderEvents() {
  const container = $('#events-list');
  if (!state.events.length) return renderEmpty(container, 'Nenhum prazo cadastrado. Adicione uma prova ou entrega para planejar seus estudos.');
  container.innerHTML = state.events.map(event => `
    <article class="item"><div class="item-icon" aria-hidden="true">${event.type === 'PROVA' ? 'P' : 'E'}</div>
      <div class="item-info"><strong>${escapeHtml(event.title)}</strong><small>${escapeHtml(subjectName(event.subjectId))} · ${event.type === 'PROVA' ? 'Prova' : 'Entrega'} · prazo ${dateLabel(event.dueDate, { day: 'numeric', month: 'long', year: 'numeric' })} · ${Number(event.estimatedHours)}h estimadas</small></div>
      <div class="item-actions"><button class="button secondary" data-action="edit-event" data-id="${event.id}">Editar</button><button class="button danger" data-action="delete-event" data-id="${event.id}">Excluir</button></div>
    </article>`).join('');
}
function renderAvailability() {
  const container = $('#availability-list');
  const picker = $('#day-picker');
  picker.innerHTML = dayNames.map((name, index) => `<label class="day-option"><input type="checkbox" name="daysOfWeek" value="${index + 1}"><span>${name}</span></label>`).join('');
  if (!state.availability.length) return renderEmpty(container, 'Nenhum horário de estudo salvo.');
  container.innerHTML = state.availability.map(item => `
    <article class="item"><div class="item-icon" aria-hidden="true">H</div><div class="item-info"><strong>${item.daysOfWeek.map(day => dayNames[day - 1]).join(', ')}</strong><small>${timeLabel(item.startTime)}–${timeLabel(item.endTime)}</small></div></article>`).join('');
  if (state.availability.length === 1) {
    const saved = state.availability[0];
    $$('input[name="daysOfWeek"]', $('#day-picker')).forEach(input => { input.checked = saved.daysOfWeek.includes(Number(input.value)); });
    $('#availability-form [name="startTime"]').value = timeLabel(saved.startTime);
    $('#availability-form [name="endTime"]').value = timeLabel(saved.endTime);
  }
}
function renderToday() {
  const container = $('#today-list');
  if (!state.today.length) renderEmpty(container, 'Nenhum bloco programado para hoje.');
  else container.innerHTML = state.today.map(block => taskMarkup(block)).join('');
  const upcoming = $('#upcoming-list');
  const future = [...state.events].filter(event => event.dueDate >= dateKey(new Date())).sort((a, b) => a.dueDate.localeCompare(b.dueDate)).slice(0, 3);
  if (!future.length) renderEmpty(upcoming, 'Nenhuma prova ou entrega próxima.');
  else upcoming.innerHTML = future.map(event => `<article class="item"><div class="item-icon" aria-hidden="true">${event.type === 'PROVA' ? 'P' : 'E'}</div><div class="item-info"><strong>${escapeHtml(event.title)}</strong><small>${escapeHtml(subjectName(event.subjectId))} · ${dateLabel(event.dueDate, { day: 'numeric', month: 'short' })}</small></div></article>`).join('');
}
function taskMarkup(block) {
  const event = eventForBlock(block);
  const time = new Date(block.startAt).toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit' });
  return `<article class="task ${block.completed ? 'complete' : ''}"><input type="checkbox" aria-label="Marcar ${escapeHtml(block.title)} como ${block.completed ? 'pendente' : 'concluído'}" data-action="toggle-block" data-id="${block.id}" ${block.completed ? 'checked' : ''}><div class="task-info"><strong>${escapeHtml(block.title)}</strong><small>${escapeHtml(subjectName(event?.subjectId))} · ${time} · ${duration(block)} minutos</small></div><span class="badge ${block.completed ? 'done' : ''}">${block.completed ? 'Concluído' : 'Pendente'}</span></article>`;
}
function renderSchedule() {
  const warnings = $('#schedule-warnings');
  const computedWarnings = state.events.flatMap(event => {
    const planned = state.blocks.filter(block => block.eventId === event.id).reduce((sum, block) => sum + duration(block), 0);
    const required = Math.round(Number(event.estimatedHours) * 60);
    const missing = Math.max(0, required - planned);
    return missing ? [`${event.title}: ${Math.ceil(missing / 6) / 10}h ainda não distribuídas antes do prazo.`] : [];
  });
  const messages = state.scheduleWarnings.length ? state.scheduleWarnings : computedWarnings;
  warnings.innerHTML = messages.map(message => `<p class="warning">${escapeHtml(message)}</p>`).join('');
  const container = $('#schedule-list');
  const mondayIndex = (new Date().getDay() + 6) % 7;
  const monday = new Date(); monday.setDate(monday.getDate() - mondayIndex);
  const week = Array.from({ length: 7 }, (_, offset) => { const d = new Date(monday); d.setDate(monday.getDate() + offset); return d; });
  container.innerHTML = week.map((date, index) => {
    const key = dateKey(date);
    const dayBlocks = state.blocks.filter(block => block.startAt.slice(0, 10) === key).sort((a, b) => a.startAt.localeCompare(b.startAt));
    return `<section class="schedule-day"><h3>${dayShort[index]}<span>${dateLabel(key)}</span></h3>${dayBlocks.length ? dayBlocks.map(block => `<button class="study-block ${block.completed ? 'done' : ''}" data-action="toggle-block" data-id="${block.id}" aria-pressed="${block.completed}"><strong>${new Date(block.startAt).toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit' })} · ${duration(block)} min</strong>${escapeHtml(block.title)}${block.completed ? ' · Concluído' : ''}</button>`).join('') : '<p class="muted">Livre</p>'}</section>`;
  }).join('');
}

function showEditor(title, fields, onSave) {
  $('#editor-title').textContent = title;
  $('#editor-fields').innerHTML = fields;
  $('#editor-form').onsubmit = async event => {
    event.preventDefault();
    const form = new FormData(event.currentTarget);
    try { await onSave(form); $('#editor').close(); await refresh(); showNotice('Alterações salvas.'); }
    catch (error) { showNotice(error.message, true); }
  };
  $('#editor').showModal();
  $('input,select', $('#editor-fields'))?.focus();
}
function openSubject(subject = null) {
  showEditor(subject ? 'Editar matéria' : 'Nova matéria', `
    <label>Nome da matéria<input name="name" maxlength="100" required value="${escapeHtml(subject?.name || '')}" autocomplete="off"></label>
    <label>Cor (opcional)<input name="color" maxlength="20" value="${escapeHtml(subject?.color || '')}" placeholder="#2858c8"></label>`, async form => {
    const body = JSON.stringify({ name: form.get('name'), color: form.get('color') || null });
    await api(subject ? `/subjects/${subject.id}` : '/subjects', { method: subject ? 'PUT' : 'POST', body });
  });
}
function openEvent(event = null) {
  if (!state.subjects.length) { showNotice('Cadastre uma matéria antes de criar um prazo.', true); navigate('subjects'); return; }
  const today = dateKey(new Date());
  const subjectOptions = state.subjects.map(subject => `<option value="${subject.id}" ${event?.subjectId === subject.id ? 'selected' : ''}>${escapeHtml(subject.name)}</option>`).join('');
  showEditor(event ? 'Editar prazo' : 'Novo prazo', `
    <label>Título<input name="title" maxlength="150" required value="${escapeHtml(event?.title || '')}"></label>
    <label>Matéria<select name="subjectId" required>${subjectOptions}</select></label>
    <label>Tipo<select name="type"><option value="PROVA" ${event?.type === 'PROVA' ? 'selected' : ''}>Prova</option><option value="ENTREGA" ${event?.type === 'ENTREGA' ? 'selected' : ''}>Entrega</option></select></label>
    <label>Data do prazo<input name="dueDate" type="date" min="${today}" required value="${escapeHtml(event?.dueDate || today)}"></label>
    <label>Esforço estimado em horas<input name="estimatedHours" type="number" min="0.25" max="100" step="0.25" required value="${escapeHtml(event?.estimatedHours ?? 3)}"></label>`, async form => {
    const body = JSON.stringify({ title: form.get('title'), subjectId: Number(form.get('subjectId')), type: form.get('type'), dueDate: form.get('dueDate'), estimatedHours: Number(form.get('estimatedHours')) });
    await api(event ? `/events/${event.id}` : '/events', { method: event ? 'PUT' : 'POST', body });
  });
}
function navigate(name) {
  $$('.view').forEach(view => { view.hidden = view.id !== `view-${name}`; });
  $$('.nav-link').forEach(button => { const active = button.dataset.view === name; button.classList.toggle('active', active); if (active) button.setAttribute('aria-current', 'page'); else button.removeAttribute('aria-current'); });
  $(`#view-${name}`)?.scrollIntoView({ block: 'start', behavior: 'smooth' });
}
async function mutate(action) {
  try { await action(); state.scheduleWarnings = []; await refresh(); showNotice('Alterações salvas.'); }
  catch (error) { connection(false, 'Falha na sincronização'); showNotice(error.message, true); }
}

document.addEventListener('click', async event => {
  const nav = event.target.closest('[data-view]'); if (nav) return navigate(nav.dataset.view);
  const go = event.target.closest('[data-go]'); if (go) return navigate(go.dataset.go);
  const action = event.target.closest('[data-action]'); if (!action) return;
  const id = Number(action.dataset.id);
  if (action.dataset.action === 'edit-subject') return openSubject(state.subjects.find(item => item.id === id));
  if (action.dataset.action === 'edit-event') return openEvent(state.events.find(item => item.id === id));
  if (action.dataset.action === 'delete-subject') {
    const subject = state.subjects.find(item => item.id === id);
    if (subject && window.confirm(`Excluir “${subject.name}” e seus eventos e blocos?`)) await mutate(() => api(`/subjects/${id}`, { method: 'DELETE' }));
  }
  if (action.dataset.action === 'delete-event') {
    const item = state.events.find(eventItem => eventItem.id === id);
    if (item && window.confirm(`Excluir “${item.title}” e seus blocos?`)) await mutate(() => api(`/events/${id}`, { method: 'DELETE' }));
  }
  if (action.dataset.action === 'toggle-block') {
    const block = state.blocks.find(item => item.id === id);
    if (block) await mutate(() => api(`/blocks/${id}/completion`, { method: 'PATCH', body: JSON.stringify({ completed: !block.completed }) }));
  }
});

$('#new-subject').addEventListener('click', () => openSubject());
$('#new-event').addEventListener('click', () => openEvent());
$('#close-editor').addEventListener('click', () => $('#editor').close());
$('#cancel-editor').addEventListener('click', () => $('#editor').close());
$('#editor').addEventListener('click', event => { if (event.target === $('#editor')) $('#editor').close(); });
$('#availability-form').addEventListener('submit', async event => {
  event.preventDefault();
  const form = new FormData(event.currentTarget);
  const daysOfWeek = form.getAll('daysOfWeek').map(Number);
  const startTime = form.get('startTime'); const endTime = form.get('endTime');
  if (!daysOfWeek.length) return showNotice('Selecione ao menos um dia.', true);
  if (startTime >= endTime) return showNotice('O horário inicial precisa ser anterior ao final.', true);
  await mutate(() => api('/availability', { method: 'PUT', body: JSON.stringify({ daysOfWeek, startTime, endTime }) }));
});
$('#clear-availability').addEventListener('click', async () => {
  if (state.availability.length && window.confirm('Limpar todos os horários disponíveis?')) await mutate(() => api('/availability', { method: 'DELETE' }));
});
$('#generate-schedule').addEventListener('click', async () => {
  try {
    const result = await api('/schedule/generate', { method: 'POST' });
    state.blocks = result.blocks;
    state.scheduleWarnings = result.warnings;
    await refresh();
    showNotice('Cronograma atualizado com dados salvos no servidor.');
  } catch (error) { showNotice(error.message, true); }
});

refresh().catch(error => {
  connection(false, 'Servidor indisponível');
  showNotice(`Não foi possível carregar os dados da aplicação. ${error.message}`, true);
});
