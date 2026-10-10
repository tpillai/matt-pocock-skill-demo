# Learning Matt Pocock's Skills with Claude Code

Source: <https://github.com/mattpocock/skills>. The skill names and the main flow
below come from the repo's README, `ask-matt`, `setup-matt-pocock-skills` and
`to-tickets` skills (checked October 2026). The week-by-week pacing is my suggestion.

## The idea in one paragraph

The skills are small, composable prompts that push you to do real engineering
with an agent instead of vibe coding. The core loop is:

```
/grill-with-docs  →  /to-spec  →  /to-tickets  →  /implement (per ticket, /clear between)  →  /retro
                                                    └─ drives /tdd, ends with /code-review
```

Side entrances: `/diagnosing-bugs` for something broken, `/improve-codebase-architecture`
for design debt, `/wayfinder` for work too big for one session, `/prototype` for a
throwaway design question. Not sure which? Ask `/ask-matt`.

## Progress (updated 10 October 2026)

| Phase | Status |
|---|---|
| 0 Setup | **Done.** mattpocock-skills 1.3.1 from Matt's marketplace, local-markdown tracker, pre-commit hook runs `mvn -q test`. |
| 1 Alignment | **Exercise A grilled.** `/grill-with-docs` produced `GLOSSARY.md` and ADR 0001. `/grill-me`, `/wait-what` and reading `/codebase-design` aren't recorded in the repo. |
| 2 Build | **Done.** Holds spec and tickets 01-05 implemented with `/implement`, `/tdd` and `/code-review`; spec and tickets closed. Two `/retro`s. |
| 3 Bugs and architecture | **In progress.** Exercise C done: `/improve-codebase-architecture` proposed three deepenings, and candidate 1 (seal the store: `LibraryStore` answers questions, `Loan` is a record) was grilled and shipped as `.scratch/seal-the-store/` ticket 01. Candidate 2 (a Hold queue module) is left for a later run. `/diagnosing-bugs` was used once on a real Holds bug (the Holds query reported Expired Holds as Ready for pickup), so Exercise B is still open, as is `/prototype`. |
| 4 Working style | Not started. |

What the retros added: a pre-commit hook, `LibraryServiceContractTest` (every
operation takes `today`), rules for closing tickets and specs in
`docs/agents/issue-tracker.md`, ADR 0002 (lazy expiry), `CODING_STANDARDS.md`,
and the Holds tests split into one class per behaviour.

Next step: Exercise B. `/clear`, then give `/diagnosing-bugs` only the symptom.

## Phase 0: Install and set up (30 minutes)

1. Install the plugin (one way only, or you get every skill twice):
   ```bash
   claude plugin marketplace add mattpocock/skills
   claude plugin install mattpocock-skills@mattpocock
   ```
   Install from Matt's own marketplace: `claude-plugins-official` pins an older
   version. Update later with `claude plugin marketplace update mattpocock`.
2. Make this folder a git repo with a baseline commit. `/code-review` and `/pr`
   work on diffs, so they need one:
   ```bash
   git init && git add -A && git commit -m "Baseline library lending project"
   git config core.hooksPath .githooks   # pre-commit runs mvn -q test
   ```
3. Start `claude` in this folder and run `/setup-matt-pocock-skills`.
   - **Issue tracker:** pick **Local markdown**. Tickets become files under
     `.scratch/<feature>/issues/`. No GitHub needed. Switch to GitHub later by
     re-running setup once you `gh repo create`.
   - **Triage labels:** accept the defaults.
   - **Domain docs:** single-context (root `GLOSSARY.md` + `docs/adr/`).
   It writes `docs/agents/*.md` and adds a block to `CLAUDE.md`. Read them.
4. **Name collisions:** Claude Code has its own `/code-review`, and other plugins
   (e.g. `agent-skills`) have `/spec`, `/test`, `/review`. Type `/` and check
   autocomplete. If a name is ambiguous, use the namespaced form, e.g.
   `/mattpocock-skills:code-review`.

