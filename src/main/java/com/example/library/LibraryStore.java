package com.example.library;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** In-memory Books, Members, Loans and Holds. Answers questions; never hands out its collections. */
public class LibraryStore {
    private final Map<String, Book> books = new HashMap<>();
    private final Map<String, Member> members = new HashMap<>();
    private final List<Loan> loans = new ArrayList<>();
    private final List<Hold> holds = new ArrayList<>();

    public void saveBook(Book book) { books.put(book.isbn(), book); }
    public void saveMember(Member member) { members.put(member.id(), member); }
    public void saveLoan(Loan loan) { loans.add(loan); }
    public void saveHold(Hold hold) { holds.add(hold); }

    public boolean isKnownBook(String isbn) { return books.containsKey(isbn); }

    public boolean isKnownMember(String memberId) { return members.containsKey(memberId); }

    /** The Loan on the Book that has not been returned; there is at most one. */
    public Optional<Loan> activeLoanOn(String isbn) {
        return loans.stream()
                .filter(l -> l.isbn().equals(isbn) && l.isActive())
                .findFirst();
    }

    public long activeLoanCount(String memberId) {
        return loans.stream()
                .filter(l -> l.memberId().equals(memberId) && l.isActive())
                .count();
    }

    /** The Book's Hold queue: every Hold placed on it, in placement order, whatever its state. */
    public List<Hold> holdQueueOn(String isbn) {
        return holds.stream().filter(h -> h.isbn().equals(isbn)).toList();
    }

    /** The Member's Holds on any Book, in placement order, whatever their state. */
    public List<Hold> holdsPlacedBy(String memberId) {
        return holds.stream().filter(h -> h.memberId().equals(memberId)).toList();
    }

    /** Replaces a Loan in place. */
    public void replaceLoan(Loan existing, Loan updated) {
        replace(loans, existing, updated, "Unknown loan: ");
    }

    /** Replaces a Hold in place, keeping its position in the Hold queue. */
    public void replaceHold(Hold existing, Hold updated) {
        replace(holds, existing, updated, "Unknown hold: ");
    }

    private static <T> void replace(List<T> items, T existing, T updated, String unknown) {
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i) == existing) {
                items.set(i, updated);
                return;
            }
        }
        throw new IllegalArgumentException(unknown + existing);
    }
}
