package com.example.library;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Cancelling a Hold before it is Fulfilled. */
class CancelHoldTest extends HoldsFixture {
    @Test
    void cancellingAWaitingHoldMovesTheMembersBehindItUpOnePlace() {
        library.checkout("b1", "m1", TODAY);
        library.placeHold("b1", "m2", TODAY);
        library.placeHold("b1", "m3", TODAY);
        library.placeHold("b1", "m4", TODAY);

        library.cancelHold("b1", "m2", TODAY.plusDays(1));
        library.returnBook("b1", TODAY.plusDays(5));

        LocalDate returned = TODAY.plusDays(5);
        assertEquals(HoldState.CANCELLED, library.holdsOf("m2", returned).get(0).state());
        assertEquals(HoldState.READY_FOR_PICKUP, library.holdsOf("m3", returned).get(0).state());
        assertEquals(HoldState.WAITING, library.holdsOf("m4", returned).get(0).state());
    }

    @Test
    void cancellingAReadyForPickupHoldMakesTheNextHoldReadyWithAFreshPickupWindow() {
        library.checkout("b1", "m1", TODAY);
        library.placeHold("b1", "m2", TODAY);
        library.placeHold("b1", "m3", TODAY);
        LocalDate returned = TODAY.plusDays(5);
        library.returnBook("b1", returned);
        LocalDate cancelledOn = returned.plusDays(2);

        library.cancelHold("b1", "m2", cancelledOn);

        assertEquals(HoldState.CANCELLED, library.holdsOf("m2", cancelledOn).get(0).state());
        Hold next = library.holdsOf("m3", cancelledOn).get(0);
        assertEquals(HoldState.READY_FOR_PICKUP, next.state());
        assertEquals(cancelledOn, next.readyOn());
        assertEquals(HoldState.READY_FOR_PICKUP, library.holdsOf("m3", cancelledOn.plusDays(3)).get(0).state());
        assertEquals(HoldState.EXPIRED, library.holdsOf("m3", cancelledOn.plusDays(4)).get(0).state());
    }

    @Test
    void cancellingAWaitingHoldInTheMiddleOfTheQueueKeepsTheOrderOfTheOthers() {
        library.checkout("b1", "m1", TODAY);
        library.placeHold("b1", "m2", TODAY);
        library.placeHold("b1", "m3", TODAY);
        library.placeHold("b1", "m4", TODAY);
        library.cancelHold("b1", "m3", TODAY.plusDays(1));

        library.returnBook("b1", TODAY.plusDays(5));
        library.checkout("b1", "m2", TODAY.plusDays(5));
        library.returnBook("b1", TODAY.plusDays(8));

        assertEquals(HoldState.CANCELLED, library.holdsOf("m3", TODAY.plusDays(8)).get(0).state());
        assertEquals(HoldState.READY_FOR_PICKUP, library.holdsOf("m4", TODAY.plusDays(8)).get(0).state());
    }

    @Test
    void bookGoesBackOnTheShelfWhenTheLastReadyForPickupHoldIsCancelled() {
        library.checkout("b1", "m1", TODAY);
        library.placeHold("b1", "m2", TODAY);
        library.returnBook("b1", TODAY.plusDays(5));

        library.cancelHold("b1", "m2", TODAY.plusDays(6));

        assertDoesNotThrow(() -> library.checkout("b1", "m3", TODAY.plusDays(6)));
    }

    @Test
    void cancellingForAnUnknownBookOrMemberIsRejectedAsBadInput() {
        library.checkout("b1", "m1", TODAY);
        library.placeHold("b1", "m2", TODAY);

        assertThrows(IllegalArgumentException.class, () -> library.cancelHold("nope", "m2", TODAY));
        assertThrows(IllegalArgumentException.class, () -> library.cancelHold("b1", "nobody", TODAY));
    }

    @Test
    void cancellingAHoldThatDoesNotExistOrIsFulfilledOrCancelledIsRejected() {
        library.checkout("b1", "m1", TODAY);
        library.placeHold("b1", "m2", TODAY);
        library.placeHold("b1", "m3", TODAY);
        library.cancelHold("b1", "m3", TODAY);
        library.returnBook("b1", TODAY.plusDays(5));
        library.checkout("b1", "m2", TODAY.plusDays(5));

        for (String member : List.of("m4", "m2", "m3")) {
            IllegalStateException e = assertThrows(IllegalStateException.class,
                    () -> library.cancelHold("b1", member, TODAY.plusDays(6)));
            assertTrue(e.getMessage().startsWith("Member has no active hold on this book"), e.getMessage());
        }
        assertEquals(HoldState.FULFILLED, library.holdsOf("m2", TODAY.plusDays(6)).get(0).state());
    }

    @Test
    void cancellingAHoldWhosePickupWindowHasPassedIsRejectedBecauseItExpired() {
        library.checkout("b1", "m1", TODAY);
        library.placeHold("b1", "m2", TODAY);
        LocalDate returned = TODAY.plusDays(5);
        library.returnBook("b1", returned);

        assertThrows(IllegalStateException.class, () -> library.cancelHold("b1", "m2", returned.plusDays(4)));
        assertEquals(HoldState.EXPIRED, library.holdsOf("m2", returned.plusDays(4)).get(0).state());
    }

    @Test
    void memberWhoCancelledCanHoldTheBookAgainAtTheBackOfTheQueue() {
        library.checkout("b1", "m1", TODAY);
        library.placeHold("b1", "m2", TODAY);
        library.placeHold("b1", "m3", TODAY);
        library.cancelHold("b1", "m2", TODAY.plusDays(1));

        library.placeHold("b1", "m2", TODAY.plusDays(2));
        LocalDate returned = TODAY.plusDays(5);
        library.returnBook("b1", returned);

        List<Hold> holds = library.holdsOf("m2", returned);
        assertEquals(List.of(HoldState.CANCELLED, HoldState.WAITING), holds.stream().map(Hold::state).toList());
        assertEquals(HoldState.READY_FOR_PICKUP, library.holdsOf("m3", returned).get(0).state());
    }
}
