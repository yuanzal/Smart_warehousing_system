package com.qst.smart_warehousing.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("wms_parcel")
@Schema(title = "包裹档案表")
public class WmsParcel implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "parcel_id", type = IdType.AUTO)
    @Schema(title = "包裹主键")
    private Long parcelId;

    @Schema(title = "包裹条码/快递单号")
    private String barcode;

    @Schema(title = "AI视觉测量长度(mm)")
    private BigDecimal length;

    @Schema(title = "AI视觉测量宽度(mm)")
    private BigDecimal width;

    @Schema(title = "AI视觉测量高度(mm)")
    private BigDecimal height;

    @Schema(title = "动态称重秤重量(kg)")
    private BigDecimal weight;

    @Schema(title = "计算体积(mm³)")
    private BigDecimal volume;

    @Schema(title = "AI外观检测结果: 0完好, 1破损异常")
    private Integer isDamaged;

    @Schema(title = "AI核验抓拍的缺陷/破损图片路径")
    private String damageImgUrl;

    @Schema(title = "流转状态: 1传送带进线, 2AI核验中, 3分拣流转中, 4已上架入库, 5已出库")
    private Integer status;

    @Schema(title = "当前物理所在货位ID")
    private Long currentSlotId;

    @TableField(fill = FieldFill.INSERT)
    @Schema(title = "包裹进线时间")
    private Date createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    @Schema(title = "状态更新时间")
    private Date updateTime;

    @Schema(title = "项目ID")
    private Integer projectId;

    @Schema(title = "租户ID")
    private Integer tenantId;
}