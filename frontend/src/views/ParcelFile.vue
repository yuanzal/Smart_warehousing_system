<template>
    <div class="parcel-file-container">
        <el-card class="query-card" shadow="never">
            <el-form :inline="true" :model="queryParams" size="default">
                <el-form-item label="包裹条码">
                    <el-input v-model="queryParams.barcode" placeholder="请输入完整或部分条码" clearable />
                </el-form-item>
                <el-form-item label="包裹状态">
                    <el-select v-model="queryParams.status" placeholder="全部状态" clearable style="width: 140px;">
                        <el-option v-for="(val, key) in statusMap" :key="key" :label="val.label" :value="Number(key)" />
                    </el-select>
                </el-form-item>
                <el-form-item label="破损异常">
                    <el-select v-model="queryParams.isDamaged" placeholder="不限" clearable style="width: 110px;">
                        <el-option label="正常" :value="0" />
                        <el-option label="破损异常" :value="1" />
                    </el-select>
                </el-form-item>
                <el-form-item>
                    <el-button type="primary" @click="handleQuery">🔍 查询</el-button>
                    <el-button @click="resetQuery">🔄 重置</el-button>
                </el-form-item>
            </el-form>
        </el-card>

        <el-card class="table-card" shadow="never">
            <div class="table-tool-bar">
                <span class="total-text">共检索到 {{ total }} 个包裹档案</span>
                <el-button type="success" size="default" @click="openAddDialog">➕ 登记新包裹</el-button>
            </div>

            <el-table :data="tableData" v-loading="loading" style="width: 100%" border>
                <el-table-column prop="id" label="系统ID" width="100" show-overflow-tooltip />
                <el-table-column prop="barcode" label="包裹条码" min-width="150" />
                <el-table-column prop="weight" label="重量 (kg)" width="100" />
                <el-table-column label="物理尺寸 (L*W*H mm)" width="180">
                    <template #default="{ row }">
                        <span v-if="row.length && row.width && row.height" class="dim-badge">
                            {{ parseInt(row.length) }} × {{ parseInt(row.width) }} × {{ parseInt(row.height) }}
                        </span>
                        <span v-else class="text-gray-500">-</span>
                    </template>
                </el-table-column>
                <el-table-column prop="volume" label="体积 (mm³)" width="130" show-overflow-tooltip>
                    <template #default="{ row }">
                        <span>{{ row.volume ? Number(row.volume).toLocaleString() : '-' }}</span>
                    </template>
                </el-table-column>
                <el-table-column prop="tenantId" label="所属租户" width="100" align="center" v-if="isPlatformAdmin" />
                <el-table-column prop="status" label="当前状态" width="130">
                    <template #default="{ row }">
                        <el-tag :type="statusMap[row.status]?.type || 'info'">
                            {{ statusMap[row.status]?.label || '未知状态' }}
                        </el-tag>
                    </template>
                </el-table-column>
                <el-table-column prop="isDamaged" label="破损状态" width="110">
                    <template #default="{ row }">
                        <el-tag :type="row.isDamaged === 1 ? 'danger' : 'success'">
                            {{ row.isDamaged === 1 ? '💔 破损' : '💚 完好' }}
                        </el-tag>
                    </template>
                </el-table-column>
                <el-table-column prop="updateTime" label="最后更新时间" width="180" />
                <el-table-column label="快捷管理" width="180" fixed="right">
                    <template #default="{ row }">
                        <el-button type="primary" link @click="openEditDialog(row)">编辑</el-button>
                        <el-button type="danger" link @click="handleDelete(row)">删除</el-button>
                    </template>
                </el-table-column>
            </el-table>

            <div class="pagination-area">
                <el-pagination
                    v-model:current-page="queryParams.current"
                    v-model:page-size="queryParams.size"
                    :page-sizes="[10, 20, 50]"
                    layout="total, sizes, prev, pager, next, jumper"
                    :total="total"
                    @size-change="handleSizeChange"
                    @current-change="handleCurrentChange"
                />
            </div>
        </el-card>

        <el-dialog :title="dialogTitle" v-model="dialogVisible" width="550px" destroy-on-close>
            <el-form :model="form" ref="formRef" :rules="formRules" label-width="110px">
                <el-form-item label="包裹条码" prop="barcode">
                    <el-input v-model="form.barcode" placeholder="请输入条码 (如 SF100293)" :disabled="!!form.id" />
                </el-form-item>

                <!-- 【新增】仅平台超管+新增模式下显示租户ID输入框 -->
                <el-form-item v-if="isPlatformAdmin && !isEdit" label="所属租户" prop="tenantId">
                    <el-input-number
                        v-model="form.tenantId"
                        :min="0"
                        controls-position="right"
                        style="width: 100%;"
                        placeholder="请输入目标租户ID"
                    />
                </el-form-item>

                <el-form-item label="物理重量" prop="weight">
                    <el-input-number v-model="form.weight" :precision="2" :step="0.1" :min="0" style="width: 100%" placeholder="重量 (kg)" />
                </el-form-item>

                <el-form-item label="包裹尺寸" required>
                    <div class="dimensions-group">
                        <el-input-number
                            v-model="form.length"
                            :min="0"
                            :controls="false"
                            placeholder="长 (L)"
                            class="dim-input"
                            @change="calcVolume"
                        />
                        <span class="multiply-sign">×</span>
                        <el-input-number
                            v-model="form.width"
                            :min="0"
                            :controls="false"
                            placeholder="宽 (W)"
                            class="dim-input"
                            @change="calcVolume"
                        />
                        <span class="multiply-sign">×</span>
                        <el-input-number
                            v-model="form.height"
                            :min="0"
                            :controls="false"
                            placeholder="高 (H)"
                            class="dim-input"
                            @change="calcVolume"
                        />
                        <span class="unit-text">mm</span>
                    </div>
                </el-form-item>

                <el-form-item label="自动体积 (mm³)">
                    <el-input :value="form.volume ? Number(form.volume).toLocaleString() : '0'" disabled>
                        <template #append>mm³</template>
                    </el-input>
                </el-form-item>

                <el-form-item label="破损情况" prop="isDamaged">
                    <el-radio-group v-model="form.isDamaged">
                        <el-radio :value="0">无异常,完好</el-radio>
                        <el-radio :value="1">有异常,破损</el-radio>
                    </el-radio-group>
                </el-form-item>
            </el-form>
            <template #footer>
                <el-button @click="dialogVisible = false">取消</el-button>
                <el-button type="primary" :loading="submitLoading" @click="submitForm">确定保存</el-button>
            </template>
        </el-dialog>
    </div>
