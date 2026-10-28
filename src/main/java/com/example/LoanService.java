package com.example;

import java.time.Clock;
import java.time.LocalDate;

/*
* The class under test in every demo.
* It doesn't do much on its own - it coordinates its four dependencies, which are all passed in through the constructor.
* Because they're passed in (not created with "new" inside this class), a test can hand it mocks instead.
* */
public class LoanService {

    static final int LOAN_DAYS = 14;

    private final BookRepository books;
    private final MemberRepository members;
    private final EmailService email;
    //The current date comes from a Clock rather than LocalDate.now(), so tests can fix "today" to a known date.
    private final Clock clock;

    public LoanService(BookRepository books, MemberRepository members, EmailService email, Clock clock) {
        this.books = books;
        this.members = members;
        this.email = email;
        this.clock = clock;
    }

    public Loan borrowBook(String memberId, String isbn) {
        Member member = members.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown member: " + memberId));
        Book book = books.findByIsbn(isbn)
                .orElseThrow(() -> new IllegalArgumentException("Unknown book: " + isbn));
        if (!book.available()) {
            throw new IllegalStateException("Book already on loan: " + book.title());
        }

        //Records can't be changed, so "marking it borrowed" means creating a new Book with available = false.
        Book borrowed = new Book(book.isbn(), book.title(), false);
        books.save(borrowed);
        LocalDate dueDate = LocalDate.now(clock).plusDays(LOAN_DAYS);
        sendConfirmation(member, borrowed, dueDate);
        return new Loan(member, borrowed, dueDate);
    }

    private void sendConfirmation(Member member, Book book, LocalDate dueDate) {
        try {
            email.send(member.email(),
                    "Your loan: " + book.title(),
                    "Hi " + member.name() + ", you borrowed \"" + book.title() + "\". Please return it by " + dueDate + ".");
        } catch (RuntimeException e) {
            //In real code, log it - a failed email shouldn't cancel the loan.
        }
    }
}
