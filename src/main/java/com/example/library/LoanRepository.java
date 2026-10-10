package com.example.library;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Thin wrapper around in-memory maps. */
public class LoanRepository {
    private final Map<String, Book> books = new HashMap<>();
    private final Map<String, Member> members = new HashMap<>();
    private final List<Loan> loans = new ArrayList<>();
    private final List<Hold> holds = new ArrayList<>();

    public Map<String, Book> getBooks() { return books; }
    public Map<String, Member> getMembers() { return members; }
    public List<Loan> getLoans() { return loans; }
    public List<Hold> getHolds() { return Collections.unmodifiableList(holds); }

    public void saveBook(Book book) { books.put(book.isbn(), book); }
    public void saveMember(Member member) { members.put(member.id(), member); }
    public void saveLoan(Loan loan) { loans.add(loan); }
    public void saveHold(Hold hold) { holds.add(hold); }
}
