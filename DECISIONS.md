# Architecture decisions

## 2026-11-26 — Serve the frontend from Spring Boot
- **Decision:** Keep the MVP as one deployable Spring Boot application and serve the frontend from `src/main/resources/static`.
- **Alternatives considered:** Keep an independently opened HTML file or add a separate frontend build system.
- **Reason:** Same-origin requests simplify local setup and avoid a second deployment pipeline.

## 2026-11-26 — SQLite with Liquibase migrations
- **Decision:** Use SQLite with Liquibase changesets, an external configurable database path, and Hibernate schema validation.
- **Alternatives considered:** Hibernate `ddl-auto=update`, Flyway SQLite extension, or a server database.
- **Reason:** Versioned migrations make schema changes reviewable; keeping local database files out of Git prevents personal data from entering source control. The initially tried Flyway SQLite artifact was not available through the configured Maven Central, so Liquibase is used.

## 2026-11-26 — Weekly availability uses ISO weekdays
- **Decision:** Store weekdays as ISO numbers, Monday `1` through Sunday `7`; replace the full weekly schedule through one API request.
- **Alternatives considered:** JavaScript's Sunday-first indexing or treating equal time ranges as a shared configuration group.
- **Reason:** One explicit contract avoids weekday shifts and makes updates atomic and predictable.

## 2026-11-26 — Deadline and schedule rules
- **Decision:** Do not schedule on the deadline date; use 50-minute study sessions with a 10-minute break and allow a shorter final session. Preserve completed blocks when regenerating.
- **Alternatives considered:** Use the due date or discard all previous blocks on regeneration.
- **Reason:** A buffer before exams and assignments is safer, and completed work must remain part of the student's history.

## 2026-11-26 — No account system in this MVP
- **Decision:** Keep local single-user use without authentication and document the limitation.
- **Alternatives considered:** Add login, user ownership, and authorization to the current delivery.
- **Reason:** There is no user model or shared deployment requirement in the project. Any multi-user hosting must wait until authentication and record ownership are implemented.
