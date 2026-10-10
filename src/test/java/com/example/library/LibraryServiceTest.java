package com.example.library;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LibraryServiceTest {
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 1);

    private LibraryStore repo;
    private LibraryService library;

    @BeforeEach
    void setUp() {
        repo = new LibraryStore();
        library = new LibraryService(repo, new FeeCalculator());
        repo.saveBook(new Book("b1", "Book One", "A"));
        repo.saveBook(new Book("b2", "Book Two", "B"));
        repo.saveBook(new Book("b3", "Book Three", "C"));
        repo.saveBook(new Book("b4", "Book Four", "D"));
        repo.saveMember(new Member("m1", "Member One"));
        repo.saveMember(new Member("m2", "Member Two"));
    }

    @Test
    void checkoutSetsDueDateFourteenDaysOut() {
        Loan loan = library.checkout("b1", "m1", TODAY);
        assertEquals(TODAY.plusDays(14), loan.dueDate());
    }

    @Test
    void cannotCheckoutBookAlreadyOnLoan() {
        library.checkout("b1", "m1", TODAY);
        assertThrows(IllegalStateException.class, () -> library.checkout("b1", "m2", TODAY));
    }

    @Test
    void memberCannotExceedMaxActiveLoans() {
        library.checkout("b1", "m1", TODAY);
        library.checkout("b2", "m1", TODAY);
        library.checkout("b3", "m1", TODAY);
        assertThrows(IllegalStateException.class, () -> library.checkout("b4", "m1", TODAY));
    }

    @Test
    void earlyReturnIsFree() {
        library.checkout("b1", "m1", TODAY);
        assertEquals(BigDecimal.ZERO, library.returnBook("b1", TODAY.plusDays(3)));
    }

    @Test
    void lateReturnChargesAFee() {
        library.checkout("b1", "m1", TODAY);
        BigDecimal fee = library.returnBook("b1", TODAY.plusDays(20));
        assertTrue(fee.signum() > 0);
    }

    @Test
    void returnedBookCanBeBorrowedAgain() {
        library.checkout("b1", "m1", TODAY);
        library.returnBook("b1", TODAY.plusDays(1));
        assertDoesNotThrow(() -> library.checkout("b1", "m2", TODAY.plusDays(1)));
    }
}
