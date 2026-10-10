package com.example.library;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

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
        requireKnown(isbn, memberId);
        if (findActiveLoan(isbn).isPresent()) {
            throw new IllegalStateException("Book already on loan: " + isbn);
        }
        Optional<Hold> ready = findReadyHold(isbn);
        if (ready.isPresent() && !ready.get().memberId().equals(memberId)) {
            throw new IllegalStateException("Book is held for another member: " + isbn);
        }
        long active = repo.getLoans().stream()
                .filter(l -> l.getMemberId().equals(memberId) && l.isActive())
                .count();
        if (active >= MAX_ACTIVE_LOANS) {
            throw new IllegalStateException("Member has too many loans: " + memberId);
        }
        Loan loan = new Loan(isbn, memberId, today, today.plusDays(LOAN_PERIOD_DAYS));
        repo.saveLoan(loan);
        ready.ifPresent(h -> repo.replaceHold(h, h.fulfilled()));
        return loan;
    }

    /** Returns the book and the late fee owed. */
    public BigDecimal returnBook(String isbn, LocalDate today) {
        Loan loan = findActiveLoan(isbn)
                .orElseThrow(() -> new IllegalStateException("Book is not on loan: " + isbn));
        loan.setReturnDate(today);
        firstHoldOn(isbn, Hold::isWaiting).ifPresent(h -> repo.replaceHold(h, h.readyForPickup(today)));
        return fees.lateFee(loan.getDueDate(), today);
    }

    public Hold placeHold(String isbn, String memberId, LocalDate today) {
        requireKnown(isbn, memberId);
        Optional<Loan> loan = findActiveLoan(isbn);
        if (loan.isEmpty() && findReadyHold(isbn).isEmpty()) {
            throw new IllegalStateException("Book is on the shelf, borrow it instead: " + isbn);
        }
        if (loan.isPresent() && loan.get().getMemberId().equals(memberId)) {
            throw new IllegalStateException("Member already has this book on loan: " + isbn);
        }
        if (holdsOf(memberId).stream().anyMatch(h -> h.isActive() && h.isbn().equals(isbn))) {
            throw new IllegalStateException("Member already holds this book: " + isbn);
        }
        Hold hold = new Hold(isbn, memberId, today, HoldState.WAITING, null);
        repo.saveHold(hold);
        return hold;
    }

    /** The Member's Holds, in the order they were placed. */
    public List<Hold> holdsOf(String memberId) {
        return repo.getHolds().stream()
                .filter(h -> h.memberId().equals(memberId))
                .toList();
    }

    private Optional<Hold> findReadyHold(String isbn) {
        return firstHoldOn(isbn, Hold::isReadyForPickup);
    }

    /** The first Hold in the Book's Hold queue (placement order) that matches. */
    private Optional<Hold> firstHoldOn(String isbn, Predicate<Hold> matching) {
        return repo.getHolds().stream()
                .filter(h -> h.isbn().equals(isbn) && matching.test(h))
                .findFirst();
    }

    private void requireKnown(String isbn, String memberId) {
        if (!repo.getBooks().containsKey(isbn)) {
            throw new IllegalArgumentException("Unknown book: " + isbn);
        }
        if (!repo.getMembers().containsKey(memberId)) {
            throw new IllegalArgumentException("Unknown member: " + memberId);
        }
    }

    public Optional<Loan> findActiveLoan(String isbn) {
        return repo.getLoans().stream()
                .filter(l -> l.getIsbn().equals(isbn) && l.isActive())
                .findFirst();
    }
}
