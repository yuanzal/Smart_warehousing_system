package com.qst.smart_warehousing.VO;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Schema(name = "用户列表展示对象")
@Data
public class WmsSystemUserVO {
    private Long userId;
    private String username;
    private String realName;
    private String email;
    private Integer status; // 1-正常 2-冻结
    private LocalDateTime createTime;
    private List<RoleInfo> roles; // 用户拥有的角色标签组

    @Data
    public static class RoleInfo {
        private Long id;
        private String name;
    }
}