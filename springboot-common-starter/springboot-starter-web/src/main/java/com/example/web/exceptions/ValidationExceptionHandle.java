package com.example.web.exceptions;

import com.example.common.result.ResultBody;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.UnexpectedTypeException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * 参数校验通用异常处理
 *
 * @author tangsm
 */
@Slf4j
@Order(70)
@RestControllerAdvice
public class ValidationExceptionHandle {

    /**
     * BindException异常处理
     * <p>BindException: 作用于@Validated @Valid 注解，仅对于表单提交有效，对于以json格式提交将会失效</p>
     *
     * @param e BindException异常信息
     * @return 响应数据
     */
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(BindException.class)
    public ResultBody bindExceptionHandler(BindException e) {
        String msg = e.getBindingResult().getFieldErrors()
                .stream()
                .map(n -> String.format("%s: %s", n.getField(), n.getDefaultMessage()))
                .reduce((x, y) -> String.format("%s; %s", x, y))
                .orElse("参数输入有误");

        log.error("BindException异常，参数校验异常：{}", msg);
        return ResultBody.error(msg);
    }

    /**
     * MethodArgumentNotValidException-Spring封装的参数验证异常处理
     * <p>MethodArgumentNotValidException：作用于 @Validated @Valid 注解，接收参数加上@RequestBody注解（json格式）才会有这种异常。</p>
     *
     * @param e MethodArgumentNotValidException异常信息
     * @return 响应数据
     */
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(value = MethodArgumentNotValidException.class)
    public ResultBody methodArgumentNotValidExceptionHandler(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors()
                .stream()
                .map(n -> String.format("%s: %s", n.getField(), n.getDefaultMessage()))
                .reduce((x, y) -> String.format("%s; %s", x, y))
                .orElse("参数输入有误");
        log.error("MethodArgumentNotValidException异常，参数校验异常：{}", msg);
        return ResultBody.error(msg);
    }

    /**
     * ConstraintViolationException-jsr规范中的验证异常，嵌套检验问题
     * <p>ConstraintViolationException：作用于 @NotBlank @NotNull @NotEmpty 注解，校验单个String、Integer、Collection等参数异常处理。</p>
     * <p>注：Controller类上必须添加@Validated注解，否则接口单个参数校验无效</p>
     *
     * @param e ConstraintViolationException异常信息
     * @return 响应数据
     */
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(value = ConstraintViolationException.class)
    public ResultBody constraintViolationExceptionHandler(ConstraintViolationException e) {
        String msg = e.getConstraintViolations()
                .stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining("; "));

        log.error("ConstraintViolationException，参数校验异常：{}", msg);
        return ResultBody.error(msg);
    }

    /**
     * UnexpectedTypeException
     */
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(value = UnexpectedTypeException.class)
    public ResultBody unexpectedTypeExceptionnHandler(UnexpectedTypeException e) {
        String localizedMessage = e.getLocalizedMessage();
        log.error("UnexpectedTypeException，参数校验异常：{}", localizedMessage);
        return ResultBody.error(localizedMessage);
    }


}