package com.example.library;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Ready-for-pickup Holds Expire after the Pickup window (ADR 0002). */
class PickupWindowTest extends HoldsFixture {
    @Test
    void readyForPickupMemberCanBorrowOnTheLastDayOfThePickupWindow() {
        library.checkout("b1", "m1", TODAY);
        library.placeHold("b1", "m2", TODAY);
        library.placeHold("b1", "m3", TODAY);
        LocalDate returned = TODAY.plusDays(5);
        library.returnBook("b1", returned);

        assertDoesNotThrow(() -> library.checkout("b1", "m2", returned.plusDays(3)));
        assertEquals(HoldState.FULFILLED, library.holdsOf("m2", returned.plusDays(3)).get(0).state());
    }

    @Test
    void holdExpiresTheDayAfterThePickupWindowAndTheNextHoldBecomesReady() {
        library.checkout("b1", "m1", TODAY);
        library.placeHold("b1", "m2", TODAY);
        library.placeHold("b1", "m3", TODAY);
        LocalDate returned = TODAY.plusDays(5);
        library.returnBook("b1", returned);

        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> library.checkout("b1", "m2", returned.plusDays(4)));
        assertTrue(e.getMessage().startsWith("Book is held for another member"), e.getMessage());
        assertEquals(HoldState.EXPIRED, library.holdsOf("m2", returned.plusDays(4)).get(0).state());
        assertEquals(HoldState.READY_FOR_PICKUP, library.holdsOf("m3", returned.plusDays(4)).get(0).state());
        assertEquals(returned.plusDays(4), library.holdsOf("m3", returned.plusDays(4)).get(0).readyOn());
    }

    @Test
    void nextPickupWindowStartsTheDayThePreviousOneExpiredEvenIfNoticedLater() {
        library.checkout("b1", "m1", TODAY);
        library.placeHold("b1", "m2", TODAY);
        library.placeHold("b1", "m3", TODAY);
        library.placeHold("b1", "m4", TODAY);
        LocalDate returned = TODAY.plusDays(5);
        library.returnBook("b1", returned);

        assertThrows(IllegalStateException.class, () -> library.checkout("b1", "m1", returned.plusDays(6)));
        assertEquals(returned.plusDays(4), library.holdsOf("m3", returned.plusDays(6)).get(0).readyOn());

        assertThrows(IllegalStateException.class, () -> library.checkout("b1", "m3", returned.plusDays(8)));
        assertEquals(HoldState.EXPIRED, library.holdsOf("m3", returned.plusDays(8)).get(0).state());
    }

    @Test
    void queueAdvancesThroughSeveralPassedPickupWindows() {
        library.checkout("b1", "m1", TODAY);
        library.placeHold("b1", "m2", TODAY);
        library.placeHold("b1", "m3", TODAY);
        library.placeHold("b1", "m4", TODAY);
        LocalDate returned = TODAY.plusDays(5);
        library.returnBook("b1", returned);

        assertThrows(IllegalStateException.class, () -> library.checkout("b1", "m1", returned.plusDays(10)));

        assertEquals(HoldState.EXPIRED, library.holdsOf("m2", returned.plusDays(10)).get(0).state());
        assertEquals(HoldState.EXPIRED, library.holdsOf("m3", returned.plusDays(10)).get(0).state());
        assertEquals(HoldState.READY_FOR_PICKUP, library.holdsOf("m4", returned.plusDays(10)).get(0).state());
        assertEquals(returned.plusDays(8), library.holdsOf("m4", returned.plusDays(10)).get(0).readyOn());
    }

    @Test
    void bookGoesBackOnTheShelfWhenTheLastHoldExpires() {
        library.checkout("b1", "m1", TODAY);
        library.placeHold("b1", "m2", TODAY);
        LocalDate returned = TODAY.plusDays(5);
        library.returnBook("b1", returned);

        assertDoesNotThrow(() -> library.checkout("b1", "m3", returned.plusDays(4)));
        assertEquals(HoldState.EXPIRED, library.holdsOf("m2", returned.plusDays(4)).get(0).state());
    }

    @Test
    void cannotHoldABookWhoseLastHoldHasExpired() {
        library.checkout("b1", "m1", TODAY);
        library.placeHold("b1", "m2", TODAY);
        LocalDate returned = TODAY.plusDays(5);
        library.returnBook("b1", returned);

        assertThrows(IllegalStateException.class, () -> library.placeHold("b1", "m3", returned.plusDays(4)));
        assertEquals(HoldState.EXPIRED, library.holdsOf("m2", returned.plusDays(4)).get(0).state());
    }

    @Test
    void memberWhoseHoldExpiredCanHoldTheBookAgainAtTheBackOfTheQueue() {
        library.checkout("b1", "m1", TODAY);
        library.placeHold("b1", "m2", TODAY);
        library.placeHold("b1", "m3", TODAY);
        LocalDate returned = TODAY.plusDays(5);
        library.returnBook("b1", returned);

        library.placeHold("b1", "m2", returned.plusDays(4));

        List<Hold> holds = library.holdsOf("m2", returned.plusDays(4));
        assertEquals(List.of(HoldState.EXPIRED, HoldState.WAITING), holds.stream().map(Hold::state).toList());
        assertEquals(HoldState.READY_FOR_PICKUP, library.holdsOf("m3", returned.plusDays(4)).get(0).state());
    }

    @Test
    void holdsQueryReportsExpiredOnceThePickupWindowHasPassedWithNoOtherOperation() {
        library.checkout("b1", "m1", TODAY);
        library.placeHold("b1", "m2", TODAY);
        LocalDate returned = TODAY.plusDays(5);
        library.returnBook("b1", returned);

        assertEquals(HoldState.READY_FOR_PICKUP, library.holdsOf("m2", returned.plusDays(3)).get(0).state());
        assertEquals(HoldState.EXPIRED, library.holdsOf("m2", returned.plusDays(4)).get(0).state());
    }

    @Test
    void holdsQueryOfAWaitingMemberAdvancesTheQueuePastPassedPickupWindows() {
        library.checkout("b1", "m1", TODAY);
        library.placeHold("b1", "m2", TODAY);
        library.placeHold("b1", "m3", TODAY);
        LocalDate returned = TODAY.plusDays(5);
        library.returnBook("b1", returned);

        Hold hold = library.holdsOf("m3", returned.plusDays(4)).get(0);
        assertEquals(HoldState.READY_FOR_PICKUP, hold.state());
        assertEquals(returned.plusDays(4), hold.readyOn());
    }
}
