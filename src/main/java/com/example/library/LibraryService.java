package com.example.library;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

public class LibraryService {
    static final int LOAN_PERIOD_DAYS = 14;
    static final int MAX_ACTIVE_LOANS = 3;

    private final LoanRepository repo;
    private final FeeCalculator fees;

    public LibraryService(LoanRepository repo, FeeCalculator fees) {
        this.repo = repo;
        this.fees = fees;
    }

    public LoanRepository getRepo() { return repo; }

    public Loan checkout(String isbn, String memberId, LocalDate today) {
        if (!repo.getBooks().containsKey(isbn)) {
            throw new IllegalArgumentException("Unknown book: " + isbn);
        }
        if (!repo.getMembers().containsKey(memberId)) {
            throw new IllegalArgumentException("Unknown member: " + memberId);
        }
        if (findActiveLoan(isbn).isPresent()) {
            throw new IllegalStateException("Book already on loan: " + isbn);
        }
        long active = repo.getLoans().stream()
                .filter(l -> l.getMemberId().equals(memberId) && l.isActive())
                .count();
        if (active >= MAX_ACTIVE_LOANS) {
            throw new IllegalStateException("Member has too many loans: " + memberId);
        }
        Loan loan = new Loan(isbn, memberId, today, today.plusDays(LOAN_PERIOD_DAYS));
        repo.saveLoan(loan);
        return loan;
    }

    /** Returns the book and the late fee owed. */
    public BigDecimal returnBook(String isbn, LocalDate today) {
        Loan loan = findActiveLoan(isbn)
                .orElseThrow(() -> new IllegalStateException("Book is not on loan: " + isbn));
        loan.setReturnDate(today);
        return fees.lateFee(loan.getDueDate(), today);
    }

    public Optional<Loan> findActiveLoan(String isbn) {
        return repo.getLoans().stream()
                .filter(l -> l.getIsbn().equals(isbn) && l.isActive())
                .findFirst();
    }
}
