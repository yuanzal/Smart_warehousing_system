<template>
    <div class="user-management-container">
        <el-card class="query-card" shadow="never">
            <el-form :inline="true" :model="queryParams" class="demo-form-inline" size="default">
                <el-form-item label="用户检索">
                    <el-input v-model="queryParams.username" placeholder="请输入用户名/真实姓名" clearable />
                </el-form-item>
                <el-form-item label="系统角色">
                    <el-select v-model="queryParams.roleId" placeholder="全部角色" clearable style="width: 160px;">
                        <el-option v-for="role in roleOptions" :key="role.id" :label="role.name" :value="role.id" />
                    </el-select>
                </el-form-item>
                <el-form-item label="账户状态">
                    <el-select v-model="queryParams.status" placeholder="全部" clearable style="width: 120px;">
                        <el-option label="正常启用" :value="1" />
                        <el-option label="安全冻结" :value="0" />
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
                <div class="bar-left">
                    <el-button type="primary" plain @click="openUserDialog('add')">➕ 新增管理员</el-button>
                </div>
                <div class="bar-right">
                    <span class="total-text">系统内置安全策略已启用</span>
                </div>
            </div>

            <el-table :data="userList" v-loading="loading" border style="width: 100%" class="dark-table">
                <el-table-column prop="id" label="UID" width="80" align="center" />
                <el-table-column prop="username" label="登录账户" min-width="120" />
                <el-table-column prop="realName" label="真实姓名" min-width="120" />
                <el-table-column prop="roles" label="所属角色" min-width="160">
                    <template #default="scope">
                        <el-tag
                                v-for="role in scope.row.roles"
                                :key="role.id"
                                type="success"
                                effect="dark"
                                class="role-tag"
                        >
                            {{ role.name }}
                        </el-tag>
                    </template>
                </el-table-column>
                <el-table-column prop="mobile" label="手机号码" min-width="120" align="center" />
                <el-table-column prop="tenantId" label="所属租户ID" width="100" align="center" />
                <el-table-column prop="email" label="电子邮箱" min-width="160" show-overflow-tooltip />
                <el-table-column prop="status" label="账户状态" width="100" align="center">
                    <template #default="scope">
                        <el-switch
                                v-model="scope.row.status"
                                :active-value="1"
                                :inactive-value="0"
                                active-color="#10b981"
                                inactive-color="#f43f5e"
                                @change="handleStatusChange(scope.row)"
                        />
                    </template>
                </el-table-column>
                <el-table-column prop="createTime" label="创建时间" width="180" align="center" />
                <el-table-column label="管理操作" width="220" fixed="right" align="center">
                    <template #default="scope">
                        <el-button size="small" type="primary" link @click="openUserDialog('edit', scope.row)">编辑</el-button>
                        <el-button size="small" type="warning" link @click="openPermissionDialog(scope.row)">角色赋权</el-button>
                        <el-button size="small" type="danger" link @click="handleDelete(scope.row)">删除</el-button>
                    </template>
                </el-table-column>
            </el-table>

            <div class="pagination-container">
                <el-pagination
                        v-model:current-page="queryParams.pageNum"
                        v-model:page-size="queryParams.pageSize"
                        :page-sizes="[10, 20, 50]"
                        layout="total, sizes, prev, pager, next, jumper"
                        :total="total"
                        @size-change="handleQuery"
                        @current-change="fetchUserData"
                />
            </div>
        </el-card>

        <el-dialog
                v-model="userDialog.visible"
                :title="userDialog.type === 'add' ? '新增系统管理员' : '编辑管理员信息'"
                width="500px"
                destroy-on-close
                class="dark-dialog"
        >
            <el-form :model="userForm" :rules="userRules" ref="userFormRef" label-width="90px" label-position="left">
                <el-form-item label="登录账户" prop="username">
                    <el-input v-model="userForm.username" :disabled="userDialog.type === 'edit'" placeholder="请输入登录账户名" />
                </el-form-item>
                <el-form-item label="真实姓名" prop="realName">
                    <el-input v-model="userForm.realname" placeholder="请输入使用者姓名" />
                </el-form-item>
                <el-form-item label="登录密码" prop="password" :required="userDialog.type === 'add'">
                    <el-input
                            v-model="userForm.password"
                            type="password"
                            show-password
                            :placeholder="userDialog.type === 'add' ? '请输入初始密码' : '留空则代表不修改密码'"
                    />
                </el-form-item>
                <el-form-item label="电子邮箱" prop="email">
                    <el-input v-model="userForm.email" placeholder="请输入联系邮箱" />
                </el-form-item>
                <el-form-item label="手机号码" prop="mobile">
                    <el-input v-model="userForm.mobile" placeholder="请输入联系手机号" />
                </el-form-item>
                <el-form-item
                    v-if="currentLoginUser.tenantId === 0"
                    label="所属租户"
                    prop="tenantId"
                    :rules="[{ required: true, message: '请选择或输入所属租户ID', trigger: 'change' }]"
                >
                    <el-input-number
                        v-model="userForm.tenantId"
                        :min="0"
                        placeholder="请输入目标租户ID"
                        controls-position="right"
                        style="width: 100%;"
                    />
                </el-form-item>
                <el-form-item label="账户状态" prop="status">
                    <el-radio-group v-model="userForm.status">
                        <el-radio :label="1">启用账户</el-radio>
                        <el-radio :label="0">冻结锁定</el-radio>
                    </el-radio-group>
                </el-form-item>
            </el-form>
            <template #footer>
        <span class="dialog-footer">
          <el-button @click="userDialog.visible = false">取 消</el-button>
          <el-button type="primary" :loading="submitLoading" @click="submitUserForm">确 定</el-button>
        </span>
            </template>
        </el-dialog>

        <el-dialog
                v-model="permDialog.visible"
                :title="`变更 [${permDialog.username}] 的安全角色`"
                width="450px"
                class="dark-dialog"
        >
            <div class="perm-transfer-box">
                <p class="perm-tip">请勾选该用户在WMS系统中所具备的调度与管理权限角色：</p>
                <el-checkbox-group v-model="permDialog.checkedRoles">
                    <div v-for="role in roleOptions" :key="role.id" class="role-checkbox-item">
                        <el-checkbox :label="role.id">
                            <span class="role-name-text">{{ role.name }}</span>
                            <span class="role-desc-text">（{{ role.description || '暂无安全描述' }}）</span>
                        </el-checkbox>
                    </div>
                </el-checkbox-group>
            </div>
            <template #footer>
        <span class="dialog-footer">
          <el-button @click="permDialog.visible = false">取 消</el-button>
          <el-button type="primary" :loading="submitLoading" @click="submitRoleAssignment">确认赋权</el-button>
        </span>
            </template>
        </el-dialog>
    </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
    getUserList, createUser, updateUser, deleteUser,
    updateUserStatus, getRoleList, assignUserRoles
} from '@/api/user'
import * as Lockr from 'lockr' //  确保在 <script setup> 顶部引入了 Lockr

