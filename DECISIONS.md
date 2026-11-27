# Project decisions

**English** · [Português (Brasil)](DECISIONS.pt-BR.md)

The team's important decisions, in the order they were made. Each entry states what was decided,
the alternatives considered and the reason for the choice. This file is the script for the
decisions segment of the presentation.

## 23/11/2026 — Tech Lead

- **Decision:** Ana Carolina is the Tech Lead.
- **Alternatives considered:** Raphael.
- **Rationale:** Ana has the most knowledge in the team.

## 23/11/2026 — Presentation lead

- **Decision:** Victor presents the project.
- **Alternatives considered:** Yuri.
- **Rationale:** Victor is communicative and comfortable with English.

## 24/11/2026 — Back-end stack

- **Decision:** Java with Spring Boot.
- **Alternatives considered:** Node.js.
- **Rationale:** every member is familiar with Java.

## 24/11/2026 — Front-end stack

- **Decision:** HTML, CSS and JavaScript, without a framework.
- **Alternatives considered:** none.
- **Rationale:** simplicity; the project is not that large.

## 25/11/2026 — Project skeleton

- **Decision:** generate the project skeleton and a working MVP with AI assistance.
- **Alternatives considered:** Claude.
- **Rationale:** the AI tool chosen at the time was more functional for the team.

## 27/11/2026 — Rebuild the application from scratch

- **Decision:** discard the first prototype and rebuild back-end and front-end, keeping the stack.
- **Alternatives considered:** fixing the prototype in place.
- **Rationale:** the prototype had a nested folder structure, a pipeline pointing at a folder
  that did not exist, documents outside the repository root, a single 1,700-line script in the
  front-end and no AI in the product. Fixing each of these would cost more than rebuilding with
  the briefing's checklist as the guide, and the result would still be harder to explain.

## 27/11/2026 — MVP scope

- **Decision:** deliver the six requirements of the briefing (register, availability, generate,
  view, complete and adjust, "what to do today") plus the plan explanation, and nothing else.
- **Left out:** integration with the school system, notifications, data import and group study
  sessions.
- **Rationale:** the briefing lists the first as out of scope, and the others do not change the
  main flow. Accounts were first left out as well and added later the same day (see below).

## 27/11/2026 — The scheduler is deterministic; the AI only explains

- **Decision:** a set of fixed rules written in plain Java decides the schedule. The AI receives
  the result and writes the explanation and the tips.
- **Alternatives considered:** asking the language model to produce the schedule directly.
- **Rationale:** a language model can place a block outside the student's free hours, schedule
  the same hour twice or change its answer between runs, and none of that can be tested. With
  fixed rules, "a block never lands outside availability" is a guarantee covered by tests, and
  the answer to "how do you avoid a pretty but impossible calendar?" is the code itself. The AI
  does what it is good at: turning numbers into a clear, human explanation.

## 27/11/2026 — Scheduling rules

- **Decision:** 50-minute focus sessions with 10-minute breaks; earliest deadline first, ties
  broken by discipline weight; switch subject after two sessions in a row when the urgent one
  still has slack; exams are not studied on the exam day and end with a review session;
  deadlines beyond the week take only a proportional share of it.
- **Alternatives considered:** spreading every event evenly across the week.
- **Rationale:** earliest deadline first is the rule that guarantees that if everything can fit,
  it fits. An even spread looks balanced but can leave Friday's exam under-studied. The subject
  switch avoids four hours in a row of the same content without putting any deadline at risk.

## 27/11/2026 — Effort that does not fit is shown, not hidden

- **Decision:** when the available time is smaller than the estimated effort, the plan schedules
  what fits and shows a warning with the missing time per event.
- **Alternatives considered:** shrinking sessions or exceeding the availability to fit it all.
- **Rationale:** the student is the one who can decide what to cut, anticipate or re-estimate.
  A plan that silently overbooks would repeat the problem the product exists to solve.

## 27/11/2026 — Adjusting the plan: done, locked, postponed

- **Decision:** regenerating keeps done blocks, locked blocks and postponed slots, and
  redistributes the rest. Blocks whose time passed without being done return to the pool.
- **Alternatives considered:** always rebuilding the whole week from zero.
- **Rationale:** it answers three questions from the briefing: the student can lock blocks they
  do not want moved, can correct an optimistic plan by postponing, and never loses what was
  already done when a deadline changes. The interface lists what moved after each rebuild.

