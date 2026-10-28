package com.example;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/*
* Demo 1 - testing a class that has dependencies, WITHOUT Mockito.
* LoanService needs a BookRepository, a MemberRepository and an EmailService, but none of them has a real implementation.
* So we write our own "fake" versions at the bottom of this file. It works... but count the lines!
* Every new test scenario means more fake code to write and maintain. Demo 2 replaces all of it with Mockito.
* */
public class LoanServiceDemo1Test {

    private final Member ada = new Member("m1", "Ada", "ada@example.com");
    private final Book hobbit = new Book("978-0261103344", "The Hobbit", true);

    private final FakeBookRepository books = new FakeBookRepository();
    private final FakeMemberRepository members = new FakeMemberRepository();
    private final FakeEmailService email = new FakeEmailService();
    //Clock.fixed always returns the same instant, so "today" is 2 March 2026 in every test run.
    private final Clock clock = Clock.fixed(Instant.parse("2026-03-02T10:00:00Z"), ZoneOffset.UTC);

    private final LoanService loanService = new LoanService(books, members, email, clock);

    @Test
    void borrowingAnAvailableBookReturnsALoanDueInTwoWeeks() {
        members.add(ada);
        books.add(hobbit);

        Loan loan = loanService.borrowBook("m1", "978-0261103344");

        assertThat(loan.member()).isEqualTo(ada);
        assertThat(loan.book().title()).isEqualTo("The Hobbit");
        assertThat(loan.dueDate()).isEqualTo(LocalDate.of(2026, 3, 16));
    }

    //EmailService.send() returns nothing, so there's no return value to check.
    //Instead, our fake RECORDS what it was asked to do, and the test checks that record.
    @Test
    void borrowingABookSendsAConfirmationEmail() {
        members.add(ada);
        books.add(hobbit);

        loanService.borrowBook("m1", "978-0261103344");

        assertThat(email.sentTo).containsExactly("ada@example.com");
    }

    //---------- Hand-written fakes: everything below exists only to support the two tests above ----------

    static class FakeBookRepository implements BookRepository {
        private final Map<String, Book> booksByIsbn = new HashMap<>();

        void add(Book book) {
            booksByIsbn.put(book.isbn(), book);
        }

        @Override
        public Optional<Book> findByIsbn(String isbn) {
            return Optional.ofNullable(booksByIsbn.get(isbn));
        }

        @Override
        public void save(Book book) {
            booksByIsbn.put(book.isbn(), book);
        }
    }

    static class FakeMemberRepository implements MemberRepository {
        private final Map<String, Member> membersById = new HashMap<>();

        void add(Member member) {
            membersById.put(member.id(), member);
        }

        @Override
        public Optional<Member> findById(String id) {
            return Optional.ofNullable(membersById.get(id));
        }
    }

    static class FakeEmailService implements EmailService {
        private final List<String> sentTo = new ArrayList<>();

        @Override
        public void send(String to, String subject, String body) {
            sentTo.add(to);
        }
    }
}
