<template>
    <div class="sorting-tasks-container">
        <el-row :gutter="16" class="metrics-row">
            <el-col :span="6">
                <div class="metric-card">
                    <div class="metric-icon blue">📦</div>
                    <div class="metric-info">
                        <span class="metric-title">今日分拣总任务量</span>
                        <span class="metric-value">{{ overview.totalCount ?? 0 }} <small>单</small></span>
                    </div>
                </div>
            </el-col>
            <el-col :span="6">
                <div class="metric-card">
                    <div class="metric-icon orange">⚡</div>
                    <div class="metric-info">
                        <span class="metric-title">AGV 无缝转运中</span>
                        <span class="metric-value orange-text">{{ overview.transportingCount ?? 0 }} <small>单</small></span>
                    </div>
                </div>
            </el-col>
            <el-col :span="6">
                <div class="metric-card">
                    <div class="metric-icon green">✅</div>
                    <div class="metric-info">
                        <span class="metric-title">机械臂已入库</span>
                        <span class="metric-value green-text">{{ overview.completedCount ?? 0 }} <small>单</small></span>
                    </div>
                </div>
            </el-col>
            <el-col :span="6">
                <div class="metric-card">
                    <div class="metric-icon red">🚨</div>
                    <div class="metric-info">
                        <span class="metric-title">异常拦截/终止</span>
                        <span class="metric-value red-text">{{ overview.exceptionCount ?? 0 }} <small>单</small></span>
                    </div>
                </div>
            </el-col>
        </el-row>

        <div class="filter-card">
            <el-form :inline="true" :model="queryForm" class="dark-form">
                <el-form-item label="任务/包裹单号">
                    <el-input
                        v-model="queryForm.keyword"
                        placeholder="搜索任务单号或条码"
                        clearable
                        style="width: 220px;"
                    />
                </el-form-item>
                <el-form-item label="调度状态">
                    <el-select v-model="queryForm.status" placeholder="全部状态" clearable style="width: 180px;">
                        <el-option label="1 - 任务创建" :value="1" />
                        <el-option label="2 - 路由计算完毕" :value="2" />
                        <el-option label="3 - AGV无缝转运中" :value="3" />
                        <el-option label="4 - 已成功分拣入库" :value="4" />
                        <el-option label="5 - 异常拦截终止" :value="5" />
                    </el-select>
                </el-form-item>
                <el-form-item label="生成时间">
                    <el-date-picker
                        v-model="queryForm.dateRange"
                        type="daterange"
                        range-separator="至"
                        start-placeholder="开始日期"
                        end-placeholder="结束日期"
                        value-format="YYYY-MM-DD HH:mm:ss"
                        style="width: 260px;"
                    />
                </el-form-item>
                <el-form-item>
                    <el-button type="primary" class="glow-button" @click="handleSearch">
                        🔍 查询
                    </el-button>
                    <el-button class="dark-button" @click="resetQuery">重置</el-button>
                </el-form-item>
            </el-form>
        </div>

        <div class="table-card">
            <el-table
                :data="taskList"
                style="width: 100%"
                class="dark-table"
                v-loading="loading"
            >
                <el-table-column prop="taskCode" label="任务单号" min-width="170">
                    <template #default="{ row }">
                        <span class="code-text">{{ row.taskCode }}</span>
                    </template>
                </el-table-column>
                <el-table-column prop="barcode" label="关联包裹条码" min-width="160">
                    <template #default="{ row }">
                        <span class="barcode-badge">🏷️ {{ row.barcode }}</span>
                    </template>
                </el-table-column>
                <el-table-column prop="sourceStationId" label="进线滑道" width="100" align="center">
                    <template #default="{ row }">
                        <el-tag size="small" type="info" class="dark-tag">#{{ row.sourceStationId }} 滑道</el-tag>
                    </template>
                </el-table-column>
                <el-table-column prop="targetSlotCode" label="目标预定货位" width="140" align="center">
                    <template #default="{ row }">
                        <span class="slot-text">{{ row.targetSlotCode || '算法匹配中...' }}</span>
                    </template>
                </el-table-column>
                <el-table-column prop="assignedAgvCode" label="调度 AGV" width="130" align="center">
                    <template #default="{ row }">
                        <span v-if="row.assignedAgvCode" class="agv-badge">🤖 {{ row.assignedAgvCode }}</span>
                        <span v-else class="text-muted">待指派</span>
                    </template>
                </el-table-column>
                <el-table-column prop="status" label="当前状态" width="150" align="center">
                    <template #default="{ row }">
                        <el-tag :type="getStatusTagType(row.status)" effect="dark" class="status-tag">
                            <span class="dot" :class="getStatusDotClass(row.status)"></span>
                            {{ getStatusLabel(row.status) }}
                        </el-tag>
                    </template>
                </el-table-column>
                <el-table-column prop="createTime" label="任务生成时间" min-width="160" sortable />

                <el-table-column label="操作" width="170" fixed="right" align="center">
                    <template #default="{ row }">
                        <div class="action-cell">
                            <el-button link type="primary" @click="openTaskDetail(row)">
                                <span>👁️ 跟踪</span>
                            </el-button>

                            <el-dropdown
                                v-if="[1, 2, 3, 5].includes(row.status)"
                                trigger="click"
                                popper-class="dark-dropdown-popper"
                                @command="(cmd) => handleAction(cmd, row)"
                            >
                                <el-button link type="info" class="more-btn">
                                    <span>更多 ▾</span>
                                </el-button>
                                <template #dropdown>
                                    <el-dropdown-menu>
                                        <el-dropdown-item
                                            v-if="row.status === 5"
                                            command="retry"
                                            class="menu-item-retry"
                                        >
                                            🔄 重新调度
                                        </el-dropdown-item>
                                        <el-dropdown-item
                                            v-if="[1, 2, 3].includes(row.status)"
                                            command="cancel"
                                            class="menu-item-cancel"
                                        >
                                            🚫 紧急拦截
                                        </el-dropdown-item>
                                    </el-dropdown-menu>
                                </template>
                            </el-dropdown>

                            <span v-else class="no-more-action">-</span>
                        </div>
                    </template>
                </el-table-column>
            </el-table>

            <div class="pagination-bar">
                <el-pagination
                    v-model:current-page="currentPage"
                    v-model:page-size="pageSize"
                    :page-sizes="[10, 20, 50]"
                    layout="total, sizes, prev, pager, next, jumper"
                    :total="total"
                    @size-change="handleSearch"
                    @current-change="handleSearch"
                />
            </div>
        </div>

        <el-drawer
            v-model="drawerVisible"
            title="智能分拣调度全流程追踪"
            direction="rtl"
            size="520px"
            custom-class="dark-drawer"
        >
            <div v-if="currentTask" class="drawer-content">
                <div class="snapshot-box">
                    <div class="snapshot-item">
                        <span class="label">任务单号：</span>
                        <span class="val cyan">{{ currentTask.taskCode }}</span>
                    </div>
                    <div class="snapshot-item">
                        <span class="label">包裹条码：</span>
                        <span class="val">{{ currentTask.barcode }}</span>
                    </div>
                    <div class="snapshot-item">
                        <span class="label">执行 AGV：</span>
                        <span class="val orange">{{ currentTask.assignedAgvCode || '未分配' }}</span>
                    </div>
                    <div class="snapshot-item">
                        <span class="label">落位货位：</span>
                        <span class="val green">{{ currentTask.targetSlotCode || '计算中' }}</span>
                    </div>
                </div>

                <el-divider class="dark-divider"><span class="divider-title">节点状态链</span></el-divider>

                <el-steps :active="getStepActive(currentTask.status)" finish-status="success" align-center class="dark-steps">
                    <el-step title="创建" description="传送带进线" />
                    <el-step title="算路" description="AI中台路由" />
                    <el-step title="转运" description="AGV/PLC搬运" />
                    <el-step title="落位" description="机械臂入库" />
                </el-steps>

                <el-divider class="dark-divider"><span class="divider-title">时序执行日志</span></el-divider>

                <el-timeline class="dark-timeline">
                    <el-timeline-item :timestamp="currentTask.endTime" type="success" placement="top" v-if="currentTask.status === 4">
                        <h4>机械臂码垛完成</h4>
                        <p>包裹已安全核验落位至货位 <code>{{ currentTask.targetSlotCode }}</code></p>
                    </el-timeline-item>

                    <el-timeline-item :timestamp="currentTask.startTime" type="primary" placement="top" v-if="currentTask.status >= 3">
                        <h4>AGV 实时承接搬运</h4>
                        <p>分配车辆 <code>{{ currentTask.assignedAgvCode }}</code>，已锁定向拓扑节点巡航</p>
                    </el-timeline-item>

                    <el-timeline-item :timestamp="currentTask.createTime" type="info" placement="top" v-if="currentTask.status >= 2">
                        <h4>中台 AI 算法匹配路由</h4>
                        <p>基于 Three.js 孪生空间动态计算出最优推荐货位 <code>{{ currentTask.targetSlotCode }}</code></p>
                    </el-timeline-item>

                    <el-timeline-item :timestamp="currentTask.createTime" type="info" placement="top">
                        <h4>分拣任务初始化生成</h4>
                        <p>包裹在 #{{ currentTask.sourceStationId }} 号进线滑道完成扫码识别</p>
                    </el-timeline-item>
                </el-timeline>

                <el-alert
                    v-if="currentTask.status === 5"
                    title="系统异常拦截警告"
                    type="error"
                    description="AGV 在搬运途中触发防撞传感器，或算法检测到目标货位已被物理占用，任务已人工/自动挂起。"
                    show-icon
                    :closable="false"
                    style="margin-top: 20px;"
                />
            </div>
        </el-drawer>
    </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request'

