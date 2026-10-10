# Library Lending (practice project)

Java 21 + Maven + JUnit 5. No frameworks, all in-memory.

- Build and test: `mvn -q test`
- Run the scripted scenario: `mvn -q compile exec:java`
- Code: `src/main/java/com/example/library/`, tests: `src/test/java/com/example/library/`

This repo is a sandbox for learning Matt Pocock's skills. See `ROADMAP.md`.

## Agent skills

### Issue tracker

Issues live as local markdown files under `.scratch/<feature>/`. See `docs/agents/issue-tracker.md`.

### Triage labels

Default vocabulary: `needs-triage`, `needs-info`, `ready-for-agent`, `ready-for-human`, `wontfix`. See `docs/agents/triage-labels.md`.

### Domain docs

Single-context: one `CONTEXT.md` plus `docs/adr/` at the repo root. See `docs/agents/domain.md`.
