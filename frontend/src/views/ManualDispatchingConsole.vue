<template>
    <div class="warehouse-op-container">
        <div class="op-header-panels">
            <div class="stat-box inbound">
                <div class="stat-icon">📥</div>
                <div class="stat-info">
                    <div class="stat-title">入库落位上架</div>
                    <div class="stat-desc">将进线包裹与物理货位绑定</div>
                </div>
            </div>
            <div class="stat-box move">
                <div class="stat-icon">🔄</div>
                <div class="stat-info">
                    <div class="stat-title">库内移库调整</div>
                    <div class="stat-desc">变更包裹的当前存储货位</div>
                </div>
            </div>
            <div class="stat-box outbound">
                <div class="stat-icon">📤</div>
                <div class="stat-info">
                    <div class="stat-title">包裹下架出库</div>
                    <div class="stat-desc">解除货位占用并移出仓库</div>
                </div>
            </div>
        </div>

        <el-row :gutter="16" class="main-op-row">
            <el-col :xs="24" :sm="24" :md="15" :lg="16">
                <el-card class="op-card" shadow="never">
                    <el-tabs v-model="activeTab" class="custom-tabs">

                        <el-tab-pane name="inbound">
                            <template #label><span class="tab-label">📥 包裹入库落位</span></template>
                            <div class="pane-content">
                                <div class="pane-tip">操作指引：请使用PDA或在此处输入进线包裹的快递单号/条码，并分配合法空闲货位。</div>
                                <el-form :model="inboundForm" ref="inboundRef" :rules="opRules" label-width="100px" size="large">
                                    <el-form-item label="包裹条码" prop="barcode">
                                        <el-input v-model="inboundForm.barcode" placeholder="请输入或扫描包裹条码 (如 SF1688992026)" clearable>
                                            <template #prefix>🔍</template>
                                        </el-input>
                                    </el-form-item>
                                    <el-form-item label="分配货位" prop="targetSlotId">
                                        <el-select
                                                v-model="inboundForm.targetSlotId"
                                                placeholder="请选择仓库空闲货位"
                                                filterable
                                                loading-text="正在检索全仓空闲货位..."
                                                style="width: 100%;"
                                        >
                                            <el-option
                                                    v-for="slot in emptySlots"
                                                    :key="slot.slotId"
                                                    :label="`${slot.slotCode} [${slot.zoneName}]`"
                                                    :value="slot.slotId"
                                            />
                                        </el-select>
                                    </el-form-item>
                                    <el-form-item style="margin-top: 32px;">
                                        <el-button type="success" :loading="submitLoading" class="action-btn" @click="handleInbound">
                                            确认落位上架
                                        </el-button>
                                    </el-form-item>
                                </el-form>
                            </div>
                        </el-tab-pane>

                        <el-tab-pane name="move">
                            <template #label><span class="tab-label">🔄 库内移库调整</span></template>
                            <div class="pane-content">
                                <div class="pane-tip">操作指引：适用于包裹损坏、货位清空等场景，直接将已入库的包裹移动至新空闲货位。</div>
                                <el-form :model="moveForm" ref="moveRef" :rules="opRules" label-width="100px" size="large">
                                    <el-form-item label="包裹条码" prop="barcode">
                                        <el-input v-model="moveForm.barcode" placeholder="扫描或输入需要移库的包裹条码" clearable>
                                            <template #prefix>🔍</template>
                                        </el-input>
                                    </el-form-item>
                                    <el-form-item label="目的货位" prop="destSlotId">
                                        <el-select
                                                v-model="moveForm.destSlotId"
                                                placeholder="请选择调拨的目的新空闲货位"
                                                filterable
                                                style="width: 100%;"
                                        >
                                            <el-option
                                                    v-for="slot in emptySlots"
                                                    :key="slot.slotId"
                                                    :label="`${slot.slotCode} [${slot.zoneName}]`"
                                                    :value="slot.slotId"
                                            />
                                        </el-select>
                                    </el-form-item>
                                    <el-form-item style="margin-top: 32px;">
                                        <el-button type="primary" :loading="submitLoading" class="action-btn" @click="handleMove">
                                            确认变更移库
                                        </el-button>
                                    </el-form-item>
                                </el-form>
                            </div>
                        </el-tab-pane>

                        <el-tab-pane name="outbound">
                            <template #label><span class="tab-label">📤 下架出库确认</span></template>
                            <div class="pane-content">
                                <div class="pane-tip">操作指引：核对包裹出库任务，执行此操作后货位将立即被释放变为空闲。</div>
                                <el-form :model="outboundForm" ref="outboundRef" :rules="outboundRules" label-width="100px" size="large">
                                    <el-form-item label="包裹条码" prop="barcode">
                                        <el-input v-model="outboundForm.barcode" placeholder="扫描或输入准备出库的包裹单号" clearable>
                                            <template #prefix>🔍</template>
                                        </el-input>
                                    </el-form-item>
                                    <el-form-item style="margin-top: 32px;">
                                        <el-button type="danger" :loading="submitLoading" class="action-btn" @click="handleOutbound">
                                            确认扣减下架出库
                                        </el-button>
                                    </el-form-item>
                                </el-form>
                            </div>
                        </el-tab-pane>

                    </el-tabs>
                </el-card>
            </el-col>

            <el-col :xs="24" :sm="24" :md="9" :lg="8">
                <el-card class="verify-card" shadow="never">
                    <div class="verify-header">🛡️ 条码即时快照核验</div>
                    <p class="verify-desc">在上方办理作业前，可输入条码随时检索该包裹在WMS中的生命周期及当前物理关联。</p>

                    <div class="search-box">
                        <el-input v-model="searchBarcode" placeholder="键入条码敲回车快速验证" size="default" clearable @keyup.enter="verifyBarcode">
                            <template #append>
                                <el-button @click="verifyBarcode">核验</el-button>
                            </template>
                        </el-input>
                    </div>

                    <div v-loading="verifyLoading" class="snapshot-container">
                        <div v-if="parcelSnapshot" class="snapshot-content">
                            <div class="snapshot-row">
                                <span class="label">包裹ID:</span>
                                <span class="value val-id">{{ parcelSnapshot.parcelId }}</span>
                            </div>
                            <div class="snapshot-row">
                                <span class="label">当前条码:</span>
                                <span class="value text-amber-400 font-mono">{{ parcelSnapshot.barcode }}</span>
                            </div>
                            <div class="snapshot-row">
                                <span class="label">生命周期:</span>
                                <span class="value">
                  <el-tag :type="statusMap[parcelSnapshot.status]?.type || 'info'" size="small">
                    {{ statusMap[parcelSnapshot.status]?.label || '未知' }}
                  </el-tag>
                </span>
                            </div>
                            <div class="snapshot-row">
                                <span class="label">物理货位ID:</span>
                                <span class="value text-sky-400">{{ parcelSnapshot.currentSlotId || '暂无(未上架/已出库)' }}</span>
                            </div>
                            <div class="snapshot-row">
                                <span class="label">异常状态:</span>
                                <span class="value">
                  <el-tag :type="parcelSnapshot.isDamaged === 1 ? 'danger' : 'success'" size="small">
                    {{ parcelSnapshot.isDamaged === 1 ? '💔 破损异常' : '💚 完好正常' }}
                  </el-tag>
                </span>
                            </div>
                            <div class="snapshot-row">
                                <span class="label">重量/体积:</span>
                                <span class="value text-slate-300">
                  {{ parcelSnapshot.weight }}kg / {{ parcelSnapshot.volume ? Number(parcelSnapshot.volume).toLocaleString() : '-' }}mm³
                </span>
                            </div>
                        </div>
                        <div v-else class="snapshot-empty">
                            <span>📭 暂无核验快照，请输入条码查询</span>
                        </div>
                    </div>
                </el-card>
            </el-col>
        </el-row>
    </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import request from "@/utils/request.js"