## Phase 1: Alignment (week 1)

Goal: learn that most bugs are misalignment, and the cure is being interviewed.

| Skill | Try it on this project |
|---|---|
| `/grill-me` | Outside the repo: "grill me on how I'd plan a library app". Stateless. |
| `/grill-with-docs` | **Exercise A** (below). Watch `GLOSSARY.md` and ADRs appear. |
| `/wait-what` | When an answer doesn't land, ask for a re-explanation. |
| `/codebase-design` | Read it. Learn the vocabulary: module, interface, depth, seam. |

Exit check: you can explain the difference between a "Loan", a "Hold" and a
"Reservation" in this domain, and the answers are written in `GLOSSARY.md`.

## Phase 2: Spec → tickets → build (week 2)

Goal: run the full main flow once, end to end.

1. Same session as your grilling (don't `/clear` yet): `/to-spec`.
2. `/to-tickets`. Read each file under `.scratch/<feature>/issues/`. Note the
   "Blocked by" edges and that each ticket is a thin vertical slice.
3. `/clear`, then `/implement .scratch/<feature>/issues/01-...md`. Watch it drive
   `/tdd` (red → green → refactor, one slice at a time) and finish with `/code-review`.
4. Repeat per ticket, `/clear` between each.
5. `/retro` before you clear the last session.

Exit check: `mvn -q test` is green, the new tests read like the spec.

## Phase 3: Bugs and architecture (week 3)

| Skill | Exercise |
|---|---|
| `/diagnosing-bugs` | **Exercise B**. It refuses to guess until one command goes red on the bug. |
| `/improve-codebase-architecture` | **Exercise C**. HTML report, then a grill on the option you pick. |
| `/prototype` | Ask a design question, e.g. "should holds expire?", and let it spike throwaway code. |

## Phase 4: Working style (week 4)

| Skill | When |
|---|---|
| `/handoff` | Moving work to a new directory, harness, or colleague. |
| `/research` | Background agent reads primary sources, e.g. "how do real libraries handle holds?" |
| `/to-questionnaire` | A decision only someone else can make. |
| `/pr` | After `gh repo create`, write a PR body with before/after evidence. |
| `/wayfinder` | Only for something genuinely foggy, e.g. "turn this into a multi-branch library system". |
| `/teach` | Learn a concept over multiple sessions. |
| `/writing-for-agents` | Write your own skill or improve `CLAUDE.md`. |

## The exercises planted in this project

Run `mvn -q compile exec:java` to see all three in the scenario output.

### Exercise A: missing feature (Phase 1-2)
Ravi can't borrow a book Asha has, and there is no way to queue. Build **holds**:
a member places a hold on a borrowed book; on return, the first holder gets it.
Start with `/grill-with-docs` and let it ask you the hard questions (How long
is a hold kept? Can you hold a book that's on the shelf? Does a hold count
toward the 3-loan limit?). Don't answer them in advance.

**Done.** The spec and tickets are in `.scratch/holds/`, the terms in
`GLOSSARY.md`, the decisions in `docs/adr/`.

### Exercise B: seeded bug (Phase 3)
The scenario prints `Fee charged: $0.25` for a book returned **on** its due
date. The tests are green because `lateReturnChargesAFee` only asserts the fee
is positive. Tell `/diagnosing-bugs` the symptom only, not where it is.

### Exercise C: shallow, leaky design (Phase 3)
`LoanRepository` hands out its raw maps and list, `Loan` has public setters,
and `LibraryApp` rewrites a due date directly. Run
`/improve-codebase-architecture` and see what "deepening" it proposes.

## Habits to keep

- Grill before you build. It's cheaper than fixing the wrong thing.
- Keep grill → spec → tickets in one context window. `/clear` between tickets.
- Vertical slices, test-first.
- Run `/retro` after a session that went sideways. Turn mistakes into checks.
