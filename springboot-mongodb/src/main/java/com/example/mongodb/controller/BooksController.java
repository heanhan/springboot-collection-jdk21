package com.example.mongodb.controller;


import com.example.common.enums.CommonEnum;
import com.example.common.result.ResultBody;
import com.example.mongodb.entity.Books;
import com.example.mongodb.model.vo.SelectBooksParam;
import com.example.mongodb.service.BooksService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RequestMapping(value = "/books")
@RestController
@Slf4j
public class BooksController {

    @Resource
    private BooksService booksService;

    @PostMapping(value = "/findAllBooks")
    public ResultBody findAllBooks(@RequestBody SelectBooksParam selectBooksParam) {
        try{
            List<Books> allBooks = booksService.findAllBooks(selectBooksParam);
            return ResultBody.success(allBooks);
        }catch (Exception ignored){
            ResultBody.error(CommonEnum.ERROR);
        }

        return ResultBody.error(CommonEnum.ERROR);
    }


    @PostMapping(value = "/findAllBooksAndPage")
    public ResultBody findAllBooksAndPage(@RequestBody SelectBooksParam selectBooksParam) {
        try{
            Page<Books> allBooksByPage = booksService.findAllBooksByPage(selectBooksParam);
            return ResultBody.success(allBooksByPage);
        }catch (Exception ignored){
            ResultBody.error(CommonEnum.ERROR);
        }

        return ResultBody.error(CommonEnum.ERROR);
    }
}