// 数据加载控制
const loading = ref(false)
const submitLoading = ref(false)
const total = ref(0)
const userList = ref([])
const roleOptions = ref([])

// 搜索过滤参数
const queryParams = reactive({
    pageNum: 1,
    pageSize: 10,
    username: '',
    mobile:'',
    tenantId:null,
    roleId: null,
    status: null
})

// 用户增删改表单及验证
const userFormRef = ref(null)
const userForm = reactive({
    id: null,
    username: '',
    realname: '',
    password: '',
    email: '',
    mobile:'',
    tenantId: null,
    status: 1
})

const userRules = {
    username: [{ required: true, message: '登录账号不能为空', trigger: 'blur' }],
    realname: [{ required: true, message: '真实姓名不能为空', trigger: 'blur' }],
    email: [{ type: 'email', message: '请输入正确的邮箱地址', trigger: ['blur', 'change'] }],
    mobile: [
        { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的 11 位手机号码', trigger: 'blur' }
    ]
}

// 弹窗状态管理
const userDialog = reactive({ visible: false, type: 'add' })
const permDialog = reactive({ visible: false, userId: null, username: '', checkedRoles: [] })


//  1. 尝试从 Lockr 或者 原生 localStorage 获取用户信息
const getLocalUserInfo = () => {
    // 优先尝试从 Lockr 获取 'userInfo' 对象
    let user = Lockr.get('userInfo')

    // 如果 Lockr 没拿到，尝试从原生 localStorage 获取
    if (!user) {
        const raw = localStorage.getItem('userInfo')
        if (raw) {
            try {
                user = JSON.parse(raw)
            } catch (e) {
                console.error('解析本地 userInfo 失败', e)
            }
        }
    }

    // 如果最终都拿不到（比如用户未登录或缓存清空了），提供安全的默认兜底
    // 注意：默认值写 "tenantId: 1" 代表普通租户管理员，防止误给 0（超管特权）
    return user || { tenantId: 1 }
}

// 💡 2. 使用 reactive 包装，使其具有响应式
const currentLoginUser = reactive(getLocalUserInfo())

/** 加载表格数据 */
const fetchUserData = async () => {
    loading.value = true
    try {
        const res = await getUserList(queryParams)
        if (res.code === 0) {
            userList.value = res.data.list || []
            total.value = res.data.total || 0
        }
    } catch (err) {
        console.error('拉取用户列表失败', err)
    } finally {
        loading.value = false
    }
}

/** 加载所有角色选项 */
const fetchRoleData = async () => {
    try {
        const res = await getRoleList()
        if (res.code === 0) {
            roleOptions.value = res.data || []
        }
    } catch (err) {
        console.error('加载系统角色失败', err)
    }
}

/** 查询与重置 */
const handleQuery = () => {
    queryParams.pageNum = 1
    fetchUserData()
}
const resetQuery = () => {
    queryParams.username = ''
    queryParams.roleId = null
    queryParams.status = null
    handleQuery()
}

/** 打开新增/编辑弹窗 */
const openUserDialog = (type, row = null) => {
    userDialog.type = type
    userDialog.visible = true
    if (type === 'edit' && row) {
        userForm.id = row.id
        userForm.username = row.username
        userForm.realname = row.realName
        userForm.email = row.email
        userForm.mobile = row.mobile || ''      // 💡 新增：回显手机号
        userForm.tenantId = row.tenantId        // 💡 新增：回显租户ID
        userForm.status = row.status
        userForm.password = '' // 密码编辑时留空
    } else {
        // 🌟 新增模式：初始化表单数据
        userForm.id = null
        userForm.username = ''
        userForm.realname = ''
        userForm.email = ''
        userForm.mobile = ''                   // 💡 新增：清空手机号
        userForm.status = 1
        userForm.password = ''

        // 💡 新增：根据登录人身份初始化租户 ID
        if (currentLoginUser.tenantId === 0) {
            // 如果是平台超级管理员，让输入框亮起，初始值设为 null 让超管手动填写
            userForm.tenantId = null
        } else {
            // 如果是普通租户管理员，输入框会隐藏，直接塞入他自己的租户ID，随表单一起提交
            userForm.tenantId = currentLoginUser.tenantId
        }
    }
}

/** 提交用户表单 */
const submitUserForm = async () => {
    if (!userFormRef.value) return
    await userFormRef.value.validate(async (valid) => {
        if (valid) {
            submitLoading.value = true
            try {
                if (userDialog.type === 'add') {
                    await createUser(userForm)
                    ElMessage.success('系统管理员创建成功')
                } else {
                    await updateUser(userForm)
                    ElMessage.success('管理员信息更新成功')
                }
                userDialog.visible = false
                fetchUserData()
            } catch (err) {
                console.error(err)
            } finally {
                submitLoading.value = false
            }
        }
    })
}

/** 账户状态拦截切换 */
const handleStatusChange = async (row) => {
    const text = row.status === 1 ? '启用' : '冻结'
    try {
        await ElMessageBox.confirm(`确定要${text}该管理员账户吗？冻结后其将失去一切WMS系统访问权。`, '系统安全警告', {
            confirmButtonText: '确定',
            cancelButtonText: '取消',
            type: 'warning'
        })
        await updateUserStatus(row.id, row.status)
        ElMessage.success(`账户状态已成功变更为【${text}】`)
    } catch (err) {
        // 撤销前端状态的强行切换
        row.status = row.status === 1 ? 0 : 1
    }
}

/** 打开权限分配弹窗 */
const openPermissionDialog = (row) => {
    permDialog.userId = row.id
    permDialog.username = row.username
    // 将用户当前拥有的角色ID提取出来放入数组
    permDialog.checkedRoles = row.roles ? row.roles.map(r => r.id) : []
    permDialog.visible = true
}

/** 提交角色分配 */
const submitRoleAssignment = async () => {
    submitLoading.value = true
    try {
        await assignUserRoles(permDialog.userId, permDialog.checkedRoles)
        ElMessage.success(`成功为用户 [${permDialog.username}] 更新安全角色授权`)
        permDialog.visible = false
        fetchUserData() // 刷新带出最新的角色标签
    } catch (err) {
        console.error(err)
    } finally {
        submitLoading.value = false
    }
}

/** 删除操作 */
const handleDelete = async (row) => {
    try {
        await ElMessageBox.confirm(`此操作将永久注销账户 ${row.username}，是否继续？`, '高危操作提示', {
            confirmButtonText: '确定',
            cancelButtonText: '取消',
            type: 'error'
        })
        await deleteUser(row.id)
        ElMessage.success('账户已被安全移出系统')
        fetchUserData()
    } catch (err) {
        console.log('取消删除')
    }
}

onMounted(() => {
    fetchUserData()
    fetchRoleData()
})
</script>

<style scoped>
.user-management-container {
    padding: 4px;
    display: flex;
    flex-direction: column;
    gap: 16px;
}

.query-card, .table-card {
    background-color: #1e293b !important;
    border: 1px solid #334155 !important;
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

.table-tool-bar {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 16px;
}

.total-text {
    font-size: 13px;
    color: #64748b;
}

.role-tag {
    margin-right: 6px;
}

.pagination-container {
    margin-top: 20px;
    display: flex;
    justify-content: flex-end;
}

/* 弹窗样式优化 */
:deep(.dark-dialog) {
    background-color: #1e293b !important;
    border: 1px solid #475569;
}
:deep(.dark-dialog .el-dialog__title) {
    color: #38bdf8 !important;
}

.perm-transfer-box {
    padding: 10px 0;
}
.perm-tip {
    font-size: 13px;
    color: #94a3b8;
    margin-bottom: 16px;
}
.role-checkbox-item {
    margin-bottom: 12px;
}
.role-name-text {
    color: #f8fafc;
    font-weight: bold;
}
.role-desc-text {
    color: #64748b;
    font-size: 12px;
}
</style>