# StudyFlow

**English** · [Português (Brasil)](README.pt-BR.md)

StudyFlow is a study planner for students. It records subjects, exams and assignments, collects weekly availability, and creates study blocks that can be viewed and marked complete.

## MVP features

- Create, edit, list, and delete subjects and study events.
- Record recurring weekly study availability.
- Generate 50-minute study blocks with 10-minute breaks from availability and event deadlines.
- View the weekly plan and today's blocks; mark blocks complete or reopen them.
- Keep data in a local SQLite database.

The current planner is rule-based. It does not connect to an AI provider. If available time is insufficient, it creates the blocks that fit; it does not yet report the remaining unscheduled effort. Editing a deadline or availability requires generating the plan again.

## Stack

- Java 21.0.7 and Spring Boot 3.5.7
- HTML, CSS, and JavaScript served by Spring Boot
- SQLite through JDBC
- Maven Wrapper

## Repository layout

```text
.
├── .github/workflows/ci.yml
├── docs/                                 # Briefings and archived material
├── studyflow-springboot/studyflow-springboot/
│   ├── pom.xml
│   └── src/
│       ├── main/java/                    # REST API and business logic
│       ├── main/resources/static/        # Web front-end
│       └── test/java/                   # Automated tests
├── README.md
├── README.pt-BR.md
├── DECISIONS.md
├── DECISIONS.pt-BR.md
├── AI_LOG.md
└── AI_LOG.pt-BR.md
```

## Run locally

Requirements: Java 21.0.7. The Maven Wrapper downloads Maven when needed.

From `studyflow-springboot/studyflow-springboot`:

```powershell
.\mvnw.cmd spring-boot:run
```

On macOS or Linux:

```bash
bash ./mvnw spring-boot:run
```

Open <http://localhost:8080>. The database is created at `data/studyflow.db`.

## Quality checks

From the backend project directory:

```bash
bash ./mvnw checkstyle:check
bash ./mvnw clean verify
```

The GitHub Actions workflow runs Checkstyle and Maven verification for pushes and pull requests.

## Delivery status

- Deployment: not configured.
- GitHub repository: <https://github.com/ravydamsHACKATOM/HACKATOM>
- The instructor collaborator, `main` branch protection, and required review rules must be confirmed in GitHub repository settings.
