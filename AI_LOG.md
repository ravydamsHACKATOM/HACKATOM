# AI usage log

**English** · [Português (Brasil)](AI_LOG.pt-BR.md)

This log records AI use in the project. Entries below summarize uses documented by the team and the current Codex assistance; the team should confirm that the summaries reflect its own decisions before presenting.

## Tools

- ChatGPT / Codex
- GitHub Copilot or other tools: not recorded in the available project notes

## 2026-11-25 — MVP planning and implementation

- **Main prompt:** Ask for help turning the StudyFlow prototype into a working application using HTML, CSS, Java, Spring Boot, and SQLite.
- **What the AI generated:** A development outline, feature suggestions, and integration steps.
- **What the team kept:** The functional scope, integrated front-end/back-end direction, and staged implementation plan, as recorded in the previous team log.
- **What the team corrected or rejected, and why:** The team emphasized fidelity to the Figma design and rejected unnecessary visual or technical complexity.

## 2026-11-25 — GitHub Actions setup

- **Main prompt:** Create an Actions workflow for the backend build on pull requests and pushes, using Java 21 and Maven.
- **What the AI generated:** A workflow outline and Maven setup guidance.
- **What the team kept:** The CI structure and Maven Wrapper guidance, as recorded in the previous team log.
- **What the team corrected or rejected, and why:** The team temporarily omitted tests during the first pipeline step and fixed the Maven Wrapper executable bit. The current workflow now runs verification; the team should update this entry after reviewing the current pipeline.

## 2026-11-27 — Requirements review and weekly scheduling

- **Main prompt:** Compare the project with the instructor's Study Planner briefing and make the feasible improvements.
- **What the AI generated:** A requirements audit, canonical bilingual root documentation, a Checkstyle workflow step, and a local change to cap generated blocks at the selected week's Sunday and wrap schedule generation in a transaction. A focused regression test was added.
- **What the team kept:** Pending team review. The changes are currently uncommitted. Local Checkstyle and `mvnw clean verify` both passed under Java 21.0.7; the schedule regression test passed.
- **What the team corrected or rejected, and why:** The first local checks found an unused import and then missing explicit DTO imports; these were corrected. The team should still review the changes before accepting them.
