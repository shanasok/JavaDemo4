# Unit Testing with Mocks: JUnit 5 & Mockito

A step-by-step demo of **unit testing classes that have dependencies**, using JUnit 5 and
[Mockito](https://site.mockito.org/). It follows on from the String Calculator TDD demo:
the String Calculator had no dependencies, but most real classes do.

## Why Maven?

The company this course is taught at uses **Maven** as its build tool, so this demo uses
Maven too. You don't need Maven installed: the project includes the **Maven Wrapper**
(`mvnw` / `mvnw.cmd`), which downloads the correct Maven version the first time you run it.

## Requirements

- Java 17 or newer

## Running the tests

```bash
./mvnw test        # macOS / Linux
mvnw.cmd test      # Windows
```

In IntelliJ, you can also click the green arrow next to a test class or method.

> **Tip:** if you rename or delete a test class and get a confusing `ClassNotFoundException`,
> run `./mvnw clean test`. An old compiled copy of the class is still sitting in `target/`.

## The scenario: a library

`LoanService.borrowBook(memberId, isbn)` lets a member borrow a book. It:

1. looks up the member, and throws if they don't exist;
2. looks up the book, and throws if it doesn't exist or is already on loan;
3. saves the book as unavailable;
4. works out the due date (today + 14 days);
5. emails the member a confirmation (if the email fails, the loan still goes ahead);
6. returns the `Loan`.

`LoanService` gets four dependencies through its constructor:

| Dependency | What it does | How the tests handle it |
|------------|--------------|-------------------------|
| `MemberRepository` | Finds a member | **Stub** it: tell it what to return |
| `BookRepository` | Finds and saves books | **Stub** `findByIsbn`, **verify** `save` |
| `EmailService` | Sends an email (returns nothing) | **Verify** it was called, with the right arguments |
| `java.time.Clock` | Says what "today" is | **Don't mock it**: use a real `Clock.fixed(...)` |

The three interfaces have **no implementation at all**. In a real app they'd talk to a database
and a mail server. Mocks let us test `LoanService` before those even exist, and without them
being slow, flaky or sending real emails.

## The demos

Unlike the String Calculator demo, the production code here is **the same for every demo**.
What changes is the test technique. Each test class is self-contained, so you can open any of
them on its own.

| Demo | Test class | What it teaches |
|------|------------|-----------------|
| 1 | `LoanServiceDemo1Test` | **No Mockito**: hand-written fake classes. It works, but there's a lot of code. |
| 2 | `LoanServiceDemo2Test` | **`mock()` and `when(...).thenReturn(...)`**: Mockito builds the fakes. Unstubbed methods return empty defaults. |
| 3 | `LoanServiceDemo3Test` | **`verify()` and `never()`**: check what the class *did* to its dependencies. Argument matchers `eq()`, `any()`, `anyString()`. |
| 4 | `LoanServiceDemo4Test` | **Unhappy paths**: stub the awkward cases (unknown member, book on loan). Make a dependency fail with `doThrow`. |
| 5 | `LoanServiceDemo5Test` | **`ArgumentCaptor`**: grab an argument and assert on parts of it. |
| 6 | `LoanServiceDemo6Test` | **`MockitoExtension`, `@Mock`, `@Captor`**, and strict stubs. Uncomment the marked line to see `UnnecessaryStubbingException`. |

## Mocking ideas shown

- **Stub vs verify**: *stubbing* (`when...thenReturn`) sets up what a dependency returns; *verifying*
  (`verify`) checks what your class did to it. Stub queries, verify commands.
- **Inject dependencies through the constructor**: that's what lets a test swap in mocks.
  A class that calls `new EmailServiceImpl()` inside itself can't be tested this way.
- **Mock interfaces, not values**: records like `Book` and `Member` are just created with `new`.
  The `Clock` is a real `Clock.fixed(...)`. Don't mock what you can just create.
- **Verify only what matters**: verifying every single call ties the test to the implementation.
  Check the side effects that are part of the behaviour (an email was sent, a book was saved).
- **Failed operations leave no side effects**: after an exception, check with `never()` /
  `verifyNoInteractions()` that nothing was saved or sent (Demo 4).
- **`void` methods are stubbed the other way round**: `doThrow(...).when(mock).send(...)`, because
  `when(mock.send(...))` won't compile (Demo 4).
- **If one argument uses a matcher, they all must**: `verify(email).send(eq("ada@example.com"), anyString(), anyString())` (Demo 3).
- **Strict stubs**: with `MockitoExtension`, a stub the test never uses fails the test (Demo 6).

## Exercises: try it yourself

Use TDD. Write the failing test **first** for each one:

1. **A member can have at most 3 books on loan.** Add `int countActiveLoans(String memberId)` to a
   new `LoanRepository` interface, stub it, and reject the 4th loan. Which existing tests need changing, and why?
2. **Returning a book.** Add `returnBook(String isbn)`, which marks the book available again and emails the member.
3. **Late returns are charged a fee.** Use a different `Clock.fixed(...)` in the test to pretend time has passed.
