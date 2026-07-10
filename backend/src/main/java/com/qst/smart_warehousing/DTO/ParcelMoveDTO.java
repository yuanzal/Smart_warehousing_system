package com.qst.smart_warehousing.DTO;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(title = "包裹库内移位请求参数")
public class ParcelMoveDTO {

    @NotBlank(message = "包裹条码不能为空")
    @Schema(title = "包裹条码", requiredMode = Schema.RequiredMode.REQUIRED)
    private String barcode;

    @NotNull(message = "目的新货位ID不能为空")
    @Schema(title = "目的新货位主键ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long destSlotId;
}