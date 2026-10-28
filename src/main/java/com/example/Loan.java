package com.example;

import java.time.LocalDate;

public record Loan(Member member, Book book, LocalDate dueDate) {
}
