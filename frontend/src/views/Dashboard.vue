<template>
    <div class="dashboard-scene-wrapper">
        <div class="scene-overlay-card">
            <h3>🛸 智能仓库 3D 数字孪生大屏</h3>
            <p>集成多源传感器与AGV实时定位上报。当前坐标跟踪就绪。</p>
        </div>

        <ThreeScene ref="threeSceneRef" class="three-viewport" />

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

        <div class="video-section">
            <div class="video-header">
                <span class="video-title">📹 实时监控</span>
                <div class="video-controls">
                    <button
                        v-if="!isRunning"
                        class="btn-start"
                        @click="startPush"
                        :disabled="videoLoading"
                    >
                        {{ videoLoading ? '启动中...' : '启动推流' }}
                    </button>
                    <button
                        v-else
                        class="btn-stop"
                        @click="stopPush"
                        :disabled="videoLoading"
                    >
                        {{ videoLoading ? '停止中...' : '停止推流' }}
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
import { ElMessage } from 'element-plus'
import ThreeScene from '../components/ThreeScene.vue'
import VideoPlayer from '../components/VideoPlayer.vue'

const threeSceneRef = ref(null)
const hlsUrl = ref('')
const isRunning = ref(false)
const videoLoading = ref(false)
const streamId = ref('test')
const HLS_BASE_URL = 'http://localhost:8081/hls/'
let statusTimer = null

// ===== 获取推流状态 =====
const fetchStatus = async () => {
    try {
        const res = await axios.get('/api/video/status', {
            params: { streamId: streamId.value }
        })
        if (res.data.code === 0 || res.data.code === 200) {
            const data = res.data.data
            isRunning.value = data.status === 'running'
            hlsUrl.value = isRunning.value ? `${HLS_BASE_URL}${streamId.value}.m3u8` : ''
        }
    } catch (error) {
        console.error('获取视频状态失败:', error)
    }
}

// ===== 启动与停止推流 =====
const startPush = async () => {
    videoLoading.value = true
    try {
        const res = await axios.post('/api/video/start2', null, {
            params: { streamId: streamId.value, rtspUrl: '' }
        })
        if (res.data.code === 0 || res.data.code === 200) {
            isRunning.value = true
            hlsUrl.value = `${HLS_BASE_URL}${streamId.value}.m3u8`
            ElMessage.success('推流启动成功')
        } else {
            ElMessage.error('启动失败: ' + (res.data.msg || '未知错误'))
        }
    } catch (error) {
        ElMessage.error('启动推流网络异常')
    } finally {
        videoLoading.value = false
    }
}

const stopPush = async () => {
    videoLoading.value = true
    try {
        const res = await axios.post('/api/video/stop', null, {
            params: { streamId: streamId.value }
        })
        if (res.data.code === 0 || res.data.code === 200) {
            isRunning.value = false
            hlsUrl.value = ''
            ElMessage.success('推流已停止')
        }
    } catch (error) {
        console.error('停止推流失败:', error)
    } finally {
        videoLoading.value = false
    }
}

// ===== 3D 操作 =====
const switchFloor = (floor) => {
    threeSceneRef.value?.switchFloor(floor)
}
const flyToView = (view) => {
    threeSceneRef.value?.flyToView(view)
}

// ===== 生命周期轮询管理 =====
onMounted(() => {
    fetchStatus()
    statusTimer = setInterval(fetchStatus, 3000)
})

onBeforeUnmount(() => {
    if (statusTimer) clearInterval(statusTimer)
})
</script>

<style scoped>
.dashboard-scene-wrapper {
    position: relative;
    width: 100%;
    height: 100%;
    border-radius: 8px;
    overflow: hidden;
    border: 1px solid #334155;
    background-color: #0b0f19;
}
.three-viewport {
    width: 100%;
    height: 100%;
}
.scene-overlay-card {
    position: absolute;
    top: 20px;
    left: 20px;
    z-index: 20;
    background-color: rgba(15, 23, 42, 0.85);
    padding: 16px 24px;
    border-radius: 8px;
    border: 1px solid #38bdf8;
    box-shadow: 0 4px 12px rgba(0,0,0,0.5);
    pointer-events: none;
}
.scene-overlay-card h3 {
    margin: 0 0 6px 0;
    color: #38bdf8;
}
.scene-overlay-card p {
    margin: 0;
    font-size: 12px;
    color: #94a3b8;
}
.controls {
    position: absolute;
    top: 130px;
    left: 20px;
    display: flex;
    flex-direction: column;
    gap: 10px;
    z-index: 20;
    background: rgba(15, 23, 42, 0.85);
    padding: 12px 15px;
    border-radius: 8px;
    border: 1px solid #334155;
    box-shadow: 0 4px 12px rgba(0,0,0,0.5);
}
.floor-btns, .view-btns {
    display: flex;
    gap: 8px;
}
.controls button {
    background: #1e293b;
    border: 1px solid #334155;
    color: #f8fafc;
    padding: 6px 14px;
    border-radius: 4px;
    cursor: pointer;
    font-size: 13px;
    transition: all 0.2s;
}
.controls button:hover {
    background: #38bdf8;
    color: #0f172a;
    border-color: #38bdf8;
}
.video-section {
    position: absolute;
    bottom: 20px;
    right: 20px;
    width: 360px;
    background: rgba(15, 23, 42, 0.9);
    border-radius: 12px;
    padding: 12px;
    backdrop-filter: blur(8px);
    border: 1px solid #334155;
    z-index: 20;
    box-shadow: 0 8px 32px rgba(0, 0, 0, 0.6);
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
    background: #38bdf8;
    color: #0f172a;
    font-weight: bold;
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
    background: #10b981;
}
.status-tag.stopped {
    background: #475569;
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
    color: #94a3b8;
    font-size: 14px;
}
.video-placeholder .hint {
    font-size: 12px;
    color: #64748b;
    margin-top: 4px;
}
</style>