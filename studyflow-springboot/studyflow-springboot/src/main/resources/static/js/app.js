// ==========================================
// STUDYFLOW - JAVASCRIPT
// Integração com Spring Boot e SQLite
// ==========================================


// ==========================================
// 1. CONFIGURAÇÕES INICIAIS
// ==========================================

const API_URL = "";

const state = {
    disciplines: [],
    events: [],
    availabilities: [],
    blocks: [],
    scheduleWarnings: [],
    todayBlocks: [],
    week: getMonday(new Date()),
    editing: null,
    modalType: null,
    modalOpener: null
};

const $ = selector => document.querySelector(selector);

const $$ = selector => [...document.querySelectorAll(selector)];

const days = [
    "Domingo",
    "Segunda-feira",
    "Terça-feira",
    "Quarta-feira",
    "Quinta-feira",
    "Sexta-feira",
    "Sábado"
];

let toastTimer;
let csrfToken = null;
let currentProfile = null;


// ==========================================
// 2. FUNÇÕES AUXILIARES
// ==========================================

function getMonday(date) {

    const d = new Date(date);

    d.setHours(12, 0, 0, 0);

    const day = d.getDay();

    d.setDate(d.getDate() - (day === 0 ? 6 : day - 1));

    return d;
}


function addDays(date, amount) {

    const d = new Date(date);

    d.setDate(d.getDate() + amount);

    return d;
}


