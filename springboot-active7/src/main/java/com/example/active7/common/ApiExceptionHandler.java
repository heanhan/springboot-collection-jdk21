package com.example.active7.common;

import com.example.common.exceptins.BizException;
import com.example.common.result.ResultBody;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * 把参数错误、业务错误和权限错误转换成前端容易识别的响应。
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(BizException.class)
    public ResponseEntity<ResultBody<Void>> business(BizException exception) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        if (Integer.valueOf(401).equals(exception.getErrorCode())) {
            status = HttpStatus.UNAUTHORIZED;
        } else if (Integer.valueOf(403).equals(exception.getErrorCode())) {
            status = HttpStatus.FORBIDDEN;
        } else if (Integer.valueOf(404).equals(exception.getErrorCode())) {
            status = HttpStatus.NOT_FOUND;
        } else if (Integer.valueOf(409).equals(exception.getErrorCode())) {
            status = HttpStatus.CONFLICT;
        }
        return ResponseEntity.status(status).body(ResultBody.error(exception.getErrorMsg()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ResultBody<Void>> validation(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("；"));
        return ResponseEntity.badRequest().body(ResultBody.error(400, message));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ResultBody<Void>> unauthenticated(AuthenticationException exception) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ResultBody.error(401, "账号或密码错误"));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ResultBody<Void>> denied() {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ResultBody.error(403, "没有操作权限"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ResultBody<Void>> unexpected(Exception exception) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ResultBody.error(500, "服务异常：" + exception.getMessage()));
    }
}
