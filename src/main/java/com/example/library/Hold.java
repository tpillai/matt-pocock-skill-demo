package com.example.library;

import java.time.LocalDate;

/**
 * A Member's claim to a Book that is not on the shelf. See CONTEXT.md.
 * {@code readyOn} is the day the Pickup window started, or null while Waiting.
 */
public record Hold(String isbn, String memberId, LocalDate placedOn, HoldState state, LocalDate readyOn) {

    /** Still in the Hold queue: Waiting or Ready for pickup. */
    public boolean isActive() { return isWaiting() || isReadyForPickup(); }

    public boolean isWaiting() { return state == HoldState.WAITING; }

    public boolean isReadyForPickup() { return state == HoldState.READY_FOR_PICKUP; }

    Hold readyForPickup(LocalDate today) { return new Hold(isbn, memberId, placedOn, HoldState.READY_FOR_PICKUP, today); }

    Hold expired() { return new Hold(isbn, memberId, placedOn, HoldState.EXPIRED, readyOn); }

    Hold fulfilled() { return new Hold(isbn, memberId, placedOn, HoldState.FULFILLED, readyOn); }
}
