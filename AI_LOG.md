# AI usage log

## 2026-11-26 — StudyFlow integration and reliability work
- **Prompt summary:** Analyze the supplied StudyFlow requirements, inspect the repository before editing, then connect the browser UI to the Spring REST API and improve persistence, validation, scheduling, tests, CI, and documentation.
- **Generated work:** Refactored REST contracts and services, separated static assets, added Liquibase schema management, error handling, integration tests, and bilingual project documentation.
- **Accepted:** Changes were checked against the actual Maven project; tests were used to find and correct SQLite/JPA type mismatches and scheduling behavior.
- **Corrected or rejected:** A Flyway SQLite dependency was rejected after Maven Central could not resolve the artifact in this environment; Liquibase was selected instead. Existing local databases were untracked from Git while their local copies were preserved.
- **Human review still needed:** Confirm whether authentication is needed before any shared/public deployment. The current MVP is single-user and local.
