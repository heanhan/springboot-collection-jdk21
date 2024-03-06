package org.example.global.exceptins;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.example.global.enums.CommonEnum;
import org.example.global.result.ResultBody;
import org.springframework.beans.NotReadablePropertyException;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @Classname GlobalExceptionHandler
 * @Description 全局异常的类
 * @Date 2022/7/28 10:54 上午
 * @Created by zhaojh0912
 */

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理自定义的业务异常
     *
     * @param req
     * @param e
     * @return
     */
    @ExceptionHandler(value = BizException.class)
    public ResultBody bizExceptionHandler(HttpServletRequest req, BizException e, HttpServletResponse response) {
        log.error("发生业务异常！原因是：{}", e.getErrorMsg());

        return ResultBody.error(e.getErrorCode(), e.getErrorMsg());
    }

    @ExceptionHandler({MethodArgumentNotValidException.class})
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public ResultBody handleMethodArgumentNotValidException(MethodArgumentNotValidException ex, HttpServletResponse response) {
        BindingResult bindingResult = ex.getBindingResult();
        StringBuilder sb = new StringBuilder("校验失败:");
        for (FieldError fieldError : bindingResult.getFieldErrors()) {
            sb.append(fieldError.getField()).append("：").append(fieldError.getDefaultMessage()).append(", ");
        }
        String msg = sb.toString();
        return ResultBody.error(CommonEnum.PARAMTER_ERROR.getResultCode(), msg);
    }

    @ExceptionHandler({ConstraintViolationException.class})
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public ResultBody handleConstraintViolationException(ConstraintViolationException ex, HttpServletResponse response) {
        //将body返回值清空，避免双重回复
        return ResultBody.error(CommonEnum.PARAMTER_ERROR.getResultCode(), ex.getMessage());
    }

    @ExceptionHandler({NotReadablePropertyException.class})
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public ResultBody handleNotReadablePropertyException(NotReadablePropertyException ex, HttpServletResponse response) {
        return ResultBody.error(CommonEnum.PARAMTER_ERROR.getResultCode(), ex.getMessage());
    }

    @ExceptionHandler({NumberFormatException.class})
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public ResultBody handleNumberFormatException(NumberFormatException ex, HttpServletResponse response) {
        return ResultBody.error(CommonEnum.NUMBER_FORMAT_EXCEPTION.getResultCode(), ex.getMessage());
    }

    /**
     * Assert异常
     */
    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResultBody exception(IllegalArgumentException e,HttpServletRequest request, HttpServletResponse response) {
        log.error("request error!! method:{} uri:{}", request.getMethod(), request.getRequestURI());
        String message1 = e.getMessage();
        String message2 = getExceptionDetail(e);
        return ResultBody.error(CommonEnum.NUMBER_FORMAT_EXCEPTION.getResultCode(), message1);
    }



    /**
     * 获取代码报错详细位置信息
     */
    public String getExceptionDetail(Exception e) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(e.getClass()).append(System.getProperty("line.separator"));
        stringBuilder.append(e.getLocalizedMessage()).append(System.getProperty("line.separator"));
        StackTraceElement[] arr = e.getStackTrace();
        for (StackTraceElement stackTraceElement : arr) {
            stringBuilder.append(stackTraceElement.toString()).append(System.getProperty("line.separator"));
        }
        return stringBuilder.toString();
    }
    /**
     * 处理其他异常
     *
     * @param e
     * @return
     */
    @ExceptionHandler(value = Exception.class)
    public ResultBody exceptionHandler(HttpServletRequest request, Exception e, HttpServletResponse response) {
        log.error("未知异常！原因是:", e);
        //将body返回值清空，避免双重回复
        try {
            response.resetBuffer();
        }catch (Exception x){
            log.error("异常处理body清空错误",x);
        }
        return ResultBody.error(String.valueOf(CommonEnum.INTERNAL_SERVER_ERROR));
    }
}
