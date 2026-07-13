package com.qst.smart_warehousing.entity;

public interface ResultCode {

    /**
     * 系统响应码
     *
     * @return code
     */
    public int getCode();

    /**
     * 默认系统响应提示，code=0时此处为空
     *
     * @return msg
     */
    public String getMsg();
}
