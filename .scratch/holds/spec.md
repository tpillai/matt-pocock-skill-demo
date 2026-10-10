# Spec: Holds on books that are out

Status: ready-for-agent

## Problem Statement

When a Member wants a Book that another Member has on Loan, the library simply refuses ("Book already on loan") and forgets about them. There is no way to queue for the Book, so the Member has to keep checking back, and whoever happens to ask first after the Book is returned gets it, regardless of who wanted it first.

## Solution

Members can place a Hold on a Book that is not on the shelf. Holds on the same Book form a Hold queue in the order they were placed. When the Book is returned, the first Hold in the queue becomes Ready for pickup: the Book is set aside, and only that Member may borrow it during a 3-day Pickup window. If they borrow it, the Hold is Fulfilled. If they cancel or miss the window, the next Hold becomes Ready for pickup, or the Book goes back on the shelf if nobody is waiting.

## User Stories

1. As a Member, I want to place a Hold on a Book that is on Loan to someone else, so that I get it when it comes back without having to keep checking.
2. As a Member, I want my Hold to be honoured in the order I placed it, so that the queue is fair.
3. As a Member, I want to be told I cannot place a Hold on a Book that is on the shelf, so that I borrow it directly instead.
4. As a Member, I want to place a Hold on a Book that is Ready for pickup for someone else, so that I join the queue behind them.
5. As a Member, I want to be prevented from placing a second Hold on a Book I already hold, so that I don't occupy two places in the queue.
6. As a Member, I want to be prevented from placing a Hold on a Book I currently have on Loan, so that I don't queue for my own Book.
7. As a Member, I want to be limited to 3 active Holds, so that no one Member can queue for the whole library.
8. As a Member, I want my Holds not to count toward my 3-Loan limit, so that queueing doesn't stop me borrowing other Books.
9. As a Member, I want my Hold to wait for as long as the current Loan lasts, so that a long or overdue Loan doesn't silently drop me from the queue.
10. As a Member whose Hold is first in the queue, I want the Book set aside for me when it is returned, so that nobody else takes it.
11. As a Member whose Hold is Ready for pickup, I want 3 days from the return date (inclusive) to borrow the Book, so that I have time to collect it.
12. As a Member whose Hold is Ready for pickup, I want borrowing the Book to Fulfil my Hold, so that it leaves the queue.
13. As a Member whose Hold is Ready for pickup but who already has 3 Loans, I want my Hold to stay Ready for pickup until the window closes, so that returning another Book in time still lets me collect it.
14. As a Member, I want to cancel my Hold at any time before it is Fulfilled, whether it is waiting or Ready for pickup, so that I'm not holding a Book I no longer want.
15. As a Member behind a cancelled Hold, I want to move up the queue, so that the cancellation doesn't cost me my turn.
16. As the next Member in the queue, I want to become Ready for pickup with a fresh 3-day window when the Hold ahead of me Expires or is Cancelled while Ready for pickup, so that I get my turn.
17. As any Member, I want a returned Book with an empty Hold queue to go straight back on the shelf, so that I can borrow it.
18. As any Member, I want a Book whose last Hold Expired or was Cancelled while Ready for pickup to go back on the shelf, so that it isn't stuck set aside for nobody.
19. As a Member who isn't the Ready-for-pickup Member, I want a clear "Book is held for another member" rejection when I try to borrow a set-aside Book, so that I understand why, rather than being told it's on Loan.
20. As a Member whose Hold Expired or was Cancelled, I want to be able to place a new Hold on the same Book, at the back of the queue, so that I can still get it later.
21. As a Member, I want to see my Holds and whether each is waiting, Ready for pickup, Fulfilled, Cancelled or Expired, so that I know where I stand.
22. As the library, I want unknown Books and Members to be rejected when placing or cancelling a Hold, as they are when borrowing, so that bad input fails loudly.
23. As the library, I want cancelling a Hold that doesn't exist (or has already ended) to be rejected, so that mistakes are visible.

