package com.qst.smart_warehousing.exception; // 💡 换成你项目的包名

/**
 * 自定义业务异常（ServiceException）
 * 继承 RuntimeException 使得事务（@Transactional）在抛出此异常时能自动回滚
 */
public class ServiceException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * 错误码（默认500，也可以根据需要支持不同的业务错误码）
     */
    private Integer code = 500;

    /**
     * 无参构造
     */
    public ServiceException() {
        super();
    }

    /**
     * 最常用的构造方法：只传错误描述
     */
    public ServiceException(String message) {
        super(message);
    }

    /**
     * 支持自定义错误码的构造方法
     */
    public ServiceException(Integer code, String message) {
        super(message);
        this.code = code;
    }

    /**
     * 支持传递异常链的构造方法
     */
    public ServiceException(String message, Throwable cause) {
        super(message, cause);
    }

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }
}