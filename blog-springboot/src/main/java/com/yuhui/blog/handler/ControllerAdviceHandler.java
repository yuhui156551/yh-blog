package com.yuhui.blog.handler;

import com.yuhui.blog.exception.BizException;
import com.yuhui.blog.vo.Result;
import lombok.extern.log4j.Log4j2;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Objects;

import static com.yuhui.blog.enums.StatusCodeEnum.SYSTEM_ERROR;
import static com.yuhui.blog.enums.StatusCodeEnum.VALID_ERROR;


/**
 * 全局异常处理
 **/
@Log4j2
@RestControllerAdvice
public class ControllerAdviceHandler {

    /**
     * 处理服务异常
     */
    @ExceptionHandler(value = BizException.class)
    public Result<?> errorHandler(BizException e) {
        return Result.fail(e.getCode(), e.getMessage());
    }

    /**
     * 处理参数校验异常
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<?> errorHandler(MethodArgumentNotValidException e) {
        return Result.fail(VALID_ERROR.getCode(),
                // 获取校验异常包装的结果的 第一个 错误（如果有）
                // Objects.requireNonNull：检验对象（第一个错误对象）是否为空，为空抛空指针，不为空返回该对象
                Objects.requireNonNull(e.getBindingResult().getFieldError()).getDefaultMessage());
    }

    /**
     * 处理系统异常
     */
    @ExceptionHandler(value = Exception.class)
    public Result<?> errorHandler(Exception e) {
        e.printStackTrace();
        return Result.fail(SYSTEM_ERROR.getCode(), SYSTEM_ERROR.getDesc());
    }

}
