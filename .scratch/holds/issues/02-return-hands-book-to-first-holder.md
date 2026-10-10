# 02: Return hands the Book to the first holder

Spec: `.scratch/holds/spec.md`

**What to build:** When a Book with Holds is returned, the first Hold in the Hold queue becomes Ready for pickup and the Book is set aside for that Member. Only they can borrow it, which Fulfils the Hold. Others can still join the queue.

**Blocked by:** 01 (Place a Hold on a Book that is on Loan).

**Status:** done

- [x] Returning a Book makes the first waiting Hold, by placement order, Ready for pickup; its Pickup window starts on the return date.
- [x] With three Members holding the same Book, Holds become Ready for pickup strictly in the order they were placed.
- [x] While a Book is Ready for pickup, any other Member who tries to borrow it is rejected with "Book is held for another member" (not "Book already on loan").
- [x] The Ready-for-pickup Member can borrow the Book; their Hold becomes Fulfilled and the Loan is created as normal.
- [x] A Member can place a Hold on a Book that is Ready for pickup for someone else and joins the back of the queue.
- [x] Returning a Book with no Holds leaves it on the shelf, exactly as today, and late-fee behaviour is unchanged.
- [x] The Holds query reports Ready for pickup and Fulfilled states.
