package com.example;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/*
* Demos 2-5 - created every mock and captor by hand with mock(...) and ArgumentCaptor.forClass(...).
* In this demo MockitoExtension does that for us: annotate a field with @Mock or @Captor and it's created before each test.
* The extension also uses "strict stubs": a when(...) that the test never actually uses makes the test FAIL.
* That keeps tests honest - leftover stubbing usually means the test isn't testing what you think.
* */
@ExtendWith(MockitoExtension.class)
public class LoanServiceDemo6Test {

    private final Member ada = new Member("m1", "Ada", "ada@example.com");
    private final Book hobbit = new Book("978-0261103344", "The Hobbit", true);

    //@Mock is short for mock(BookRepository.class), and @Captor for ArgumentCaptor.forClass(String.class).
    //MockitoExtension fills these fields with FRESH mocks/captors before EACH test, so no recorded calls leak between tests.
    //Try removing @ExtendWith above: the fields stay null and every test fails with a NullPointerException.
    //The fields can't be final, because Mockito fills them in after the test object is created.
    @Mock
    private BookRepository mockBookRepository;
    @Mock
    private MemberRepository mockMemberRepository;
    @Mock
    private EmailService mockEmailService;
    @Captor
    private ArgumentCaptor<String> body;

    private LoanService loanService;

    //Why not @InjectMocks? It builds LoanService from the @Mock fields - but the Clock isn't a mock,
    //so @InjectMocks would pass null for it. Building the service ourselves is clearer anyway.
    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-03-02T10:00:00Z"), ZoneOffset.UTC);
        loanService = new LoanService(mockBookRepository, mockMemberRepository, mockEmailService, clock);
    }

    @Test
    void borrowingAnAvailableBookReturnsALoanDueInTwoWeeks() {
        when(mockMemberRepository.findById("m1")).thenReturn(Optional.of(ada));
        when(mockBookRepository.findByIsbn("978-0261103344")).thenReturn(Optional.of(hobbit));

        Loan loan = loanService.borrowBook("m1", "978-0261103344");

        assertThat(loan.dueDate()).isEqualTo(LocalDate.of(2026, 3, 16));
    }

    //Live demo: uncomment the when(...) line below and run this test.
    //It fails with UnnecessaryStubbingException - LoanService throws before it ever looks up the book.
    @Test
    void noEmailIsSentForAnUnknownMember() {
        //when(mockBookRepository.findByIsbn("978-0261103344")).thenReturn(Optional.of(hobbit));

        assertThatThrownBy(() -> loanService.borrowBook("m1", "978-0261103344"))
                .isInstanceOf(IllegalArgumentException.class);

        verify(mockEmailService, never()).send(any(), any(), any());
    }

    @Test
    void confirmationEmailMentionsTheTitleAndDueDate() {
        when(mockMemberRepository.findById("m1")).thenReturn(Optional.of(ada));
        when(mockBookRepository.findByIsbn("978-0261103344")).thenReturn(Optional.of(hobbit));

        loanService.borrowBook("m1", "978-0261103344");

        //body.capture() doesn't check the third parameter - like anyString(), it accepts anything, but it also SAVES the value.
        //The real check on the body happens below, with assertThat(body.getValue()).
        verify(mockEmailService).send(eq("ada@example.com"), anyString(), body.capture());
        assertThat(body.getValue())
                .contains("The Hobbit")
                .contains("2026-03-16");
    }
}
