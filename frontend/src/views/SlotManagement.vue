<template>
    <div class="slot-container p-4">
        <el-card class="query-card mb-4" shadow="never">
            <el-form :inline="true" :model="queryParams" size="default">
                <el-form-item label="货位代码">
                    <el-input v-model="queryParams.slotCode" placeholder="如: A-1-01" clearable />
                </el-form-item>
                <el-form-item label="区域名称">
                    <el-input v-model="queryParams.zoneName" placeholder="A区冷链" clearable />
                </el-form-item>
                <el-form-item label="占用状态">
                    <el-select v-model="queryParams.status" placeholder="全部" clearable style="width: 160px;">
                        <el-option label="0-空闲" :value="0" />
                        <el-option label="1-已占用" :value="1" />
                        <el-option label="2-分拣锁定" :value="2" />
                        <el-option label="3-故障维修" :value="3" />
                    </el-select>
                </el-form-item>
                <el-form-item>
                    <el-button type="primary" @click="fetchSlots">🔍 查询</el-button>
                    <el-button @click="resetQuery">🔄 重置</el-button>
                    <el-button type="success" @click="openAddDialog">➕ 新增立体货位</el-button>
                </el-form-item>
            </el-form>
        </el-card>

        <el-card class="table-card" shadow="never">
            <el-table :data="tableData" v-loading="loading" border>
                <el-table-column prop="slotId" label="货位主键ID" width="80" align="center" />
                <el-table-column prop="slotCode" label="货位编码" width="120" />
                <el-table-column prop="zoneName" label="所属库区" width="120" />
                <el-table-column label="货架 行/列/层" width="140">
                    <template #default="{ row }">
                        {{ row.shelfRow || '-' }} / {{ row.shelfColumn || '-' }} / {{ row.shelfLayer || '-' }}
                    </template>
                </el-table-column>
                <el-table-column label="3D坐标 (X,Y,Z)" width="220">
                    <template #default="{ row }">
                        <el-tag size="small">X: {{ row.xcoordinate }}</el-tag>
                        <el-tag size="small" class="ml-1">Y: {{ row.ycoordinate }}</el-tag>
                        <el-tag size="small" class="ml-1">Z: {{ row.zcoordinate }}</el-tag>
                    </template>
                </el-table-column>
                <el-table-column prop="maxWeight" label="最大承重 (kg)" width="120" />
                <el-table-column prop="currentWeight" label="当前载荷 (kg)" width="120">
                    <template #default="{ row }">
            <span :class="{ 'text-red-500 font-bold': row.currentWeight > row.maxWeight }">
              {{ row.currentWeight ?? 0 }}
            </span>
                    </template>
                </el-table-column>
                <el-table-column prop="status" label="货位状态" width="130">
                    <template #default="{ row }">
                        <el-badge :is-dot="row.currentWeight > row.maxWeight" class="item">
                            <el-tag :type="statusMap[row.status].type">{{ statusMap[row.status].label }}</el-tag>
                        </el-badge>
                    </template>
                </el-table-column>
                <el-table-column prop="parcelCode" label="关联包裹" min-width="150">
                    <template #default="{ row }">
                        <span v-if="row.parcelCode" class="text-blue-400 font-mono">{{ row.parcelCode }}</span>
                        <span v-else class="text-gray-500">- 空闲 -</span>
                    </template>
                </el-table-column>
                <!-- 仅超管显示租户ID -->
                <el-table-column v-if="isPlatformAdmin" prop="tenantId" label="租户ID" width="90" align="center" />
                <el-table-column label="快捷操作" width="240" fixed="right">
                    <template #default="{ row }">
                        <el-button link type="primary" @click="openEditDialog(row)">编辑</el-button>
                        <el-button link type="warning" @click="toggleRepairStatus(row)">
                            {{ row.status === 3 ? '解除维修' : '设为故障' }}
                        </el-button>
                        <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
                    </template>
                </el-table-column>
            </el-table>
            <!-- 分页 -->
            <div class="pagination-area mt-4 flex justify-end">
                <el-pagination
                        v-model:current-page="queryParams.current"
                        v-model:page-size="queryParams.size"
                        :page-sizes="[10,20,50]"
                        layout="total, sizes, prev, pager, next, jumper"
                        :total="total"
                        @size-change="fetchSlots"
                        @current-change="fetchSlots"
                />
            </div>
        </el-card>

        <!-- 新增/编辑弹窗 -->
        <!-- 新增/编辑弹窗 -->
        <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑货位信息' : '新增立体仓储货位'" width="720px" destroy-on-close>
            <el-form :model="slotForm" ref="slotFormRef" label-width="110px" :rules="slotRules">
                <el-form-item label="货位编码" prop="slotCode">
                    <el-input v-model="slotForm.slotCode" placeholder="例如: A-1-05" :disabled="isEdit" />
                </el-form-item>
                <el-form-item label="库区名称" prop="zoneName">
                    <el-input v-model="slotForm.zoneName" placeholder="A区冷链 / B区重货" />
                </el-form-item>
                <div class="flex gap-2">
                    <el-form-item label="货架行" class="inline-item" prop="shelfRow">
                        <el-input-number v-model="slotForm.shelfRow" :min="0" style="width:100%" />
                    </el-form-item>
                    <el-form-item label="货架列" class="inline-item" prop="shelfColumn">
                        <el-input-number v-model="slotForm.shelfColumn" :min="0" style="width:100%" />
                    </el-form-item>
                    <el-form-item label="货架层" class="inline-item" prop="shelfLayer">
                        <el-input-number v-model="slotForm.shelfLayer" :min="0" style="width:100%" />
                    </el-form-item>
                </div>
                <div class="flex gap-2">
                    <el-form-item label="X坐标(米)" class="inline-item" prop="xCoordinate">
                        <el-input-number v-model="slotForm.xcoordinate" :step="0.001" :precision="3" style="width:100%" placeholder="支持小数 1.200" />
                    </el-form-item>
                    <el-form-item label="Y坐标(米)" class="inline-item" prop="yCoordinate">
                        <el-input-number v-model="slotForm.ycoordinate" :step="0.001" :precision="3" style="width:100%" />
                    </el-form-item>
                    <el-form-item label="Z坐标(米)" class="inline-item" prop="zCoordinate">
                        <el-input-number v-model="slotForm.zcoordinate" :step="0.001" :precision="3" style="width:100%" />
                    </el-form-item>
                </div>
                <el-form-item label="最大承重(kg)" prop="maxWeight">
                    <el-input-number v-model="slotForm.maxWeight" :min="10" :precision="2" style="width:100%" />
                </el-form-item>
                <!-- 仅顶级管理员显示租户输入框 -->
                <el-form-item v-if="isPlatformAdmin && !isEdit" label="租户ID" prop="tenantId">
                    <el-input-number v-model="slotForm.tenantId" :min="0" controls-position="right" style="width:100%" placeholder="普通租户自动填充自身ID" />
                </el-form-item>
            </el-form>
            <template #footer>
                <el-button @click="dialogVisible = false">取消</el-button>
                <el-button type="primary" :loading="submitLoading" @click="submitSlot">保存</el-button>
            </template>
        </el-dialog>
    </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from "@/utils/request.js"