</template>


<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from "@/utils/request.js"

const loading = ref(false)
const tableData = ref([])
const dialogVisible = ref(false)
const isEdit = ref(false)
const dialogTitle = ref('创建包裹档案')
const submitLoading = ref(false)
const formRef = ref(null)

// ========== 【新增】读取当前登录用户信息，判断是否平台超级管理员 ==========
const getCurrentUser = () => {
    try {
        const raw = localStorage.getItem('userInfo')
        if (!raw) return { tenantId: 1 }
        const userInfo = JSON.parse(raw)
        // 兼容两种存储结构：直接对象 / 带data外层包裹
        return userInfo.data || userInfo
    } catch (e) {
        console.error('解析用户信息失败', e)
        return { tenantId: 1 }
    }
}
const currentUser = getCurrentUser()
// 租户ID为0 = 平台顶级管理员
const isPlatformAdmin = currentUser.tenantId === 0
// ======================================================================

const queryParams = reactive({
    current: 1,
    size: 10,
    barcode: '',
    status: null,
    isDamaged: null
})

const form = reactive({
    id: null,
    barcode: '',
    weight: 0,
    length: null,
    width: null,
    height: null,
    volume: null,
    isDamaged: 0,
    tenantId: null // 【新增】租户ID字段
})

const formRules = {
    barcode: [{ required: true, message: '条码不能为空', trigger: 'blur' }],
    weight: [{ required: true, message: '重量不能为空', trigger: 'blur' }],
    length: [{ required: true, message: '长度不能为空', trigger: 'blur' }],
    width: [{ required: true, message: '宽度不能为空', trigger: 'blur' }],
    height: [{ required: true, message: '高度不能为空', trigger: 'blur' }],
    // 【新增】超管场景下租户ID必填
    tenantId: isPlatformAdmin ? [{ required: true, message: '请输入所属租户ID', trigger: 'blur' }] : []
}

const statusMap = {
    1: { label: '传送带进线', type: 'info' },
    2: { label: 'AI核验中', type: 'info' },
    3: { label: '分拣流转', type: 'info' },
    4: { label: '已上架入库', type: 'success'},
    5: { label: '已出库', type: 'success'}
}

const total = ref(0)

// 自动计算体积
const calcVolume = () => {
    if (form.length && form.width && form.height) {
        form.volume = parseFloat((form.length * form.width * form.height).toFixed(2))
    } else {
        form.volume = null
    }
}

const fetchParcels = async () => {
    loading.value = true
    try {
        const { data } = await request.get('/wms/parcel/page', { params: queryParams })
        const rawRecords = data.records || data.data?.records || []
        tableData.value = rawRecords.map(item => {
            const mappedId = item.id || item.parcelId;
            return {
                ...item,
                id: mappedId
            }
        })
        total.value = data.total || data.data?.total || 0
    } catch (e) {
        ElMessage.error('获取包裹列表失败')
    } finally {
        loading.value = false
    }
}

