<template>
  <div class="video-grid-container">
    <!-- 空状态 -->
    <div v-if="Object.keys(streams).length === 0" class="empty-state">
      <div class="empty-icon">📹</div>
      <p>暂无视频流，请通过 API 启动推流</p>
      <el-button type="primary" @click="fetchStreams">刷新</el-button>
    </div>

    <!-- 视频网格 -->
    <div v-else class="video-grid">
      <div v-for="(stream, id) in streams" :key="id" class="video-item">
        <div class="video-header">
          <span class="stream-id">{{ id }}</span>
          <div class="stream-status">
            <el-tag :type="stream.status === 'running' ? 'success' : 'danger'" size="small">
              {{ stream.status === 'running' ? '● 推流中' : '○ 已停止' }}
            </el-tag>
            <span class="pid" v-if="stream.pid">PID: {{ stream.pid }}</span>
          </div>
        </div>

        <div class="video-wrapper">
          <VideoPlayer 
            v-if="stream.status === 'running'" 
            :key="id"
            :hls-url="stream.hlsUrl" 
          />
          <div v-else class="placeholder">
            <span>未启动推流</span>
          </div>
        </div>

        <div class="video-controls">
          <el-button 
            size="small" 
            type="primary" 
            @click="startStream(id)" 
            :disabled="stream.status === 'running'"
          >
            启动
          </el-button>
          <el-button 
            size="small" 
            type="danger" 
            @click="stopStream(id)" 
            :disabled="stream.status !== 'running'"
          >
            停止
          </el-button>
          <el-button size="small" @click="copyHlsUrl(id, stream.hlsUrl)">
            复制 HLS
          </el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'
import axios from 'axios'
import { ElMessage } from 'element-plus'
import VideoPlayer from './VideoPlayer.vue'

const streams = ref({})
let timer = null

// 获取所有流状态
const fetchStreams = async () => {
  try {
    const res = await axios.get('/api/video/list')
    if (res.data.code === 0 || res.data.code === 200) {
      streams.value = res.data.data || {}
    }
  } catch (error) {
    console.error('获取流列表失败:', error)
  }
}

// 启动流
const startStream = async (streamId) => {
  try {
    const res = await axios.post('/api/video/start2', null, {
      params: { streamId, rtspUrl: '' }
    })
    if (res.data.code === 0 || res.data.code === 200) {
      ElMessage.success(`${streamId} 启动成功`)
      await fetchStreams()
    } else {
      ElMessage.error(res.data.msg || '启动失败')
    }
  } catch (error) {
    ElMessage.error(error.response?.data?.msg || '启动失败')
  }
}

// 停止流
const stopStream = async (streamId) => {
  try {
    const res = await axios.post('/api/video/stop', null, {
      params: { streamId }
    })
    if (res.data.code === 0 || res.data.code === 200) {
      ElMessage.success(`${streamId} 已停止`)
      await fetchStreams()
    } else {
      ElMessage.error(res.data.msg || '停止失败')
    }
  } catch (error) {
    ElMessage.error(error.response?.data?.msg || '停止失败')
  }
}

// 复制 HLS 地址
const copyHlsUrl = async (streamId, hlsUrl) => {
  if (!hlsUrl) {
    ElMessage.warning('该流未启动，无 HLS 地址')
    return
  }
  try {
    await navigator.clipboard.writeText(hlsUrl)
    ElMessage.success(`已复制 ${streamId} 的 HLS 地址`)
  } catch {
    ElMessage.error('复制失败，请手动复制')
  }
}

// 定时刷新
const startPolling = () => {
  if (timer) clearInterval(timer)
  timer = setInterval(fetchStreams, 3000)
}

// 生命周期
onMounted(() => {
  fetchStreams()
  startPolling()
})

onBeforeUnmount(() => {
  if (timer) clearInterval(timer)
})
</script>

<style scoped>
.video-grid-container {
  padding: 16px;
  background: #0d0d1a;
  min-height: 100vh;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 60vh;
  color: #888;
}
.empty-icon {
  font-size: 64px;
  margin-bottom: 16px;
}
.empty-state p {
  font-size: 16px;
  margin-bottom: 16px;
}

.video-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(400px, 1fr));
  gap: 20px;
}

.video-item {
  background: #1a1a2e;
  border-radius: 12px;
  overflow: hidden;
  padding: 12px;
  border: 1px solid #2a2a4a;
  transition: border-color 0.3s;
}
.video-item:hover {
  border-color: #4a4a8a;
}

.video-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}
.stream-id {
  color: #fff;
  font-weight: 600;
  font-size: 14px;
}
.stream-status {
  display: flex;
  align-items: center;
  gap: 8px;
}
.pid {
  color: #666;
  font-size: 11px;
}

.video-wrapper {
  width: 100%;
  aspect-ratio: 16 / 9;
  background: #000;
  border-radius: 6px;
  overflow: hidden;
}
.video-wrapper video {
  width: 100%;
  height: 100%;
  display: block;
}
.placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #555;
  font-size: 14px;
}

.video-controls {
  display: flex;
  gap: 8px;
  margin-top: 10px;
  flex-wrap: wrap;
}
.video-controls .el-button {
  flex: 1;
  min-width: 60px;
}
</style>