const loading = ref(false)
const submitLoading = ref(false)
const tableData = ref([])
const total = ref(0)
const dialogVisible = ref(false)
const isEdit = ref(false)
const slotFormRef = ref(null)

// ========== 多租户身份判断 ==========
const getCurrentUser = () => {
    try {
        const raw = localStorage.getItem('userInfo')
        if (!raw) return { tenantId: 1 }
        const user = JSON.parse(raw)
        return user.data || user
    } catch {
        return { tenantId: 1 }
    }
}
const currentUser = getCurrentUser()
const isPlatformAdmin = currentUser.tenantId === 0

// 查询参数
const queryParams = reactive({
    current: 1,
    size: 10,
    slotCode: '',
    zoneName: '',
    status: null
})

// 表单模型 完全对齐wms_storage_slot表字段
const slotForm = reactive({
    slotId: null,
    slotCode: '',
    zoneName: '',
    shelfRow: null,
    shelfColumn: null,
    shelfLayer: null,
    xcoordinate: 0.000,
    ycoordinate: 0.000,
    zcoordinate: 0.000,
    maxWeight: 100.00,
    tenantId: null
})

// 表单校验规则
const slotRules = {
    slotCode: [{ required: true, message: '货位编码不能为空', trigger: 'blur' }],
    zoneName: [{ required: true, message: '库区名称不能为空', trigger: 'blur' }],
    xcoordinate: [{ required: true, message: 'X坐标必填', trigger: 'blur' }],
    ycoordinate: [{ required: true, message: 'Y坐标必填', trigger: 'blur' }],
    zcoordinate: [{ required: true, message: 'Z坐标必填', trigger: 'blur' }],
    maxWeight: [{ required: true, message: '最大承重必填', trigger: 'blur' }],
    tenantId: isPlatformAdmin ? [{ required: true, message: '请输入租户ID', trigger: 'blur' }] : []
}

