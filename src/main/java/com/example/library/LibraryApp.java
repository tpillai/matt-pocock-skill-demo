package com.example.library;

import java.time.LocalDate;

/** Tiny scripted scenario so you can watch the domain in action: mvn -q compile exec:java */
public class LibraryApp {
    public static void main(String[] args) {
        LibraryStore store = new LibraryStore();
        LibraryService library = new LibraryService(store, new FeeCalculator());

        store.saveBook(new Book("978-0134685991", "Effective Java", "Joshua Bloch"));
        store.saveBook(new Book("978-0132350884", "Clean Code", "Robert C. Martin"));
        store.saveMember(new Member("m-1", "Asha"));
        store.saveMember(new Member("m-2", "Ravi"));

        LocalDate day0 = LocalDate.of(2026, 10, 1);

        Loan loan = library.checkout("978-0134685991", "m-1", day0);
        System.out.println("Asha borrowed Effective Java, due " + loan.dueDate());

        // Ravi wants the same book. Today there is no way to queue for it.
        try {
            library.checkout("978-0134685991", "m-2", day0.plusDays(2));
        } catch (IllegalStateException e) {
            System.out.println("Ravi could not borrow it: " + e.getMessage());
        }

        // Asha returns it exactly on the due date.
        var fee = library.returnBook("978-0134685991", loan.dueDate());
        System.out.println("Asha returned it on the due date. Fee charged: $" + fee);
    }
}