const activeTab = ref('inbound')
const submitLoading = ref(false)
const verifyLoading = ref(false)

// 对应各 Controller 接口实体 DTO 规范的表单定义
const inboundForm = reactive({ barcode: '', targetSlotId: null })
const moveForm = reactive({ barcode: '', destSlotId: null })
const outboundForm = reactive({ barcode: '' })

// 校验规则
const opRules = {
    barcode: [{ required: true, message: '业务扫描条码不能为空', trigger: 'blur' }],
    targetSlotId: [{ required: true, message: '必须分配合法的物理落位上架货位', trigger: 'change' }],
    destSlotId: [{ required: true, message: '必须选择调拨迁移的目的新货位', trigger: 'change' }]
}
const outboundRules = {
    barcode: [{ required: true, message: '出库前必须校验包裹条码', trigger: 'blur' }]
}

// 货位状态：0空闲, 1已占用, 2锁定, 3故障维修
const emptySlots = ref([])

// 包裹状态映射表
const statusMap = {
    1: { label: '传送带进线', type: 'info' },
    2: { label: 'AI核验中', type: 'info' },
    3: { label: '分拣流转', type: 'info' },
    4: { label: '已上架入库', type: 'success' },
    5: { label: '已出库', type: 'warning' }
}

// 快照数据
const searchBarcode = ref('')
const parcelSnapshot = ref(null)

