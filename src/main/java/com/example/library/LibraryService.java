package com.example.library;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

public class LibraryService {
    static final int LOAN_PERIOD_DAYS = 14;
    static final int MAX_ACTIVE_LOANS = 3;
    static final int MAX_ACTIVE_HOLDS = 3;

    private final LibraryStore store;
    private final FeeCalculator fees;

    public LibraryService(LibraryStore store, FeeCalculator fees) {
        this.store = store;
        this.fees = fees;
    }

    public Loan checkout(String isbn, String memberId, LocalDate today) {
        requireKnown(isbn, memberId);
        expireHoldsPastPickupWindow(isbn, today);
        if (store.activeLoanOn(isbn).isPresent()) {
            throw new IllegalStateException("Book already on loan: " + isbn);
        }
        Optional<Hold> ready = findReadyHold(isbn);
        if (ready.isPresent() && !ready.get().memberId().equals(memberId)) {
            throw new IllegalStateException("Book is held for another member: " + isbn);
        }
        long active = store.activeLoanCount(memberId);
        if (active >= MAX_ACTIVE_LOANS) {
            throw new IllegalStateException("Member has too many loans: " + memberId);
        }
        Loan loan = new Loan(isbn, memberId, today, today.plusDays(LOAN_PERIOD_DAYS));
        store.saveLoan(loan);
        ready.ifPresent(h -> store.replaceHold(h, h.fulfilled()));
        return loan;
    }

    /** Returns the book and the late fee owed. */
    public BigDecimal returnBook(String isbn, LocalDate today) {
        Loan loan = store.activeLoanOn(isbn)
                .orElseThrow(() -> new IllegalStateException("Book is not on loan: " + isbn));
        store.replaceLoan(loan, loan.returned(today));
        handToNextWaitingHold(isbn, today);
        return fees.lateFee(loan.dueDate(), today);
    }

    public Hold placeHold(String isbn, String memberId, LocalDate today) {
        requireKnown(isbn, memberId);
        expireHoldsPastPickupWindow(isbn, today);
        Optional<Loan> loan = store.activeLoanOn(isbn);
        if (loan.isEmpty() && findReadyHold(isbn).isEmpty()) {
            throw new IllegalStateException("Book is on the shelf, borrow it instead: " + isbn);
        }
        if (loan.isPresent() && loan.get().memberId().equals(memberId)) {
            throw new IllegalStateException("Member already has this book on loan: " + isbn);
        }
        if (findActiveHold(isbn, memberId).isPresent()) {
            throw new IllegalStateException("Member already holds this book: " + isbn);
        }
        if (activeHoldCount(memberId, today) >= MAX_ACTIVE_HOLDS) {
            throw new IllegalStateException("Member has too many holds: " + memberId);
        }
        Hold hold = new Hold(isbn, memberId, today, HoldState.WAITING, null);
        store.saveHold(hold);
        return hold;
    }

    public Hold cancelHold(String isbn, String memberId, LocalDate today) {
        requireKnown(isbn, memberId);
        expireHoldsPastPickupWindow(isbn, today);
        Hold hold = findActiveHold(isbn, memberId)
                .orElseThrow(() -> new IllegalStateException("Member has no active hold on this book: " + isbn));
        Hold cancelled = hold.cancelled();
        store.replaceHold(hold, cancelled);
        if (hold.isReadyForPickup()) {
            handToNextWaitingHold(isbn, today);
        }
        return cancelled;
    }

    /** The Member's Holds as of today, in the order they were placed. */
    public List<Hold> holdsOf(String memberId, LocalDate today) {
        store.holdsPlacedBy(memberId).stream()
                .filter(Hold::isActive)
                .map(Hold::isbn)
                .distinct()
                .forEach(isbn -> expireHoldsPastPickupWindow(isbn, today));
        return store.holdsPlacedBy(memberId);
    }

    /** Waiting and Ready-for-pickup Holds, after Expiring any on the Member's other Books whose window has passed. */
    private long activeHoldCount(String memberId, LocalDate today) {
        return holdsOf(memberId, today).stream().filter(Hold::isActive).count();
    }

    /**
     * Expires each Ready-for-pickup Hold whose Pickup window ended before today and hands the Book
     * to the next Waiting Hold. Each new window starts on the day the previous Hold Expired, so when
     * several windows have passed the queue advances through each in turn.
     */
    private void expireHoldsPastPickupWindow(String isbn, LocalDate today) {
        Optional<Hold> ready = findReadyHold(isbn);
        while (ready.isPresent() && today.isAfter(ready.get().lastPickupDay())) {
            Hold hold = ready.get();
            store.replaceHold(hold, hold.expired());
            handToNextWaitingHold(isbn, hold.lastPickupDay().plusDays(1));
            ready = findReadyHold(isbn);
        }
    }

    /** Sets the Book aside for the first Waiting Hold, if any; otherwise it is on the shelf. */
    private void handToNextWaitingHold(String isbn, LocalDate readyOn) {
        firstHoldOn(isbn, Hold::isWaiting).ifPresent(h -> store.replaceHold(h, h.readyForPickup(readyOn)));
    }

    /** The Member's Waiting or Ready-for-pickup Hold on the Book; there is at most one. */
    private Optional<Hold> findActiveHold(String isbn, String memberId) {
        return firstHoldOn(isbn, h -> h.isActive() && h.memberId().equals(memberId));
    }

    private Optional<Hold> findReadyHold(String isbn) {
        return firstHoldOn(isbn, Hold::isReadyForPickup);
    }

    /** The first Hold in the Book's Hold queue (placement order) that matches. */
    private Optional<Hold> firstHoldOn(String isbn, Predicate<Hold> matching) {
        return store.holdQueueOn(isbn).stream().filter(matching).findFirst();
    }

    private void requireKnown(String isbn, String memberId) {
        if (!store.isKnownBook(isbn)) {
            throw new IllegalArgumentException("Unknown book: " + isbn);
        }
        if (!store.isKnownMember(memberId)) {
            throw new IllegalArgumentException("Unknown member: " + memberId);
        }
    }
}
