package com.meiye.common.exception;

import com.meiye.common.web.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BizException.class)
    public R<Void> handleBiz(BizException e) {
        return R.fail(e.getCode(), e.getMessage());
    }

    @ExceptionHandler({AccessDeniedException.class})
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public R<Void> handleDenied(AccessDeniedException e) {
        return R.fail(403, "无权访问该资源");
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public R<Void> handleValid(Exception e) {
        FieldError fe = null;
        if (e instanceof MethodArgumentNotValidException m) {
            fe = m.getBindingResult().getFieldError();
        } else if (e instanceof BindException b) {
            fe = b.getBindingResult().getFieldError();
        }
        String msg = fe == null ? "参数校验失败" : fe.getField() + " " + fe.getDefaultMessage();
        return R.fail(400, msg);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public R<Void> handleNotFound(NoResourceFoundException e) {
        return R.fail(404, "接口不存在");
    }

    @ExceptionHandler(Exception.class)
    public R<Void> handle(Exception e) {
        log.error("系统异常", e);
        return R.fail(500, "系统繁忙：" + e.getMessage());
    }
}
