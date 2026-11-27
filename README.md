# StudyFlow

**English** · [Português (Brasil)](README.pt-BR.md)

StudyFlow is a study planner that turns exams and assignments into a realistic weekly schedule.
The student says what is coming and when they can really study; StudyFlow distributes focus
blocks up to each deadline, shows what to do today, and rebuilds the plan when the week changes.

Related documents: [DECISIONS.md](DECISIONS.md) · [AI_LOG.md](AI_LOG.md) ·
[Briefing](docs/BRIEFING.md) · [AGENTS.md](AGENTS.md)

## Team

- **Class:** TDS — mixed groups 6570, 6756 and 6863
- **Tech Lead:** Ana Carolina Dutra
- **Presentation lead:** Victor Fanti Antonietti
- **Members:**
  - Ana Carolina Dutra — [@dutranacarolina](https://github.com/dutranacarolina)
  - Victor Fanti Antonietti
  - Raphael Vieira Lutkemeyer Sausen
  - Yuri José Machado Ribeiro

## Theme / area

Study planner (Agenda de estudos) — campus life and community.

## Problem

In a technical course the week fills up with exams, assignments and projects. The official
calendar says when something is due, but not when to study or how much time to reserve. Many
students improvise the night before, underestimate the effort or end up with two heavy
deadlines on the same day. When a plan exists, it rarely survives the first change.

This hits students who have several deadlines in the same week, students who freeze when it is
time to start, and students who had an unexpected event and need to rearrange everything.

## Solution

The student registers disciplines, exams and assignments (with a date and an effort estimate in
hours) and the weekly windows in which they can study. From that, StudyFlow:

1. **Generates the week**: 50-minute focus blocks with 10-minute breaks, placed only inside the
   student's free hours, most urgent deadline first.
2. **Shows what to do today**, with progress for the day and the next deadlines.
3. **Shows the whole week** as a simple calendar.
4. **Adjusts the plan**: a block can be marked as done, locked in place or postponed. Postponing
   or changing a deadline rebuilds the rest of the week and lists what moved.
5. **Warns when the effort does not fit** before a deadline, instead of producing a schedule that
   looks good but cannot be followed. The student decides what to cut or anticipate.
6. **Explains the plan** in plain language: how the week looks, what changed, and practical tips.

### Where AI comes in

- **In the product:** each generated plan comes with an explanation written by Claude (Anthropic)
  from the facts computed by the scheduler. The model never decides the schedule, so a block can
  never land outside the student's real availability. Without an API key, or if the provider
  fails, a rule-based advisor answers instead and the app keeps working.
- **In the process:** code, tests, images and documentation were produced with AI assistance and
  reviewed by the team. Everything is recorded in [AI_LOG.md](AI_LOG.md).

### MVP scope

In: the six points above, light and dark themes, responsive layout and an example week that can
be loaded in one click. Out, on purpose: accounts and login, integration with the school system,
notifications, group study sessions and data import. The reasons are in
[DECISIONS.md](DECISIONS.md).

## Stack

| Layer | Technology |
| --- | --- |
| Back-end | Java 21, Spring Boot 3.5 (Web, Validation, JDBC) |
| Database | SQLite, with Flyway migrations |
| AI | Claude through the official Anthropic Java SDK (optional) |
| Front-end | HTML, CSS and JavaScript modules, no build step |
| Tests | JUnit 5, AssertJ, MockMvc; Node test runner for the front-end |
| Lint | Spotless (google-java-format), ESLint, Prettier, commitlint |
| CI | GitHub Actions |

## Folder structure

```text
/
├── README.md / README.pt-BR.md
├── DECISIONS.md / DECISIONS.pt-BR.md
├── AI_LOG.md / AI_LOG.pt-BR.md
├── AGENTS.md                  conventions for people and AI agents working here
├── backend/                   Spring Boot application
│   └── src/main/java/com/studyflow/
│       ├── discipline/        disciplines (CRUD)
│       ├── event/             exams and assignments (CRUD)
│       ├── availability/      weekly free hours (CRUD)
│       ├── plan/              weekly plan: service, endpoints, persistence
│       │   └── engine/        the scheduler, pure Java with no framework
│       ├── advisor/           plan explanation: Claude and rule-based fallback
│       └── common/            errors, clock, health check
├── frontend/                  static interface served by the back-end
│   ├── index.html
│   ├── styles/                design tokens, base, components, views
│   ├── js/                    API client, templating, one module per screen
│   ├── assets/                illustrations, icons and paper textures
│   └── tests/                 unit tests of the formatting helpers
├── docs/                      original briefing of the hackathon
└── .github/workflows/ci.yml   pipeline
```

## How to run

### Prerequisites

- Java 21 (JDK)
- Nothing else to run the app: Maven comes with the wrapper and SQLite is embedded.
- Node 22 or newer, only to lint and test the front-end.

### Back-end and front-end together

The back-end serves the front-end, so one command starts everything:

```bash
cd backend
./mvnw spring-boot:run        # Windows: mvnw.cmd spring-boot:run
```

Open <http://localhost:8080>. The database is created at `backend/data/studyflow.db`.
On the first screen, type a name and choose **"Preencher com um exemplo"** to load a sample week.

### Turning the AI advisor on (optional)

```bash
export ANTHROPIC_API_KEY=...   # never commit this value
./mvnw spring-boot:run
```

`.env.example` lists every setting. Without the key the advisor uses built-in rules and the
interface shows "Regras automáticas" instead of "Escrito por IA".

### Tests and lint

```bash
cd backend && ./mvnw verify          # compile, 35 tests, format check, jar
cd frontend && npm ci && npm run lint && npm test
```

`./mvnw spotless:apply` and `npm run format` fix formatting.

### Main endpoints

| Method | Endpoint | Purpose |
| --- | --- | --- |
| GET, POST, PUT, DELETE | `/api/disciplines` | Disciplines |
| GET, POST, PUT, DELETE | `/api/events` | Exams and assignments, with progress |
| GET, POST, PUT, DELETE | `/api/availability` | Weekly free hours |
| GET | `/api/plan?date=` | Plan of the week containing the date |
| POST | `/api/plan/generate?date=` | Generate or rebuild the week |
| PATCH | `/api/blocks/{id}/done` | Mark a block as done or undo it |
| PATCH | `/api/blocks/{id}/locked` | Lock a block in its slot |
| POST | `/api/blocks/{id}/postpone` | Postpone a block and rebuild the week |
| GET | `/api/health` | Health check |

## CI/CD

The pipeline in [`.github/workflows/ci.yml`](.github/workflows/ci.yml) runs on every pull request
and on every push to `main` and `develop`:

- **Backend:** `./mvnw verify`: build, unit and API tests, Spotless format check and the
  runnable jar. Test reports are uploaded as an artifact.
- **Frontend:** ESLint, Prettier check and unit tests.
- **Conventional Commits:** on pull requests, commitlint validates the PR title and every commit
  message against [`commitlint.config.js`](commitlint.config.js).

## Deploy

Automatic deploy has not been set up. The application runs locally from a single jar
(`backend/target/studyflow-1.0.0.jar`), which is how it is demonstrated.

## Links

- **Repository:** <https://github.com/ravydamsHACKATOM/HACKATOM>
- **Deploy (if any):** not published
- **Board / GitHub project (if any):** none
