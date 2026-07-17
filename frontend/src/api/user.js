// src/api/user.js
import request from '@/utils/request'

/** 获取用户列表（支持分页与按用户名/角色筛选） */
export function getUserList(params) {
    return request.get('/wms/user/list', { params })
}

/** 新增系统用户 */
export function createUser(data) {
    return request.post('/wms/user/add', data)
}

/** 修改系统用户信息 */
export function updateUser(data) {
    return request.put('/wms/user/update', data)
}

/** 删除用户 */
export function deleteUser(id) {
    return request.delete(`/wms/user/delete/${id}`)
}

/** 更改用户启用/禁用状态 */
export function updateUserStatus(id, status) {
    return request.put(`/wms/user/status/${id}`, { status })
}

/** 获取所有角色列表（用于下拉框选择或角色赋权） */
export function getRoleList() {
    return request.get('/wms/role/list')
}

/** 为用户分配角色 */
export function assignUserRoles(userId, roleIds) {
    return request.post('/wms/user/assign-roles', { userId, roleIds })
}