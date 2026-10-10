package com.example.library;

import java.time.LocalDate;

/** A Member's claim to a Book that is not on the shelf. See CONTEXT.md. */
public record Hold(String isbn, String memberId, LocalDate placedOn, HoldState state) {

    /** Still in the Hold queue: not yet Fulfilled, Cancelled or Expired. */
    public boolean isActive() { return state == HoldState.WAITING; }
}
