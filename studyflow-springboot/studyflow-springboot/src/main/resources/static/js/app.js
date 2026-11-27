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
    todayBlocks: [],
    week: getMonday(new Date()),
    editing: null,
    modalType: null
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

    const response = await fetch(API_URL + endpoint, {

        ...options,

        headers: {
            "Content-Type": "application/json",
            ...(options.headers || {})
        }

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

        button.classList.toggle(
            "active",
            button.dataset.view === viewName
        );

    });


    const labels = {

        dashboard: "Visão geral",

        schedule: "Meu cronograma",

        events: "Provas e trabalhos",

        disciplines: "Disciplinas",

        availability: "Disponibilidade"

    };


    $("#crumb").textContent = labels[viewName] || "Visão geral";


    $("#sidebar").classList.remove("open");


    if (viewName === "schedule") {
        loadBlocks();
    }

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

    const today = state.todayBlocks
        .slice()
        .sort((a, b) => a.startTime.localeCompare(b.startTime));


    if (today.length === 0) {

        $("#todayList").innerHTML = `
            <div class="empty">
                Nenhum bloco previsto para hoje.
                Organize sua semana!
            </div>
        `;

        return;
    }


    $("#todayList").innerHTML = today.map(block => {

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

    }).join("");

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
                Nenhum prazo futuro cadastrado.
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
                Nenhuma prova ou trabalho cadastrado.
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
                Cadastre sua primeira disciplina.
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
                Adicione suas janelas livres acima.
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


        const total = result ? result.length : 0;


        showToast(
            `Cronograma gerado: ${total} bloco(s) criado(s).`
        );


    } catch (error) {

        showToast(error.message);

    }

}


// ==========================================
// 25. NAVEGAÇÃO DAS SEMANAS
// ==========================================

function previousWeek() {

    state.week = addDays(state.week, -7);

    loadBlocks();

}


function nextWeek() {

    state.week = addDays(state.week, 7);

    loadBlocks();

}


function currentWeek() {

    state.week = getMonday(new Date());

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

    $("#sidebar").classList.toggle("open");

});


// ==========================================
// 31. TECLA ESC PARA FECHAR MODAL
// ==========================================

document.addEventListener("keydown", event => {

    if (event.key === "Escape") {
        closeModal();
    }

});


// ==========================================
// 32. INICIALIZAÇÃO DO STUDYFLOW
// ==========================================

loadAll();