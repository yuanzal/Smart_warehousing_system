<template>
  <div class="device-management">
    <div class="header">
      <h2>设备管理</h2>
      <el-button type="primary" @click="fetchDevices" :loading="loading">
        刷新
      </el-button>
    </div>

    <el-table :data="deviceList" style="width: 100%" v-loading="loading">
      <el-table-column prop="deviceId" label="设备ID" width="150" />
      <el-table-column prop="deviceTypeName" label="设备类型" width="120" />
      <el-table-column label="位置信息" min-width="150">
        <template #default="{ row }">
          <span v-if="row.deviceType === 2">
            X: {{ row.data?.x?.toFixed(2) }}, Z: {{ row.data?.z?.toFixed(2) }}
          </span>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column prop="timestamp" label="最后上报时间" width="180">
        <template #default="{ row }">
          {{ formatTimestamp(row.timestamp) }}
        </template>
      </el-table-column>
      <el-table-column label="在线状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.online ? 'success' : 'danger'">
            {{ row.online ? '在线' : '离线' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="120">
        <template #default="{ row }">
          <el-button type="text" @click="showDetail(row)">查看详情</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 详情对话框 -->
    <el-dialog v-model="dialogVisible" title="设备详情" width="600px">
      <pre>{{ JSON.stringify(selectedDevice, null, 2) }}</pre>
      <template #footer>
        <el-button @click="dialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'
import axios from 'axios'
import { ElMessage } from 'element-plus'

// 设备类型映射
const deviceTypeMap = {
  1: '摄像头',
  2: 'AGV小车',
  3: '环境传感器',
  4: '边缘网关',
  5: 'PLC控制器'
}

const deviceList = ref([])
const loading = ref(false)
const dialogVisible = ref(false)
const selectedDevice = ref(null)

let timer = null

// 格式化时间戳
const formatTimestamp = (ts) => {
  if (!ts) return '-'
  const date = new Date(ts)
  return date.toLocaleString('zh-CN')
}

// 获取设备列表
const fetchDevices = async () => {
  loading.value = true
  try {
    const res = await axios.get('/api/device/status/all')
    if (res.data.code === 200 || res.data.code === 0) {
      const data = res.data.data
      const list = Object.keys(data).map(key => {
        const raw = JSON.parse(data[key])
        return {
          ...raw,
          deviceId: key,
          deviceTypeName: deviceTypeMap[raw.deviceType] || '未知',
          online: raw.timestamp && (Date.now() - raw.timestamp) < 5 * 60 * 1000
        }
      })
      deviceList.value = list
    } else {
      ElMessage.error(res.data.msg || '获取设备列表失败')
    }
  } catch (error) {
    console.error('获取设备列表失败:', error)
    ElMessage.error('获取设备列表失败')
  } finally {
    loading.value = false
  }
}

// 查看详情
const showDetail = (row) => {
  selectedDevice.value = row
  dialogVisible.value = true
}

// 定时刷新
const startPolling = () => {
  if (timer) clearInterval(timer)
  timer = setInterval(fetchDevices, 5000)
}

// 生命周期
onMounted(() => {
  fetchDevices()
  startPolling()
})

onBeforeUnmount(() => {
  if (timer) clearInterval(timer)
})
</script>

<style scoped>
.device-management {
  padding: 20px;
}
.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}
</style>