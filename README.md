# StudyFlow

StudyFlow is a single-user study planner for students who need to organize subjects, exams, assignment deadlines, available study hours, and focused study blocks. The MVP turns a weekly availability window and real deadlines into a saved schedule and tracks completed work.

## Stack

- Java 21, Spring Boot 4.2, Spring MVC, Jakarta Validation, Spring Data JPA/Hibernate
- SQLite with Liquibase versioned migrations
- Responsive HTML, CSS, and vanilla JavaScript served by Spring Boot
- Maven Wrapper and GitHub Actions

## Repository layout

```text
/
├── README.md, README.pt-BR.md
├── DECISIONS.md, DECISIONS.pt-BR.md
├── AI_LOG.md, AI_LOG.pt-BR.md
├── .github/workflows/ci.yml
├── docs/archive/                 # Previous single-file frontend, retained for reference
└── studyFlow/studyFlow/
    ├── src/main/java/com/example/studyFlow/
    │   ├── config/               # Database path and CORS
    │   ├── controller/           # REST API
    │   ├── dto/                  # Validated request and response contracts
    │   ├── entity/               # JPA entities
    │   ├── exception/            # API errors and exception mapping
    │   ├── repository/           # Spring Data repositories
    │   └── service/              # Domain and scheduling rules
    ├── src/main/resources/
    │   ├── db/                   # Liquibase changelog and SQL migrations
    │   └── static/               # index.html, css, and js
    └── src/test/                 # API and persistence integration tests
```

## Run locally

Install a Java 21 JDK. From `studyFlow/studyFlow`:

```bash
./mvnw clean verify
./mvnw spring-boot:run
```

On Windows PowerShell, use `.\mvnw.cmd clean verify` and `.\mvnw.cmd spring-boot:run`.
Open [http://localhost:8080](http://localhost:8080) or `/index.html`. The application serves the frontend and `/api` from the same origin.
When JavaScript is disabled, `/no-js` provides server-rendered forms for the same core subject, event, availability, schedule, and completion actions.

By default, SQLite data is stored at `${user.home}/.studyflow/study-flow.db`, outside the repository and independent of the current working directory. Set `STUDYFLOW_DB_PATH` to choose another absolute file path. Set `STUDYFLOW_CORS_ORIGINS` to a comma-separated list of trusted origins when the frontend runs elsewhere. SQL logging is off by default; `STUDYFLOW_SQL_LOG=true` enables it for local diagnosis.

Liquibase creates the schema and records applied changesets. Hibernate validates the schema at startup. Back up the database file before upgrading an existing installation. To continue with a database from the previous project version, set `STUDYFLOW_DB_PATH` to that existing file's absolute path; the new default does not silently move or merge old database files.

## REST API

All endpoints use JSON unless they return `204 No Content`.

| Method | Path | Purpose |
| --- | --- | --- |
| GET | `/api/health` | Application health |
| GET, POST | `/api/subjects` | List or create subjects |
| PUT, DELETE | `/api/subjects/{id}` | Update or delete a subject and its events/blocks |
| GET, POST | `/api/events` | List or create exams and assignments |
| PUT, DELETE | `/api/events/{id}` | Update or delete an event and its blocks |
| GET | `/api/availability` | Read grouped weekly availability |
| PUT, DELETE | `/api/availability` | Replace or clear the complete weekly availability |
| GET | `/api/blocks` | Read saved study blocks |
| GET | `/api/today` | Read today's blocks |
| POST | `/api/schedule/generate` | Rebuild pending blocks and return warnings |
| PATCH | `/api/blocks/{id}/completion` | Set a block's completion state |

Example subject: `{"name":"Calculus","color":"#2858c8"}`.

Example event: `{"title":"Midterm","type":"PROVA","subjectId":1,"dueDate":"2026-12-10","estimatedHours":3.5}`. Types are `PROVA` and `ENTREGA`; estimated effort is greater than zero and at most 100 hours.

Example availability replacement: `{"daysOfWeek":[1,3,5],"startTime":"18:00","endTime":"21:00"}`. Days use ISO numbering: Monday `1` through Sunday `7`. This MVP saves one common time interval for the selected days.

Example block completion: `{"completed":true}`. The response contains `status`, `message`, `code`, `fields`, and `timestamp`; validation errors use HTTP 422, missing resources 404, and data conflicts 409.

## Tests and CI

Run `./mvnw clean test` for the isolated SQLite integration suite and `./mvnw clean verify` for the complete local build. GitHub Actions runs `clean verify` on pushes and pull requests and stores Surefire reports. Tests use temporary SQLite databases and never use the local application database.

## Security and MVP limits

The MVP has no login and is intended for one local user. Do not expose it as a multi-user service: records are not associated with accounts and there is no authorization boundary. Add authenticated users and ownership checks before any shared deployment. CORS is restricted to the configured origins and defaults to localhost. The default deployment model is a local application, not a public hosted service.

The scheduler uses the day before a deadline as the last eligible study day, creates 50-minute sessions when possible with a 10-minute break, allows a shorter final session, preserves completed blocks, and reports effort it could not place. The current availability editor uses one interval shared across selected days.
