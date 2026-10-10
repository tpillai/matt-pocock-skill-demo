# Every operation Expires overdue Holds, including the Holds query

There is no clock or background job, so a Ready-for-pickup Hold only becomes Expired when something notices its Pickup window has passed. We decided every `LibraryService` operation takes `today` and, before doing its own work, Expires each overdue Hold on the Books it touches and advances their Hold queues. That includes the Holds query (`holdsOf`) and the Hold-limit count in `placeHold`, both of which touch every Book the Member holds, not only the one named in the call. We considered keeping the query read-only, either reporting stored state or projecting expiry without saving it. Stored state reported a Member's Hold as Ready for pickup days after it had Expired, and a projection would duplicate the queue-advancing logic. Saving the expiry gives exactly the result the next operation on that Book would have produced.

## Consequences

- Placing a Hold, or querying Holds, can Expire a Hold on a different Book and hand that Book to the next Waiting Member. This is intended.
- Each new Pickup window starts on the day the previous Hold Expired or was Cancelled, not the day the expiry is noticed. That keeps the outcome the same however late an operation runs.
- Every public operation must take `today`; `LibraryServiceContractTest` enforces this.
