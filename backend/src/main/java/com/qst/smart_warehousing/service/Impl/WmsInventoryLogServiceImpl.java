package com.qst.smart_warehousing.service.Impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qst.smart_warehousing.entity.WmsInventoryLog;
import com.qst.smart_warehousing.mapper.WmsInventoryLogMapper;
import com.qst.smart_warehousing.service.WmsInventoryLogService;
import org.springframework.stereotype.Service;

@Service
public class WmsInventoryLogServiceImpl extends ServiceImpl<WmsInventoryLogMapper, WmsInventoryLog> implements WmsInventoryLogService {
}
