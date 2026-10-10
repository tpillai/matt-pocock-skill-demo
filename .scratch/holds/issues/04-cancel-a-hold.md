# 04: Cancel a Hold

Spec: `.scratch/holds/spec.md`

**What to build:** A Member can cancel their Hold at any time before it is Fulfilled. The queue moves up, and a cancelled or expired Member can place a fresh Hold at the back.

**Blocked by:** 03 (Pickup window expiry), which provides the queue advancement this reuses.

**Status:** ready-for-agent

- [ ] Cancelling a waiting Hold marks it Cancelled; Members behind it move up one place.
- [ ] Cancelling a Ready-for-pickup Hold marks it Cancelled and advances the queue exactly as an expiry does: the next Hold becomes Ready for pickup with a fresh window, or the Book goes back on the shelf.
- [ ] Cancelling for an unknown Book or Member is rejected as bad input.
- [ ] Cancelling a Hold that doesn't exist, or one already Fulfilled, Cancelled or Expired, is rejected.
- [ ] After a Hold is Cancelled or Expired, the same Member can place a new Hold on that Book, and it goes to the back of the queue.
- [ ] The Holds query reports Cancelled.
