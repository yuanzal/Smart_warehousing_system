package com.qst.smartbuildings.common;

public class RedisConst {
    //    通过Redis根据设备资产查询客户端id
    public static final String ASSETS_NAME_HEAD = "assets_client_";
    //    通过Redis根据客户端id查询设备资产
    public static final String CLIENT_NAME_HEAD = "client_assets_";
    //    设备发送的property缓存前缀
    public static final String PROPERTY_NAME_HEAD = "property_";
    // 监测点缓存前缀
    public static final String MONITOR_POINT_NAME_HEAD = "monitor_point_";
    // AI告警缓存前缀,监测总目标数
    public static final String AI_ALERT_TOTAL_HEAD = "ai_alert_total_";
    //AI告警缓存前缀,监测有效目标数
    public static final String AI_ALERT_VALID_HEAD = "ai_alert_valid_";
    //塔机力矩预警前缀
    public static final String TOWER_MOMENT_HEAD = "tower_moment_";
    //塔机大风预警前缀
    public static final String TOWER_WIND_HEAD = "tower_wind_";
    //塔机报警前缀
    public static final String TOWER_ALARM_HEAD = "tower_alarm_";
    //升降机倾角预警前缀
    public static final String ELEVATOR_ANGLE_HEAD = "elevator_angle_";
    //升降机超重预警前缀
    public static final String ELEVATOR_OVER_WEIGHT_HEAD = "elevator_over_weight_";
    //升降机超速预警前缀
    public static final String ELEVATOR_OVER_SPEED_HEAD = "elevator_over_speed_";

}
