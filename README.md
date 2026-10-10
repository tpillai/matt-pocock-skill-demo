# Library Lending: built with Matt Pocock's skills

A small Java library-lending app (Books, Members, Loans, Holds), built as a
practice project for [Matt Pocock's agent skills](https://github.com/mattpocock/skills)
in Claude Code. The app is deliberately simple. This README shows **how the skills
were used to build it**, and where each skill left its mark in the repo.

```bash
mvn -q test                   # build and run the tests
mvn -q compile exec:java      # run the scripted scenario
```

Java 21, Maven, JUnit 5. No frameworks, everything in memory.

## The workflow in one picture

```
 setup ─► grill ─► spec ─► tickets ─► implement each ticket ─► retro
                                        (tdd + code-review)
                                                              ┌─ diagnosing-bugs (a bug)
 side entrances, used when needed ────────────────────────────┤
                                                              └─ improve-codebase-architecture (design debt)
```

Every arrow produced a file you can open. The table below lists them in the order
they were made.

| Step | Skill | What it did here | What it left behind |
|---|---|---|---|
| 0 | `/setup-matt-pocock-skills` | Chose a local-markdown issue tracker, the default triage labels and a single glossary | `docs/agents/*.md`, the *Agent skills* block in `CLAUDE.md` |
| 1 | `/grill-with-docs` | Interviewed me about **holds** until the rules were pinned down (how long is a hold kept? can you hold a book on the shelf? does a hold count toward the loan limit?) | `GLOSSARY.md`, `docs/adr/0001-holds-on-single-copy-books.md` |
| 2 | `/to-spec` | Turned the grilling into a spec with 20 user stories | `.scratch/holds/spec.md` |
| 3 | `/to-tickets` | Split the spec into five thin vertical slices, each one testable | `.scratch/holds/issues/01…05-*.md` |
| 4 | `/implement` (×5) | One ticket per session, with `/clear` in between. Each run drove `/tdd` (red → green → refactor) and finished with `/code-review` | One commit per ticket, plus an "Address ticket NN review" commit when review found something |
| 5 | `/retro` (×2) | Turned mistakes from those sessions into checks, so they can't come back | `.githooks/pre-commit`, `LibraryServiceContractTest`, ADR 0002, `CODING_STANDARDS.md`, Holds tests split into one class per behaviour |
| 6 | `/improve-codebase-architecture` | Found that the store handed out its raw collections and `Loan` had public setters, proposed three "deepenings", then grilled me on the one I picked | `.scratch/seal-the-store/issues/01-seal-the-store.md`, the sealed `LibraryStore` and `Loan` record |
| 7 | `/diagnosing-bugs` | Given only the symptom ("returning on the due date costs $0.25"), built a failing test **before** reading the code, then found and fixed the cause | `returnOnDueDateIsFree` and `returnOneDayLateChargesOneDaysFee` in `LibraryServiceTest`, the fix in `FeeCalculator` |

## The story, step by step

### 1. Align before building (`/grill-with-docs`)

The project started with a missing feature: Ravi can't borrow a book Asha has,
and there is no way to queue for it. Instead of describing the feature up front,
I let `/grill-with-docs` ask questions one at a time. My answers became:

- **`GLOSSARY.md`**: one word per concept (*Hold*, *Waiting*, *Ready for pickup*,
  *Pickup window*, *Fulfilled*, *Cancelled*, *Expired*) and the synonyms to avoid
  (*Reservation*, *Waitlist*, …). The code, tests and tickets all use these words.
- **ADR 0001**: a hold is on the library's one copy of a Book, not on a title,
  and why we chose that.

### 2. Spec, then tickets (`/to-spec`, `/to-tickets`)

In the same session, `/to-spec` wrote `.scratch/holds/spec.md`, and `/to-tickets`
cut it into five tickets. Each ticket names what it is blocked by:

1. Place a Hold on a Book that is on Loan
2. Returning a Book hands it to the first holder
3. The Pickup window expires
4. Cancel a Hold
5. Hold limits (at most 3)

### 3. Build one ticket per session (`/implement` → `/tdd` → `/code-review`)

For each ticket: `/clear`, then `/implement .scratch/holds/issues/NN-….md`. The git
log shows the pattern clearly:

```
bdda6e5 Members can place a Hold on a Book that is on Loan (ticket 01)
055614c Address ticket 01 review: Waiting term, active-only duplicate check
e33756d Returning a Book hands it to the first holder (ticket 02)
f7eb547 Address ticket 02 review: Hold state predicates, one queue lookup
...
69d55da A Member can have at most 3 active Holds (ticket 05)
```

The tests (`PlaceHoldTest`, `ReturnHandOffTest`, `PickupWindowTest`,
`CancelHoldTest`, `HoldLimitsTest`) read like the spec's user stories, because
`/tdd` wrote them first, one behaviour at a time.

### 4. Learn from the sessions (`/retro`)

Two retros turned problems into permanent guards:

- A bug where the Holds query showed Expired Holds as *Ready for pickup* led to
  **ADR 0002** (every operation expires overdue Holds first) and
  **`LibraryServiceContractTest`** (every operation must take `today`).
- Review comments that kept repeating became **`CODING_STANDARDS.md`**, which
  `/code-review` checks changes against.
- A **pre-commit hook** runs `mvn -q test`, so a red build can't be committed.
  Enable it with `git config core.hooksPath .githooks`.

### 5. Fix the design (`/improve-codebase-architecture`)

The skill wrote a report with three candidates. I chose "seal the store", grilled
it, and it was shipped as a ticket: `LibraryStore` now answers questions (returning
an `Optional`, a count or an unmodifiable list) instead of handing out its maps,
`Loan` became an immutable record, and only `LibraryService` writes Loans and Holds.

### 6. Diagnose a bug without guessing (`/diagnosing-bugs`)

The bug was planted in the code: returning a book **on** its due date charged
$0.25. The existing test passed because it only checked that a late fee was
positive. The skill:

1. **Built a feedback loop first**: a failing test that calls `returnBook` on the
   due date. It went red with `fee charged: 0.25`.
2. **Ranked four hypotheses** before looking for the cause.
3. **Confirmed the top one**: `FeeCalculator.lateFee` added `+ 1` to the day count,
   so the due date counted as a late day. That also overcharged *every* late
   return by $0.25, which a second test now pins down.
4. **Fixed it**, watched both tests go green, and put the cause in the commit
   message (`2d61448`).

## Where to look

| To see… | Open |
|---|---|
| The domain words | `GLOSSARY.md` |
| Why the design is the way it is | `docs/adr/` |
| What was asked for, and how it was split | `.scratch/holds/spec.md`, `.scratch/*/issues/` |
| The rules reviews check against | `CODING_STANDARDS.md` |
| How the agent should use the tracker, labels and docs | `docs/agents/` |
| The learning plan and progress | `ROADMAP.md` (`roadmap.html` is a visual copy) |
| The code | `src/main/java/com/example/library/` (`LibraryService` is the entry point) |

## What's next

`/prototype` is the last Phase 3 skill left. After that comes Phase 4 (`/handoff`,
`/research`, `/pr`, `/wayfinder` and others). See `ROADMAP.md`.
