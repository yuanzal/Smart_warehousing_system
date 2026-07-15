<template>
  <div class="dashboard">
    <!-- ===== 3D 场景 ===== -->
    <ThreeScene ref="threeSceneRef" />

    <!-- ===== 控制面板（楼层切换 + 视角切换） ===== -->
    <div class="controls">
      <div class="floor-btns">
        <button v-for="f in [1,2,3]" :key="f" @click="switchFloor(f)">楼层 {{ f }}</button>
      </div>
      <div class="view-btns">
        <button @click="flyToView('top')">俯视图</button>
        <button @click="flyToView('front')">正面</button>
        <button @click="flyToView('side')">侧面</button>
      </div>
    </div>

    <!-- ===== 视频监控区域（含启动/停止推流按钮） ===== -->
    <div class="video-section">
      <div class="video-header">
        <span class="video-title">📹 实时监控</span>
        <div class="video-controls">
          <button 
            v-if="!isRunning" 
            class="btn-start" 
            @click="startPush"
            :disabled="loading"
          >
            {{ loading ? '启动中...' : '启动推流' }}
          </button>
          <button 
            v-else 
            class="btn-stop" 
            @click="stopPush"
            :disabled="loading"
          >
            {{ loading ? '停止中...' : '停止推流' }}
          </button>
          <span class="status-tag" :class="isRunning ? 'running' : 'stopped'">
            {{ isRunning ? '● 推流中' : '○ 已停止' }}
          </span>
        </div>
      </div>
      <div class="video-wrapper">
        <VideoPlayer 
          v-if="hlsUrl" 
          :key="hlsUrl"
          :hls-url="hlsUrl" 
        />
        <div v-else class="video-placeholder">
          <span>未启动推流</span>
          <span class="hint">点击「启动推流」开始播放测试视频</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'
import axios from 'axios'
import ThreeScene from '@/components/ThreeScene.vue'
import VideoPlayer from '@/components/VideoPlayer.vue'

const threeSceneRef = ref(null)

// ===== 状态 =====
const hlsUrl = ref('')
const isRunning = ref(false)
const loading = ref(false)
const streamId = ref('test')
const HLS_BASE_URL = 'http://localhost:8081/hls/'

// ===== 获取推流状态 =====
const fetchStatus = async () => {
  try {
    const res = await axios.get('/api/video/status', {
      params: { streamId: streamId.value }
    })
    if (res.data.code === 0 || res.data.code === 200) {
      const data = res.data.data
      isRunning.value = data.status === 'running'
      if (isRunning.value) {
        hlsUrl.value = `${HLS_BASE_URL}${streamId.value}.m3u8`
      } else {
        hlsUrl.value = ''
      }
    }
  } catch (error) {
    console.error('获取视频状态失败:', error)
  }
}

// ===== 启动推流 =====
const startPush = async () => {
  console.log('=== startPush 被调用 ===')
  loading.value = true
  try {
    const res = await axios.post('/api/video/start2', null, {
      params: { streamId: streamId.value, rtspUrl: '' }
    })
    if (res.data.code === 0 || res.data.code === 200) {
      const data = res.data.data
      isRunning.value = true
      hlsUrl.value = `${HLS_BASE_URL}${streamId.value}.m3u8`
      console.log('推流启动成功')
    } else {
      console.error('启动失败:', res.data.msg)
      alert('启动失败: ' + (res.data.msg || '未知错误'))
    }
  } catch (error) {
    console.error('启动推流失败:', error)
    alert('启动推流失败: ' + (error.response?.data?.msg || error.message || '网络错误'))
  } finally {
    loading.value = false
  }
}

// ===== 停止推流 =====
const stopPush = async () => {
  console.log('=== stopPush 被调用 ===')
  loading.value = true
  try {
    const res = await axios.post('/api/video/stop', null, {
      params: { streamId: streamId.value }
    })
    if (res.data.code === 0 || res.data.code === 200) {
      isRunning.value = false
      hlsUrl.value = ''
      console.log('推流已停止')
    } else {
      console.error('停止失败:', res.data.msg)
    }
  } catch (error) {
    console.error('停止推流失败:', error)
  } finally {
    loading.value = false
  }
}

// ===== 定时轮询 =====
let statusTimer = null
const startStatusPolling = () => {
  if (statusTimer) clearInterval(statusTimer)
  statusTimer = setInterval(fetchStatus, 3000)
}

// ===== 3D 控制方法 =====
const switchFloor = (floor) => {
  threeSceneRef.value?.switchFloor(floor)
}
const flyToView = (view) => {
  threeSceneRef.value?.flyToView(view)
}

// ===== 生命周期 =====
onMounted(() => {
  fetchStatus()
  startStatusPolling()
})

onBeforeUnmount(() => {
  if (statusTimer) clearInterval(statusTimer)
})
</script>

<style scoped>
.dashboard {
  position: relative;
  width: 100%;
  height: 100vh;
  overflow: hidden;
}

/* 左上角控制面板 */
.controls {
  position: absolute;
  top: 20px;
  left: 20px;
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  z-index: 10;
  background: rgba(0, 0, 0, 0.6);
  padding: 10px 15px;
  border-radius: 8px;
}
.controls button {
  background: #3b82f6;
  border: none;
  color: white;
  padding: 6px 14px;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
  transition: background 0.2s;
}
.controls button:hover {
  background: #2563eb;
}

/* 右下角视频区域 */
.video-section {
  position: absolute;
  bottom: 30px;
  right: 30px;
  width: 360px;
  background: rgba(0, 0, 0, 0.8);
  border-radius: 12px;
  padding: 12px;
  backdrop-filter: blur(8px);
  border: 1px solid rgba(255, 255, 255, 0.1);
  z-index: 20;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.5);
}
.video-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}
.video-title {
  color: #fff;
  font-size: 14px;
  font-weight: 600;
}
.video-controls {
  display: flex;
  align-items: center;
  gap: 8px;
}
.video-controls button {
  padding: 4px 12px;
  border: none;
  border-radius: 4px;
  font-size: 12px;
  cursor: pointer;
  transition: opacity 0.2s;
}
.video-controls button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
.btn-start {
  background: #3b82f6;
  color: white;
}
.btn-stop {
  background: #ef4444;
  color: white;
}
.status-tag {
  font-size: 12px;
  padding: 2px 8px;
  border-radius: 12px;
  color: #fff;
}
.status-tag.running {
  background: #22c55e;
}
.status-tag.stopped {
  background: #6b7280;
}
.video-wrapper {
  width: 100%;
  aspect-ratio: 16 / 9;
  background: #000;
  border-radius: 6px;
  overflow: hidden;
}
.video-placeholder {
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  width: 100%;
  height: 100%;
  color: #aaa;
  font-size: 14px;
}
.video-placeholder .hint {
  font-size: 12px;
  color: #666;
  margin-top: 4px;
}
</style>