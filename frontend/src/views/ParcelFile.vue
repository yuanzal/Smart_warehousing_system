<template>
    <div class="parcel-file-container">
        <div class="filter-card">
            <el-form :inline="true" :model="queryParams" size="default" class="dark-form">
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
                    <el-button type="primary" class="glow-button" @click="handleQuery">🔍 查询</el-button>
                    <el-button class="dark-button" @click="resetQuery">🔄 重置</el-button>
                </el-form-item>
            </el-form>
        </div>

        <div class="table-card">
            <div class="table-tool-bar">
                <span class="total-text">共检索到 {{ total }} 个包裹档案</span>
                <el-button type="primary" class="glow-button" size="default" @click="openAddDialog">➕ 登记新包裹</el-button>
            </div>

            <el-table :data="tableData" v-loading="loading" style="width: 100%" class="dark-table">
                <el-table-column prop="id" label="系统ID" width="100" show-overflow-tooltip />
                <el-table-column prop="barcode" label="包裹条码" min-width="150">
                    <template #default="{ row }">
                        <span class="code-text">{{ row.barcode }}</span>
                    </template>
                </el-table-column>
                <el-table-column prop="weight" label="重量 (kg)" width="100" align="center" />
                <el-table-column label="物理尺寸 (L*W*H mm)" width="180" align="center">
                    <template #default="{ row }">
                        <span v-if="row.length && row.width && row.height" class="dim-badge">
                            {{ parseInt(row.length) }} × {{ parseInt(row.width) }} × {{ parseInt(row.height) }}
                        </span>
                        <span v-else class="text-muted">-</span>
                    </template>
                </el-table-column>
                <el-table-column prop="volume" label="体积 (mm³)" width="130" show-overflow-tooltip align="center">
                    <template #default="{ row }">
                        <span>{{ row.volume ? Number(row.volume).toLocaleString() : '-' }}</span>
                    </template>
                </el-table-column>
                <el-table-column prop="tenantId" label="所属租户" width="100" align="center" v-if="isPlatformAdmin" />
                <el-table-column prop="status" label="当前状态" width="130" align="center">
                    <template #default="{ row }">
                        <el-tag :type="statusMap[row.status]?.type || 'info'" effect="dark" class="status-tag">
                            {{ statusMap[row.status]?.label || '未知状态' }}
                        </el-tag>
                    </template>
                </el-table-column>
                <el-table-column prop="isDamaged" label="破损状态" width="110" align="center">
                    <template #default="{ row }">
                        <el-tag :type="row.isDamaged === 1 ? 'danger' : 'success'" effect="dark">
                            {{ row.isDamaged === 1 ? '💔 破损' : '💚 完好' }}
                        </el-tag>
                    </template>
                </el-table-column>
                <el-table-column prop="updateTime" label="最后更新时间" width="180" align="center" />
                <el-table-column label="快捷管理" width="180" fixed="right" align="center">
                    <template #default="{ row }">
                        <div class="action-cell">
                            <el-button link type="primary" @click="openEditDialog(row)">
                                <span>编辑</span>
                            </el-button>
                            <el-button link type="danger" @click="handleDelete(row)">
                                <span>删除</span>
                            </el-button>
                        </div>
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
        </div>

        <el-dialog :title="dialogTitle" v-model="dialogVisible" width="550px" destroy-on-close custom-class="dark-dialog">
            <el-form :model="form" ref="formRef" :rules="formRules" label-width="110px" class="dark-form">
                <el-form-item label="包裹条码" prop="barcode">
                    <el-input v-model="form.barcode" placeholder="请输入条码 (如 SF100293)" :disabled="!!form.id" />
                </el-form-item>

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
                <div class="dialog-footer">
                    <el-button class="dark-button" @click="dialogVisible = false">取消</el-button>
                    <el-button type="primary" class="glow-button" :loading="submitLoading" @click="submitForm">确定保存</el-button>
                </div>
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

const getCurrentUser = () => {
    try {
        const raw = localStorage.getItem('userInfo')
        if (!raw) return { tenantId: 1 }
        const userInfo = JSON.parse(raw)
        return userInfo.data || userInfo
    } catch (e) {
        console.error('解析用户信息失败', e)
        return { tenantId: 1 }
    }
}
const currentUser = getCurrentUser()
const isPlatformAdmin = currentUser.tenantId === 0

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
    tenantId: null
})

const formRules = {
    barcode: [{ required: true, message: '条码不能为空', trigger: 'blur' }],
    weight: [{ required: true, message: '重量不能为空', trigger: 'blur' }],
    length: [{ required: true, message: '长度不能为空', trigger: 'blur' }],
    width: [{ required: true, message: '宽度不能为空', trigger: 'blur' }],
    height: [{ required: true, message: '高度不能为空', trigger: 'blur' }],
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
                tenantId: form.tenantId,
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
    display: flex;
    flex-direction: column;
    gap: 16px;
}

.filter-card {
    background: #1e293b;
    border: 1px solid #334155;
    border-radius: 8px;
    padding: 18px 20px 2px 20px;
}

.table-card {
    background: #1e293b;
    border: 1px solid #334155;
    border-radius: 8px;
    padding: 16px;
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

/* 表格全局暗黑 与分拣页面完全统一 */
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

.code-text {
    font-family: monospace;
    color: #38bdf8;
    font-weight: 600;
}
.dim-badge {
    font-family: monospace;
    color: #38bdf8;
}
.text-muted { color: #64748b; }

.dimensions-group {
    display: flex;
    align-items: center;
    gap: 6px;
    width: 100%;
}
.multiply-sign {
    color: #64748b;
    font-weight: bold;
}
.unit-text {
    color: #94a3b8;
    padding-left: 6px;
}

.action-cell {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 8px;
}

/* 分页 */
.pagination-area {
    margin-top: 16px;
    display: flex;
    justify-content: flex-end;
}
:deep(.el-pagination) {
    --el-pagination-button-bg-color: #0f172a;
    --el-pagination-hover-color: #38bdf8;
    color: #94a3b8;
}

/* 表单控件暗黑 */
:deep(.dark-form .el-form-item__label) {
    color: #94a3b8 !important;
}
:deep(.el-input__wrapper), :deep(.el-select__wrapper), :deep(.el-input-number__inner) {
    background-color: #0f172a !important;
    box-shadow: 0 0 0 1px #334155 inset !important;
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

/* 按钮全局统一 */
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

/* Dialog 弹窗暗黑 */
:deep(.dark-dialog) {
    background-color: #1e293b !important;
    border: 1px solid #334155;
}
:deep(.dark-dialog .el-dialog__header) {
    border-bottom: 1px solid #334155;
}
:deep(.dark-dialog .el-dialog__title) {
    color: #f8fafc;
}
:deep(.dark-dialog .el-dialog__body) {
    color: #cbd5e1;
}
:deep(.dark-dialog .el-dialog__footer) {
    border-top: 1px solid #334155;
}

/* Radio单选 */
:deep(.el-radio__label) {
    color: #cbd5e1;
}
:deep(.el-radio__inner) {
    background-color: #0f172a;
    border-color: #334155;
}
</style>