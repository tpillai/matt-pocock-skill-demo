# 01: Place a Hold on a Book that is on Loan

Spec: `.scratch/holds/spec.md`

**What to build:** A Member can place a Hold on a Book that another Member has on Loan, and can see their Holds. Placing a Hold that makes no sense is rejected. This ticket does not change borrowing or returning yet.

**Blocked by:** None (can start immediately).

**Status:** done

- [x] A Member can place a Hold on a Book that is on Loan to another Member; the Hold is waiting.
- [x] Placing a Hold for an unknown Book or unknown Member is rejected as bad input, matching how borrowing rejects them.
- [x] Placing a Hold on a Book that is on the shelf is rejected.
- [x] Placing a second Hold on a Book the Member already holds is rejected.
- [x] Placing a Hold on a Book the Member currently has on Loan is rejected.
- [x] A read-only query returns a Member's Holds with their state (waiting at this point).
- [x] Tests go through `LibraryService` only, in the style of the existing service tests, and use glossary terms.
