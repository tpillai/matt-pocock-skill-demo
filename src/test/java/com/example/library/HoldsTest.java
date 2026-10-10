package com.example.library;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class HoldsTest {
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 1);

    private LoanRepository repo;
    private LibraryService library;

    @BeforeEach
    void setUp() {
        repo = new LoanRepository();
        library = new LibraryService(repo, new FeeCalculator());
        repo.saveBook(new Book("b1", "Book One", "A"));
        repo.saveMember(new Member("m1", "Member One"));
        repo.saveMember(new Member("m2", "Member Two"));
        repo.saveMember(new Member("m3", "Member Three"));
        repo.saveMember(new Member("m4", "Member Four"));
    }

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
    void readyForPickupMemberAtTheLoanLimitStaysReadyForPickup() {
        repo.saveBook(new Book("b2", "Book Two", "B"));
        repo.saveBook(new Book("b3", "Book Three", "C"));
        repo.saveBook(new Book("b4", "Book Four", "D"));
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
    void readyForPickupMemberAtTheLoanLimitStillExpiresAfterThePickupWindow() {
        repo.saveBook(new Book("b2", "Book Two", "B"));
        repo.saveBook(new Book("b3", "Book Three", "C"));
        repo.saveBook(new Book("b4", "Book Four", "D"));
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
