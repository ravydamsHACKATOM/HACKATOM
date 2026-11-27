# AGENTS.md

Shared conventions for everyone who changes this repository: team members and AI coding agents
(Claude Code, Codex, Copilot and others). Read it before changing anything and record new
decisions in [DECISIONS.md](DECISIONS.md) instead of duplicating them elsewhere.

## Project in one paragraph

StudyFlow is a study planner for the "Hack com IA" hackathon (Senac, TDS). A Spring Boot back-end
(Java 21, SQLite) exposes a REST API and serves a static front-end (HTML, CSS, JavaScript
modules). The scheduling rules live in `backend/src/main/java/com/studyflow/plan/engine` as pure
Java. The AI only explains plans; it never decides the schedule.

## Commands

```bash
cd backend && ./mvnw spring-boot:run     # run the app at http://localhost:8080
cd backend && ./mvnw verify              # build, tests and format check (must pass before a PR)
cd backend && ./mvnw spotless:apply      # fix Java formatting
cd frontend && npm ci && npm run lint && npm test
cd frontend && npm run format            # fix front-end formatting
```

## Commit messages

Conventional Commits 1.0.0, **in English**, enforced by commitlint in the CI.

```text
<type>(<scope>): <summary>

<body: what changed and why, wrapped at 72 columns>

<footer: Refs #12, BREAKING CHANGE: ...>
```

- **Types:** `feat`, `fix`, `refactor`, `test`, `docs`, `ci`, `build`, `chore`, `style`, `perf`.
- **Scopes:** `backend`, `frontend`, `scheduler`, `plan`, `ai`, `api`, `db`, `ci`, `docs`, `deps`.
  The scope is optional for changes that span the repository.
- **Summary:** imperative mood, lower case, no final period, at most 72 characters.
  Good: `feat(scheduler): reserve a review session before each exam`. Bad: `update files`.
- One logical change per commit. Explain the reason in the body when it is not obvious.
- Breaking changes use `!` after the type or scope and a `BREAKING CHANGE:` footer.

## Branches and pull requests

- Never commit directly to `main`. Work on a branch and open a pull request.
- Branch names: `<type>/<short-topic>`; AI agents use the `claude/` or `codex/` prefix.
- PR titles follow the same Conventional Commits format.
- A PR needs a green pipeline and a review from another team member before merging.

## Code conventions

- **Language:** code, comments, commits and the main documentation are in English. Text shown to
  the student is in Brazilian Portuguese.
- **Documentation:** `README`, `DECISIONS` and `AI_LOG` exist in English and in `.pt-BR`. Change
  both versions in the same commit.
- **Back-end:** one package per feature. Controllers stay thin; rules that deserve a test go in a
  service or in `plan/engine`. Read the time from the injected `Clock`, never from `now()`.
  Schema changes are new Flyway migrations (`V2__...sql`); never edit an applied one.
- **Front-end:** no framework and no build step. Build markup with the `html` tagged template in
  `js/dom.js`, which escapes every value. Colors, sizes and timings come from
  `styles/tokens.css`. Animations must respect `prefers-reduced-motion`.
- **Tests:** a change in the scheduling rules comes with a test in `SchedulePlannerTest`; a
  change in an endpoint comes with a test in `StudyFlowApiTest`.

## AI usage

- Every relevant use of AI is recorded in [AI_LOG.md](AI_LOG.md) and `AI_LOG.pt-BR.md`: the main
  prompt, what was generated, what was kept and what was corrected or rejected.
- Review AI output before committing it. Whoever commits must be able to explain the code.

## Secrets

Never commit API keys, passwords, `.env` files or database files. Keys go in environment
variables locally and in GitHub Secrets in the CI. `.env.example` lists the variable names only.
