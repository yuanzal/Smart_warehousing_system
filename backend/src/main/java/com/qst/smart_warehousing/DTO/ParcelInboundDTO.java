package com.qst.smart_warehousing.DTO;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(title = "包裹确认入库请求参数")
public class ParcelInboundDTO {

    @NotBlank(message = "包裹条码/单号不能为空")
    @Schema(title = "包裹条码", requiredMode = Schema.RequiredMode.REQUIRED, example = "SF1688992026")
    private String barcode;

    @NotNull(message = "目标货位ID不能为空")
    @Schema(title = "目标货位主键ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "15")
    private Long targetSlotId;
}