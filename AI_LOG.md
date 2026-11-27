# AI usage log (AI_LOG)

**English** · [Português (Brasil)](AI_LOG.pt-BR.md)

How the team used artificial intelligence in the project: the tools and, for every relevant use,
the main prompt, what the AI generated, what the team kept and what was corrected or rejected,
and why. This file is the script for the "AI in practice" segment of the presentation.

API keys, passwords and other secrets never go into the repository. They live in environment
variables and in GitHub Secrets.

## Tools

- **ChatGPT**: planning, environment setup and project analysis in the first days.
- **Codex**: attempted in the development environment (it did not work with our account).
- **Claude Code**: rebuild of the application on 27/11 (code, tests, CI and documentation).
- **Z-Image Turbo on ComfyUI**, running locally: illustrations, icons and paper textures.

## Log

### 25/11/2026 — GitHub Actions setup

- **Main prompt:** create a CI workflow with GitHub Actions to run the build on PRs and pushes to
  the main branch, using Java 21 and Maven.
- **What the AI generated:** a `ci.yml` file with the Java and Maven setup and the build step.
- **What the team kept:** the structure of the workflow and the setup guidance.
- **What the team corrected or rejected, and why:** we temporarily removed the tests to follow
  the requested order. We also fixed the execute permission of `mvnw`, which made the build work.

### 25/11/2026 — Planning and development of StudyFlow

- **Main prompt:** ask for help turning the StudyFlow prototype into a working application,
  respecting the Figma screens and using HTML, CSS, Java, Spring Boot and SQLite.
- **What the AI generated:** a proposal for organizing development, the required features and the
  implementation steps.
- **What the team kept:** the feature requirements, the guidance for integrating front-end and
  back-end, and the step-by-step organization.
- **What the team corrected or rejected, and why:** we reinforced the need to keep the original
  Figma design, avoiding unnecessary visual changes and overly complex technical solutions.

### 26/11/2026 — Guidance on AI tools for development

- **Main prompt:** ask how to use ChatGPT Work and Codex to access, analyze and help develop
  StudyFlow, with VS Code and IntelliJ IDEA.
- **What the AI generated:** explanations of how Work operates, how it can interact with files and
  projects, and guidance for using Codex in the development environment.
- **What the team kept:** the guidance for using AI tools to support code analysis, error
  detection and improvements.
- **What the team corrected or rejected, and why:** Codex failed with a model incompatibility
  error on our ChatGPT account, so we considered using VS Code with its AI features instead.

### 26/11/2026 — Development environment and Git setup

- **Main prompt:** ask for help working on the project in VS Code, including how to manage
  branches from the terminal.
- **What the AI generated:** instructions to identify the current branch, list branches and
  switch branches with Git commands.
- **What the team kept:** the guidance for organizing work with Git and continuing development on
  the right branch.
- **What the team corrected or rejected, and why:** the environment had to be adapted because of
  the difficulties with IntelliJ IDEA and Codex.

### 26/11/2026 — Analysis of the structure and state of StudyFlow

- **Main prompt:** ask for an analysis of the project from the repository ZIP file, comparing its
  structure with the instructor's checklist.
- **What the AI generated:** a survey of the project structure: existing files, documentation,
  GitHub Actions, tests, back-end organization and pending items.
- **What the team kept:** the list of points that needed attention: the main MVP flow, automated
  tests, the pipeline and the documentation.
- **What the team corrected or rejected, and why:** we used the analysis as a diagnosis, without
  treating partially implemented items as done. We saw that documentation files and settings did
  not yet represent the expected state of the project.

### 26/11/2026 — Planning the pending work for the presentation

- **Main prompt:** ask for a review of the Day 4 checklist of the hackathon, comparing the
  instructor's requirements with the state of the project.
- **What the AI generated:** a priority list split into implementation, tests and pipeline,
  documentation, security and presentation preparation.
- **What the team kept:** the order of the tasks, favoring a working end-to-end flow before
  secondary features and visual improvements.
- **What the team corrected or rejected, and why:** not recorded at the time.

