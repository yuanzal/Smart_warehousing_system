package com.qst.smartbuildings.entity;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.io.Serializable;

/**
 * 精简版分页类
 * 保留核心分页功能，兼容MyBatis-Plus的IPage接口
 *
 */
public class BPage<T> extends Page<T> implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 额外数据存储 */
    private Object extraData;

    /** 是否查询总数（低版本MP需自己维护该字段） */
    private boolean searchCount = true;


    // 构造方法
    public BPage() {
        super();
    }

    public BPage(long current, long size) {
        super(current, size);
    }

    public BPage(long current, long size, long total) {
        super(current, size, total);
    }

    public BPage(long current, long size, boolean searchCount) {
        super(current, size);
        this.searchCount = searchCount;
    }
}
