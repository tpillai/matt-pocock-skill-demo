package com.example.library;

import java.time.LocalDate;

/** Tiny scripted scenario so you can watch the domain in action: mvn -q compile exec:java */
public class LibraryApp {
    public static void main(String[] args) {
        LoanRepository repo = new LoanRepository();
        LibraryService library = new LibraryService(repo, new FeeCalculator());

        repo.saveBook(new Book("978-0134685991", "Effective Java", "Joshua Bloch"));
        repo.saveBook(new Book("978-0132350884", "Clean Code", "Robert C. Martin"));
        repo.saveMember(new Member("m-1", "Asha"));
        repo.saveMember(new Member("m-2", "Ravi"));

        LocalDate day0 = LocalDate.of(2026, 10, 1);

        Loan loan = library.checkout("978-0134685991", "m-1", day0);
        System.out.println("Asha borrowed Effective Java, due " + loan.getDueDate());

        // Ravi wants the same book. Today there is no way to queue for it.
        try {
            library.checkout("978-0134685991", "m-2", day0.plusDays(2));
        } catch (IllegalStateException e) {
            System.out.println("Ravi could not borrow it: " + e.getMessage());
        }

        // Asha returns it exactly on the due date.
        var fee = library.returnBook("978-0134685991", loan.getDueDate());
        System.out.println("Asha returned it on the due date. Fee charged: $" + fee);

        // Callers can reach straight into the repository and rewrite history.
        Loan second = library.checkout("978-0132350884", "m-2", day0);
        repo.getLoans().get(repo.getLoans().indexOf(second)).setDueDate(day0.plusYears(1));
        System.out.println("Ravi's Clean Code is now due " + second.getDueDate() + " (nobody stopped that)");
    }
}
