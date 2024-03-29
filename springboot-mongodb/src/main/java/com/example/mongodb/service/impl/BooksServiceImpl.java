package com.example.mongodb.service.impl;

import com.example.mongodb.dao.BooksRepository;
import com.example.mongodb.entity.Books;
import com.example.mongodb.model.vo.SelectBooksParam;
import com.example.mongodb.service.BooksService;
import jakarta.annotation.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class BooksServiceImpl implements BooksService {

    @Resource
    private MongoTemplate mongoTemplate;

    @Resource
    private BooksRepository booksRepository;

    /**
     * 查找所以的book
     *
     * @param selectBooksParam
     * @return
     */
    @Override
    public List<Books> findAllBooks(SelectBooksParam selectBooksParam) {
        Iterable<Books> all = booksRepository.findAll();
        List<Books> list = new ArrayList<>();
        all.forEach(data->{list.add(data);});
        return list;
    }


    /**
     * 查找所以的book  带分页
     *
     * @param selectBooksParam 条件参数
     * @return
     */
    @Override
    public Page<Books> findAllBooksByPage(SelectBooksParam selectBooksParam) {
        Pageable pages = PageRequest.of(0,10);
        Page<Books> all = booksRepository.findAll(pages);
        return all;
    }
}
