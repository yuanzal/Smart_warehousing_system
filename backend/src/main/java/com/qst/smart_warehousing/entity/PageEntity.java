package com.qst.smart_warehousing.entity;

import com.baomidou.mybatisplus.core.metadata.OrderItem;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Data
@Schema(description = "通用分页参数实体，包含分页控制、排序规则配置")
public class PageEntity {

    @Schema(
            description = "当前页数，默认值：1",
            example = "1",
            defaultValue = "1"
    )
    private Integer page = 1;

    @Schema(
            description = "每页展示条数，默认值：15；分页时最大限制100条",
            example = "15",
            defaultValue = "15"
    )
    private Integer limit = 15;

    @Schema(
            description = "是否分页：0=不分页（查询所有，默认返回10000条），1=分页",
            example = "1",
            defaultValue = "1",
            allowableValues = {"0", "1"}
    )
    private Integer pageType = 1;

    @Schema(
            description = "排序字段名（与sortingType配合使用，优先级低于orders字段）",
            example = "create_time"
    )
    private String fieldName;

    @Schema(
            description = "排序方式（与fieldName配合使用）：ASC=升序，DESC=降序",
            example = "DESC",
            allowableValues = {"ASC", "DESC"}
    )
    private String sortingType;

    @Schema(
            description = "排序字段列表（MyBatis-Plus排序对象，优先级高于fieldName/sortingType）",
            example = "[{\"column\":\"create_time\",\"asc\":false}, {\"column\":\"id\",\"asc\":true}]"
    )
    private List<OrderItem> orders = new ArrayList<>();

    /**
     * 将分页参数转换为BasePage分页对象
     * @param <T> 分页数据类型
     * @return 包含分页+排序规则的BasePage对象
     */
    public <T> BPage<T> parse() {
        // 初始化BasePage（当前页、每页条数）
        BPage<T> page = new BPage<>(getPage(), getLimit());

        // 不分页时，设置超大每页条数（默认10000条）
        if (Objects.equals(0, pageType)) {
            page.setSize(10000);
        }

        // 构建排序条件：先添加fieldName/sortingType的排序，再添加orders的排序
        List<OrderItem> orders2 = new ArrayList<>();
        if (fieldName != null && !"".equals(fieldName)) {
            OrderItem orderItem = new OrderItem();
            orderItem.setColumn(fieldName);
            // 排序方式：默认升序（未传sortingType或非DESC时）
            if (sortingType != null && "DESC".equals(sortingType)) {
                orderItem.setAsc(false);
            } else {
                orderItem.setAsc(true);
            }
            orders2.add(orderItem);
        }
        // 追加orders字段中的排序条件
        orders2.addAll(orders);

        return page;
    }



    /**
     * 获取每页条数：分页时最大限制100条，避免查询数据量过大
     * @return 最终每页条数
     */
    public Integer getLimit() {
        // 分页状态下，限制最大每页条数为100
        if (limit > 100 && Objects.equals(1, pageType)) {
            return 100;
        }
        return limit;
    }

    /**
     * 手动添加排序条件（便捷方法）
     * @param column 排序字段名（数据库列名）
     * @param asc 是否升序：true=升序，false=降序
     */
    public void setOrdersBd(String column, boolean asc) {
        OrderItem orderItem = new OrderItem();
        orderItem.setColumn(column);
        orderItem.setAsc(asc);
        this.orders.add(orderItem);
    }

}