const inboundRef = ref(null)
const moveRef = ref(null)
const outboundRef = ref(null)

/**
 * 核心联动：拉取全仓目前物理状态为空闲 (status = 0) 的货位列表
 * 对应 WmsStorageSlotController 的 /wms/storage-slot/page 接口
 */
const fetchEmptyStorageSlots = async () => {
    try {
        const { data } = await request.get('/wms/storage-slot/page', {
            params: { current: 1, size: 1000, status: 0 } // 大页长一次性拉取备选空闲货位
        })
        // 兼容可能存在的数据包格式包装
        emptySlots.value = data.records || data.data?.records || []
    } catch (e) {
        console.error('拉取货位失败', e)
        ElMessage.error('动态加载仓库空闲货位名录失败')
    }
}

/**
 * 动作1：包裹入库确认上架落位
 * 对应 WmsParcelController: POST /wms/parcel/inbound
 */
const handleInbound = () => {
    inboundRef.value?.validate(async (valid) => {
        if (!valid) return
        submitLoading.value = true
        try {
            // 严格按照后端 DTO 传参结构发送 payload
            await request.post('/wms/parcel/inbound', {
                barcode: inboundForm.barcode,
                targetSlotId: inboundForm.targetSlotId
            })
            ElMessage.success(`包裹 [${inboundForm.barcode}] 成功于指定货位落位上架`)
            // 成功后清空表单项
            inboundForm.barcode = ''
            inboundForm.targetSlotId = null
            // 级联刷新可用空货位列表和快照
            fetchEmptyStorageSlots()
        } catch (e) {
            ElMessage.error(e.response?.data?.message || '上架落位执行失败')
        } finally {
            submitLoading.value = false
        }
    })
}

/**
 * 动作2：包裹内移库移位
 * 对应 WmsParcelController: POST /wms/parcel/move
 */
const handleMove = () => {
    moveRef.value?.validate(async (valid) => {
        if (!valid) return
        submitLoading.value = true
        try {
            await request.post('/wms/parcel/move', {
                barcode: moveForm.barcode,
                destSlotId: moveForm.destSlotId
            })
            ElMessage.success(`包裹 [${moveForm.barcode}] 移库成功`)
            moveForm.barcode = ''
            moveForm.destSlotId = null
            fetchEmptyStorageSlots()
        } catch (e) {
            ElMessage.error(e.response?.data?.message || '移库调整失败')
        } finally {
            submitLoading.value = false
        }
    })
}

/**
 * 动作3：包裹物理下架扣减出库
 * 对应 WmsParcelController: POST /wms/parcel/outbound
 */
const handleOutbound = () => {
    outboundRef.value?.validate(async (valid) => {
        if (!valid) return
        submitLoading.value = true
        try {
            await request.post('/wms/parcel/outbound', {
                barcode: outboundForm.barcode
            })
            ElMessage.success(`包裹 [${outboundForm.barcode}] 成功办理下架出库，货位释放`)
            outboundForm.barcode = ''
            fetchEmptyStorageSlots()
        } catch (e) {
            ElMessage.error(e.response?.data?.message || '执行出库失败')
        } finally {
            submitLoading.value = false
        }
    })
}

/**
 * 即时快照核验：利用已编写的分页查询接口进行单条码的精准过滤检索
 */
const verifyBarcode = async () => {
    if (!searchBarcode.value.trim()) {
        ElMessage.warning('请输入待检索验证的条码')
        return
    }
    verifyLoading.value = true
    try {
        const { data } = await request.get('/wms/parcel/page', {
            params: { current: 1, size: 1, barcode: searchBarcode.value.trim() }
        })
        const list = data.records || data.data?.records || []
        if (list.length > 0) {
            parcelSnapshot.value = list[0]
            ElMessage.success('成功捕捉包裹系统即时档案快照')
        } else {
            parcelSnapshot.value = null
            ElMessage.error('全仓未查到该条码注册档案')
        }
    } catch (e) {
        ElMessage.error('核验异常')
    } finally {
        verifyLoading.value = false
    }
}

