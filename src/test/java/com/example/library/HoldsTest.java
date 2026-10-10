package com.example.library;

import static org.junit.jupiter.api.Assertions.*;

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
    }

    @Test
    void memberCanPlaceHoldOnBookOnLoan() {
        library.checkout("b1", "m1", TODAY);

        library.placeHold("b1", "m2", TODAY.plusDays(1));

        List<Hold> holds = library.holdsOf("m2");
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
        assertTrue(library.holdsOf("m2").isEmpty());
    }

    @Test
    void cannotHoldTheSameBookTwice() {
        library.checkout("b1", "m1", TODAY);
        library.placeHold("b1", "m2", TODAY);

        assertThrows(IllegalStateException.class, () -> library.placeHold("b1", "m2", TODAY.plusDays(1)));
        assertEquals(1, library.holdsOf("m2").size());
    }

    @Test
    void cannotHoldABookYouHaveOnLoan() {
        library.checkout("b1", "m1", TODAY);

        assertThrows(IllegalStateException.class, () -> library.placeHold("b1", "m1", TODAY));
        assertTrue(library.holdsOf("m1").isEmpty());
    }
}
