package com.example;

//A record is a plain data holder: Java generates the constructor, getters, equals() and toString() for us.
//Tests just create real Books with "new" - there's no reason to mock a simple value like this.
public record Book(String isbn, String title, boolean available) {
}
