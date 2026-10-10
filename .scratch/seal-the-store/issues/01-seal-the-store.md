# 01: Seal the store

From `/improve-codebase-architecture` (Exercise C), candidate 1.

**What to build:** `LoanRepository` hands out its raw maps and lists, and `Loan` has public setters, so any caller can rewrite a Loan past every rule (`LibraryApp` moves a due date a year out). Seal it: the store answers domain questions instead of handing out collections, and a Loan changes only through `LibraryService`, the same way a Hold does. Behaviour is unchanged.

**Status:** done

## Decisions

- Rename `LoanRepository` to `LibraryStore`: it holds Books, Members, Loans and Holds. A technical name, not a domain term, so `GLOSSARY.md` is unchanged.
- The store stays a separate in-process module passed to `LibraryService`. No Java `interface`: there is one adapter, so a seam would be hypothetical.
- The store answers: is this Book known, is this Member known, the active Loan on a Book, a Member's active Loan count, the Hold queue on a Book (placement order), the Holds a Member placed. Each answer is an `Optional`, a count, or an unmodifiable list. The `save*` methods stay for seeding.
- `Loan` becomes a `record`. Returning a Loan makes a new one, saved with a Loan replace on the store that mirrors `replaceHold`.
- `firstHoldOn` and Pickup-window expiry stay in `LibraryService`, leaving room for a later Hold queue module.

## Acceptance criteria

- [x] No store method returns a modifiable collection, and nothing outside the store can reach its maps or lists.
- [x] `Loan` is an immutable record; returning a Book saves a new Loan through the store.
- [x] `LibraryService.getRepo()` and `findActiveLoan` are gone (`findActiveLoan` became a pass-through to the store, so it was inlined); `LibraryServiceContractTest` has no exemptions.
- [x] `LibraryApp` no longer rewrites a due date.
- [x] `CODING_STANDARDS.md` describes Loan transitions alongside Hold transitions.
- [x] `mvn -q test` passes with no assertion changes and no new tests; test edits are only the rename, record accessors and the removed exemptions.
