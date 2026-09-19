package com.example.ddd.inventory.infrastructure.config;

import com.example.ddd.common.exception.BusinessException;
import com.example.ddd.common.exception.ErrorCode;
import com.example.ddd.common.result.Result;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * inventory-service 全局异常处理器。
 *
 * <p>把领域/应用层抛出的 {@link BusinessException} 统一转换为 {@link Result}，
 * 并对乐观锁冲突单独兜底（正常应被应用层重试消化，此处为最后防线）。</p>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Result<Void>> handleBusiness(BusinessException ex) {
        log.warn("[Business] code={} message={}", ex.getCode(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Result.fail(ex.getCode(), ex.getMessage()));
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<Result<Void>> handleOptimisticLock(ObjectOptimisticLockingFailureException ex) {
        log.warn("[OptimisticLock] 库存并发冲突且重试耗尽: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Result.fail(ErrorCode.INVENTORY_LOCK_FAILED));
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public ResponseEntity<Result<Void>> handleValidation(BindException ex) {
        String msg = ex.getBindingResult().getFieldErrors().stream()
                .map(this::formatFieldError).collect(Collectors.joining("; "));
        return ResponseEntity.badRequest().body(Result.fail(ErrorCode.BAD_REQUEST, msg));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Result<Void>> handleConstraint(ConstraintViolationException ex) {
        String msg = ex.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage).collect(Collectors.joining("; "));
        return ResponseEntity.badRequest().body(Result.fail(ErrorCode.BAD_REQUEST, msg));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Result<Void>> handleIllegalArg(IllegalArgumentException ex) {
        log.warn("[IllegalArg] {}", ex.getMessage());
        return ResponseEntity.badRequest().body(Result.fail(ErrorCode.BAD_REQUEST, ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> handleOther(Exception ex) {
        log.error("[System] unexpected error", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Result.fail(ErrorCode.SYSTEM_ERROR));
    }

    private String formatFieldError(FieldError fe) {
        return fe.getField() + ": " + fe.getDefaultMessage();
    }
}