### 27/11/2026 — Diagnosis and rebuild plan (Claude Code)

- **Main prompt:** "Read every .md file. The current project is not good enough, we will need to
  rebuild it from scratch."
- **What the AI generated:** the list of what the briefing requires, the problems of the prototype
  (pipeline pointing at a missing folder, documents outside the root, no AI in the product, IDE
  folders committed) and a rebuild plan with open questions for the team.
- **What the team kept:** the diagnosis and the proposed structure (`backend/`, `frontend/`,
  documents at the root).
- **What the team corrected or rejected, and why:** we kept Java and Spring Boot, which the team
  knows, instead of changing the stack. We required every commit to be in English and to follow
  Conventional Commits strictly, and the interface to be design-focused with subtle animation.

### 27/11/2026 — Back-end and scheduler (Claude Code)

- **Main prompt:** rebuild the back-end in Java 21 and Spring Boot covering the six requirements
  of the briefing, with AI helping to propose and to rebuild the plan.
- **What the AI generated:** the REST API (disciplines, exams and assignments, free hours, plan),
  the scheduler in plain Java, the Flyway schema and 35 automated tests (44 after accounts were added).
- **What the team kept:** the split between the deterministic scheduler and the AI explanation,
  and the scheduling rules (earliest deadline first, 50-minute sessions, review before the exam,
  a warning when the effort does not fit).
- **What the team corrected or rejected, and why:** an API test showed that, on the first
  generation of a week, the explanation came out without the "does not fit" warning, because the
  summary was built before the plan was marked as generated; the bug was fixed. Code formatting
  is now enforced by the build (Spotless) so it does not depend on manual review.

### 27/11/2026 — AI inside the product: plan explanation (tried and removed)

- **Main prompt (system instruction of the product, written in Portuguese):** "You are the study
  advisor of StudyFlow. You receive, as JSON, the facts of a weekly plan already computed by an
  algorithm. Explain the plan in Portuguese, in a calm and direct tone. Use only the facts you
  received. Do not invent times, dates, disciplines or exam content."
- **What the AI generated:** a one-to-three-sentence summary and up to four practical tips, as a
  structured output (JSON validated by the SDK).
- **What the team kept:** the AI as an explainer: what changed after an adjustment, which day is
  the heaviest, what to do when time is short.
- **What the team corrected or rejected, and why:** we rejected letting the AI build the
  schedule, because it could place blocks outside the free hours and that cannot be tested. We
  also rejected making the AI mandatory: without a key or on failure a rule-based advisor answers,
  and the interface showed where the text came from. Later the same day we removed the
  integration altogether: the product should not depend on a paid key, and the rule-based
  explanation covered the same ground. The integration was never exercised against the real API.

### 27/11/2026 — Interface (Claude Code)

- **Main prompt:** "I want the app to be beautiful, professional, subtly animated, with a focus on
  design", with a textured paper background that does not tire the eyes.
- **What the AI generated:** a design system in CSS tokens (paper and ink, light and dark themes),
  six screens in HTML, CSS and JavaScript modules, and the animations.
- **What the team kept:** the visual direction, the "Today" screen as the entry point and the
  example week that can be loaded in one click during the demo.
- **What the team corrected or rejected, and why:** in the browser review the week columns were
  cramped and broke words in the middle; the grid got a minimum width and horizontal scrolling.
  The "no block" label was shown in red even for deadlines in future weeks, which was alarming
  for no reason; now only what the current plan really could not fit is highlighted. The landing
  illustration had a paper tone different from the background and was blended with CSS.

### 27/11/2026 — Illustrations, icons and paper texture (local image model)

- **Main prompt:** a single art direction for every image: "calm, editorial, paper-and-ink look;
  cream, sage green, terracotta, mustard and dusty blue palette; minimalist, lots of empty space,
  no text, no logos, no faces", plus the description of each piece.
- **What the AI generated:** several candidates per image with Z-Image Turbo on ComfyUI, running
  on our own notebook: paper texture, landing illustration, four empty-state illustrations and ten
  discipline icons.
