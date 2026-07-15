<template>
  <div class="video-wrapper">
    <video ref="videoRef" controls autoplay muted playsinline style="width:100%;height:100%;"></video>
    <div v-if="loading" class="loading">加载中...</div>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, watch } from 'vue'
import Hls from 'hls.js'

const props = defineProps({
  hlsUrl: { type: String, required: true }
})

const videoRef = ref(null)
let hls = null
const loading = ref(true)

/**
 * 初始化 HLS 播放器（强制使用 HLS.js）
 */
const initHls = () => {
  const video = videoRef.value
  if (!video || !props.hlsUrl) {
    console.warn('VideoPlayer: 缺少 video 元素或 hlsUrl')
    return
  }

  // 销毁旧实例
  if (hls) {
    hls.destroy()
    hls = null
  }

  // 强制使用 HLS.js（跳过 Safari 原生判断，统一行为）
  if (Hls.isSupported()) {
    console.log('VideoPlayer: 使用 HLS.js 播放')
    hls = new Hls({
      debug: true,                // 开启调试日志
      enableWorker: true,
      lowLatencyMode: false,      // 提高兼容性
    })

    hls.loadSource(props.hlsUrl)
    hls.attachMedia(video)

    hls.on(Hls.Events.MANIFEST_PARSED, () => {
      loading.value = false
      video.play().catch((err) => {
        console.warn('VideoPlayer: 自动播放失败，等待用户交互', err)
      })
    })

    hls.on(Hls.Events.ERROR, (event, data) => {
      console.error('VideoPlayer: HLS 错误', data)
      loading.value = false
      if (data.fatal) {
        // 尝试恢复
        hls.recoverMediaError()
      }
    })

    // 增加网络错误重试日志
    hls.on(Hls.Events.LEVEL_LOAD_ERROR, (event, data) => {
      console.warn('VideoPlayer: 加载 M3U8 失败，重试中...')
    })
  } else {
    loading.value = false
    console.error('VideoPlayer: 浏览器不支持 HLS')
  }
}

/**
 * 销毁 HLS 实例
 */
const destroyHls = () => {
  if (hls) {
    hls.destroy()
    hls = null
  }
}

/**
 * 监听 hlsUrl 变化，重新初始化
 */
watch(() => props.hlsUrl, (newUrl, oldUrl) => {
  if (newUrl && newUrl !== oldUrl) {
    loading.value = true
    destroyHls()
    // 下一帧初始化，确保 DOM 已更新
    setTimeout(initHls, 100)
  }
})

// 生命周期
onMounted(() => {
  if (props.hlsUrl) {
    initHls()
  } else {
    console.warn('VideoPlayer: 初始 hlsUrl 为空，等待更新')
  }
})

onBeforeUnmount(() => {
  destroyHls()
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