function formatISO(date) {

    const d = new Date(date);

    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(d.getDate()).padStart(2, "0")}`;
}


function formatDate(date, options = {
    day: "2-digit",
    month: "short"
}) {

    if (!date) {
        return "—";
    }

    return new Intl.DateTimeFormat("pt-BR", options)
        .format(new Date(date + "T12:00:00"));
}


function escapeHTML(value = "") {

    return String(value).replace(/[&<>"']/g, character => {

        const entities = {
            "&": "&amp;",
            "<": "&lt;",
            ">": "&gt;",
            '"': "&quot;",
            "'": "&#39;"
        };

        return entities[character];

    });
}


function showToast(message) {

    const toast = $("#toast");

    toast.textContent = message;

    toast.classList.add("show");

    clearTimeout(toastTimer);

    toastTimer = setTimeout(() => {
        toast.classList.remove("show");
    }, 3000);

}


// ==========================================
// 3. COMUNICAÇÃO COM O BACK-END
// ==========================================

async function api(endpoint, options = {}) {

    const method = (options.method || "GET").toUpperCase();
    const headers = {
        "Content-Type": "application/json",
        ...(options.headers || {})
    };

    if (!["GET", "HEAD", "OPTIONS"].includes(method)) {
        if (!csrfToken) await refreshCsrf();
        headers[csrfToken.headerName] = csrfToken.token;
    }

    const response = await fetch(API_URL + endpoint, {

        ...options,

        method,
        credentials: "same-origin",
        headers

    });


    if (!response.ok) {

        let message = `Erro ${response.status}`;

        try {

            const data = await response.json();

            message = data.message || data.detail || message;

        } catch (error) {
            // Mantém a mensagem padrão.
        }

        throw new Error(message);
    }


    if (response.status === 204) {
        return null;
    }


    const contentType = response.headers.get("content-type") || "";

    if (contentType.includes("application/json")) {
        return response.json();
    }

    return null;

}

async function refreshCsrf() {
    const response = await fetch(`${API_URL}/api/auth/csrf`, {
        credentials: "same-origin"
    });
    if (!response.ok) throw new Error("Não foi possível iniciar uma sessão segura.");
    csrfToken = await response.json();
    return csrfToken;
}

function setAuthMode(mode) {
    const registering = mode === "register";
    $("#loginForm").hidden = registering;
    $("#registerForm").hidden = !registering;
    $("#authTitle").textContent = registering ? "Crie sua conta" : "Que bom ter você de volta";
    $("#authDescription").textContent = registering
        ? "Organize suas disciplinas, prazos e horários em um só lugar."
        : "Entre para acessar seu cronograma e continuar seus estudos.";
    $("#authMessage").hidden = true;
}

function showAuth(message = "") {
    $("#appLayout").hidden = true;
    $("#authScreen").hidden = false;
    setAuthMode("login");
    $("#authMessage").textContent = message;
    $("#authMessage").hidden = !message;
}

function showApp(profile) {
    currentProfile = profile;
    $("#authScreen").hidden = true;
    $("#appLayout").hidden = false;
    $("#userName").textContent = profile.name;
    $("#userAvatar").textContent = profile.name.trim().charAt(0).toUpperCase();
    loadAll();
}

async function initializeApp() {
    try {
        await refreshCsrf();
        const profile = await api("/api/auth/me");
        showApp(profile);
    } catch (error) {
        if (error.message === "Faça login para continuar.") {
            showAuth();
        } else {
            showAuth("Não foi possível conectar ao StudyFlow. Confira se o servidor está rodando.");
        }
    }
}


// ==========================================
// 4. DADOS DAS DISCIPLINAS E EVENTOS
// ==========================================

function getDisciplineName(id) {

    const discipline = state.disciplines.find(
        item => Number(item.id) === Number(id)
    );

    return discipline ? discipline.name : "Disciplina";
}


function getDisciplineColor(id) {

    const discipline = state.disciplines.find(
        item => Number(item.id) === Number(id)
    );

    const color = discipline?.color;

    return /^#[0-9a-f]{3,8}$/i.test(color || "")
        ? color
        : "#6d5ce7";
}


function getEventTitle(id) {

    const event = state.events.find(
        item => Number(item.id) === Number(id)
    );

    return event ? event.title : "Sessão de estudo";

}


// ==========================================
// 5. NAVEGAÇÃO ENTRE AS TELAS
// ==========================================

function showView(viewName) {

    $$(".view").forEach(view => {

        view.classList.toggle(
            "active",
            view.id === viewName
        );

    });


    $$(".nav").forEach(button => {

        const isActive = button.dataset.view === viewName;
        button.classList.toggle("active", isActive);

        if (isActive) {
            button.setAttribute("aria-current", "page");
        } else {
            button.removeAttribute("aria-current");
        }

    });


    const labels = {

        dashboard: "Visão geral",

        schedule: "Meu cronograma",

        events: "Provas e trabalhos",

        disciplines: "Disciplinas",

        availability: "Disponibilidade"

    };


    $("#crumb").textContent = labels[viewName] || "Visão geral";


    setSidebarOpen(false);


    if (viewName === "schedule") {
        loadBlocks();
    }

}

function setSidebarOpen(isOpen) {
    $("#sidebar").classList.toggle("open", isOpen);
    $("#sidebarOverlay").hidden = !isOpen;
    $("#menu").setAttribute("aria-expanded", String(isOpen));
    $("#menu").setAttribute("aria-label", isOpen ? "Fechar menu" : "Abrir menu");
    document.body.classList.toggle("menu-open", isOpen);
}


// ==========================================
// 6. STATUS DA CONEXÃO
// ==========================================

function updateConnectionStatus(connected) {

    $("#statusText").textContent = connected
        ? "Conectado ao servidor"
        : "Servidor desconectado";

    $("#statusDot").classList.toggle(
        "offline",
        !connected
    );

}


// ==========================================
// 7. CARREGAR DADOS DO SISTEMA
// ==========================================

async function loadAll() {

    try {

        const [
            disciplines,
            events,
            availabilities
        ] = await Promise.all([

            api("/api/disciplines"),

            api("/api/events"),

            api("/api/availabilities")

        ]);


        state.disciplines = disciplines || [];

        state.events = events || [];

        state.availabilities = availabilities || [];


        updateConnectionStatus(true);


        renderAll();


        await Promise.all([

            loadBlocks(),

            loadTodayBlocks()

        ]);


    } catch (error) {

        updateConnectionStatus(false);

        showToast("Erro ao carregar os dados: " + error.message);

        $("#todayList").innerHTML = `
            <div class="empty">
                Não foi possível conectar ao servidor.
                Verifique se o Spring Boot está funcionando.
            </div>
        `;

    }

}


// ==========================================
// 8. CARREGAR CRONOGRAMA SEMANAL
// ==========================================

async function loadBlocks() {

    try {

        const start = formatISO(state.week);

        const end = formatISO(addDays(state.week, 6));


        const blocks = await api(
            `/api/blocks?from=${start}&to=${end}`
        );


        state.blocks = blocks || [];


        renderSchedule();

        renderDashboard();


    } catch (error) {

        $("#scheduleList").innerHTML = `
            <div class="empty">
                ${escapeHTML(error.message)}
            </div>
        `;

    }

}


// ==========================================
// 9. CARREGAR ATIVIDADES DE HOJE
// ==========================================

async function loadTodayBlocks() {

    try {

        const today = formatISO(new Date());

        const blocks = await api(
            `/api/blocks/today?date=${today}`
        );

        state.todayBlocks = blocks || [];

        renderDashboard();

    } catch (error) {

        state.todayBlocks = [];

        renderDashboard();

    }

}


// ==========================================
// 10. ATUALIZAR TODAS AS TELAS
// ==========================================

function renderAll() {

    renderDashboard();

    renderEvents();

    renderDisciplines();

    renderAvailability();

    renderSchedule();

}


// ==========================================
// 11. DASHBOARD
// ==========================================

function renderDashboard() {

    $("#statDisciplines").textContent =
        state.disciplines.length;


    $("#statEvents").textContent =
        state.events.length;


    const completed = state.blocks.filter(
        block => block.completed
    ).length;


    $("#statDone").textContent = completed;


    const minutes = state.blocks.reduce(
        (total, block) => total + Number(block.durationMinutes || 0),
        0
    );


    $("#statHours").textContent =
        (minutes / 60).toFixed(1).replace(".", ",") + "h";


    $("#todayLabel").textContent =
        new Intl.DateTimeFormat("pt-BR", {
            weekday: "long",
            day: "numeric",
            month: "long"
        }).format(new Date()).toUpperCase();


    renderTodayTasks();

    renderUpcomingEvents();

}


// ==========================================
// 12. ATIVIDADES DE HOJE
// ==========================================

function renderTodayTasks() {

    const todayDate = formatISO(new Date());
    const todayBlocks = state.todayBlocks
        .slice()
        .sort((a, b) => a.startTime.localeCompare(b.startTime));
    const dueToday = state.events
        .filter(event => event.dueDate === todayDate)
        .sort((a, b) => a.title.localeCompare(b.title, "pt-BR"));


    if (todayBlocks.length === 0 && dueToday.length === 0) {

        $("#todayList").innerHTML = `
            <div class="empty">
                <span class="empty-icon" aria-hidden="true">☀</span>
                <strong>Nada planejado para hoje.</strong>
                <span>Cadastre um prazo ou configure seus horários e gere o cronograma.</span>
                <button class="text-button" data-view="availability">Configurar horários</button>
            </div>
        `;

        return;
    }


    const studyTasks = todayBlocks.map(block => {

        return `

            <div class="task">

                <button
                    class="check ${block.completed ? "done" : ""}"
                    data-complete="${block.id}"
                >
                    ${block.completed ? "✓" : ""}
                </button>

                <div class="task-info">

                    <b>
                        ${escapeHTML(getEventTitle(block.studyEventId))}
                    </b>

                    <small>
                        ${escapeHTML(getDisciplineName(block.disciplineId))}
                    </small>

                </div>

                <span class="time">
                    ${escapeHTML(block.startTime)}
                    –
                    ${escapeHTML(block.endTime)}
                </span>

            </div>

        `;

    });

    const deadlineTasks = dueToday.map(event => {
        const type = event.eventType === "EXAM" ? "Prova" : "Trabalho";
        return `
            <div class="task task-deadline">
                <span class="deadline-marker" aria-hidden="true">!</span>
                <div class="task-info">
                    <b>${escapeHTML(event.title)}</b>
                    <small>${type} · ${escapeHTML(getDisciplineName(event.disciplineId))}</small>
                </div>
                <span class="time deadline-time">${type} hoje</span>
            </div>
        `;
    });

    $("#todayList").innerHTML = [...deadlineTasks, ...studyTasks].join("");

}


// ==========================================
// 13. PRÓXIMOS PRAZOS
// ==========================================

function renderUpcomingEvents() {

    const today = formatISO(new Date());


    const upcoming = state.events

        .filter(event => event.dueDate >= today)

        .sort((a, b) => a.dueDate.localeCompare(b.dueDate))

        .slice(0, 4);


    if (upcoming.length === 0) {

        $("#upcoming").innerHTML = `
            <div class="empty">
                <span class="empty-icon" aria-hidden="true">◷</span>
                <strong>Sem prazos por enquanto.</strong>
                <span>Adicione uma prova ou trabalho para planejar seus estudos.</span>
                <button class="text-button" data-action="new-event">Adicionar compromisso</button>
            </div>
        `;

        return;
    }


    $("#upcoming").innerHTML = upcoming.map(event => {

        return `

            <div class="deadline">

                <div class="datebox">

                    <b>
                        ${formatDate(event.dueDate, { day: "2-digit" })}
                    </b>

                    <small>
                        ${formatDate(event.dueDate, { month: "short" })}
                    </small>

                </div>


                <div class="deadline-info">

                    <b>
                        ${escapeHTML(event.title)}
                    </b>

                    <small>
                        ${escapeHTML(getDisciplineName(event.disciplineId))}
                        ·
                        ${event.eventType === "EXAM" ? "Prova" : "Trabalho"}
                    </small>

                </div>

            </div>

        `;

    }).join("");

}


// ==========================================
// 14. LISTAR PROVAS E TRABALHOS
// ==========================================

function renderEvents() {

    const container = $("#eventsList");


    const events = state.events.slice().sort(
        (a, b) => a.dueDate.localeCompare(b.dueDate)
    );


    if (events.length === 0) {

        container.innerHTML = `
            <div class="empty">
                <span class="empty-icon" aria-hidden="true">◷</span>
                <strong>Você ainda não tem compromissos.</strong>
                <span>Cadastre uma prova ou trabalho para organizar o que precisa estudar.</span>
                <button class="text-button" data-action="new-event">Adicionar compromisso</button>
            </div>
        `;

        return;
    }


    container.innerHTML = events.map(event => {

        return `

            <div class="row">

                <div class="row-title">

                    <b>
                        ${escapeHTML(event.title)}
                    </b>

                    <small>
                        ${escapeHTML(getDisciplineName(event.disciplineId))}
                        ·
                        ${event.estimatedHours}h estimadas
                    </small>

                </div>


                <span class="tag">
                    ${event.eventType === "EXAM" ? "Prova" : "Trabalho"}
                </span>


                <span class="muted">
                    ${formatDate(event.dueDate, {
                        day: "2-digit",
                        month: "short",
                        year: "numeric"
                    })}
                </span>


                <div class="row-actions">

                    <button data-edit-event="${event.id}">
                        Editar
                    </button>

                    <button data-del-event="${event.id}">
                        Excluir
                    </button>

                </div>

            </div>

        `;

    }).join("");

}


// ==========================================
// 15. LISTAR DISCIPLINAS
// ==========================================

function renderDisciplines() {

    const container = $("#disciplinesList");


    if (state.disciplines.length === 0) {

        container.innerHTML = `
            <div class="empty">
                <span class="empty-icon" aria-hidden="true">▤</span>
                <strong>Comece cadastrando uma disciplina.</strong>
                <span>Assim você pode relacionar provas e trabalhos a cada matéria.</span>
                <button class="text-button" data-action="new-discipline">Adicionar disciplina</button>
            </div>
        `;

        return;
    }


    container.innerHTML = state.disciplines.map(discipline => {

        return `

            <article
                class="discipline-card"
                style="--c: ${getDisciplineColor(discipline.id)}"
            >

                <b>
                    ${escapeHTML(discipline.name)}
                </b>

                <p>
                    ${escapeHTML(discipline.description || "Sem descrição")}
                </p>

                <div class="row-actions">

                    <button data-edit-discipline="${discipline.id}">
                        Editar
                    </button>

                    <button data-del-discipline="${discipline.id}">
                        Excluir
                    </button>

                </div>

            </article>

        `;

    }).join("");

}


// ==========================================
// 16. LISTAR DISPONIBILIDADES
// ==========================================

function renderAvailability() {

    const container = $("#availabilityList");


    if (state.availabilities.length === 0) {

        container.innerHTML = `
            <div class="empty">
                <span class="empty-icon" aria-hidden="true">◴</span>
                <strong>Nenhum horário cadastrado ainda.</strong>
                <span>Use o formulário acima para dizer quando costuma poder estudar.</span>
            </div>
        `;

        return;
    }


    const availabilities = state.availabilities.slice().sort(
        (a, b) => a.dayOfWeek - b.dayOfWeek
    );


    container.innerHTML = availabilities.map(availability => {

        const dayName = days[Number(availability.dayOfWeek) % 7];


        return `

            <div class="availability-row">

                <div>

                    <b>${dayName}</b>

                    <span>
                        ${escapeHTML(availability.startTime)}
                        –
                        ${escapeHTML(availability.endTime)}
                    </span>

                </div>

                <button
                    class="remove"
                    data-del-availability="${availability.id}"
                >
                    Remover
                </button>

            </div>

        `;

    }).join("");

}


// ==========================================
// 17. CRONOGRAMA SEMANAL
// ==========================================

function renderSchedule() {

    renderScheduleWarnings(state.scheduleWarnings);

    const end = addDays(state.week, 6);


    $("#weekTitle").textContent =
        `${formatDate(formatISO(state.week))} – ${formatDate(formatISO(end))}`;


    $("#weekRange").textContent =
        `${formatDate(formatISO(state.week), {
            day: "2-digit",
            month: "long",
            year: "numeric"
        })} até ${formatDate(formatISO(end), {
            day: "2-digit",
            month: "long",
            year: "numeric"
        })}`;


    const weekDays = Array.from(
        { length: 7 },
        (_, index) => addDays(state.week, index)
    );


    $("#scheduleList").innerHTML = weekDays.map(day => {

        const date = formatISO(day);


        const blocks = state.blocks

            .filter(block => block.studyDate === date)

            .sort((a, b) => a.startTime.localeCompare(b.startTime));


        return `

            <div class="day">

                <div class="day-title">

                    ${days[day.getDay()]}

                    <span>
                        ${formatDate(date, {
                            day: "2-digit",
                            month: "long"
                        })}
                    </span>

                </div>


                ${blocks.length > 0

                    ? blocks.map(block => `

                        <div
                            class="block ${block.completed ? "completed" : ""}"
                            style="border-left-color: ${getDisciplineColor(block.disciplineId)}"
                        >

                            <span class="block-time">

                                ${escapeHTML(block.startTime)}
                                –
                                ${escapeHTML(block.endTime)}

                            </span>


                            <div class="block-info">

                                <b>
                                    ${escapeHTML(getEventTitle(block.studyEventId))}
                                </b>

                                <small>
                                    ${escapeHTML(getDisciplineName(block.disciplineId))}
                                    ·
                                    ${block.durationMinutes} min
                                </small>

                            </div>


                            <button
                                class="action-link"
                                data-complete="${block.id}"
                            >
                                ${block.completed ? "Desfazer" : "Concluir"}
                            </button>

                        </div>

                    `).join("")

                    : `<small class="muted">
                        Nenhum bloco planejado.
                    </small>`

                }

            </div>

        `;

    }).join("");

}


// ==========================================
// 18. ABRIR MODAL DE CADASTRO
// ==========================================

function openModal(type, record = null) {

    if (type === "event" && state.disciplines.length === 0) {

        showToast("Cadastre uma disciplina antes de criar um compromisso.");

        return;
    }


    state.modalOpener = document.activeElement;
    state.modalType = type;

    state.editing = record ? record.id : null;


    $("#modalTitle").textContent = record
        ? `Editar ${type === "event" ? "compromisso" : "disciplina"}`
        : type === "event"
            ? "Novo compromisso"
            : "Nova disciplina";


    let fields = "";


    if (type === "discipline") {

        fields = `

            <label class="full">

                Nome

                <input
                    name="name"
                    required
                    maxlength="80"
                    value="${escapeHTML(record?.name || "")}"
                    placeholder="Ex.: Programação"
                >

            </label>


            <label>

                Cor

                <input
                    type="color"
                    name="color"
                    value="${escapeHTML(record?.color || "#6d5ce7")}"
                >

            </label>


            <label class="full">

                Descrição

                <textarea
                    name="description"
                    maxlength="500"
                >${escapeHTML(record?.description || "")}</textarea>

            </label>

        `;

    } else {

        const options = state.disciplines.map(discipline => {

            return `

                <option
                    value="${discipline.id}"
                    ${Number(record?.disciplineId) === Number(discipline.id) ? "selected" : ""}
                >
                    ${escapeHTML(discipline.name)}
                </option>

            `;

        }).join("");


        fields = `

            <label class="full">

                Título

                <input
                    name="title"
                    required
                    maxlength="120"
                    value="${escapeHTML(record?.title || "")}"
                    placeholder="Ex.: Prova de programação"
                >

            </label>


            <label>

                Tipo

                <select name="eventType">

                    <option
                        value="EXAM"
                        ${record?.eventType === "EXAM" ? "selected" : ""}
                    >
                        Prova
                    </option>

                    <option
                        value="ASSIGNMENT"
                        ${record?.eventType === "ASSIGNMENT" ? "selected" : ""}
                    >
                        Trabalho
                    </option>

                </select>

            </label>


            <label>

                Disciplina

                <select name="disciplineId" required>

                    ${options}

                </select>

            </label>


            <label>

                Data

                <input
                    name="dueDate"
                    type="date"
                    required
                    value="${escapeHTML(record?.dueDate || "")}"
                >

            </label>


            <label>

                Horas estimadas

                <input
                    name="estimatedHours"
                    type="number"
                    min="0.5"
                    max="300"
                    step="0.5"
                    required
                    value="${record?.estimatedHours ?? 2}"
                >

            </label>


            <label class="full">

                Descrição

                <textarea
                    name="description"
                    maxlength="1000"
                >${escapeHTML(record?.description || "")}</textarea>

            </label>

        `;

    }


    $("#fields").innerHTML = fields;

    $("#modal").hidden = false;
    requestAnimationFrame(() => $("#fields").querySelector("input, select, textarea")?.focus());

}


// ==========================================
// 19. FECHAR MODAL
// ==========================================

function closeModal() {

    $("#modal").hidden = true;

    $("#modalForm").reset();

    $("#fields").innerHTML = "";

    state.editing = null;

    state.modalType = null;

    state.modalOpener?.focus();
    state.modalOpener = null;

}


// ==========================================
// 20. SALVAR DISCIPLINA OU EVENTO
// ==========================================

async function saveModal(event) {

    event.preventDefault();


    const formData = new FormData(event.currentTarget);

    const data = Object.fromEntries(formData.entries());


    try {

        if (state.modalType === "discipline") {

            const payload = {

                name: data.name,

                color: data.color,

                description: data.description

            };


            const editing = state.editing !== null;


            const endpoint = editing

                ? `/api/disciplines/${state.editing}`

                : "/api/disciplines";


            await api(endpoint, {

                method: editing ? "PUT" : "POST",

                body: JSON.stringify(payload)

            });

        }


        else if (state.modalType === "event") {

            const payload = {

                title: data.title,

                eventType: data.eventType,

                dueDate: data.dueDate,

                estimatedHours: Number(data.estimatedHours),

                disciplineId: Number(data.disciplineId),

                description: data.description

            };


            const editing = state.editing !== null;


            const endpoint = editing

                ? `/api/events/${state.editing}`

                : "/api/events";


            await api(endpoint, {

                method: editing ? "PUT" : "POST",

                body: JSON.stringify(payload)

            });

        }


        closeModal();

        await loadAll();

        showToast("Cadastro salvo com sucesso!");


    } catch (error) {

        showToast(error.message);

    }

}


// ==========================================
// 21. CADASTRAR DISPONIBILIDADE
// ==========================================

async function saveAvailability(event) {

    event.preventDefault();


    const form = event.currentTarget;

    const data = Object.fromEntries(
        new FormData(form).entries()
    );


    if (data.endTime <= data.startTime) {

        showToast("O horário final deve ser depois do inicial.");

        return;
    }


    try {

        const payload = {

            dayOfWeek: Number(data.dayOfWeek),

            startTime: data.startTime,

            endTime: data.endTime

        };


        await api("/api/availabilities", {

            method: "POST",

            body: JSON.stringify(payload)

        });


        form.reset();

        await loadAll();

        showToast("Horário adicionado com sucesso!");


    } catch (error) {

        showToast(error.message);

    }

}


// ==========================================
// 22. EXCLUIR REGISTROS
// ==========================================

async function deleteRecord(endpoint, description) {

    const confirmed = confirm(
        `Tem certeza de que deseja excluir ${description}?`
    );


    if (!confirmed) {
        return;
    }


    try {

        await api(endpoint, {

            method: "DELETE"

        });


        await loadAll();

        showToast("Registro excluído com sucesso!");


    } catch (error) {

        showToast(error.message);

    }

}


// ==========================================
// 23. CONCLUIR OU REABRIR BLOCO DE ESTUDO
// ==========================================

async function toggleBlockCompletion(id) {

    const block = state.blocks.find(
        item => Number(item.id) === Number(id)
    );


    if (!block) {
        return;
    }


    try {

        await api(`/api/blocks/${id}/completion`, {

            method: "PATCH",

            body: JSON.stringify({

                completed: !block.completed

            })

        });


        await loadBlocks();

        await loadTodayBlocks();


        showToast(
            block.completed
                ? "Bloco reaberto."
                : "Bloco concluído!"
        );


    } catch (error) {

        showToast(error.message);

    }

}


// ==========================================
// 24. GERAR CRONOGRAMA SEMANAL
// ==========================================

async function generateSchedule() {

    try {

        const weekStart = formatISO(state.week);


        const result = await api(
            `/api/schedule/generate?weekStart=${weekStart}`,
            {
                method: "POST"
            }
        );


        await loadBlocks();

        await loadTodayBlocks();


        const blocks = result?.blocks || [];
        state.scheduleWarnings = result?.warnings || [];
        renderScheduleWarnings(state.scheduleWarnings);


        showToast(
            `Cronograma gerado: ${blocks.length} bloco(s) criado(s).`
        );


    } catch (error) {

        showToast(error.message);

    }

}

function renderScheduleWarnings(warnings) {
    const container = $("#scheduleWarning");
    if (!container) return;

    if (!warnings.length) {
        container.hidden = true;
        container.innerHTML = "";
        return;
    }

    const warningItems = warnings.map(warning => {
        const minutes = Number(warning.remainingMinutes || 0);
        const hours = Math.floor(minutes / 60);
        const rest = minutes % 60;
        const remaining = [
            hours ? `${hours}h` : "",
            rest ? `${rest}min` : ""
        ].filter(Boolean).join(" ");

        return `<li><strong>${escapeHTML(getEventTitle(warning.studyEventId))}</strong>: ${remaining} de esforço estimado ficaram sem horário nesta semana.</li>`;
    }).join("");

    container.innerHTML = `
        <strong>Não foi possível encaixar todo o esforço estimado.</strong>
        <ul>${warningItems}</ul>
        <small>Considere revisar sua disponibilidade ou o prazo da entrega.</small>
    `;
    container.hidden = false;
}


// ==========================================
// 25. NAVEGAÇÃO DAS SEMANAS
// ==========================================

function previousWeek() {

    state.week = addDays(state.week, -7);
    state.scheduleWarnings = [];

    loadBlocks();

}


function nextWeek() {

    state.week = addDays(state.week, 7);
    state.scheduleWarnings = [];

    loadBlocks();

}


function currentWeek() {

    state.week = getMonday(new Date());
    state.scheduleWarnings = [];

    loadBlocks();

}


// ==========================================
// 26. EVENTOS DE CLIQUE
// ==========================================

document.addEventListener("click", event => {


    // Navegação entre telas

    const viewButton = event.target.closest("[data-view]");

    if (viewButton) {

        showView(viewButton.dataset.view);

        return;
    }


    // Ações rápidas

    const actionButton = event.target.closest("[data-action]");

    if (actionButton) {

        const action = actionButton.dataset.action;


        if (action === "new-event") {
            openModal("event");
        }


        if (action === "new-discipline") {
            openModal("discipline");
        }


        if (action === "generate") {
            generateSchedule();
        }


        if (action === "recommend-study") {
            requestStudyRecommendation(actionButton);
        }

    }


    // Concluir bloco

    const completeButton = event.target.closest("[data-complete]");

    if (completeButton) {

        toggleBlockCompletion(
            completeButton.dataset.complete
        );

    }


    // Editar disciplina

    const editDiscipline = event.target.closest("[data-edit-discipline]");

    if (editDiscipline) {

        const discipline = state.disciplines.find(
            item => Number(item.id) === Number(editDiscipline.dataset.editDiscipline)
        );

        if (discipline) {
            openModal("discipline", discipline);
        }

    }


    // Editar evento

    const editEvent = event.target.closest("[data-edit-event]");

    if (editEvent) {

        const eventData = state.events.find(
            item => Number(item.id) === Number(editEvent.dataset.editEvent)
        );

        if (eventData) {
            openModal("event", eventData);
        }

    }


    // Excluir disciplina

    const deleteDiscipline = event.target.closest("[data-del-discipline]");

    if (deleteDiscipline) {

        deleteRecord(
            `/api/disciplines/${deleteDiscipline.dataset.delDiscipline}`,
            "esta disciplina"
        );

    }


    // Excluir evento

    const deleteEvent = event.target.closest("[data-del-event]");

    if (deleteEvent) {

        deleteRecord(
            `/api/events/${deleteEvent.dataset.delEvent}`,
            "este compromisso"
        );

    }


    // Excluir disponibilidade

    const deleteAvailability = event.target.closest("[data-del-availability]");

    if (deleteAvailability) {

        deleteRecord(
            `/api/availabilities/${deleteAvailability.dataset.delAvailability}`,
            "este horário"
        );

    }

});


async function requestStudyRecommendation(button) {
    const panel = $("#aiRecommendation");
    const originalText = button.textContent;
    button.disabled = true;
    button.textContent = "Pensando...";
    panel.hidden = false;
    panel.classList.remove("has-error");
    panel.classList.add("is-loading");
    $("#aiRecommendationTitle").textContent = "Preparando sua recomendação...";
    $("#aiRecommendationReason").textContent = "Analisando seus prazos e sua rotina de estudos.";
    $("#aiRecommendationAction").textContent = "";
    $("#aiRecommendationDuration").textContent = "";

    try {
        const recommendation = await api("/api/ai/study-recommendation", {
            method: "POST",
            body: JSON.stringify({})
        });
        $("#aiRecommendationTitle").textContent = recommendation.title;
        $("#aiRecommendationReason").textContent = recommendation.reason;
        $("#aiRecommendationAction").textContent = recommendation.action;
        $("#aiRecommendationDuration").textContent = `${recommendation.durationMinutes} min`;
        panel.classList.remove("is-loading");
    } catch (error) {
        $("#aiRecommendationTitle").textContent = "Não foi possível gerar a sugestão";
        $("#aiRecommendationReason").textContent = error.message;
        panel.classList.remove("is-loading");
        panel.classList.add("has-error");
    } finally {
        button.disabled = false;
        button.textContent = originalText;
    }
}


// ==========================================
// 27. CONFIGURAÇÃO DOS FORMULÁRIOS
// ==========================================

$("#modalForm").addEventListener(
    "submit",
    saveModal
);


$("#availabilityForm").addEventListener(
    "submit",
    saveAvailability
);

$$('[data-auth-mode]').forEach(button => {
    button.addEventListener("click", () => setAuthMode(button.dataset.authMode));
});

$("#loginForm").addEventListener("submit", async event => {
    event.preventDefault();
    const form = event.currentTarget;
    const data = Object.fromEntries(new FormData(form).entries());
    const message = $("#authMessage");
    message.hidden = true;

    try {
        const profile = await api("/api/auth/login", {
            method: "POST",
            body: JSON.stringify(data)
        });
        await refreshCsrf();
        form.reset();
        showApp(profile);
    } catch (error) {
        message.textContent = error.message;
        message.hidden = false;
    }
});

$("#registerForm").addEventListener("submit", async event => {
    event.preventDefault();
    const form = event.currentTarget;
    const data = Object.fromEntries(new FormData(form).entries());
    const message = $("#authMessage");
    message.hidden = true;

    if (data.password !== data.confirmPassword) {
        message.textContent = "As senhas digitadas não são iguais.";
        message.hidden = false;
        return;
    }

    try {
        await api("/api/auth/register", {
            method: "POST",
            body: JSON.stringify({ name: data.name, email: data.email, password: data.password })
        });
        const profile = await api("/api/auth/login", {
            method: "POST",
            body: JSON.stringify({ email: data.email, password: data.password })
        });
        await refreshCsrf();
        form.reset();
        showApp(profile);
    } catch (error) {
        message.textContent = error.message;
        message.hidden = false;
    }
});

$("#profileButton").addEventListener("click", () => {
    $("#profileName").value = currentProfile.name;
    $("#profileEmail").value = currentProfile.email;
    $("#profileModal").hidden = false;
    requestAnimationFrame(() => $("#profileName").focus());
});

function closeProfile() {
    $("#profileModal").hidden = true;
    $("#profileButton").focus();
}

$("#closeProfile").addEventListener("click", closeProfile);
$("#profileCancel").addEventListener("click", closeProfile);
$("#profileModal").addEventListener("click", event => {
    if (event.target.id === "profileModal") closeProfile();
});

$("#profileForm").addEventListener("submit", async event => {
    event.preventDefault();
    const name = $("#profileName").value.trim();
    try {
        currentProfile = await api("/api/auth/profile", {
            method: "PUT",
            body: JSON.stringify({ name })
        });
        $("#userName").textContent = currentProfile.name;
        $("#userAvatar").textContent = currentProfile.name.trim().charAt(0).toUpperCase();
        closeProfile();
        showToast("Perfil atualizado.");
    } catch (error) {
        showToast(error.message);
    }
});

$("#logoutButton").addEventListener("click", async () => {
    try {
        await api("/api/auth/logout", { method: "POST" });
        currentProfile = null;
        showAuth();
    } catch (error) {
        showToast(error.message);
    }
});


// ==========================================
// 28. BOTÕES DO MODAL
// ==========================================

$("#close").addEventListener(
    "click",
    closeModal
);


$("#cancel").addEventListener(
    "click",
    closeModal
);


$("#modal").addEventListener("click", event => {

    if (event.target.id === "modal") {
        closeModal();
    }

});


// ==========================================
// 29. CONTROLES DO CRONOGRAMA
// ==========================================

$("#prev").addEventListener(
    "click",
    previousWeek
);


$("#next").addEventListener(
    "click",
    nextWeek
);


$("#todayWeek").addEventListener(
    "click",
    currentWeek
);


// ==========================================
// 30. MENU PARA CELULARES
// ==========================================

$("#menu").addEventListener("click", () => {
    setSidebarOpen(!$("#sidebar").classList.contains("open"));
});

$("#sidebarOverlay").addEventListener("click", () => setSidebarOpen(false));


// ==========================================
// 31. TECLA ESC PARA FECHAR MODAL
// ==========================================

document.addEventListener("keydown", event => {

    if (event.key === "Escape") {
        if (!$("#modal").hidden) {
            closeModal();
        }
        if (!$("#profileModal").hidden) {
            closeProfile();
        }
        setSidebarOpen(false);
    }

});


// ==========================================
// 32. INICIALIZAÇÃO DO STUDYFLOW
// ==========================================

initializeApp();
