package com.qst.smart_warehousing.exception;
/*
* 设备异常
* */
public class EquipmentException  extends RuntimeException {
    private String msg;
    private int code;
    public EquipmentException (String msg, int code) {
        super(msg);
        this.msg = msg;
        this.code = code;
    }
    public EquipmentException (String message) {
        super(message);
    }

    public EquipmentException (String message, Throwable cause) {
        super(message, cause);
    }
    public EquipmentException (Throwable cause) {
        super(cause);
    }
    public EquipmentException () {
        super();
    }

    public EquipmentException(int i, String s) {
        this.msg = s;
        this.code = i;
    }
    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }
}
