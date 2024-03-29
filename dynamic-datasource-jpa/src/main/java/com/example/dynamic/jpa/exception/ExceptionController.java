package com.example.dynamic.jpa.exception;


import com.example.common.result.ResultBody;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.sql.SQLException;

@Slf4j
@RestControllerAdvice
public class ExceptionController {


    /**
     * 参数校验错误
     */
    @ExceptionHandler(BindException.class)
    public Object validExceptionHandler(BindException e) {
        log.error("参数类型错误：", e);
        FieldError fieldError = e.getBindingResult().getFieldError();
        String errMsg;
        if (null != fieldError) {
            errMsg = fieldError.getDefaultMessage();
        } else {
            errMsg = "请求参数有误";
        }
        return ResultBody.error(errMsg);
    }


    /**
     * 数据操作处理异常
     */
    @ExceptionHandler({DataAccessException.class, SQLException.class})
    public ResultBody handleDataAccessException(DataAccessException e) {
        log.error("数据库操作错误：", e);
        return ResultBody.error("操作异常");
    }


    /**
     * 参数校验错误
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResultBody validationBodyException(MethodArgumentNotValidException exception) {
        BindingResult result = exception.getBindingResult();
        String errMsg;
        if (result.hasErrors() && null != result.getFieldError()) {
            errMsg = result.getFieldError().getDefaultMessage();
        } else {
            errMsg = "请求参数有误";
        }
        log.error("异常：{}", errMsg);
        return ResultBody.error(errMsg);
    }


    /**
     * 捕捉baseException异常
     */
    @ExceptionHandler(BaseException.class)
    public ResultBody baseException(BaseException ex) {
        log.error("异常：", ex);
        return ResultBody.error(ex.getMessage());
    }

    /**
     * 捕捉其他所有异常
     */
    @ExceptionHandler(Exception.class)
    public ResultBody globalException(HttpServletRequest request, Exception ex) {
        log.error("异常：", ex);
        return ResultBody.error(getStatus(request).value(), ex.getMessage());
    }

    private HttpStatus getStatus(HttpServletRequest request) {
        Integer statusCode = (Integer) request.getAttribute("javax.servlet.error.status_code");
        if (statusCode == null) {
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }
        return HttpStatus.valueOf(statusCode);
    }


}

