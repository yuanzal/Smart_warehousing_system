<template>
  <div class="video-wrapper">
    <!-- 视频元素 -->
    <video
      ref="videoRef"
      controls
      autoplay
      muted
      playsinline
      style="width:100%;height:100%;"
    ></video>

    <!-- Canvas 叠加层（AI 检测框） -->
    <canvas ref="canvasRef" class="overlay-canvas"></canvas>

    <!-- 加载状态 -->
    <div v-if="loading" class="loading">加载中...</div>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, watch, nextTick } from 'vue'
import Hls from 'hls.js'

// ===== Props =====
const props = defineProps({
  hlsUrl: { type: String, required: true },
  aiVideoSource: { type: String, default: '' },
  confThreshold: { type: Number, default: 0.3 },
  intervalSec: { type: Number, default: 1.0 },
})

// ===== 响应式状态 =====
const videoRef = ref(null)
const canvasRef = ref(null)
const loading = ref(true)
let hls = null
let ws = null
let reconnectTimer = null   // 重连定时器

// ========== HLS 播放 ==========
const initHls = () => {
  const video = videoRef.value
  if (!video || !props.hlsUrl) {
    console.warn('VideoPlayer: 缺少 video 元素或 hlsUrl')
    return
  }
  if (hls) {
    hls.destroy()
    hls = null
  }
  if (Hls.isSupported()) {
    console.log('VideoPlayer: 使用 HLS.js 播放')
    hls = new Hls({
      debug: true,
      enableWorker: true,
      lowLatencyMode: false,
      maxBufferLength: 30,          // 增大缓冲，减少卡顿
      maxMaxBufferLength: 60,
    })
    hls.loadSource(props.hlsUrl)
    hls.attachMedia(video)
    hls.on(Hls.Events.MANIFEST_PARSED, () => {
      loading.value = false
      video.play().catch(() => {})
    })
    hls.on(Hls.Events.ERROR, (event, data) => {
      console.error('VideoPlayer: HLS 错误', data)
      loading.value = false
      if (data.fatal) hls.recoverMediaError()
    })
  } else {
    loading.value = false
    console.error('VideoPlayer: 浏览器不支持 HLS')
  }
}

const destroyHls = () => {
  if (hls) {
    hls.destroy()
    hls = null
  }
}

// ========== AI 检测绘制 ==========
const drawBoxes = (boxes) => {
  const canvas = canvasRef.value
  const video = videoRef.value
  if (!canvas || !video || !video.videoWidth) {
    console.warn('drawBoxes: canvas/video 未就绪')
    return
  }
  const ctx = canvas.getContext('2d')
  canvas.width = video.videoWidth
  canvas.height = video.videoHeight
  ctx.clearRect(0, 0, canvas.width, canvas.height)

  console.log(`🎯 绘制 ${boxes.length} 个检测框`)
  boxes.forEach(box => {
    const x = box.x * canvas.width
    const y = box.y * canvas.height
    const w = box.width * canvas.width
    const h = box.height * canvas.height
    ctx.strokeStyle = '#00ff00'
    ctx.lineWidth = 3
    ctx.strokeRect(x, y, w, h)
    ctx.fillStyle = '#00ff00'
    ctx.font = '16px Arial'
    ctx.fillText(`${box.class_name} ${(box.confidence * 100).toFixed(1)}%`, x, y - 6)
  })
}

// ========== AI WebSocket（强制连接 + 自动重连） ==========
const initAiWebSocket = () => {
  // 清除已有重连定时器（避免重连叠加）
  if (reconnectTimer) {
    clearTimeout(reconnectTimer)
    reconnectTimer = null
  }

  // 强制使用视频源（忽略 enableAi）
  const videoSource = props.aiVideoSource || 'C:/Users/12256/Downloads/Test Jellyfin 1080p AVC 3M.mp4'
  if (!videoSource) {
    console.log('❌ 无视频源，跳过 AI')
    return
  }

  if (ws) {
    ws.close()
    ws = null
  }

  console.log('🔗 正在连接 AI WebSocket（强制模式）...')
  ws = new WebSocket('ws://localhost:8001/ws/detect')

  ws.onopen = () => {
    console.log('✅ AI WebSocket 已连接')
    ws.send(JSON.stringify({
      video_source: videoSource,
      interval_sec: props.intervalSec,
      conf_threshold: props.confThreshold,
    }))
  }

  ws.onmessage = (event) => {
    console.log('📨 收到检测数据:', event.data)
    try {
      const data = JSON.parse(event.data)
      if (data.boxes) {
        drawBoxes(data.boxes)
      } else if (data.status === 'finished') {
        console.log('🏁 AI 检测完成（视频结束）')
      }
    } catch (e) {
      console.error('解析失败:', e)
    }
  }

  ws.onerror = (err) => {
    console.error('❌ WebSocket 错误:', err)
  }

  ws.onclose = () => {
    console.log('AI WebSocket 已断开，3秒后自动重连...')
    // 启动重连定时器（如果视频源存在）
    if (props.aiVideoSource) {
      reconnectTimer = setTimeout(() => {
        console.log('🔄 执行 AI WebSocket 重连...')
        initAiWebSocket()
      }, 3000)
    }
  }
}

const destroyAiWebSocket = () => {
  // 清除重连定时器
  if (reconnectTimer) {
    clearTimeout(reconnectTimer)
    reconnectTimer = null
  }
  if (ws) {
    ws.close()
    ws = null
  }
}

// ========== 监听变化 ==========
watch(() => props.hlsUrl, (newUrl, oldUrl) => {
  if (newUrl && newUrl !== oldUrl) {
    loading.value = true
    destroyHls()
    setTimeout(initHls, 100)
  }
})

watch(() => props.aiVideoSource, (newVal) => {
  destroyAiWebSocket()
  if (newVal && newVal.trim() !== '') {
    nextTick(() => initAiWebSocket())
  }
})

// ========== 生命周期 ==========
onMounted(() => {
  if (props.hlsUrl) {
    initHls()
  } else {
    console.warn('VideoPlayer: 初始 hlsUrl 为空，等待更新')
  }
  // 强制初始化 AI（忽略 enableAi 条件）
  nextTick(() => {
    initAiWebSocket()
  })
})

onBeforeUnmount(() => {
  destroyHls()
  destroyAiWebSocket()
})
</script>

<style scoped>
.video-wrapper {
  position: relative;
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
.overlay-canvas {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
}
.loading {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  color: #fff;
  font-size: 14px;
  background: rgba(0,0,0,0.6);
  padding: 8px 16px;
  border-radius: 4px;
}
</style>