onMounted(() => {
    fetchEmptyStorageSlots()
})
</script>

<style scoped>
.warehouse-op-container {
    padding: 16px;
    display: flex;
    flex-direction: column;
    gap: 16px;
}

/* 状态概览卡片 */
.op-header-panels {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
    gap: 16px;
}
.stat-box {
    background-color: #1e293b;
    border: 1px solid #334155;
    border-radius: 8px;
    padding: 16px;
    display: flex;
    align-items: center;
    gap: 16px;
}
.stat-icon {
    font-size: 28px;
    background-color: #0f172a;
    padding: 8px;
    border-radius: 6px;
}
.stat-title {
    font-size: 16px;
    font-weight: bold;
    color: #f8fafc;
}
.stat-desc {
    font-size: 12px;
    color: #64748b;
    margin-top: 4px;
}

.main-op-row {
    margin-top: 4px;
}

/* 左侧作业卡片 */
.op-card {
    background-color: #1e293b !important;
    border: 1px solid #334155 !important;
    min-height: 480px;
}
.custom-tabs :deep(.el-tabs__item) {
    color: #94a3b8 !important;
    font-size: 15px;
    padding: 0 24px !important;
}
.custom-tabs :deep(.el-tabs__item.is-active) {
    color: #38bdf8 !important;
    font-weight: bold;
}
.custom-tabs :deep(.el-tabs__nav-wrap::after) {
    background-color: #334155 !important;
}
.custom-tabs :deep(.el-tabs__active-bar) {
    background-color: #38bdf8 !important;
}

.pane-content {
    padding: 20px 8px;
}
.pane-tip {
    background-color: #0f172a;
    border-left: 4px solid #38bdf8;
    padding: 12px;
    color: #94a3b8;
    font-size: 13px;
    margin-bottom: 24px;
    border-radius: 0 4px 4px 0;
}

.action-btn {
    width: 200px;
    font-weight: bold;
    letter-spacing: 1px;
}

/* 右侧核验卡片 */
.verify-card {
    background-color: #1e293b !important;
    border: 1px solid #334155 !important;
    height: 100%;
}
.verify-header {
    font-size: 16px;
    font-weight: bold;
    color: #38bdf8;
    margin-bottom: 12px;
}
.verify-desc {
    font-size: 13px;
    color: #94a3b8;
    line-height: 1.5;
    margin-bottom: 16px;
}
.search-box {
    margin-bottom: 20px;
}

.snapshot-container {
    background-color: #0f172a;
    border: 1px solid #334155;
    border-radius: 6px;
    min-height: 240px;
    display: flex;
    flex-direction: column;
    justify-content: center;
}
.snapshot-content {
    padding: 16px;
    display: flex;
    flex-direction: column;
    gap: 14px;
}
.snapshot-row {
    display: flex;
    justify-content: space-between;
    align-items: center;
    border-bottom: 1px dashed #1e293b;
    padding-bottom: 8px;
    font-size: 14px;
}
.snapshot-row:last-child {
    border-bottom: none;
    padding-bottom: 0;
}
.snapshot-row .label {
    color: #64748b;
}
.snapshot-row .value {
    font-weight: 500;
    color: #f8fafc;
}
.val-id {
    background-color: #1e293b;
    padding: 2px 6px;
    border-radius: 4px;
    font-size: 12px;
}
.snapshot-empty {
    text-align: center;
    color: #475569;
    font-size: 13px;
}

/* 全局组件样式深度覆写（对齐暗黑大屏风） */
:deep(.el-form-item__label) {
    color: #94a3b8 !important;
}
:deep(.el-input__wrapper), :deep(.el-select__wrapper) {
    background-color: #0f172a !important;
    border: 1px solid #334155 !important;
    box-shadow: none !important;
}
:deep(.el-input__inner) {
    color: #f8fafc !important;
}
:deep(.el-input-group__append) {
    background-color: #334155 !important;
    color: #38bdf8 !important;
    border: 1px solid #334155 !important;
    font-weight: bold;
}
:deep(.el-input-group__append button) {
    color: #38bdf8 !important;
}
</style>