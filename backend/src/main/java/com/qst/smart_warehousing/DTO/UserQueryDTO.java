package com.qst.smart_warehousing.DTO;

import com.qst.smart_warehousing.entity.PageEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "用户管理多维分页检索传输对象（复用通用分页实体）")
public class UserQueryDTO extends PageEntity {

    @Schema(description = "检索关键字（用户名或真实姓名）")
    private String username;

    @Schema(description = "根据角色过滤的用户列表")
    private Long roleId;

    @Schema(description = "根据状态过滤的用户（1-正常，0-禁用）")
    private Integer status;

    @Schema(description = "根据手机号过滤")
    private String mobile;

    @Schema(description = "根据租户id过滤")
    private Integer tenantId;

    /* * 💡 核心适配：由于前端 js 传入的是 pageNum 和 pageSize，
     * 我们提供以下 Setter，让 Spring MVC 自动将其映射到父类 PageEntity 的 page 和 limit 中！
     */
    public void setPageNum(Integer pageNum) {
        if (pageNum != null) {
            this.setPage(pageNum);
        }
    }

    public void setPageSize(Integer pageSize) {
        if (pageSize != null) {
            this.setLimit(pageSize);
        }
    }
}