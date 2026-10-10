package com.example.library;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

/** The 3-active-Hold limit, and how Holds interact with the 3-Loan limit. */
class HoldLimitsTest extends HoldsFixture {
    @Test
    void memberWithThreeActiveHoldsCannotPlaceAFourth() {
        lendFourBooksToOtherMembers();
        library.placeHold("b1", "m2", TODAY);
        library.placeHold("b2", "m2", TODAY);
        library.placeHold("b3", "m2", TODAY);

        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> library.placeHold("b4", "m2", TODAY));
        assertTrue(e.getMessage().startsWith("Member has too many holds"), e.getMessage());
        assertEquals(3, library.holdsOf("m2", TODAY).size());
    }

    @Test
    void aReadyForPickupHoldCountsTowardTheHoldLimit() {
        lendFourBooksToOtherMembers();
        library.placeHold("b1", "m2", TODAY);
        library.placeHold("b2", "m2", TODAY);
        library.placeHold("b3", "m2", TODAY);
        LocalDate returned = TODAY.plusDays(5);
        library.returnBook("b1", returned);

        assertThrows(IllegalStateException.class, () -> library.placeHold("b4", "m2", returned.plusDays(3)));
        assertEquals(HoldState.READY_FOR_PICKUP, library.holdsOf("m2", returned.plusDays(3)).get(0).state());
    }

    @Test
    void cancelledHoldsDoNotCountTowardTheHoldLimit() {
        lendFourBooksToOtherMembers();
        library.placeHold("b1", "m2", TODAY);
        library.placeHold("b2", "m2", TODAY);
        library.placeHold("b3", "m2", TODAY);
        library.cancelHold("b1", "m2", TODAY.plusDays(1));

        assertDoesNotThrow(() -> library.placeHold("b4", "m2", TODAY.plusDays(1)));
    }

    @Test
    void fulfilledHoldsDoNotCountTowardTheHoldLimit() {
        lendFourBooksToOtherMembers();
        library.placeHold("b1", "m2", TODAY);
        library.placeHold("b2", "m2", TODAY);
        library.placeHold("b3", "m2", TODAY);
        library.returnBook("b1", TODAY.plusDays(5));
        library.checkout("b1", "m2", TODAY.plusDays(5));

        assertDoesNotThrow(() -> library.placeHold("b4", "m2", TODAY.plusDays(6)));
    }

    @Test
    void expiredHoldsDoNotCountTowardTheHoldLimit() {
        lendFourBooksToOtherMembers();
        library.placeHold("b1", "m2", TODAY);
        library.placeHold("b2", "m2", TODAY);
        library.placeHold("b3", "m2", TODAY);
        LocalDate returned = TODAY.plusDays(5);
        library.returnBook("b1", returned);

        assertDoesNotThrow(() -> library.placeHold("b4", "m2", returned.plusDays(4)));
        assertEquals(HoldState.EXPIRED, library.holdsOf("m2", returned.plusDays(4)).get(0).state());
    }

    @Test
    void holdsDoNotCountTowardTheLoanLimit() {
        lendFourBooksToOtherMembers();
        library.placeHold("b1", "m2", TODAY);
        library.placeHold("b2", "m2", TODAY);
        library.placeHold("b3", "m2", TODAY);
        repo.saveBook(new Book("s1", "Shelf One", "S"));
        repo.saveBook(new Book("s2", "Shelf Two", "S"));
        repo.saveBook(new Book("s3", "Shelf Three", "S"));

        library.checkout("s1", "m2", TODAY);
        library.checkout("s2", "m2", TODAY);
        assertDoesNotThrow(() -> library.checkout("s3", "m2", TODAY));
    }

    @Test
    void readyForPickupMemberAtTheLoanLimitStaysReadyForPickup() {
        saveBooksB2ToB4();
        library.checkout("b1", "m1", TODAY);
        library.placeHold("b1", "m2", TODAY);
        library.checkout("b2", "m2", TODAY);
        library.checkout("b3", "m2", TODAY);
        library.checkout("b4", "m2", TODAY);
        library.returnBook("b1", TODAY.plusDays(5));

        assertThrows(IllegalStateException.class, () -> library.checkout("b1", "m2", TODAY.plusDays(5)));
        assertEquals(HoldState.READY_FOR_PICKUP, library.holdsOf("m2", TODAY.plusDays(5)).get(0).state());
    }

    @Test
    void readyForPickupMemberAtTheLoanLimitStillExpiresAfterThePickupWindow() {
        saveBooksB2ToB4();
        library.checkout("b1", "m1", TODAY);
        library.placeHold("b1", "m2", TODAY);
        library.checkout("b2", "m2", TODAY);
        library.checkout("b3", "m2", TODAY);
        library.checkout("b4", "m2", TODAY);
        LocalDate returned = TODAY.plusDays(5);
        library.returnBook("b1", returned);

        assertThrows(IllegalStateException.class, () -> library.checkout("b1", "m2", returned.plusDays(3)));
        assertEquals(HoldState.READY_FOR_PICKUP, library.holdsOf("m2", returned.plusDays(3)).get(0).state());

        assertThrows(IllegalStateException.class, () -> library.checkout("b1", "m2", returned.plusDays(4)));
        assertEquals(HoldState.EXPIRED, library.holdsOf("m2", returned.plusDays(4)).get(0).state());
    }

    @Test
    void readyForPickupMemberAtTheLoanLimitCanBorrowAfterReturningAnotherBookWithinThePickupWindow() {
        saveBooksB2ToB4();
        library.checkout("b1", "m1", TODAY);
        library.placeHold("b1", "m2", TODAY);
        library.checkout("b2", "m2", TODAY);
        library.checkout("b3", "m2", TODAY);
        library.checkout("b4", "m2", TODAY);
        LocalDate returned = TODAY.plusDays(5);
        library.returnBook("b1", returned);
        assertThrows(IllegalStateException.class, () -> library.checkout("b1", "m2", returned.plusDays(1)));

        library.returnBook("b2", returned.plusDays(2));

        assertDoesNotThrow(() -> library.checkout("b1", "m2", returned.plusDays(3)));
        assertEquals(HoldState.FULFILLED, library.holdsOf("m2", returned.plusDays(3)).get(0).state());
    }
}
