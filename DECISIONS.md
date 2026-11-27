# Project decisions

**English** · [Português (Brasil)](DECISIONS.pt-BR.md)

This log records decisions evidenced in the project documentation and implementation. The team should review it before presenting and add or correct decisions that were made outside the repository.

## 2026-11-24 — Back-end stack

- **Decision:** Use Java with Spring Boot.
- **Alternatives considered:** Node.js; the existing team notes mention Node as an alternative.
- **Rationale:** The team reported familiarity with Java, and Spring Boot provides the REST API and serves the web assets.

## 2026-11-24 — Front-end stack

- **Decision:** Use HTML, CSS, and JavaScript without a separate front-end build server.
- **Alternatives considered:** A separate framework or front-end server; no specific alternative is recorded.
- **Rationale:** Keep the MVP small and serve the static interface from the Spring Boot application.

## 2026-11-24 — Data storage

- **Decision:** Use SQLite for local persistence.
- **Alternatives considered:** No alternative is recorded in the existing team notes.
- **Rationale:** A local database keeps setup simple for the MVP.

## 2026-11-25 — MVP scope

- **Decision:** Build a usable study-planning flow with subjects, events, weekly availability, generated study blocks, a weekly/today view, and completion tracking.
- **Alternatives considered:** A broader productivity application; the team narrowed the scope to the study-planning theme and a demonstrable MVP.
- **Rationale:** Complete the central flow within the one-week immersion.

## Open item — AI-assisted planning

The current code has a deterministic schedule generator and no AI provider integration. The team still needs to decide and document whether AI-assisted suggestions/refactoring will be implemented for the presentation, and how unscheduled effort will be explained to the student.
