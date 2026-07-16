package com.qst.smart_warehousing.DTO;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Schema(name = "角色分配DTO")
@Data
public class UserAssignRoleDTO {
    private Long userId;
    private List<Long> roleIds;
}