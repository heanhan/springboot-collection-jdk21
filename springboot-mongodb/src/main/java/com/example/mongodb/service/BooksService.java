package com.example.mongodb.service;


import com.example.mongodb.entity.Books;
import com.example.mongodb.model.vo.SelectBooksParam;
import org.springframework.data.domain.Page;

import java.util.List;

public interface BooksService {

    /**
     * 查找所以的book
     * @return
     */
    List<Books> findAllBooks(SelectBooksParam selectBooksParam);

    /**
     * 查找所以的book  带分页
     * @param selectBooksParam 条件参数
     * @return
     */
    Page<Books> findAllBooksByPage(SelectBooksParam selectBooksParam);
}
