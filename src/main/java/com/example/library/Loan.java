package com.example.library;

import java.time.LocalDate;

/** A Book lent to a Member. {@code returnDate} is null while the Loan is active. */
public record Loan(String isbn, String memberId, LocalDate loanDate, LocalDate dueDate, LocalDate returnDate) {
    public Loan(String isbn, String memberId, LocalDate loanDate, LocalDate dueDate) {
        this(isbn, memberId, loanDate, dueDate, null);
    }

    public boolean isActive() { return returnDate == null; }

    Loan returned(LocalDate today) { return new Loan(isbn, memberId, loanDate, dueDate, today); }
}
