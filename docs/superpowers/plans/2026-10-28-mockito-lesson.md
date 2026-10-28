# Mockito Lesson Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** A Maven project teaching JUnit 5 + Mockito through six live demos against a library `LoanService`.

**Architecture:** One finished production `LoanService` with four constructor dependencies (three interfaces + `java.time.Clock`). Six self-contained test classes, `LoanServiceDemo1Test`…`LoanServiceDemo6Test`, each showing one technique. README in JavaDemo3's shape.

**Tech Stack:** Java 17, Maven Wrapper, JUnit 5.11.4 (BOM), AssertJ 3.26.3, Mockito 5.14.2 (`mockito-junit-jupiter`).

**Spec:** `docs/superpowers/specs/2026-10-28-mockito-lesson-design.md`

## Global Constraints

- Package `com.example`; test classes end in `Test`.
- Every demo test class opens with a `/* * Demo N - ... * */` block comment in JavaDemo3's style (see `../JavaDemo3/src/test/java/com/example/StringCalculatorDemo1aTest.java`).
- Test methods are package-private, descriptive names, no `test` prefix; AssertJ assertions only.
- Time always via `Clock.fixed(Instant.parse("2026-03-02T10:00:00Z"), ZoneOffset.UTC)`; never mock `Clock`.
- Shared sample data in every demo: `new Member("m1", "Ada", "ada@example.com")`, `new Book("978-0261103344", "The Hobbit", true)`. Expected due date `2026-03-16`.
- Email: to = member email, subject = `"Your loan: " + title`, body = `"Hi " + name + ", you borrowed \"" + title + "\". Please return it by " + dueDate + "."`
- Demos must not share test code (no common base class or helper file).

## Review Focus

1. Email service throws → `borrowBook` still returns the loan and the book is still saved (Task 4 test `loanStillSucceedsWhenEmailFails`).
2. Book unavailable → book is **not** saved and no email sent (Task 4 test `unavailableBookIsNotSavedOrEmailed`).
3. Both member and book unknown → member error wins (rule order) (Task 4 test `unknownMemberIsReportedBeforeUnknownBook`).
4. Saved book is a copy with `available=false`, other fields unchanged (Task 6 captor test).
5. Strict stubs: a leftover unused stub in Demo 6 must fail the build, so Demo 6's teaching example of it is shown in a comment, not as a live failing test.

---

### Task 1: Project scaffold and domain types