// 1. 查询条件与表格状态
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

// 顶部指标数据
const overview = ref({
    totalCount: 0,
    transportingCount: 0,
    completedCount: 0,
    exceptionCount: 0
})

const queryForm = reactive({
    keyword: '',
    status: null,
    dateRange: []
})

const taskList = ref([])

// 3. 抽屉控制
const drawerVisible = ref(false)
const currentTask = ref(null)

// 4. 辅助函数：根据状态输出渲染样式与文本
const getStatusLabel = (status) => {
    const map = {
        1: '任务创建',
        2: '路由计算完毕',
        3: 'AGV无缝转运中',
        4: '已成功分拣入库',
        5: '异常拦截终止'
    }
    return map[status] || '未知状态'
}

const getStatusTagType = (status) => {
    const map = { 1: 'info', 2: 'primary', 3: 'warning', 4: 'success', 5: 'danger' }
    return map[status] || 'info'
}

const getStatusDotClass = (status) => {
    const map = { 1: 'bg-info', 2: 'bg-primary', 3: 'bg-warning', 4: 'bg-success', 5: 'bg-danger' }
    return map[status] || ''
}

const getStepActive = (status) => {
    if (status === 5) return 2 // 终止阻断在转运环节
    return status
}

/**
 * 加载顶部统计指标
 */