const handleQuery = () => {
    queryParams.current = 1
    fetchParcels()
}
const resetQuery = () => {
    queryParams.barcode = ''
    queryParams.status = null
    queryParams.isDamaged = null
    queryParams.current = 1
    fetchParcels()
}

// 分页控制
const handleSizeChange = (newSize) => {
    queryParams.size = newSize
    queryParams.current = 1
    fetchParcels()
}

const handleCurrentChange = (newCurrent) => {
    queryParams.current = newCurrent
    fetchParcels()
}

const openAddDialog = () => {
    isEdit.value = false
    dialogTitle.value = '登记新包裹'
    form.id = null
    form.barcode = ''
    form.weight = 0
    form.length = null
    form.width = null
    form.height = null
    form.volume = null
    form.isDamaged = 0
    // 【新增】初始化租户ID：超管留空让用户输入，普通用户自动填入自身租户ID
    form.tenantId = isPlatformAdmin ? null : currentUser.tenantId
    dialogVisible.value = true
}

const openEditDialog = (row) => {
    isEdit.value = true
    dialogTitle.value = '编辑包裹档案'
    form.id = row.id || row.parcelId
    form.barcode = row.barcode
    form.weight = row.weight
    form.length = row.length ? parseFloat(row.length) : null
    form.width = row.width ? parseFloat(row.width) : null
    form.height = row.height ? parseFloat(row.height) : null
    form.volume = row.volume ? parseFloat(row.volume) : null
    form.isDamaged = row.isDamaged
    form.tenantId = row.tenantId
    dialogVisible.value = true
}

const submitForm = async () => {
    formRef.value.validate(async (valid) => {
        if (!valid) return
        if (form.length === null || form.width === null || form.height === null) {
            ElMessage.warning('请完整填写包裹的长、宽、高尺寸！')
            return
        }
        submitLoading.value = true
        try {
            const payload = {
                parcelId: form.id,
                id: form.id,
                barcode: form.barcode,
                weight: form.weight,
                length: form.length,
                width: form.width,
                height: form.height,
                volume: form.volume,
                isDamaged: form.isDamaged,
                tenantId: form.tenantId, // 【新增】携带租户ID提交
                status: isEdit.value ? undefined : 1
            }

            if (isEdit.value) {
                await request.put('/wms/parcel', payload)
                ElMessage.success('更新成功')
            } else {
                await request.post('/wms/parcel', payload)
                ElMessage.success('创建成功')
            }
            dialogVisible.value = false
            fetchParcels()
        } catch (e) {
            ElMessage.error('保存失败')
        }
        finally {
            submitLoading.value = false
        }
    })
}

const handleDelete = (row) => {
    const targetId = row.id || row.parcelId;
    ElMessageBox.confirm(`确定要彻底注销包裹 [${row.barcode}] 的系统档案吗？`, '安全警告', {
        confirmButtonText: '确定删除',
        cancelButtonText: '取消',
        type: 'warning'
    }).then(async () => {
        try {
            await request.delete(`/wms/parcel/${targetId}`)
            ElMessage.success('档案注销成功')
            fetchParcels()
        } catch (e) {
            ElMessage.error('注销包裹档案失败')
        }
    }).catch(() => {})
}

onMounted(() => fetchParcels())
</script>

<style scoped>
.parcel-file-container {
    padding: 16px;
    display: flex;
    flex-direction: column;
    gap: 16px;
}
.query-card {
    background-color: #1e293b !important;
    border: 1px solid #334155 !important;
}
.table-card {
    background-color: #1e293b !important;
    border: 1px solid #334155 !important;
}
.table-tool-bar {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 16px;
}
.total-text {
    font-size: 14px;
    color: #94a3b8;
}
.dim-badge {
    background-color: #f8fafc;
    padding: 4px 8px;
    border-radius: 4px;
    font-family: monospace;
    color: #38bdf8;
    border: 1px solid #1e293b;
}
.dimensions-group {
    display: flex;
    align-items: center;
    width: 100%;
}
.inline-item {
    margin-bottom: 0 !important;
    flex: 1;
}
.multiply-sign {
    padding: 0 8px;
    color: #64748b;
    font-weight: bold;
}
.pagination-area {
    display: flex;
    justify-content: flex-end;
    margin-top: 16px;
}
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
    background-color: #1e293b !important;
    color: #94a3b8;
    border: 1px solid #334155 !important;
    border-left: none !important;
}
</style>