// 状态映射 完全匹配数据库注释
const statusMap = {
    0: { label: '空闲', type: 'success' },
    1: { label: '已占用', type: 'primary' },
    2: { label: '分拣锁定', type: 'warning' },
    3: { label: '故障维修', type: 'danger' }
}

const resetQuery = () => {
    queryParams.slotCode = ''
    queryParams.status = null
    queryParams.zoneName = ''
    queryParams.size = 10
    queryParams.current = 1
    fetchSlots()
}

// 分页查询货位列表
const fetchSlots = async () => {
    loading.value = true
    try {
        const res = await request.get('/wms/storage-slot/page', { params: queryParams })
        if (res.code === 0) {
            tableData.value = res.data.records || []
            total.value = res.data.total || 0
        }
    } catch (e) {
        ElMessage.error('获取货位数据失败')
    } finally {
        loading.value = false
    }
}

// 打开新增弹窗
const openAddDialog = () => {
    isEdit.value = false
    dialogVisible.value = true
    // 重置表单
    slotForm.slotId = null
    slotForm.slotCode = ''
    slotForm.zoneName = ''
    slotForm.shelfRow = null
    slotForm.shelfColumn = null
    slotForm.shelfLayer = null
    slotForm.xcoordinate = 0.000
    slotForm.ycoordinate = 0.000
    slotForm.zcoordinate = 0.000
    slotForm.maxWeight = 100.00
    // 租户赋值：超管留空手动输入，普通用户自动填充自身租户ID
    slotForm.tenantId = isPlatformAdmin ? null : currentUser.tenantId
}

// 打开编辑弹窗
const openEditDialog = (row) => {
    isEdit.value = true
    dialogVisible.value = true
    slotForm.slotId = row.slotId
    slotForm.slotCode = row.slotCode
    slotForm.zoneName = row.zoneName
    slotForm.shelfRow = row.shelfRow
    slotForm.shelfColumn = row.shelfColumn
    slotForm.shelfLayer = row.shelfLayer
    slotForm.xcoordinate = row.xcoordinate
    slotForm.ycoordinate = row.ycoordinate
    slotForm.zcoordinate = row.zcoordinate
    slotForm.maxWeight = row.maxWeight
    slotForm.tenantId = row.tenantId
}

// 提交新增/编辑
const submitSlot = async () => {
    await slotFormRef.value.validate(async (valid) => {
        if (!valid) return
        submitLoading.value = true
        try {
            if (isEdit.value) {
                await request.put('/wms/storage-slot', slotForm)
                ElMessage.success('货位编辑成功')
            } else {
                await request.post('/wms/storage-slot/add', slotForm)
                ElMessage.success('货位创建成功')
            }
            dialogVisible.value = false
            fetchSlots()
        } catch (e) {
            ElMessage.error('保存货位失败')
        } finally {
            submitLoading.value = false
        }
    })
}

// 切换故障维修状态
const toggleRepairStatus = async (row) => {
    const targetStatus = row.status === 3 ? 0 : 3
    // put 请求参数拼在url上
    await request.put(`/wms/storage-slot/status?slotId=${row.slotId}&status=${targetStatus}`)
    ElMessage.success('状态切换成功')
    fetchSlots()
}

// 删除货位
const handleDelete = (row) => {
    ElMessageBox.confirm(`确定删除货位【${row.slotCode}】？删除后无法恢复`, '高危操作', {
        type: 'error'
    }).then(async () => {
        await request.delete(`/wms/storage-slot/${row.slotId}`)
        ElMessage.success('货位已删除')
        fetchSlots()
    })
}

onMounted(() => fetchSlots())
</script>

<style scoped>
.slot-container {
    color: #f8fafc;
}
.query-card, .table-card {
    background-color: #1e293b !important;
    border: 1px solid #334155 !important;
}
.inline-item {
    margin-bottom: 0 !important;
    flex: 1;
}
.pagination-area {
    color: #94a3b8;
}
:deep(.el-form-item__label) {
    color: #94a3b8 !important;
}
:deep(.el-input__wrapper), :deep(.el-input-number__inner) {
    background-color: #0f172a !important;
    border: 1px solid #334155 !important;
}
:deep(.el-input__inner) {
    color: #f8fafc !important;
}
</style>