## 27/11/2026 — AI provider and fallback (superseded)

- **Decision:** Claude (Anthropic) through the official Java SDK, with a structured output. The
  key comes only from the `ANTHROPIC_API_KEY` environment variable. Without a key, or on any
  failure, a rule-based advisor answers.
- **Alternatives considered:** making the AI mandatory; calling the HTTP API by hand.
- **Rationale:** the demo cannot depend on network or quota, and the pipeline must run without
  secrets. The official SDK gives typed requests and a validated answer format.
- **Status:** replaced by "No paid AI service in the product" below.

## 27/11/2026 — SQLite with plain SQL and Flyway

- **Decision:** SQLite accessed with Spring's `JdbcClient`, schema versioned with Flyway.
- **Alternatives considered:** PostgreSQL; JPA/Hibernate.
- **Rationale:** SQLite needs no installation, so anyone on the team (and the jury) runs the
  project with one command. With five small tables, explicit SQL is easier to read and to explain
  than ORM mappings, and Hibernate has no official SQLite dialect.

## 27/11/2026 — One application, one command

- **Decision:** the back-end serves the front-end from the same jar and origin.
- **Alternatives considered:** a separate front-end server.
- **Rationale:** there is no CORS to configure, no second process to start during the demo, and
  the folders stay separate (`backend/`, `frontend/`) so each part is linted and tested alone.

## 27/11/2026 — Visual direction

- **Decision:** a "paper and ink" look: warm low-contrast surfaces with a subtle paper grain, one
  calm green accent, a serif for headings, and light and dark themes. Motion is short and can be
  turned off by the system's reduced-motion setting.
- **Alternatives considered:** a default component library look; a bright, high-contrast palette.
- **Rationale:** a planner is opened several times a day, often at night. It should be calm and
  not tire the eyes. Illustrations and icons were generated with a local image model from a
  single art direction so the set looks consistent.

## 27/11/2026 — Commit convention enforced by the pipeline

- **Decision:** Conventional Commits in English, validated by commitlint on every pull request,
  with the rules written in `AGENTS.md`.
- **Alternatives considered:** relying on review to catch bad messages.
- **Rationale:** the first days of the history had messages such as "updating file". An automatic
  check costs nothing and keeps the history readable for the technical jury.

## 27/11/2026 — Accounts with sign-up and sign-in

- **Decision:** add accounts. Passwords are stored as BCrypt hashes. A session is a random token
  in an HttpOnly, SameSite=Lax cookie, and the database stores only its SHA-256 digest. Every
  table is scoped to its owner and every query filters by it.
- **Alternatives considered:** keeping the single-student version; JWT in the browser's local
  storage; the full Spring Security framework.
- **Rationale:** a planner holds personal routine data, and with accounts several students can use
  the same installation, including during the demo. A cookie the page's scripts cannot read is
  safer than a token in local storage, and a server-side session can be ended on sign-out. We
  used only the password-hashing module of Spring Security and wrote a small interceptor, which
  the whole team can read and explain, instead of configuring the full framework.
- **Left out:** password recovery by email and rate limiting of sign-in attempts, which need
  infrastructure outside the MVP.

## 27/11/2026 — No paid AI service in the product

- **Decision:** remove the explanation written by a language model and its API key. The plan is
  explained by the rule-based advisor only.
- **Alternatives considered:** keeping the optional integration with the fallback.
- **Rationale:** the product must not depend on a paid key that the team would have to manage and
  protect, and the rule-based explanation already answered the same questions in our tests. AI
  stays in the process (code, tests, images, documentation), documented in the AI log. The
  earlier decision that the AI never decides the schedule made this removal a small change: only
  the explanation layer was affected.

## 27/11/2026 — Dialogs in cards and a custom select

- **Decision:** rebuild every dialog as a set of framed panels with choice cards, a stepper and
  quick-pick chips, and replace the native select with a custom one that shows the discipline
  icon and color.
- **Alternatives considered:** keeping native controls with light styling.
- **Rationale:** native selects cannot show icons and look different on every system. The custom
  one keeps what makes a native control usable: a real form value, full keyboard support
  (arrows, Enter, Escape, typing to search) and the listbox roles for screen readers.
