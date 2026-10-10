package com.example.library;

import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;

/** Shared seeding for the Holds tests: a fresh library with Book b1 and Members m1 to m4. */
abstract class HoldsFixture {
    static final LocalDate TODAY = LocalDate.of(2026, 10, 1);

    LoanRepository repo;
    LibraryService library;

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

    /** Adds Books b2 to b4, on the shelf. */
    void saveBooksB2ToB4() {
        repo.saveBook(new Book("b2", "Book Two", "B"));
        repo.saveBook(new Book("b3", "Book Three", "C"));
        repo.saveBook(new Book("b4", "Book Four", "D"));
    }

    /** Puts b1 to b4 on Loan: b1 to b3 to m1, b4 to m3. */
    void lendFourBooksToOtherMembers() {
        saveBooksB2ToB4();
        library.checkout("b1", "m1", TODAY);
        library.checkout("b2", "m1", TODAY);
        library.checkout("b3", "m1", TODAY);
        library.checkout("b4", "m3", TODAY);
    }
}
