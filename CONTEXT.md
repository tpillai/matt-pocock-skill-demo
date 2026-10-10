# Library Lending

A small library that lends books to members and lets members queue for books that are out.

## Language

**Book**:
A single lendable item, identified by its ISBN. The library has exactly one of each.
_Avoid_: Copy, title, item

**Member**:
A person registered with the library who can borrow books and place holds.
_Avoid_: Patron, user, borrower

**Loan**:
A Book lent to a Member for a fixed period, active until the Book is returned.
_Avoid_: Checkout, borrowing

**Hold**:
A Member's claim to a Book that is not on the shelf (on Loan, or Ready for pickup for someone else), entitling them to borrow it in their turn.
_Avoid_: Reservation, request

**Hold queue**:
The Holds on one Book, ordered strictly by when each was placed.
_Avoid_: Waitlist, line

**Waiting**:
The state of a Hold in the Hold queue whose turn has not yet come.
_Avoid_: Pending, queued

**Ready for pickup**:
The state of the first Hold in the queue once its Book is returned: the Book is set aside, and only that Member may borrow it.
_Avoid_: Reserved, on the hold shelf

**Pickup window**:
The days a Hold stays Ready for pickup, starting on the day its Book is returned.
_Avoid_: Grace period, collection period

### How a Hold ends

**Fulfilled**:
The Hold's Member borrowed the Book, so the Hold became a Loan.
_Avoid_: Completed, converted

**Cancelled**:
The Member withdrew the Hold before it was Fulfilled.
_Avoid_: Removed, deleted

**Expired**:
The Pickup window passed without the Member borrowing the Book.
_Avoid_: Lapsed, timed out
