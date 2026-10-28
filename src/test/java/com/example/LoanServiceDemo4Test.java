package com.example;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/*
* Demo 3 - mostly tested the happy path.
* In this demo we test the unhappy paths. Mocks make these easy: just stub a dependency to return the awkward case.
* No need to set up a real database with a missing member or a book that's already on loan.
* We can even make a dependency FAIL, to check how LoanService copes when the email server is down.
* */
public class LoanServiceDemo4Test {

    private final Member ada = new Member("m1", "Ada", "ada@example.com");
    private final Book hobbit = new Book("978-0261103344", "The Hobbit", true);
    private final Book hobbitOnLoan = new Book("978-0261103344", "The Hobbit", false);

    private final BookRepository books = mock(BookRepository.class);
    private final MemberRepository members = mock(MemberRepository.class);
    private final EmailService email = mock(EmailService.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-03-02T10:00:00Z"), ZoneOffset.UTC);

    private final LoanService loanService = new LoanService(books, members, email, clock);

    @Test
    void unknownMemberIsRejected() {
        when(members.findById("m1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> loanService.borrowBook("m1", "978-0261103344"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Unknown member: m1");
    }

    @Test
    void unknownBookIsRejected() {
        when(members.findById("m1")).thenReturn(Optional.of(ada));
        when(books.findByIsbn("978-0261103344")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> loanService.borrowBook("m1", "978-0261103344"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Unknown book: 978-0261103344");
    }

    @Test
    void bookAlreadyOnLoanIsRejected() {
        when(members.findById("m1")).thenReturn(Optional.of(ada));
        when(books.findByIsbn("978-0261103344")).thenReturn(Optional.of(hobbitOnLoan));

        assertThatThrownBy(() -> loanService.borrowBook("m1", "978-0261103344"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Book already on loan: The Hobbit");
    }

    //Neither lookup is stubbed, so both return Optional.empty(). The member check comes first, so that's the error we get.
    @Test
    void unknownMemberIsReportedBeforeUnknownBook() {
        assertThatThrownBy(() -> loanService.borrowBook("m1", "978-0261103344"))
                .hasMessage("Unknown member: m1");
    }

    //Checking the exception isn't enough - a failed loan must not leave side effects behind.
    //verifyNoInteractions checks that NO method at all was called on the email mock.
    @Test
    void unavailableBookIsNotSavedOrEmailed() {
        when(members.findById("m1")).thenReturn(Optional.of(ada));
        when(books.findByIsbn("978-0261103344")).thenReturn(Optional.of(hobbitOnLoan));

        assertThatThrownBy(() -> loanService.borrowBook("m1", "978-0261103344"))
                .isInstanceOf(IllegalStateException.class);

        verify(books, never()).save(any());
        verifyNoInteractions(email);
    }

    //Making a dependency throw. For a method that RETURNS something you'd write when(...).thenThrow(...),
    //but send() is void, and when(email.send(...)) doesn't compile - you can't pass "nothing" to when().
    //So for void methods the order flips: doThrow(...).when(mock).method(...).
    @Test
    void loanStillSucceedsWhenEmailFails() {
        when(members.findById("m1")).thenReturn(Optional.of(ada));
        when(books.findByIsbn("978-0261103344")).thenReturn(Optional.of(hobbit));
        doThrow(new RuntimeException("Mail server down")).when(email).send(any(), any(), any());

        Loan loan = loanService.borrowBook("m1", "978-0261103344");

        assertThat(loan.book().title()).isEqualTo("The Hobbit");
        verify(books).save(any());
    }
}
