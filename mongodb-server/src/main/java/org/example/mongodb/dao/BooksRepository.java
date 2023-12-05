package org.example.mongodb.dao;

import org.example.mongodb.entity.Books;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BooksRepository extends CrudRepository<Books,String>, PagingAndSortingRepository<Books,String> {
}
