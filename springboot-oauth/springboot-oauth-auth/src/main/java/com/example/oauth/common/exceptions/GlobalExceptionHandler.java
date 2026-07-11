package com.example.oauth.common.exceptions;

import com.example.oauth.common.enums.CommonEnum;
import com.example.oauth.common.exceptions.BizException;
import com.example.oauth.common.result.ResultBody;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.NotReadablePropertyException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import java.util.stream.Collectors;

/**
 * 全局异常处理器（javax 版本，适配 Spring Boot 2.7.18）
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @Value("${spring.application.name:oauth-auth-service}")
    private String serverName;

    private String errorSystem;

    @PostConstruct
    public void init() {
        this.errorSystem = serverName + ": ";
    }

    /**
     * 处理自定义业务异常
     */
    @ExceptionHandler(BizException.class)
    public ResultBody<?> bizExceptionHandler(HttpServletRequest req, BizException e, HttpServletResponse response) {
        log.error("发生业务异常！原因是：{}", e.getErrorMsg());
        return ResultBody.error(e.getErrorCode(), errorSystem + e.getErrorMsg());
    }

    /**
     * 参数类型不匹配
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResultBody<?> handlerTypeMismatch(MethodArgumentTypeMismatchException e) {
        log.error("参数类型不匹配：{}", e.getMessage());
        return ResultBody.error(CommonEnum.PARAM_TYPE_MISMATCH.getResultCode(), errorSystem + e.getMessage());
    }

    @ExceptionHandler(NotReadablePropertyException.class)
    public ResultBody<?> handleNotReadableProperty(NotReadablePropertyException e) {
        log.error("属性不可读：{}", e.getMessage());
        return ResultBody.error(CommonEnum.PARAM_ERROR.getResultCode(), errorSystem + e.getMessage());
    }

    @ExceptionHandler(NumberFormatException.class)
    public ResultBody<?> handleNumberFormat(NumberFormatException e) {
        log.error("数字格式异常：{}", e.getMessage());
        return ResultBody.error(CommonEnum.PARAM_TYPE_MISMATCH.getResultCode(), errorSystem + e.getMessage());
    }

    /**
     * @Validated 参数校验异常 (JSON body)
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResultBody<?> methodArgumentNotValidHandler(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(n -> String.format("%s: %s", n.getField(), n.getDefaultMessage()))
                .reduce((x, y) -> String.format("%s; %s", x, y))
                .orElse("参数输入有误");
        log.error("参数校验异常：{}", msg);
        return ResultBody.error(msg);
    }

    /**
     * @Validated 参数校验异常 (单个参数)
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResultBody<?> constraintViolationHandler(ConstraintViolationException e) {
        String msg = e.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining("; "));
        log.error("参数校验异常：{}", msg);
        return ResultBody.error(msg);
    }

    /**
     * 非法参数/状态异常
     */
    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public ResultBody<?> illegalArgument(IllegalArgumentException e, HttpServletRequest request) {
        log.error("request method:{} uri:{}", request.getMethod(), request.getRequestURI());
        log.error("非法参数异常：{}", e.getMessage());
        return ResultBody.error(CommonEnum.PARAM_TYPE_MISMATCH.getResultCode(), errorSystem + e.getMessage());
    }

    /**
     * 算术异常
     */
    @ExceptionHandler(ArithmeticException.class)
    public ResultBody<?> arithmetic(ArithmeticException e, HttpServletRequest request) {
        log.error("request error!! method:{} uri:{}", request.getMethod(), request.getRequestURI());
        log.error("算术异常：{}", e.getMessage());
        return ResultBody.error(CommonEnum.PARAM_TYPE_MISMATCH.getResultCode(), errorSystem + e.getMessage());
    }

    /**
     * 兜底异常处理
     */
    @ExceptionHandler(Exception.class)
    public ResultBody<?> exceptionHandler(HttpServletRequest request, Exception e, HttpServletResponse response) {
        log.error("未知异常！原因是:", e);
        return ResultBody.error(CommonEnum.ERROR.getResultCode(), errorSystem + e.getMessage());
    }
}
