package org.example.mongodb.dao;

import org.example.mongodb.entity.Books;

public interface BooksRepository extends org.springframework.data.mongodb.repository.MongoRepository<Books, String> {


//    @Query("")
//    List<Books> findAllBooks();
}
