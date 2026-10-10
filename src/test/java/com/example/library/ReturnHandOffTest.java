package com.example.library;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Returning a Book hands it to the first Waiting Hold, and only that Member may borrow it. */
class ReturnHandOffTest extends HoldsFixture {
    @Test
    void returningBookMakesFirstHoldReadyForPickupFromTheReturnDate() {
        library.checkout("b1", "m1", TODAY);
        library.placeHold("b1", "m2", TODAY.plusDays(1));
        LocalDate returnDate = TODAY.plusDays(10);

        library.returnBook("b1", returnDate);

        Hold hold = library.holdsOf("m2", returnDate).get(0);
        assertEquals(HoldState.READY_FOR_PICKUP, hold.state());
        assertEquals(returnDate, hold.readyOn());
    }

    @Test
    void otherMembersCannotBorrowABookReadyForPickup() {
        library.checkout("b1", "m1", TODAY);
        library.placeHold("b1", "m2", TODAY);
        library.placeHold("b1", "m3", TODAY);
        library.returnBook("b1", TODAY.plusDays(5));

        for (String other : List.of("m1", "m3", "m4")) {
            IllegalStateException e = assertThrows(IllegalStateException.class,
                    () -> library.checkout("b1", other, TODAY.plusDays(5)));
            assertTrue(e.getMessage().startsWith("Book is held for another member"), e.getMessage());
        }
    }

    @Test
    void readyForPickupMemberBorrowsTheBookAndFulfilsTheirHold() {
        library.checkout("b1", "m1", TODAY);
        library.placeHold("b1", "m2", TODAY);
        library.returnBook("b1", TODAY.plusDays(5));

        Loan loan = library.checkout("b1", "m2", TODAY.plusDays(6));

        assertEquals("m2", loan.getMemberId());
        assertEquals(TODAY.plusDays(20), loan.getDueDate());
        assertEquals(HoldState.FULFILLED, library.holdsOf("m2", TODAY.plusDays(6)).get(0).state());
    }

    @Test
    void holdsBecomeReadyForPickupStrictlyInTheOrderTheyWerePlaced() {
        library.checkout("b1", "m1", TODAY);
        library.placeHold("b1", "m2", TODAY);
        library.placeHold("b1", "m3", TODAY);
        library.placeHold("b1", "m4", TODAY);

        library.returnBook("b1", TODAY.plusDays(5));
        assertEquals(HoldState.READY_FOR_PICKUP, library.holdsOf("m2", TODAY.plusDays(5)).get(0).state());
        assertEquals(HoldState.WAITING, library.holdsOf("m3", TODAY.plusDays(5)).get(0).state());
        assertEquals(HoldState.WAITING, library.holdsOf("m4", TODAY.plusDays(5)).get(0).state());
        library.checkout("b1", "m2", TODAY.plusDays(5));

        library.returnBook("b1", TODAY.plusDays(8));
        assertEquals(HoldState.READY_FOR_PICKUP, library.holdsOf("m3", TODAY.plusDays(8)).get(0).state());
        assertEquals(HoldState.WAITING, library.holdsOf("m4", TODAY.plusDays(8)).get(0).state());
        library.checkout("b1", "m3", TODAY.plusDays(8));

        library.returnBook("b1", TODAY.plusDays(11));
        assertEquals(HoldState.READY_FOR_PICKUP, library.holdsOf("m4", TODAY.plusDays(11)).get(0).state());
        assertEquals(TODAY.plusDays(11), library.holdsOf("m4", TODAY.plusDays(11)).get(0).readyOn());
    }

    @Test
    void lateFeeIsUnchangedWhenTheReturnedBookHasHolds() {
        Loan loan = library.checkout("b1", "m1", TODAY);
        library.placeHold("b1", "m2", TODAY);
        LocalDate returnDate = TODAY.plusDays(20);

        BigDecimal fee = library.returnBook("b1", returnDate);

        assertEquals(new FeeCalculator().lateFee(loan.getDueDate(), returnDate), fee);
    }

    @Test
    void bookGoesBackOnTheShelfWhenReturnedWithNoOneWaiting() {
        library.checkout("b1", "m1", TODAY);
        library.placeHold("b1", "m2", TODAY);
        library.returnBook("b1", TODAY.plusDays(5));
        library.checkout("b1", "m2", TODAY.plusDays(5));

        library.returnBook("b1", TODAY.plusDays(8));

        assertDoesNotThrow(() -> library.checkout("b1", "m3", TODAY.plusDays(8)));
        assertEquals(HoldState.FULFILLED, library.holdsOf("m2", TODAY.plusDays(8)).get(0).state());
    }
}
