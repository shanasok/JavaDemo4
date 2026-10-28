# Mockito Lesson: Library Loans — Design

## Intent

A live-demo lesson on **unit testing with JUnit 5 and Mockito**, the follow-up to the
String Calculator TDD lesson in `../JavaDemo3`. Students already know the red/green/refactor
rhythm, `@ParameterizedTest`, and AssertJ. This lesson answers the next question: *how do you
unit test a class that depends on other classes?*

The instructor demos it live, so every demo must be small, self-contained, and easy to open
side by side. The domain is deliberately simple and unrelated to the students' jobs, so all
attention goes to the mocking.

## Goals

- Show why mocks exist (hand-written fakes are tedious), then the core Mockito toolkit:
  `mock()`, `when/thenReturn`, `verify`, `never()`, `thenThrow`, `ArgumentCaptor`,
  `@Mock` / `@InjectMocks` / `MockitoExtension`.
- Make the point that dependencies are **interfaces with no real implementation** — you can
  test `LoanService` before the database or email system exist.
- Match JavaDemo3's style closely: same Maven setup, same naming, same comment headers,
  same README shape (demo table, "ideas shown" list, exercises).

## Non-goals

- Spies, static mocking, `inOrder`, `verifyNoMoreInteractions`, BDD-style Mockito — at most
  mentioned in exercises.
- Integration tests, Spring, databases.
- Teaching TDD again (tests may be written test-first, but the lesson isn't about that).

## Anti-goals

- A domain so rich students get lost in business rules instead of mocking.
- Demos that depend on each other's test code, so the instructor can't jump to one.
- Mocking things that are trivial to construct (records, `Clock`) — that would teach a bad habit.

## Constraints

- Maven with the Maven Wrapper, Java 17, JUnit 5 (BOM), AssertJ — same versions as JavaDemo3.
- Add `org.mockito:mockito-junit-jupiter` (current 5.x), test scope.
- Package `com.example`. Test classes named `*Test` so Maven runs them.
- Copy JavaDemo3's `.gitignore`, `mvnw`, `mvnw.cmd`, `.mvn/wrapper`.

## The domain

All in `src/main/java/com/example`:

- `record Book(String isbn, String title, boolean available)`
- `record Member(String id, String name, String email)`
- `record Loan(Member member, Book book, LocalDate dueDate)`
- `interface BookRepository` — `Optional<Book> findByIsbn(String isbn)`, `void save(Book book)`
- `interface MemberRepository` — `Optional<Member> findById(String id)`
- `interface EmailService` — `void send(String to, String subject, String body)`
- `class LoanService` — constructor takes `BookRepository`, `MemberRepository`, `EmailService`,
  `java.time.Clock`. One public method: `Loan borrowBook(String memberId, String isbn)`.

### `borrowBook` rules

1. Unknown member → `IllegalArgumentException("Unknown member: <id>")`.
2. Unknown book → `IllegalArgumentException("Unknown book: <isbn>")`.
3. Book not available → `IllegalStateException("Book already on loan: <title>")`.
4. Otherwise: save the book with `available = false`, compute due date = today (from the
   `Clock`) + 14 days, send a confirmation email to the member, return the `Loan`.
5. If the email service throws, the loan **still succeeds** (the email is caught and ignored —
   a comment says "in real code, log it"). This gives Demo 4 a meaningful `thenThrow` case.

No email and no save happen when rules 1–3 fail.

## Production code structure (builder's call, flagged)

**One** finished `LoanService` shared by all demos — unlike JavaDemo3, where production code
grew per demo. Here the lesson is about *test* techniques, so each demo is a test class
against the same production code. This keeps live demos independent and avoids six copies of
the domain classes.

## The demos

Each demo is a test class `LoanServiceDemoNTest` with a block comment header in JavaDemo3's
style explaining what it teaches. Each is self-contained.

| Demo | Teaches |
|------|---------|
| 1 | **No Mockito**: hand-written fakes (`FakeBookRepository`, `FakeMemberRepository`, `FakeEmailService` that records sent emails) as nested static classes in the test. Happy path + one check on the email. Point: it works, but it's a lot of code. |
| 2 | **`mock()` + `when(...).thenReturn(...)`**: same happy-path test, fakes replaced by mocks. Stubbing returns `Optional.of(...)`. Show that unstubbed methods return defaults (`Optional.empty()`). |
| 3 | **`verify` and `never()`**: email was sent; book was saved as unavailable; on unknown member, `verify(emailService, never()).send(any(), any(), any())`. Introduce argument matchers (`any()`, `eq()`). |
| 4 | **Unhappy paths**: unknown member, unknown book, book unavailable (each via stubbing + `assertThatThrownBy`); email service `doThrow(...)` → loan still returned. Explain `doThrow` vs `thenThrow` for `void` methods. |
| 5 | **`ArgumentCaptor`**: capture the `Book` passed to `save` and the email body; assert on their contents (title, due date in body). |
| 6 | **Tidy up**: `@ExtendWith(MockitoExtension.class)`, `@Mock`, `@InjectMocks`-style setup. Because `Clock` is a constructor arg that's not a mock, construct `LoanService` in `@BeforeEach` with `@Mock` fields rather than `@InjectMocks` — and say why in the comment. Also show strict stubs: an unused stub fails the test. |

Throughout: `Clock.fixed(...)` for time — **never mock `Clock`**; Demo 2's header points this out
("don't mock what you can just create").

## README

Same shape as JavaDemo3's: why Maven, requirements, running tests, the scenario (one
paragraph + the four dependencies), the demo table, "Mocking ideas shown" (stub vs verify,
mock interfaces you own, don't mock values/records/Clock, test behaviour not interactions
for their own sake, strict stubs), and exercises:

1. Max 3 active loans per member — needs a new `LoanRepository.countActiveLoans(memberId)`.
2. `returnBook(isbn)` that marks the book available and emails the member.
3. Late returns charge a fee — use the fixed `Clock` to simulate time passing.

## Left to the builder

Exact test method names, sample data (book titles, member names), email subject/body wording,
Mockito version pin.
