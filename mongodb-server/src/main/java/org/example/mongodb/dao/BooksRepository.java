package org.example.mongodb.dao;

import org.example.mongodb.entity.Books;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.ListPagingAndSortingRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

public interface BooksRepository extends CommonMongoRepository<Books,String> {


    @Query(value = "")
    List<Books> findAllBooks();
}
