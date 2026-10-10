package com.example.library;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;

/** Placing a Hold: who may queue for which Book. */
class PlaceHoldTest extends HoldsFixture {
    @Test
    void memberCanPlaceHoldOnBookOnLoan() {
        library.checkout("b1", "m1", TODAY);

        library.placeHold("b1", "m2", TODAY.plusDays(1));

        List<Hold> holds = library.holdsOf("m2", TODAY.plusDays(1));
        assertEquals(1, holds.size());
        assertEquals("b1", holds.get(0).isbn());
        assertEquals(HoldState.WAITING, holds.get(0).state());
    }

    @Test
    void holdOnUnknownBookIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> library.placeHold("nope", "m2", TODAY));
    }

    @Test
    void holdByUnknownMemberIsRejected() {
        library.checkout("b1", "m1", TODAY);
        assertThrows(IllegalArgumentException.class, () -> library.placeHold("b1", "nobody", TODAY));
    }

    @Test
    void cannotHoldBookOnTheShelf() {
        assertThrows(IllegalStateException.class, () -> library.placeHold("b1", "m2", TODAY));
        assertTrue(library.holdsOf("m2", TODAY).isEmpty());
    }

    @Test
    void cannotHoldTheSameBookTwice() {
        library.checkout("b1", "m1", TODAY);
        library.placeHold("b1", "m2", TODAY);

        assertThrows(IllegalStateException.class, () -> library.placeHold("b1", "m2", TODAY.plusDays(1)));
        assertEquals(1, library.holdsOf("m2", TODAY.plusDays(1)).size());
    }

    @Test
    void cannotHoldABookYouHaveOnLoan() {
        library.checkout("b1", "m1", TODAY);

        assertThrows(IllegalStateException.class, () -> library.placeHold("b1", "m1", TODAY));
        assertTrue(library.holdsOf("m1", TODAY).isEmpty());
    }

    @Test
    void memberCanHoldABookReadyForPickupForSomeoneElseAndJoinsTheBackOfTheQueue() {
        library.checkout("b1", "m1", TODAY);
        library.placeHold("b1", "m2", TODAY);
        library.returnBook("b1", TODAY.plusDays(5));

        library.placeHold("b1", "m3", TODAY.plusDays(6));

        assertEquals(HoldState.WAITING, library.holdsOf("m3", TODAY.plusDays(6)).get(0).state());
        assertEquals(HoldState.READY_FOR_PICKUP, library.holdsOf("m2", TODAY.plusDays(6)).get(0).state());
    }

    @Test
    void readyForPickupMemberCannotHoldTheirOwnSetAsideBook() {
        library.checkout("b1", "m1", TODAY);
        library.placeHold("b1", "m2", TODAY);
        library.returnBook("b1", TODAY.plusDays(5));

        assertThrows(IllegalStateException.class, () -> library.placeHold("b1", "m2", TODAY.plusDays(6)));
        assertEquals(1, library.holdsOf("m2", TODAY.plusDays(6)).size());
    }
}
