package com.qst.smart_warehousing.config;

import com.qst.smart_warehousing.exception.EquipmentException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;


// 全局异常处理器
@RestControllerAdvice
public class ConfigExceptionHandler {

    // 处理认证失败相关异常（如密码错误）
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentialsException(BadCredentialsException e) {
        ErrorResponse error = new ErrorResponse("401", "用户名或密码错误");
        return new ResponseEntity<>(error, HttpStatus.UNAUTHORIZED);
    }

    // 处理用户不存在异常
    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUsernameNotFoundException(UsernameNotFoundException e) {
        ErrorResponse error = new ErrorResponse("401", "用户不存在");
        return new ResponseEntity<>(error, HttpStatus.UNAUTHORIZED);
    }
    // 处理用户不存在异常
    @ExceptionHandler(EquipmentException.class)
    public ResponseEntity<ErrorResponse> handleEquipmentLocationException(EquipmentException e) {
        ErrorResponse error = new ErrorResponse("500", "设备异常，原因：" + e.getMsg());
        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    // 处理其他未捕获的认证相关异常
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationException(Exception e) {
        ErrorResponse error = new ErrorResponse("500", "系统错误：" + e.getMessage());
        return new ResponseEntity<>(error, HttpStatus.UNAUTHORIZED);
    }

    // 错误响应实体类
    public static class ErrorResponse {
        private String code;
        private String message;

        public ErrorResponse(String code, String message) {
            this.code = code;
            this.message = message;
        }

        // getter和setter
        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }
}