const loadOverview = async () => {
    try {
        const res = await request.get('/wms/sorting-task/overview')
        overview.value = res.data
    } catch (err) {
        console.error('加载指标失败', err)
    }
}

/**
 * 查询表格数据
 */
const handleSearch = async () => {
    loading.value = true
    try {
        const params = {
            pageNum: currentPage.value,
            pageSize: pageSize.value,
            keyword: queryForm.keyword || undefined,
            status: queryForm.status ?? undefined,
            startTime: queryForm.dateRange?.[0] ?? undefined,
            endTime: queryForm.dateRange?.[1] ?? undefined
        }
        const res = await request.get('/wms/sorting-task/page', { params })
        taskList.value = res.data.records
        total.value = res.data.total
    } catch (err) {
        ElMessage.error('查询任务列表失败')
        console.error(err)
    } finally {
        loading.value = false
    }
}

const resetQuery = () => {
    queryForm.keyword = ''
    queryForm.status = null
    queryForm.dateRange = []
    currentPage.value = 1
    handleSearch()
}

const openTaskDetail = (row) => {
    currentTask.value = row
    drawerVisible.value = true
}

const handleAction = async (command, row) => {
    if (command === 'retry') {
        try {
            await request.post(`/wms/sorting-task/retry/${row.taskId}`)
            ElMessage.success(`任务 [${row.taskCode}] 已重新下发算法引擎重新算路`)
            handleSearch()
            loadOverview()
        } catch (err) {
            ElMessage.error('重新调度失败')
        }
    } else if (command === 'cancel') {
        ElMessageBox.confirm(`确定要紧急拦截并终止任务 ${row.taskCode} 吗？`, '高危操作警告', {
            confirmButtonText: '强制拦截',
            cancelButtonText: '取消',
            type: 'warning'
        }).then(async () => {
            try {
                await request.post(`/wms/sorting-task/cancel/${row.taskId}`)
                ElMessage.error(`已向 PLC 及 AGV 下发急停指令，任务已终止`)
                handleSearch()
                loadOverview()
            } catch (err) {
                ElMessage.error('紧急拦截失败')
            }
        })
    }
}