**Files:**
- Copy from `../JavaDemo3`: `mvnw`, `mvnw.cmd`, `.mvn/wrapper/maven-wrapper.properties`, `.gitignore`
- Create: `pom.xml` (JavaDemo3's pom, artifactId `mockito-demo`, plus `org.mockito:mockito-junit-jupiter:5.14.2` test scope)
- Create: `src/main/java/com/example/Book.java`, `Member.java`, `Loan.java`, `BookRepository.java`, `MemberRepository.java`, `EmailService.java`

**Interfaces — Produces:**
- `record Book(String isbn, String title, boolean available)`
- `record Member(String id, String name, String email)`
- `record Loan(Member member, Book book, LocalDate dueDate)`
- `interface BookRepository { Optional<Book> findByIsbn(String isbn); void save(Book book); }`
- `interface MemberRepository { Optional<Member> findById(String id); }`
- `interface EmailService { void send(String to, String subject, String body); }`

Each interface gets a one-line comment: no implementation exists, and that's the point.

- [ ] **Step 1:** Create the files above.
- [ ] **Step 2:** Run `./mvnw -q compile`. Expected: BUILD SUCCESS.
- [ ] **Step 3:** Commit (include `docs/`): `git commit -m "Scaffold Mockito lesson project and library domain"`

### Task 2: Demo 1 — hand-written fakes, and the happy path

**Files:**
- Test: `src/test/java/com/example/LoanServiceDemo1Test.java`
- Create: `src/main/java/com/example/LoanService.java`

**Interfaces — Produces:** `LoanService(BookRepository books, MemberRepository members, EmailService email, Clock clock)`; `Loan borrowBook(String memberId, String isbn)`; constant `LOAN_DAYS = 14`.

- [ ] **Step 1: Write the failing test.** Nested static classes `FakeBookRepository` (map-backed, records last saved book), `FakeMemberRepository` (map-backed), `FakeEmailService` (records `List<String>` of sent `to` addresses or a small `SentEmail` record). Header comment: this works, but count the lines — Demo 2 replaces them.
  - `borrowingAnAvailableBookReturnsALoanDueInTwoWeeks`: `assertThat(loan.dueDate()).isEqualTo(LocalDate.of(2026, 3, 16))`, `loan.member()` is Ada, `loan.book().title()` is "The Hobbit".
  - `borrowingABookSendsAConfirmationEmail`: fake recorded exactly one email to `"ada@example.com"`.
- [ ] **Step 2:** `./mvnw -q test -Dtest=LoanServiceDemo1Test` → fails to compile (no `LoanService`).
- [ ] **Step 3:** Implement the happy path only (rule 4 in the spec): look up member and book with `.orElseThrow()`, save `new Book(isbn, title, false)`, due = `LocalDate.now(clock).plusDays(LOAN_DAYS)`, send email per Global Constraints, return `Loan` (holding the saved, unavailable book).
- [ ] **Step 4:** Rerun → PASS.
- [ ] **Step 5:** Commit `"Demo 1: testing LoanService with hand-written fakes"`.

### Task 3: Demo 2 — `mock()` and `when/thenReturn`

**Files:** Test: `src/test/java/com/example/LoanServiceDemo2Test.java`

- [ ] **Step 1:** Fields created with `mock(BookRepository.class)` etc.; `Clock` built with `Clock.fixed` and a header comment "don't mock what you can just create". Tests:
  - `borrowingAnAvailableBookReturnsALoanDueInTwoWeeks` — same assertions as Demo 1, stubs `when(members.findById("m1")).thenReturn(Optional.of(ada))`, same for book.
  - `unstubbedMethodsReturnEmptyDefaults` — `assertThat(books.findByIsbn("nope")).isEmpty()` with comment: Mockito returns empty Optional / null / 0 / false by default.
- [ ] **Step 2:** Run `-Dtest=LoanServiceDemo2Test` → PASS (production already exists; say so in the commit message — this demo is a test refactor, like JavaDemo3's Demo 2).
- [ ] **Step 3:** Commit `"Demo 2: replace fakes with Mockito mocks"`.

### Task 4: Demo 4 — unhappy paths (production rules 1–3 and 5)

Done before Demo 3 because Demo 3's `never()` test needs rule 1.

**Files:**
- Test: `src/test/java/com/example/LoanServiceDemo4Test.java`
- Modify: `src/main/java/com/example/LoanService.java`

- [ ] **Step 1: Write failing tests** (mocks as in Demo 2):
  - `unknownMemberIsRejected` → `IllegalArgumentException`, message `"Unknown member: m1"`.
  - `unknownBookIsRejected` → `IllegalArgumentException`, message `"Unknown book: 978-0261103344"`.
  - `bookAlreadyOnLoanIsRejected` → `IllegalStateException`, message `"Book already on loan: The Hobbit"` (book stubbed with `available=false`).
  - `unknownMemberIsReportedBeforeUnknownBook` → neither stubbed → member message.
  - `unavailableBookIsNotSavedOrEmailed` → after the exception, `verify(books, never()).save(any())`, `verifyNoInteractions(email)`.
  - `loanStillSucceedsWhenEmailFails` → `doThrow(new RuntimeException("Mail server down")).when(email).send(any(), any(), any())`; loan returned, `verify(books).save(any())`. Comment explains `doThrow` is needed because `send` is `void`, so `when(email.send(...))` won't compile.
- [ ] **Step 2:** Run `-Dtest=LoanServiceDemo4Test` → the exception-message tests and email-failure test FAIL.
- [ ] **Step 3:** In `LoanService`: member check first, then book, then availability, with the exact messages above; wrap `email.send` in `try { } catch (RuntimeException e) { }` with comment `// in real code, log it - a failed email shouldn't cancel the loan`.
- [ ] **Step 4:** `./mvnw -q test` → all PASS.
- [ ] **Step 5:** Commit `"Demo 4: unhappy paths and thenThrow/doThrow"`.

### Task 5: Demo 3 — `verify`, `never()`, argument matchers

**Files:** Test: `src/test/java/com/example/LoanServiceDemo3Test.java`

- [ ] **Step 1:** Tests:
  - `confirmationEmailIsSentToTheMember` → `verify(email).send(eq("ada@example.com"), eq("Your loan: The Hobbit"), anyString())`. Comment: once one argument uses a matcher, all must.
  - `bookIsSavedAsUnavailable` → `verify(books).save(new Book("978-0261103344", "The Hobbit", false))` (records have `equals`).
  - `noEmailIsSentForAnUnknownMember` → `assertThatThrownBy(...)`, then `verify(email, never()).send(any(), any(), any())`.
  - Header comment: stubbing = arrange what dependencies *return*; verifying = assert what the class *did* to them. Verify only what matters.
- [ ] **Step 2:** Run `-Dtest=LoanServiceDemo3Test` → PASS.
- [ ] **Step 3:** Commit `"Demo 3: verify and never"`.

### Task 6: Demo 5 — `ArgumentCaptor`

**Files:** Test: `src/test/java/com/example/LoanServiceDemo5Test.java`

- [ ] **Step 1:** Tests:
  - `savedBookIsMarkedUnavailable` → `ArgumentCaptor<Book>`; `verify(books).save(captor.capture())`; assert `available()` false, `isbn()` and `title()` unchanged.
  - `confirmationEmailMentionsTheTitleAndDueDate` → `ArgumentCaptor<String> body`; `verify(email).send(eq("ada@example.com"), anyString(), body.capture())`; `assertThat(body.getValue()).contains("The Hobbit").contains("2026-03-16")`.
  - Header comment: use a captor when you care about *parts* of an argument; Demo 3's exact `equals` match is simpler when you care about all of it.
- [ ] **Step 2:** Run → PASS. Commit `"Demo 5: ArgumentCaptor"`.

### Task 7: Demo 6 — `MockitoExtension` and `@Mock`

**Files:** Test: `src/test/java/com/example/LoanServiceDemo6Test.java`

- [ ] **Step 1:** `@ExtendWith(MockitoExtension.class)`, `@Mock` fields for the three interfaces, `@Captor ArgumentCaptor<String>`, `LoanService` built in `@BeforeEach` with the fixed clock. Header comment explains why not `@InjectMocks` (it would pass `null` for the non-mock `Clock`) and that `MockitoExtension` uses strict stubs: an unused `when(...)` fails the test with `UnnecessaryStubbingException` — include a commented-out example the instructor can uncomment live.
  - Repeat three representative tests in the tidy style: happy-path due date, unknown member with `never()`, email body via `@Captor`.
- [ ] **Step 2:** Run → PASS. Uncomment the strict-stubs example locally, confirm it FAILS with `UnnecessaryStubbingException`, re-comment.
- [ ] **Step 3:** Commit `"Demo 6: MockitoExtension, @Mock and strict stubs"`.

### Task 8: README and final check

**Files:** Create: `README.md`

- [ ] **Step 1:** Write README following JavaDemo3's structure and tone: title, intro (follow-up to the String Calculator lesson), Why Maven, Requirements, Running the tests (incl. `clean test` tip), The scenario (one paragraph + table of the four dependencies and how each is tested: stub / stub / verify / `Clock.fixed`), The demos table (Demo, Test class, What it teaches — note that production code is shared and Demo 4 explains rules Demo 3 relies on), Mocking ideas shown (stub vs verify, mock interfaces you own, don't mock values/records/`Clock`, verify only what matters, strict stubs, `doThrow` for `void`), Exercises (the three from the spec, each "write the failing test first").
- [ ] **Step 2:** `./mvnw clean test` → BUILD SUCCESS, all six demo classes run, 0 failures.
- [ ] **Step 3:** Commit `"README for the Mockito lesson"`.
