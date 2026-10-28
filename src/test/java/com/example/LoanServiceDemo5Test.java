package com.example;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/*
* Demo 3 - verified calls by matching the arguments exactly (or with any()).
* In this demo we CAPTURE an argument with an ArgumentCaptor, then use normal AssertJ assertions on it.
* Use a captor when you care about PART of an argument, e.g. "the email body mentions the due date".
* When you care about the whole argument, an exact match like in Demo 3 is simpler.
* */
public class LoanServiceDemo5Test {

    private final Member ada = new Member("m1", "Ada", "ada@example.com");
    private final Book hobbit = new Book("978-0261103344", "The Hobbit", true);

    private final BookRepository books = mock(BookRepository.class);
    private final MemberRepository members = mock(MemberRepository.class);
    private final EmailService email = mock(EmailService.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-03-02T10:00:00Z"), ZoneOffset.UTC);

    private final LoanService loanService = new LoanService(books, members, email, clock);

    @Test
    void savedBookIsMarkedUnavailable() {
        when(members.findById("m1")).thenReturn(Optional.of(ada));
        when(books.findByIsbn("978-0261103344")).thenReturn(Optional.of(hobbit));

        loanService.borrowBook("m1", "978-0261103344");

        //LoanService creates the saved Book internally, so the test never gets to see it directly.
        //An ArgumentCaptor is an empty holder that grabs the value LoanService passed to the mock:
        //  1. forClass(Book.class) creates a holder for a Book.
        //  2. capture() acts as a matcher inside verify(): it matches anything, and remembers the value that was passed.
        //  3. getValue() hands that value back, so we can check it with normal AssertJ assertions.
        //How can it capture a call that already happened? A mock records EVERY call made to it, like CCTV.
        //verify() runs AFTER borrowBook() and looks back through that recording - capture() just copies the argument out of it.
        ArgumentCaptor<Book> savedBook = ArgumentCaptor.forClass(Book.class);
        verify(books).save(savedBook.capture());
        assertThat(savedBook.getValue().available()).isFalse();
        assertThat(savedBook.getValue().isbn()).isEqualTo("978-0261103344");
        assertThat(savedBook.getValue().title()).isEqualTo("The Hobbit");
    }

    //We don't want to copy the whole email text into the test - any wording change would break it.
    //Capturing the body lets us check just the facts that matter.
    @Test
    void confirmationEmailMentionsTheTitleAndDueDate() {
        when(members.findById("m1")).thenReturn(Optional.of(ada));
        when(books.findByIsbn("978-0261103344")).thenReturn(Optional.of(hobbit));

        loanService.borrowBook("m1", "978-0261103344");

        ArgumentCaptor<String> body = ArgumentCaptor.forClass(String.class);
        verify(email).send(eq("ada@example.com"), anyString(), body.capture());
        assertThat(body.getValue())
                .contains("The Hobbit")
                .contains("2026-03-16");
    }
}
