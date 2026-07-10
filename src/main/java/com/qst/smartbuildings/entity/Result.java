package com.qst.smartbuildings.entity;

import com.alibaba.fastjson.JSON;
import com.qst.smartbuildings.common.SystemCodeEnum;
import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.util.Objects;

public class Result<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description  = "code", required = true, example = "0")
    private Integer code;

    @Schema (description  = "msg", required = true, example = "success")
    private String msg;

    private T data;

    Result() {

    }


    private Result(ResultCode resultCode) {
        this.code = resultCode.getCode();
        this.msg = resultCode.getMsg();
    }

    private Result(ResultCode resultCode, String msg) {
        this.code = resultCode.getCode();
        this.msg = msg;
    }

    private Result(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }


    public Integer getCode() {
        return code;
    }

    public String getMsg() {
        return msg;
    }

    public static Result noAuth() {
        return error(SystemCodeEnum.SYSTEM_NO_AUTH);
    }

    public static <T> Result<T> error(ResultCode resultCode) {
        return new Result<>(resultCode);
    }

    public static Result error(int code,String msg) {
        return new Result(code,msg);
    }

    public static Result error(ResultCode resultCode,String msg) {
        return new Result(resultCode,msg);
    }

    public static <T> Result<T> ok(T data) {
        Result<T> result = new Result<>(SystemCodeEnum.SYSTEM_OK);
        result.setData(data);
        return result;
    }

    public static <T> Result ok() {
        return new Result(SystemCodeEnum.SYSTEM_OK);
    }


    public Result setData(T data) {
        this.data = data;
        return this;
    }

    @SuppressWarnings("unchecked")
    public T getData() {
        return this.data;
    }

    public boolean hasSuccess() {
        return Objects.equals(SystemCodeEnum.SYSTEM_OK.getCode(), code);
    }

    public String toJSONString() {
        return JSON.toJSONString(this);
    }

    @Override
    public String toString() {
        return JSON.toJSONString(this);
    }
}
