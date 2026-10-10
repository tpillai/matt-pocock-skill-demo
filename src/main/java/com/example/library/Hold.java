package com.example.library;

import java.time.LocalDate;

/**
 * A Member's claim to a Book that is not on the shelf. See GLOSSARY.md.
 * {@code readyOn} is the day the Pickup window started, or null while Waiting.
 */
public record Hold(String isbn, String memberId, LocalDate placedOn, HoldState state, LocalDate readyOn) {
    static final int PICKUP_WINDOW_DAYS = 3;

    /** Still in the Hold queue: Waiting or Ready for pickup. */
    public boolean isActive() { return isWaiting() || isReadyForPickup(); }

    public boolean isWaiting() { return state == HoldState.WAITING; }

    public boolean isReadyForPickup() { return state == HoldState.READY_FOR_PICKUP; }

    /** The last day of the Pickup window, inclusive. Only meaningful once Ready for pickup. */
    LocalDate lastPickupDay() { return readyOn.plusDays(PICKUP_WINDOW_DAYS); }

    Hold readyForPickup(LocalDate today) { return new Hold(isbn, memberId, placedOn, HoldState.READY_FOR_PICKUP, today); }

    Hold expired() { return new Hold(isbn, memberId, placedOn, HoldState.EXPIRED, readyOn); }

    Hold fulfilled() { return new Hold(isbn, memberId, placedOn, HoldState.FULFILLED, readyOn); }
}
