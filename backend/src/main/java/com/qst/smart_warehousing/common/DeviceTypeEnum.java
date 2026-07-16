package com.qst.smart_warehousing.common;

/**
 * 设备类型枚举
 */
public enum DeviceTypeEnum {
    CAMERA(1, "摄像头"),
    AGV(2, "AGV小车"),
    SENSOR(3, "环境传感器"),
    GATEWAY(4, "边缘网关"),
    PLC(5, "PLC控制器");

    private final int code;
    private final String desc;

    DeviceTypeEnum(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public int getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    public static DeviceTypeEnum fromCode(int code) {
        for (DeviceTypeEnum type : values()) {
            if (type.code == code) {
                return type;
            }
        }
        return null;
    }
}