## Implementation Decisions

- **One seam.** All behaviour goes through `LibraryService`, alongside `checkout` and `returnBook`. New operations: place a Hold (Book, Member, today), cancel a Hold (Book, Member, today), and a query of a Member's Holds with their state (Member, today). Like every other operation, the query first Expires any Hold whose Pickup window has passed, so it never reports a stale Ready for pickup.
- **Time is passed in.** Every operation takes `today`, as the existing ones do. There is no clock and no background job. Expiry is evaluated lazily: whenever an operation touches a Book, a Ready-for-pickup Hold whose Pickup window ended before `today` is marked Expired and the queue advances (possibly repeatedly, if several windows have passed).
- **Hold states:** waiting, Ready for pickup, Fulfilled, Cancelled, Expired. The last three are final. Ready for pickup records the date its Pickup window started.
- **Pickup window:** 3 days starting the day the Book is returned. A Book returned on day D can be borrowed by the Ready-for-pickup Member up to and including day D+3. Each newly Ready-for-pickup Hold gets its own window starting from the day it became ready (the day the previous one Expired or was Cancelled).
- **Limits:** at most 3 active Holds (waiting or Ready for pickup) per Member, separate from the existing 3-Loan limit, which still applies when a Ready-for-pickup Member borrows.
- **Placing a Hold is rejected when:** the Book or Member is unknown; the Book is on the shelf (no active Loan and no Ready-for-pickup Hold); the Member already has an active Hold on that Book; the Member has the Book on Loan; the Member already has 3 active Holds.
- **Borrowing changes:** if the Book has a Ready-for-pickup Hold, only that Member may borrow it; anyone else is rejected with "Book is held for another member". Borrowing by the holder Fulfils the Hold. The existing "Book already on loan" rejection is unchanged for Books on Loan.
- **Returning changes:** returning a Book makes the first waiting Hold Ready for pickup; with no waiting Holds, the Book is on the shelf as today. Fee behaviour is unchanged.
- **Errors** follow the existing convention: unknown Book or Member is an illegal-argument error; rule violations are illegal-state errors with a short message.
- Respects ADR 0001: Holds attach to a single Book (one ISBN, one physical item); there are no Copies or title-level Holds.

## Testing Decisions

- Tests exercise external behaviour only, through `LibraryService`: what succeeds, what is rejected, and what the Member's Holds query reports. They don't inspect repository collections or Hold internals.
- Time is controlled by passing explicit dates, so Pickup-window boundaries (day D+3 allowed, day D+4 Expired) are tested directly.
- Prior art: `LibraryServiceTest`, a JUnit 5 test with a fresh repository and seeded Books and Members per test, and `assertThrows` for rejections. New tests follow the same style, likely in a sibling test class for Holds.
- Key scenarios: the queue order across three Members; a Ready-for-pickup rejection for others; expiry at the boundary; a chain of expiries advancing the queue; cancel while waiting vs while Ready for pickup; the Hold limit; a holder at the Loan limit; re-holding after Expired or Cancelled; holding a set-aside Book.

## Out of Scope

- Multiple Copies of a Book and title-level Holds (see ADR 0001).
- Notifying Members (email, SMS) when a Hold becomes Ready for pickup.
- Priority ordering or staff overrides of the Hold queue.
- Fees for missed pickups.
- Loan renewals, and whether a Hold blocks one (renewals don't exist yet).
- Fixing the due-date fee bug (Exercise B) or the leaky repository design (Exercise C).
- Changing the scripted scenario in `LibraryApp`, beyond optionally showing a Hold.

## Further Notes

- The glossary in `GLOSSARY.md` defines Book, Member, Loan, Hold, Hold queue, Ready for pickup, Pickup window, Fulfilled, Cancelled and Expired. Use those terms in code and test names.
- "Reservation" is deliberately not a term; it's listed under _Avoid_ for Hold.
