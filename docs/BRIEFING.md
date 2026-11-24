# Theme briefing: Study planner

**English** · [Português (Brasil)](BRIEFING.pt-BR.md)

**Hack with AI: Coding with Intelligence**  
Senac na Prática · Technical Program in Systems Development  
Area: campus life and community  
Immersion from 23 to 29 November · presentations on 30 November and 1 December

This briefing is the starting point for **your** theme. Read it carefully, cut an MVP that fits in one week, and record your decisions. You will turn this problem into a real product, with a front end and a back end, an organized repository, and documented use of AI. This is a collaborative immersion: the panel evaluates what the team built, decided, and learned.

## Theme and context

**Study planner** — A planner that builds a study schedule from exams and assignment due dates.

In the Systems Development program, the week fills up with exams, hand-ins, projects, and classes. The official calendar says when something is due, but it rarely helps answer when to study and how much time to set aside. Many people improvise the night before, underestimate the effort, or leave two heavy deadlines on the same day — and a plan, when it exists, does not survive the first change of date.

You will build a planner that starts from the student's own exams and assignments and produces a realistic schedule, one that can be adjusted when the week changes.

## Audience and personas

- **A student with several deadlines in the same week:** needs to see the volume and spread the effort.
- **A student who struggles to start:** benefits from concrete blocks (“today, 50 minutes on this course”), not just a list of due dates.
- **A student who had an unexpected change:** needs to rearrange the plan without starting from scratch.

## Goal

Ship an MVP in which the student enters exams, assignments, and available hours, the system builds a study schedule for the week, and they can view, complete, and adjust the blocks — with AI helping to propose and rebuild the plan.

## Minimum requirements (MVP)

The scope below is realistic for one week. Lock it on the first day and record in `DECISIONS.md` what is in, what is out, and why. A complete, demonstrable flow is better than a long list left half-done.

1. Register courses and study events: an exam or assignment, a date, and an effort estimate (for example, in hours).
2. Enter availability windows (days and times when the person can actually study).
3. Generate a weekly schedule with blocks distributed from those deadlines and that availability.
4. View the plan as a list or as a simple calendar.
5. Mark a block as done and adjust what is left when a deadline or a window of availability changes.
6. Show, for the day, what is planned — a clear “what to do today”.

## Where AI comes in

The use of AI is allowed and encouraged — in the product and in the process — and it must appear in `AI_LOG.md`. Some concrete starting points for this theme:

- Propose an initial distribution of blocks, respecting due dates and free hours.
- Rebuild the plan when a new exam is added or a block is postponed, explaining what changed.
- Suggest short review sessions close to the exam date, without crowding the day.
- Warn when the volume does not fit the time available, so the student can decide what to cut or bring forward.

Do not publish, without your review, an AI-generated text or automatic decision as if it were already the final version.

## Optional extras

Only after the MVP is standing. These are ideas, not obligations:

- Blocks with a typical focus length (for example, 25 or 50 minutes).
- A simple import of a list of deadlines (a file or a batch form).
- Different priority or weight per course.
- A slot for a group study session in the plan.

## Out of scope

To protect the deadline and the focus of the product:

- Official integration with Senac's academic system (grades, class records, or the institutional calendar).
- Diagnosing learning difficulties or predicting the contents of a specific exam.
- Replacing the course calendar published by the instructor.
- A full productivity app with dozens of study techniques.

## Questions to guide the team

Use these questions on the first day, when you define the MVP, and again before the demo:

- What does the system do when available time is less than the estimated effort?
- How does the student correct a plan that AI made too optimistic?
- How does the schedule react when a deadline is moved in the middle of the week?
- Is the plan only a suggestion, or can the student lock blocks they do not want moved?
- How do you avoid a calendar that looks good but cannot be followed?

## Deliverables and how the panel evaluates

This is a one-week **collaborative immersion**, with teams of 3 or 4 TDS students. The panel evaluates **your** work, on your own project — formative feedback, not a comparison with other teams.

**What must be in the repository** (at the root), in **English** (the main version) and in **Portuguese** (`.pt-BR`), with a language link at the top of each file:

- `README.md` and `README.pt-BR.md`
- `DECISIONS.md` and `DECISIONS.pt-BR.md`
- `AI_LOG.md` and `AI_LOG.pt-BR.md`

The application must have a **front end and a back end**. The back end may be **Java**, **Python**, or **Node.js** (alone or combined, as long as they are integrated). A front end is required (web, desktop, or mobile). Justify the stack in `DECISIONS.md`. Also required: a Conventional Commits history, work through reviewed Pull Requests, a protected `main` branch, and a green **GitHub Actions** pipeline (build, lint, and tests) on PRs and on `main`. Do not commit secrets. On the first day, add the instructor as a collaborator: **adrianoanschau@gmail.com**.

**Code freeze** is on Sunday, **29 November, at 23:59**. Presentations are **15 minutes + 5 minutes of questions**, on 30 November and 1 December, at Senac Canoas, Room 103. Rehearse the demo and the required outline (problem and solution, live demo, workflow, decisions, AI in practice, and CI/CD).

Evaluation is split equally between **two panels**:

- **Technical panel (50%):** code quality and architecture; Git, Conventional Commits, and PRs; CI/CD; command of the code in the Q&A.
- **Innovation and Impact panel (50%):** originality; impact (the real problem and for whom); user experience; clarity of the pitch; creative use of AI.

Anyone on the team may be asked about any part of the project. Use AI thoughtfully, record what it generated and what you corrected, and own the code you present.

Good work — and may the week be intense, collaborative, and well documented.

