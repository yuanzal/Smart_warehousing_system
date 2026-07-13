package com.qst.smart_warehousing.DTO;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(title = "包裹确认出库请求参数")
public class ParcelOutboundDTO {

    @NotBlank(message = "出库包裹条码不能为空")
    @Schema(title = "包裹条码", requiredMode = Schema.RequiredMode.REQUIRED, example = "SF1688992026")
    private String barcode;
}