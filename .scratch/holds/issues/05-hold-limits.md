# 05: Hold limits

Spec: `.scratch/holds/spec.md`

**What to build:** A Member can have at most 3 active Holds, separately from the 3-Loan limit, and the Loan limit still applies when collecting a Ready-for-pickup Book.

**Blocked by:** 02 (Return hands the Book to the first holder).

**Status:** ready-for-agent

- [ ] A Member with 3 active Holds (waiting or Ready for pickup) cannot place a fourth.
- [ ] Fulfilled Holds don't count toward the limit. (Count only waiting and Ready-for-pickup Holds, so Cancelled and Expired ones from 03/04 are excluded automatically, whichever order the tickets land in.)
- [ ] Holds don't count toward the 3-Loan limit: a Member with 3 Holds can still borrow up to 3 Books from the shelf.
- [ ] A Ready-for-pickup Member who already has 3 Loans is rejected when borrowing, and their Hold stays Ready for pickup until its window ends.
- [ ] If that Member returns another Book within the window, they can then borrow the held Book.
