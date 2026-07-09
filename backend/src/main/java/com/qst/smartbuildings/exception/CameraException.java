package com.qst.smartbuildings.exception;

public class CameraException  extends RuntimeException  {
    private String msg;
    private int code;
    public CameraException (String msg, int code) {
        super(msg);
        this.msg = msg;
        this.code = code;
    }
    public CameraException (String message) {
        super(message);
    }

    public CameraException (String message, Throwable cause) {
        super(message, cause);
    }
    public CameraException (Throwable cause) {
        super(cause);
    }
    public CameraException () {
        super();
    }

    public CameraException(int i, String s) {
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