- **What the team kept:** 17 final WebP images, each chosen among three or four candidates.
- **What the team corrected or rejected, and why:** candidates with garbled text or messy edges
  were discarded. The History icon was redone twice because the automatic background removal
  punched holes in the hourglass glass. The generated paper texture had stains and visible seams
  when tiled; it was processed in code (high-pass filter, edge blending and contrast reduced to
  about 3%) until it tiled seamlessly. Known limits: the green of the illustrations varies a
  little from the interface green, and the Geography icon is more detailed than the others.

### 27/11/2026 — Pipeline and commit convention (Claude Code)

- **Main prompt:** "Every commit has to be in English and follow the best possible standard. Check
  whether any document mentions this."
- **What the AI generated:** the confirmation that the briefing requires Conventional Commits, the
  rules written in `AGENTS.md`, the workflow with three jobs (back-end, front-end and commits) and
  the commitlint configuration.
- **What the team kept:** automatic validation of the messages on every pull request.
- **What the team corrected or rejected, and why:** the first version of commitlint could not find
  its own configuration in the pipeline; it is now installed as a repository dependency. One old
  commit in the history fails the rule and cannot be reworded without rewriting shared history,
  so it is listed as an exception, with the reason in the configuration file.

### 27/11/2026 — Accounts, sign-in and data per user (Claude Code)

- **Main prompt:** "Create a sign-up and sign-in system. No AI integration (API key) is needed."
- **What the AI generated:** users and sessions tables, BCrypt password hashing, a session cookie,
  an interceptor that guards the API, the owner filter in every query, nine API tests and the
  sign-in card on the landing page.
- **What the team kept:** the session in an HttpOnly cookie with only its digest stored, the same
  answer for a wrong password and an unknown email, and the test proving that one account cannot
  read or change another's data.
- **What the team corrected or rejected, and why:** we rejected the full Spring Security framework
  in favor of its password module plus a small interceptor the team can explain. The first
  pipeline run on GitHub failed for reasons that did not show up on our Windows machines: files
  committed with Windows line endings failed the format check on Linux, and the front-end test
  command only worked on a newer Node. Both were fixed with a `.gitattributes` file and a glob in
  the test command, and verified on Node 22.

### 27/11/2026 — Dialogs and the custom select (Claude Code)

- **Main prompt:** "Improve every modal and select so they look premium and subtly animated. The
  select must show the discipline icon. Use icons and a frame for each card, nothing plain."
- **What the AI generated:** a new dialog layout made of framed panels, choice cards for the
  type, the weight and the weekdays, a stepper with quick picks for the hours, and a custom
  select with icons, keyboard support and screen-reader roles.
- **What the team kept:** the layout, the live hints ("in 7 days", "about 3 sessions") and adding
  the same free hour to several days at once.
- **What the team corrected or rejected, and why:** in the browser the list of the select closed
  itself right after opening, because scrolling the active option into view also scrolled the
  dialog, which the select treated as a reason to close. Then the list appeared out of place,
  because visual effects on the dialog changed what "fixed position" is measured from. The list
  is now scrolled by hand and moved to the end of the page while open.

### 27/11/2026 — Today screen and sidebar redesign (Claude Code)

- **Main prompt:** a screenshot of the Today screen with "improve this screen, make it more
  beautiful, subtly animated and professional, and enhance the sidebar design too".
- **What the AI generated:** a greeting header with the progress of the day, four summary tiles,
  a highlighted card for the block to do now with a live "starts in" label, a timeline of the
  day, a week strip, and a sidebar with the progress of the week, grouped navigation with
  counters, a theme switch and the account card. Phones got a top bar for the account.
- **What the team kept:** the whole layout.
- **What the team corrected or rejected, and why:** in the browser the soft light behind the
  greeting ended in a hard vertical edge in the dark theme, and "Provas e entregas" wrapped onto
  two lines in the sidebar; both were fixed. On phones the summary tiles were cramped, so their
  icons are hidden there. Before this change, signing out was not reachable on phones at all.
