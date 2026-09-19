package com.example.ddd.common.infrastructure.web;

import com.example.ddd.common.exception.BusinessException;
import com.example.ddd.common.exception.ErrorCode;
import com.example.ddd.common.result.Result;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** 接口基础设施：新业务服务统一异常响应，不向调用方暴露堆栈和数据库信息。 */
@RestControllerAdvice
@ConditionalOnProperty(name = "ddd.web.errors", havingValue = "true")
public class RestErrors {
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Result<Void>> business(BusinessException e) {
        return ResponseEntity.badRequest().body(Result.fail(e.getCode(), e.getMessage()));
    }
    @ExceptionHandler({IllegalArgumentException.class, MethodArgumentNotValidException.class,
            org.springframework.http.converter.HttpMessageNotReadableException.class})
    public ResponseEntity<Result<Void>> invalid(Exception e) {
        return ResponseEntity.badRequest().body(Result.fail(ErrorCode.BAD_REQUEST, "请求参数不合法"));
    }
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> unexpected(Exception e) {
        org.slf4j.LoggerFactory.getLogger(getClass()).error("请求执行失败", e);
        return ResponseEntity.internalServerError().body(Result.fail(ErrorCode.SYSTEM_ERROR));
    }
}
