package com.qst.smart_warehousing.exception;

import com.qst.smart_warehousing.entity.ResultCode;
import org.springframework.security.access.AccessDeniedException;

public class AuthException extends AccessDeniedException {
    private ResultCode resultCode;

    public AuthException(ResultCode resultCode) {
        super(null);
        this.resultCode = resultCode;
    }

    public AuthException(String msg, Throwable t) {
        super(msg, t);
    }

    public AuthException(String msg) {
        super(msg);
    }

    public ResultCode getResultCode() {
        return this.resultCode;
    }
}