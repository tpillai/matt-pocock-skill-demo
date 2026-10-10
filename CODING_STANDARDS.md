# Coding standards

Judgement calls for reviewing changes in this repo. Mechanical rules live in tests (`LibraryServiceContractTest`) and the pre-commit hook, not here.

## Domain language

Name classes, methods, variables and tests with the terms in `GLOSSARY.md`, and leave its _Avoid_ synonyms out. Respect the decisions in `docs/adr/`; a change that contradicts one says so and why.

## LibraryService

- **One lookup per query shape.** Each way of finding Holds or Loans ("the first Hold on a Book matching X", "the Member's active Hold on a Book") lives in one private helper, built on `firstHoldOn` or a `LibraryStore` lookup, and every caller uses it. Loan lookups live in `LibraryStore` itself. A second inline stream with the same filter is a finding.
- **Lazy expiry.** An operation that touches a Book Expires that Book's overdue Holds before it does its own work (ADR 0002). When it touches other Books, as `holdsOf` and the Hold-limit count do, a one-line comment on the helper says so.
- **Named counts.** Assign a computed count or limit check to a named local or helper, then compare it, as the Loan-limit check in `checkout` does.
- **Errors.** Unknown Book or Member: `IllegalArgumentException`. Rule violation: `IllegalStateException` with a short, lowercase message ending in the ISBN or Member id, for example `"Member already holds this book: " + isbn`.
- **Hold transitions** are small methods on `Hold` that return a new record (`readyForPickup`, `fulfilled`, `cancelled`, `expired`), and the service saves them with `store.replaceHold`.
- **Loan transitions** work the same way: `Loan` is a record, `returned` returns a new one, and the service saves it with `store.replaceLoan`.
- **The store is sealed.** `LibraryStore` answers questions with an `Optional`, a count or an unmodifiable list, and never hands out its collections or anything a caller can mutate.

## Tests

- Follow the Testing Decisions in `.scratch/holds/spec.md`: tests go only through `LibraryService` and pass explicit dates.
- One test class per operation or rule, sharing the seeding fixture in `HoldsFixture`. Put a new test in the class for the behaviour it specifies.
- Name a test after the behaviour it specifies, in glossary terms, for example `cancellingAWaitingHoldMovesTheMembersBehindItUpOnePlace`.
- Take expected values from the spec or a worked example (`returned.plusDays(4)`), never from the code under test.