onMounted(() => {
    loadOverview()
    handleSearch()
})
</script>

<style scoped>
.sorting-tasks-container {
    display: flex;
    flex-direction: column;
    gap: 16px;
}

/* 核心指标卡片 */
.metrics-row {
    margin-bottom: 4px;
}
.metric-card {
    background: #1e293b;
    border: 1px solid #334155;
    border-radius: 8px;
    padding: 16px 20px;
    display: flex;
    align-items: center;
    gap: 16px;
}
.metric-icon {
    width: 48px;
    height: 48px;
    border-radius: 8px;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 22px;
    background: #0f172a;
}
.metric-info {
    display: flex;
    flex-direction: column;
}
.metric-title {
    font-size: 13px;
    color: #94a3b8;
}
.metric-value {
    font-size: 24px;
    font-weight: 700;
    color: #f8fafc;
}
.metric-value small {
    font-size: 12px;
    font-weight: 400;
    color: #64748b;
}
.orange-text { color: #f59e0b; }
.green-text { color: #10b981; }
.red-text { color: #f43f5e; }

/* 暗黑筛选区 */
.filter-card {
    background: #1e293b;
    border: 1px solid #334155;
    border-radius: 8px;
    padding: 18px 20px 2px 20px;
}
:deep(.dark-form .el-form-item__label) {
    color: #94a3b8;
}
:deep(.el-input__wrapper), :deep(.el-select__wrapper) {
    background-color: #0f172a !important;
    box-shadow: 0 0 0 1px #334155 inset !important;
    color: #f8fafc;
}
:deep(.el-input__inner) {
    color: #f8fafc;
}

.glow-button {
    background-color: #0284c7;
    border: none;
    box-shadow: 0 0 12px rgba(56, 189, 248, 0.3);
}
.dark-button {
    background: #0f172a;
    border: 1px solid #334155;
    color: #94a3b8;
}

/* 数据表格区 */
.table-card {
    background: #1e293b;
    border: 1px solid #334155;
    border-radius: 8px;
    padding: 16px;
}
:deep(.dark-table) {
    background-color: transparent !important;
    --el-table-border-color: #334155;
    --el-table-header-bg-color: #0f172a;
    --el-table-row-hover-bg-color: #1e293b;
    color: #e2e8f0;
}
:deep(.dark-table th.el-table__cell) {
    background-color: #0f172a !important;
    color: #38bdf8;
    font-weight: 600;
}
:deep(.dark-table tr) {
    background-color: #1e293b !important;
}

/* 文本和 Tag 样式提升 */
.code-text {
    font-family: monospace;
    color: #38bdf8;
    font-weight: 600;
}
.barcode-badge {
    color: #cbd5e1;
    font-family: monospace;
}
.slot-text {
    color: #10b981;
    font-weight: bold;
}
.agv-badge {
    color: #f59e0b;
    font-weight: 500;
}
.text-muted { color: #64748b; }

.status-tag {
    display: inline-flex;
    align-items: center;
    gap: 6px;
    padding: 0 10px;
}
.dot {
    width: 6px;
    height: 6px;
    border-radius: 50%;
}
.bg-info { background: #94a3b8; }
.bg-primary { background: #38bdf8; }
.bg-warning { background: #f59e0b; box-shadow: 0 0 6px #f59e0b; }
.bg-success { background: #10b981; box-shadow: 0 0 6px #10b981; }
.bg-danger { background: #f43f5e; box-shadow: 0 0 6px #f43f5e; }

/* 分页 */
.pagination-bar {
    margin-top: 16px;
    display: flex;
    justify-content: flex-end;
}
:deep(.el-pagination) {
    --el-pagination-button-bg-color: #0f172a;
    --el-pagination-hover-color: #38bdf8;
    color: #94a3b8;
}

/* 抽屉样式 */
.drawer-content {
    padding: 10px 0;
}
.snapshot-box {
    background: #0f172a;
    border: 1px solid #334155;
    border-radius: 6px;
    padding: 14px;
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 10px;
}
.snapshot-item {
    font-size: 13px;
}
.snapshot-item .label { color: #94a3b8; }
.snapshot-item .val { color: #f8fafc; font-weight: 600; }
.snapshot-item .val.cyan { color: #38bdf8; }
.snapshot-item .val.orange { color: #f59e0b; }
.snapshot-item .val.green { color: #10b981; }

.dark-divider {
    border-color: #334155;
    margin: 24px 0 16px 0;
}
.divider-title {
    color: #64748b;
    font-size: 12px;
    background: #1e293b;
    padding: 0 8px;
}

:deep(.dark-steps .el-step__title) {
    font-size: 13px;
    color: #94a3b8;
}
:deep(.dark-timeline .el-timeline-item__content) {
    color: #cbd5e1;
}
:deep(.dark-timeline h4) {
    margin: 0 0 4px 0;
    color: #f8fafc;
}
:deep(.dark-timeline p) {
    margin: 0;
    font-size: 12px;
    color: #94a3b8;
}
:deep(.dark-timeline code) {
    color: #38bdf8;
    background: #0f172a;
    padding: 2px 4px;
    border-radius: 4px;
}
/* 操作列容器 */
.action-cell {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 8px;
}

.more-btn {
    color: #94a3b8 !important;
    font-size: 13px;
}
.more-btn:hover {
    color: #38bdf8 !important;
}

.no-more-action {
    color: #475569;
    font-size: 12px;
    padding: 0 8px;
}

/* 暗黑风格下拉 Popper 弹窗样式穿透 */
:deep(.dark-dropdown-popper) {
    background-color: #0f172a !important;
    border: 1px solid #334155 !important;
    box-shadow: 0 10px 25px -5px rgba(0, 0, 0, 0.5) !important;
    border-radius: 6px !important;
}

:deep(.dark-dropdown-popper .el-dropdown-menu) {
    background-color: #0f172a !important;
    padding: 4px 0 !important;
    border: none !important;
}

:deep(.dark-dropdown-popper .el-dropdown-menu__item) {
    color: #cbd5e1 !important;
    font-size: 13px !important;
    padding: 8px 16px !important;
    line-height: 1.5;
}

:deep(.dark-dropdown-popper .el-dropdown-menu__item:hover) {
    background-color: #1e293b !important;
    color: #38bdf8 !important;
}

/* 重新调度 - 浅青色 hover */
:deep(.dark-dropdown-popper .menu-item-retry:hover) {
    background-color: rgba(56, 189, 248, 0.1) !important;
    color: #38bdf8 !important;
}

/* 紧急拦截 - 警告红色 */
:deep(.dark-dropdown-popper .menu-item-cancel) {
    color: #f43f5e !important;
}
:deep(.dark-dropdown-popper .menu-item-cancel:hover) {
    background-color: rgba(244, 63, 94, 0.15) !important;
    color: #f43f5e !important;
}

/* 箭头小尖角适配暗黑底色 */
:deep(.dark-dropdown-popper .el-popper__arrow::before) {
    background-color: #0f172a !important;
    border: 1px solid #334155 !important;
}
</style>