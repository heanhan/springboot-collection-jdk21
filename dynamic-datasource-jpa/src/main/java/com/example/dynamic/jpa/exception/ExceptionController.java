package com.example.dynamic.jpa.exception;

import com.example.common.result.ResultBody;
import com.example.dynamic.jpa.security.token.RefreshTokenReplayException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
public class ExceptionController {

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ResultBody<Void>> authenticationException(HttpServletRequest request, AuthenticationException ex) {
        log.warn("认证失败 method={} path={} type={}", request.getMethod(), request.getRequestURI(), ex.getClass().getSimpleName());
        return response(HttpStatus.UNAUTHORIZED, ExceptionCode.TOKEN_ERROR.getCode(), "认证失败或登录已过期，请重新登录");
    }

    @ExceptionHandler(RefreshTokenReplayException.class)
    public ResponseEntity<ResultBody<Void>> replayException(HttpServletRequest request, RefreshTokenReplayException ex) {
        log.warn("检测到刷新令牌重放 path={}", request.getRequestURI());
        return response(HttpStatus.UNAUTHORIZED, ExceptionCode.TOKEN_ERROR.getCode(), "登录会话已失效，请重新登录");
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ResultBody<Void>> accessDeniedException(HttpServletRequest request, AccessDeniedException ex) {
        return response(HttpStatus.FORBIDDEN, ExceptionCode.UNAUTHORIZED.getCode(), "权限不足");
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<ResultBody<Void>> bindException(BindException ex) {
        return badRequest(validationMessage(ex));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ResultBody<Void>> methodArgumentNotValidException(MethodArgumentNotValidException ex) {
        return badRequest(validationMessage(ex));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ResultBody<Void>> constraintViolationException(ConstraintViolationException ex) {
        String message = ex.getConstraintViolations().stream()
                .map(violation -> violation.getMessage())
                .findFirst().orElse("请求参数有误");
        return badRequest(message);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class})
    public ResponseEntity<ResultBody<Void>> malformedRequest(Exception ex) {
        return badRequest("请求参数格式错误");
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ResultBody<Void>> noResourceFound(NoResourceFoundException ex) {
        return response(HttpStatus.NOT_FOUND, HttpStatus.NOT_FOUND.value(), "请求的资源不存在");
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ResultBody<Void>> noHandlerFound(NoHandlerFoundException ex) {
        return response(HttpStatus.NOT_FOUND, HttpStatus.NOT_FOUND.value(), "请求的资源不存在");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ResultBody<Void>> methodNotSupported(HttpRequestMethodNotSupportedException ex) {
        return response(HttpStatus.METHOD_NOT_ALLOWED, HttpStatus.METHOD_NOT_ALLOWED.value(), "请求方法不支持");
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ResultBody<Void>> uploadTooLarge(MaxUploadSizeExceededException ex) {
        return response(HttpStatus.PAYLOAD_TOO_LARGE, HttpStatus.PAYLOAD_TOO_LARGE.value(), "请求内容过大");
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ResultBody<Void>> mediaTypeNotSupported(HttpMediaTypeNotSupportedException ex) {
        return response(HttpStatus.UNSUPPORTED_MEDIA_TYPE, HttpStatus.UNSUPPORTED_MEDIA_TYPE.value(), "请求内容类型不支持");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ResultBody<Void>> dataIntegrityViolation(HttpServletRequest request, DataIntegrityViolationException ex) {
        log.error("数据冲突 method={} path={} type={}", request.getMethod(), request.getRequestURI(), ex.getClass().getSimpleName());
        return response(HttpStatus.CONFLICT, HttpStatus.CONFLICT.value(), "数据已存在或与当前状态冲突");
    }

    @ExceptionHandler(BaseException.class)
    public ResponseEntity<ResultBody<Void>> baseException(BaseException ex) {
        HttpStatus status = switch (ex.getCode()) {
            case -6 -> HttpStatus.BAD_REQUEST;
            case -403 -> HttpStatus.FORBIDDEN;
            case -5, -9001, -9002 -> HttpStatus.UNAUTHORIZED;
            default -> HttpStatus.CONFLICT;
        };
        String message = ex.getMessage() == null || ex.getMessage().isBlank() ? "操作失败" : ex.getMessage();
        return response(status, ex.getCode(), message);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ResultBody<Void>> illegalArgument(IllegalArgumentException ex) {
        String message = ex.getMessage() == null || ex.getMessage().isBlank() ? "请求参数有误" : ex.getMessage();
        return badRequest(message);
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ResultBody<Void>> dataAccessException(HttpServletRequest request, DataAccessException ex) {
        log.error("数据库操作失败 method={} path={} type={}", request.getMethod(), request.getRequestURI(), ex.getClass().getSimpleName());
        return response(HttpStatus.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR.value(), "系统繁忙，请稍后重试");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ResultBody<Void>> globalException(HttpServletRequest request, Exception ex) {
        log.error("请求处理失败 method={} path={} type={}", request.getMethod(), request.getRequestURI(), ex.getClass().getName());
        HttpStatus status = resolveStatus(request);
        String message = switch (status) {
            case BAD_REQUEST -> "请求参数有误";
            case UNAUTHORIZED -> "认证失败或登录已过期，请重新登录";
            case FORBIDDEN -> "权限不足";
            case NOT_FOUND -> "请求的资源不存在";
            case METHOD_NOT_ALLOWED -> "请求方法不支持";
            case PAYLOAD_TOO_LARGE -> "请求内容过大";
            case UNSUPPORTED_MEDIA_TYPE -> "请求内容类型不支持";
            default -> "系统繁忙，请稍后重试";
        };
        return response(status, status.value(), message);
    }

    private ResponseEntity<ResultBody<Void>> badRequest(String message) {
        return response(HttpStatus.BAD_REQUEST, ExceptionCode.ARGUMENT.getCode(), message);
    }

    private ResponseEntity<ResultBody<Void>> response(HttpStatus status, int code, String message) {
        return ResponseEntity.status(status).body(ResultBody.error(code, message));
    }

    private String validationMessage(BindException ex) {
        return ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getDefaultMessage() == null ? "请求参数有误" : error.getDefaultMessage())
                .findFirst().orElse("请求参数有误");
    }

    private HttpStatus resolveStatus(HttpServletRequest request) {
        Object statusCode = request.getAttribute("jakarta.servlet.error.status_code");
        if (!(statusCode instanceof Integer code)) {
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }
        try {
            return HttpStatus.valueOf(code);
        } catch (IllegalArgumentException ex) {
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }
    }
}
