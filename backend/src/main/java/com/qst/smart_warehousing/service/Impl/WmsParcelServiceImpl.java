package com.qst.smart_warehousing.service.Impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qst.smart_warehousing.entity.WmsParcel;
import com.qst.smart_warehousing.mapper.WmsParcelMapper;
import com.qst.smart_warehousing.service.WmsParcelService;
import org.springframework.stereotype.Service;

@Service
public class WmsParcelServiceImpl extends ServiceImpl<WmsParcelMapper, WmsParcel> implements WmsParcelService {
}
