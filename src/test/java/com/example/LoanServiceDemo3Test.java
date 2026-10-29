package com.example;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/*
* Demo 2 - used stubbing: we told the mocks what to RETURN.
* In this demo we use verify(): we check what LoanService DID to its mocks.
* Stubbing = arranging inputs. Verifying = asserting on side effects, like "an email was sent" or "the book was saved".
* Only verify the calls that matter to the behaviour you're testing - verifying everything makes tests brittle.
* */
public class LoanServiceDemo3Test {

    private final Member ada = new Member("m1", "Ada", "ada@example.com");
    private final Book hobbit = new Book("978-0261103344", "The Hobbit", true);

    private final BookRepository mockBookRepository = mock(BookRepository.class);
    private final MemberRepository mockMemberRepository = mock(MemberRepository.class);
    private final EmailService mockEmailService = mock(EmailService.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-03-02T10:00:00Z"), ZoneOffset.UTC);

    private final LoanService loanService = new LoanService(mockBookRepository, mockMemberRepository, mockEmailService, clock);

    //We care WHO the email went to and its subject, but not the exact wording of the body - so anyString() for that.
    //Rule: if one argument uses a matcher (eq, any, anyString...), ALL arguments must use matchers.
    //That's why the plain values are wrapped in eq(...).
    @Test
    void confirmationEmailIsSentToTheMember() {
        //Stubbing: tell the mocks what to return when called with these arguments.
        when(mockMemberRepository.findById("m1")).thenReturn(Optional.of(ada));
        when(mockBookRepository.findByIsbn("978-0261103344")).thenReturn(Optional.of(hobbit));

        loanService.borrowBook("m1", "978-0261103344");

        //verify(mockEmailService) is short for verify(mockEmailService, times(1)): exactly ONE call with matching arguments.
        verify(mockEmailService).send(eq("ada@example.com"), eq("Your loan: The Hobbit"), anyString());
    }

    //Without matchers, verify() compares arguments with equals(). Records get equals() for free,
    //so this checks the saved book has the same isbn and title, and is now unavailable.
    @Test
    void bookIsSavedAsUnavailable() {
        when(mockMemberRepository.findById("m1")).thenReturn(Optional.of(ada));
        when(mockBookRepository.findByIsbn("978-0261103344")).thenReturn(Optional.of(hobbit));

        loanService.borrowBook("m1", "978-0261103344");

        verify(mockBookRepository).save(new Book("978-0261103344", "The Hobbit", false));
    }

    //never() checks that something did NOT happen. Here we are checking that the method 'send' was never called on the email mock,
    // because the member ID "m1" does not exist in the members repository.
    //No stubbing here: an unstubbed findById returns Optional.empty(), so "m1" is an unknown member.
    @Test
    void noEmailIsSentForAnUnknownMember() {
        assertThatThrownBy(() -> loanService.borrowBook("m1", "978-0261103344"))
                .isInstanceOf(IllegalArgumentException.class);

        //One any() per parameter of send(to, subject, body) - each matches whatever value is passed.
        //So this reads: "send() was never called at all, with any arguments".
        verify(mockEmailService, never()).send(any(), any(), any());
    }
}
