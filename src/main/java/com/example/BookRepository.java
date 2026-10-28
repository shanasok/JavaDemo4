package com.example;

import java.util.Optional;

//There is NO class that implements this interface - and that's the point.
//In a real app it would talk to a database. In unit tests we replace it with a mock.
public interface BookRepository {

    Optional<Book> findByIsbn(String isbn);

    void save(Book book);
}
