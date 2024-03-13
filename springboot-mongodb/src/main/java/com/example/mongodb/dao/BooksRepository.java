package com.example.mongodb.dao;


import com.example.mongodb.entity.Books;

public interface BooksRepository extends org.springframework.data.mongodb.repository.MongoRepository<Books, String> {


//    @Query("")
//    List<Books> findAllBooks();
}
