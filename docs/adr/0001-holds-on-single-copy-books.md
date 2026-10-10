# Holds are placed on a single-copy Book, not on a title

The library owns exactly one of each Book, identified by its ISBN, so a Hold is placed on that one Book and waits for that Book to come back. We considered modelling titles with multiple Copies, where a Hold would take whichever Copy is returned first. We rejected it for now because the existing Loan model has no notion of Copies, and introducing them would reshape Book, Loan and the Hold queue all at once.

## Consequences

Adding multiple Copies later means a Hold must move from Book to title, and the Hold queue must hand out whichever Copy comes back first. That is a migration, not a tweak.
