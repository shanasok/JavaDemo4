package com.example;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/*
* Demo 1 - needed a whole page of hand-written fake classes.
* In this demo Mockito creates them for us: mock(BookRepository.class) builds a fake BookRepository at runtime.
* We then "stub" it - tell it what to return - with when(...).thenReturn(...).
* The production code is unchanged: only the way we test it has changed.
* */
public class LoanServiceDemo2Test {

    private final Member ada = new Member("m1", "Ada", "ada@example.com");
    private final Book hobbit = new Book("978-0261103344", "The Hobbit", true);

    private final BookRepository mockBookRepository = mock(BookRepository.class);
    private final MemberRepository mockMemberRepository = mock(MemberRepository.class);
    private final EmailService mockEmailService = mock(EmailService.class);
    //Notice we do NOT mock the Clock. Don't mock what you can just create:
    //Clock.fixed gives us a real Clock with a known date, which is simpler and clearer than a mock.
    private final Clock clock = Clock.fixed(Instant.parse("2026-03-02T10:00:00Z"), ZoneOffset.UTC);

    private final LoanService loanService = new LoanService(mockBookRepository, mockMemberRepository, mockEmailService, clock);

    //Arrange: stub the two lookups. Act: borrow. Assert: check the returned Loan.
    //The EmailService mock needs no setup at all - by default a mock's void methods just do nothing.
    @Test
    void borrowingAnAvailableBookReturnsALoanDueInTwoWeeks() {
        //Setting up your mock situation called stubbing
        when(mockMemberRepository.findById("m1")).thenReturn(Optional.of(ada));
        when(mockBookRepository.findByIsbn("978-0261103344")).thenReturn(Optional.of(hobbit));

        //Call the method under test
        Loan loan = loanService.borrowBook("m1", "978-0261103344");

        assertThat(loan.member()).isEqualTo(ada);
        assertThat(loan.book().title()).isEqualTo("The Hobbit");
        assertThat(loan.dueDate()).isEqualTo(LocalDate.of(2026, 3, 16));
    }

    //What does a mock return if we DON'T stub a method? A sensible "empty" default:
    //an empty Optional, an empty list, null for other objects, 0 for numbers, false for booleans.
    @Test
    void unstubbedMethodsReturnEmptyDefaults() {
        assertThat(mockBookRepository.findByIsbn("978-0261103344")).isEmpty();
        assertThat(mockMemberRepository.findById("m1")).isEmpty();
    }
}
