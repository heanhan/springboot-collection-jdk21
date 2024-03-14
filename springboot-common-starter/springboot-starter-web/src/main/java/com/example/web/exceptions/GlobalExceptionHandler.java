package com.example.web.exceptions;

import com.example.common.enums.CommonEnum;
import com.example.common.exceptins.BizException;
import com.example.common.result.ResultBody;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.NotReadablePropertyException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.ui.Model;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;


/**
 * @Classname GlobalExceptionHandler
 * @Description 全局异常的类
 * @Date 2024/3/06 18:00 上午
 * @Created by zhaojh0912
 */

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    /**
     * 服务名
     */
    @Value("${spring.application.name}")
    private String serverName;

    /**
     * 微服务系统标识
     */
    private String errorSystem;

    @PostConstruct
    public void init() {
        this.errorSystem = new StringBuffer()
                .append(this.serverName)
                .append(": ").toString();
    }

    /**
     * 应用到所有@RequestMapping注解方法，在其执行之前初始化数据绑定器
     */
    @InitBinder
    public void initBinder(WebDataBinder binder) {

    }

    /**
     * 把值绑定到Model中，使全局@RequestMapping可以获取到该值
     */
    @ModelAttribute
    public void addAttributes(Model model) {

    }

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

        return ResultBody.error(e.getErrorCode(), "报错的服务: "+errorSystem +",错误详情: "+ errorSystem + e.toString());
    }

    /**
     * 非法请求异常-参数类型不匹配
     */
    @ExceptionHandler(value = MethodArgumentTypeMismatchException.class)
    public ResultBody handlerSpringAOPException(MethodArgumentTypeMismatchException exception) {
        return ResultBody.error(CommonEnum.PARAM_TYPE_MISMATCH.getResultCode(), "报错的服务: "+errorSystem +",错误详情: "+ exception.getMessage());
    }


    @ExceptionHandler({NotReadablePropertyException.class})
    public ResultBody handleNotReadablePropertyException(NotReadablePropertyException ex, HttpServletResponse response) {
        return ResultBody.error(CommonEnum.PARAM_ERROR.getResultCode(),"报错的服务: "+errorSystem +",错误详情: "+ ex.toString());
    }

    @ExceptionHandler({NumberFormatException.class})
    public ResultBody handleNumberFormatException(NumberFormatException ex, HttpServletResponse response) {
        return ResultBody.error(CommonEnum.PARAM_TYPE_MISMATCH.getResultCode(), "报错的服务: "+errorSystem +",错误详情: "+ ex.toString());
    }

    /**
     * Assert异常
     */
    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public ResultBody exception(IllegalArgumentException e,HttpServletRequest request, HttpServletResponse response) {
        log.error("request error!! method:{} uri:{}", request.getMethod(), request.getRequestURI());
        return ResultBody.error(CommonEnum.PARAM_TYPE_MISMATCH.getResultCode(), "报错的服务: "+errorSystem +",错误详情: "+ e.toString());
    }


    /**
     * 算术异常
     */
    @ExceptionHandler({ArithmeticException.class})
    public ResultBody exception(ArithmeticException e,HttpServletRequest request, HttpServletResponse response) {
        log.error("request error!! method:{} uri:{}", request.getMethod(), request.getRequestURI());
        return ResultBody.error(CommonEnum.PARAM_TYPE_MISMATCH.getResultCode(), "报错的服务: "+errorSystem +",错误详情: "+ e.toString());
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
        return ResultBody.error(CommonEnum.ERROR.getResultCode(),"报错的服务: "+errorSystem +",错误详情: "+ e.toString());
    }
}
