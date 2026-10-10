# 03: Pickup window expiry

Spec: `.scratch/holds/spec.md`

**What to build:** A Ready-for-pickup Hold lasts 3 days. If the Member doesn't borrow the Book in time, the Hold is Expired and the Book passes to the next Hold, or back to the shelf.

**Blocked by:** 02 (Return hands the Book to the first holder).

**Status:** done

- [x] For a Book returned on day D, the Ready-for-pickup Member can borrow it on day D+3.
- [x] On day D+4 their Hold is Expired and they can no longer borrow it ahead of others.
- [x] When a Hold Expires, the next waiting Hold becomes Ready for pickup with a fresh 3-day Pickup window starting on the day the previous Hold Expired (day D+4 for a Book returned on day D).
- [x] When the last Hold Expires, the Book goes back on the shelf and anyone can borrow it.
- [x] If several Pickup windows have passed by the time of the next operation, the queue advances through each in turn.
- [x] Expiry is evaluated from the `today` passed into any operation; there is no clock or background job.
- [x] The Holds query reports Expired.
