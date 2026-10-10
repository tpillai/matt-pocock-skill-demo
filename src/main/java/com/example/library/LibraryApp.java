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

        // Ravi wants the same book. He can't borrow it, so he places a Hold.
        try {
            library.checkout("978-0134685991", "m-2", day0.plusDays(2));
        } catch (IllegalStateException e) {
            System.out.println("Ravi could not borrow it: " + e.getMessage());
        }
        Hold hold = library.placeHold("978-0134685991", "m-2", day0.plusDays(2));
        System.out.println("Ravi placed a Hold instead: " + hold.state());

        // Asha returns it exactly on the due date, which is free.
        var fee = library.returnBook("978-0134685991", loan.dueDate());
        System.out.println("Asha returned it on the due date. Fee charged: $" + fee);

        // The Book is set aside for Ravi, the first Hold in the queue.
        Hold ready = library.holdsOf("m-2", loan.dueDate()).get(0);
        System.out.println("Ravi's Hold is now " + ready.state() + " until " + ready.lastPickupDay());

        Loan ravisLoan = library.checkout("978-0134685991", "m-2", loan.dueDate().plusDays(1));
        System.out.println("Ravi collected it, due " + ravisLoan.dueDate());
    }
}
