<template>
  <div ref="container" class="three-container"></div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'
import * as THREE from 'three'
import { OrbitControls } from 'three/addons/controls/OrbitControls.js'
import { CSS2DRenderer, CSS2DObject } from 'three/addons/renderers/CSS2DRenderer.js'
import axios from 'axios'

const container = ref(null)
let scene, camera, renderer, controls, labelRenderer
let slotMeshes = []
let refreshInterval = null
let ws = null
let animationFrameId = null //  新增：用于记录 requestAnimationFrame 的 ID
const raycaster = new THREE.Raycaster()
const pointer = new THREE.Vector2()
let clickableMeshes = []
let hoveredObject = null

let floorGroups = {}
let currentFloor = 1
let labelMeshes = []
let agvMeshes = []
let agvPollTimer = null

// 视角动画状态
let animating = false
let animStartPos = null
let animTargetPos = null
let animStartTarget = null
let animEndTarget = null
let animStartTime = 0
const animDuration = 1000

const statusColors = {
  0: 0x00ff88,
  1: 0xff4444,
  2: 0xffaa00,
  3: 0x888888
}

// ---------- 初始化 ----------
const initScene = () => {
  const width = container.value.clientWidth
  const height = container.value.clientHeight

  // 场景
  scene = new THREE.Scene()
  scene.background = new THREE.Color(0x111122)

  // 相机
  camera = new THREE.PerspectiveCamera(45, width / height, 0.1, 1000)
  camera.position.set(30, 20, 30)
  camera.lookAt(0, 0, 0)

  // 渲染器
  renderer = new THREE.WebGLRenderer({ antialias: true })
  renderer.setSize(width, height)
  renderer.shadowMap.enabled = true
  container.value.appendChild(renderer.domElement)

  // ===== 新增：CSS2D 渲染器（用于文本标签） =====
  labelRenderer = new CSS2DRenderer()
  labelRenderer.setSize(width, height)
  labelRenderer.domElement.style.position = 'absolute'
  labelRenderer.domElement.style.top = '0px'
  labelRenderer.domElement.style.left = '0px'
  labelRenderer.domElement.style.pointerEvents = 'none'
  container.value.appendChild(labelRenderer.domElement)

  // 轨道控制器
  controls = new OrbitControls(camera, renderer.domElement)
  controls.enableDamping = true
  controls.target.set(0, 0, 0)

  // 灯光
  const ambientLight = new THREE.AmbientLight(0x404060)
  scene.add(ambientLight)

  const dirLight = new THREE.DirectionalLight(0xffffff, 1)
  dirLight.position.set(20, 30, 10)
  dirLight.castShadow = true
  scene.add(dirLight)

  // 辅助地面（灰色网格）
  const gridHelper = new THREE.GridHelper(40, 20, 0x8888ff, 0x444466)
  scene.add(gridHelper)

  // ===== 新增：点击事件 =====
  container.value.addEventListener('click', onPointerClick)
  // ===== 新增：鼠标移动事件（货位高亮） =====
  container.value.addEventListener('mousemove', onPointerMove)

  // 加载货位数据
  fetchSlots()
  // ===== 新增：加载 AGV 数据 =====
  fetchAgvData()
  // 启动 AGV 定时轮询（如果 WebSocket 未连接）
  startAgvPolling()
}

// ---------- AGV 小车 ----------
const createAGV = (color = 0x00aaff) => {
  const group = new THREE.Group()
  
  // 车身
  const bodyGeo = new THREE.BoxGeometry(1.2, 0.4, 0.8)
  const bodyMat = new THREE.MeshStandardMaterial({ color })
  const body = new THREE.Mesh(bodyGeo, bodyMat)
  body.position.y = 0.2
  group.add(body)

  const lightGeo = new THREE.SphereGeometry(0.15, 8, 8)
  const lightMat = new THREE.MeshStandardMaterial({ color: 0xffaa00, emissive: 0xffaa00, emissiveIntensity: 0.5 })
  const light = new THREE.Mesh(lightGeo, lightMat)
  light.position.set(0, 0.5, 0)
  group.add(light)
  
  return group
}

// ---------- 标签 ----------
const addLabel = (slot) => {
  const div = document.createElement('div')
  div.textContent = slot.id
  // ===== 优化：标签样式调整 =====
  div.style.color = '#ffffff'
  div.style.fontSize = '14px'
  div.style.fontWeight = 'bold'
  div.style.textShadow = '1px 1px 3px rgba(0,0,0,0.9)'
  div.style.background = 'rgba(0,0,0,0.5)'
  div.style.padding = '2px 8px'
  div.style.borderRadius = '4px'
  div.style.border = '1px solid rgba(255,255,255,0.2)'
  div.style.fontFamily = 'Arial, sans-serif'
  div.style.pointerEvents = 'none'
  div.style.backdropFilter = 'blur(2px)'

  const label = new CSS2DObject(div)
  label.position.set(slot.x, slot.y + 1.8, slot.z)
  label.userData = { floor: slot.floor || 1 }
  scene.add(label)
  slotMeshes.push(label)
  labelMeshes.push(label)
  return label
}

// ---------- 交互事件 ----------
const onPointerClick = (event) => {
  const rect = container.value.getBoundingClientRect()
  pointer.x = ((event.clientX - rect.left) / rect.width) * 2 - 1
  pointer.y = -((event.clientY - rect.top) / rect.height) * 2 + 1

  raycaster.setFromCamera(pointer, camera)
  const intersects = raycaster.intersectObjects(clickableMeshes)

  if (intersects.length > 0) {
    const data = intersects[0].object.userData
    if (data && data.id) {
      const statusText = ['空闲', '已占用', '锁定', '故障'][data.status] || '未知'
      // ===== 优化：显示承载量和包裹编号 =====
      alert(`📦 货位: ${data.id}\n📊 状态: ${statusText}\n📏 最大承重: ${data.maxWeight || 0}kg\n⚖️ 当前载重: ${(data.currentWeight || 0).toFixed(1)}kg\n📦 包裹: ${data.parcelCode || '无'}\n📍 坐标: (${data.x.toFixed(1)}, ${data.y.toFixed(1)}, ${data.z.toFixed(1)})`)
    }
  }
}

// ===== 新增：货位悬停高亮 =====
const onPointerMove = (event) => {
  const rect = container.value.getBoundingClientRect()
  pointer.x = ((event.clientX - rect.left) / rect.width) * 2 - 1
  pointer.y = -((event.clientY - rect.top) / rect.height) * 2 + 1

  raycaster.setFromCamera(pointer, camera)
  const intersects = raycaster.intersectObjects(clickableMeshes)

  // 重置之前高亮的货位
  if (hoveredObject) {
    const prev = hoveredObject
    const status = prev.userData.status
    const color = statusColors[status] || 0x888888
    prev.material.color.setHex(color)
    prev.material.emissive.setHex(0x000000)
    hoveredObject = null
    container.value.style.cursor = 'default'
  }

  if (intersects.length > 0) {
    const obj = intersects[0].object
    if (obj.userData && obj.userData.id) {
      // 高亮：变亮并增加发光效果
      obj.material.color.setHex(0xffffff)
      obj.material.emissive.setHex(0x444444)
      hoveredObject = obj
      container.value.style.cursor = 'pointer'
    }
  }
}

// ---------- AGV 数据 ----------
const fetchAgvData = async () => {
  try {
    const res = await axios.get('/api/agv/positions')
    if (res.data.code === 200) {
      updateAgvs(res.data.data)
    }
  } catch (error) {
    console.warn('AGV 数据加载失败，使用模拟数据:', error)
    updateAgvs(generateMockAgvData())
  }
}

// ===== 新增：生成模拟 AGV 数据 =====
const generateMockAgvData = () => {
  const list = []
  const time = Date.now() / 1000
  for (let i = 0; i < 4; i++) {
    const phase = i * Math.PI / 2
    const radius = 5 + i * 0.5
    const x = Math.sin(time * 0.3 + phase) * radius
    const z = Math.cos(time * 0.3 + phase) * (radius * 0.7)
    const angle = -Math.atan2(Math.cos(time * 0.3 + phase) * (radius * 0.7),
                              Math.sin(time * 0.3 + phase) * radius)
    list.push({ id: i+1, x, z, angle })
  }
  return list
}

// ===== 新增：更新 AGV 模型位置 =====
const updateAgvs = (agvList) => {
  // 确保 agvMeshes 数量与 agvList 一致
  while (agvMeshes.length < agvList.length) {
    const color = 0x00aaff + (agvMeshes.length * 0x223344)
    const agv = createAGV(color)
    scene.add(agv)
    agvMeshes.push(agv)
  }
  while (agvMeshes.length > agvList.length) {
    const m = agvMeshes.pop()
    scene.remove(m)
  }

  agvList.forEach((data, index) => {
    const mesh = agvMeshes[index]
    if (mesh) {
      mesh.position.set(data.x, 0.2, data.z)
      if (data.angle !== undefined) mesh.rotation.y = data.angle
    }
  })
}

// ===== 新增：启动 AGV 轮询（若 WebSocket 未连接） =====
const startAgvPolling = () => {
  if (agvPollTimer) clearInterval(agvPollTimer)
  agvPollTimer = setInterval(fetchAgvData, 3000)
}

// ---------- 货位数据 ----------
const fetchSlots = async () => {
  try {
    // ===== 优化：对接真实 API =====
    const res = await axios.get('/api/storage/slots')
    if (res.data.code === 200) {
      const slots = res.data.data.map(s => ({
        id: s.slotCode,
        x: s.xcoordinate || s.xCoordinate || 0,
        y: s.ycoordinate || s.yCoordinate || 0,
        z: s.zcoordinate || s.zCoordinate || 0,
        status: s.status !== undefined ? s.status : 0,
        maxWeight: s.maxWeight || 0,
        currentWeight: s.currentWeight || 0,
        parcelCode: s.parcelCode || '',
        floor: s.floor || 1
      }))
      renderSlots(slots)
    } else {
      console.warn('API 返回错误，使用模拟数据')
      renderSlots(generateMockSlots())
    }
  } catch (error) {
    console.error('加载货位数据失败，使用模拟数据:', error)
    renderSlots(generateMockSlots())
  }
}

// ===== 新增：定时刷新数据（降级方案） =====
const startAutoRefresh = () => {
  if (refreshInterval) clearInterval(refreshInterval)
  refreshInterval = setInterval(fetchSlots, 5000)  // 每 5 秒刷新
}

// ===== 新增：WebSocket 实时推送 =====
const connectWebSocket = () => {
  try {
    ws = new WebSocket('ws://localhost:8080/ws/slots')
    
    ws.onopen = () => {
      console.log('WebSocket 连接成功')
      // 如果 WebSocket 连接成功，停止轮询
      if (refreshInterval) clearInterval(refreshInterval)
    }
    
    ws.onmessage = (event) => {
      try {
        const data = JSON.parse(event.data)
        if (data.code === 200 && data.data) {
          const slots = data.data.map(s => ({
            id: s.slotCode,
            x: s.xcoordinate || s.xCoordinate || 0,
            y: s.ycoordinate || s.yCoordinate || 0,
            z: s.zcoordinate || s.zCoordinate || 0,
            status: s.status !== undefined ? s.status : 0,
            maxWeight: s.maxWeight || 0,
            currentWeight: s.currentWeight || 0,
            parcelCode: s.parcelCode || '',
            floor: s.floor || 1
          }))
          renderSlots(slots)
          console.log('WebSocket 数据更新')
        }
      } catch (e) {
        console.warn('WebSocket 数据解析失败:', e)
      }
    }
    
    ws.onerror = (error) => {
      console.error('WebSocket 错误:', error)
    }
    
    ws.onclose = () => {
      console.log('WebSocket 断开，启用定时轮询')
      startAutoRefresh()
    }
  } catch (e) {
    console.warn('WebSocket 不可用，启用定时轮询')
    startAutoRefresh()
  }
}

// 生成模拟货位数据（3排 × 5列 × 3层）
const generateMockSlots = () => {
  const slots = []
  for (let row = 0; row < 3; row++) {
    for (let col = 0; col < 5; col++) {
      for (let layer = 0; layer < 3; layer++) {
        slots.push({
          id: `A-${row+1}-${col+1}-${layer+1}`,
          x: (col - 2) * 4,
          y: layer * 2.5 + 1.25,
          z: (row - 1) * 4,
          status: Math.floor(Math.random() * 4),
          maxWeight: 500,
          currentWeight: Math.random() * 300,
          parcelCode: Math.random() > 0.6 ? `P-${Date.now()%100000}` : '',
          floor: (row + col + layer) % 3 + 1
        })
      }
    }
  }
  return slots
}

// 渲染货位
const renderSlots = (slots) => {
  // 清除旧货位
  slotMeshes.forEach(mesh => scene.remove(mesh))
  slotMeshes = []
  clickableMeshes = []
  floorGroups = {}
  labelMeshes = []

  const geometry = new THREE.BoxGeometry(1.5, 2.0, 1.5)

  slots.forEach(slot => {
    const status = slot.status !== undefined ? slot.status : 0
    const material = new THREE.MeshStandardMaterial({
      color: statusColors[status] || 0x888888,
      transparent: true,
      opacity: 0.9
    })
    const cube = new THREE.Mesh(geometry, material)
    cube.position.set(slot.x, slot.y, slot.z)
    cube.castShadow = true
    cube.userData = {
      id: slot.id,
      status,
      x: slot.x,
      y: slot.y,
      z: slot.z,
      maxWeight: slot.maxWeight || 0,
      currentWeight: slot.currentWeight || 0,
      parcelCode: slot.parcelCode || '',
      floor: slot.floor || 1
    }
    scene.add(cube)
    slotMeshes.push(cube)
    clickableMeshes.push(cube)

    // 边框
    const edges = new THREE.EdgesGeometry(geometry)
    const line = new THREE.LineSegments(edges, new THREE.LineBasicMaterial({ color: 0xffffff }))
    line.position.copy(cube.position)
    scene.add(line)
    slotMeshes.push(line)

    // ===== 新增：添加文本标签 =====
    const label = addLabel(slot)
    // 将标签也加入楼层分组
    const floor = slot.floor || 1
    if (!floorGroups[floor]) floorGroups[floor] = []
    floorGroups[floor].push(cube, line, label)
  })

  // 应用当前楼层过滤
  applyFloorFilter(currentFloor)
}

// ---------- 楼层 ----------
const applyFloorFilter = (floor) => {
  Object.keys(floorGroups).forEach(key => {
    const visible = parseInt(key) === floor
    floorGroups[key].forEach(obj => {
      obj.visible = visible
    })
  })
}

// ===== 新增：切换楼层（供外部调用） =====
const switchFloor = (floor) => {
  currentFloor = floor
  applyFloorFilter(floor)
}

// ---------- 视角动画 ----------
const flyTo = (targetPos, targetLookAt) => {
  console.log('flyTo 开始执行, 目标位置:', targetPos, '目标注视点:', targetLookAt)
  animStartPos = camera.position.clone()
  animStartTarget = controls.target.clone()
  animTargetPos = targetPos.clone()
  animEndTarget = targetLookAt.clone()
  animStartTime = performance.now()
  animating = true
  controls.enableDamping = false
}

// ===== 新增：快捷视角 =====
const flyToView = (view) => {
  console.log('flyToView 被调用, view:', view)
  const views = {
    top:    { pos: new THREE.Vector3(0, 30, 0.1), look: new THREE.Vector3(0, 0, 0) },
    front:  { pos: new THREE.Vector3(0, 5, 25),   look: new THREE.Vector3(0, 0, 0) },
    side:   { pos: new THREE.Vector3(25, 5, 0),   look: new THREE.Vector3(0, 0, 0) }
  }
  const v = views[view]
  if (v) flyTo(v.pos, v.look)
}

// ---------- 动画循环 ----------
const animate = () => {
    animationFrameId = requestAnimationFrame(animate) // 🌟 每次保存帧 ID

  // 视角动画插值
  if (animating) {
    const elapsed = performance.now() - animStartTime
    const progress = Math.min(elapsed / animDuration, 1)
    const eased = progress < 0.5 ? 2 * progress * progress : 1 - Math.pow(-2 * progress + 2, 2) / 2
    camera.position.lerpVectors(animStartPos, animTargetPos, eased)
    controls.target.lerpVectors(animStartTarget, animEndTarget, eased)
    if (progress >= 1) {
      animating = false
      controls.enableDamping = true
      controls.update()
      console.log('视角动画完成')
    }
  }
  if (controls) controls.update()

  controls.update()
  if (renderer && scene && camera) {
      renderer.render(scene, camera)
  }
  if (labelRenderer && scene && camera) {
      labelRenderer.render(scene, camera)
  }
}

// ---------- 响应式 ----------
const onResize = () => {
  const width = container.value.clientWidth
  const height = container.value.clientHeight
  camera.aspect = width / height
  camera.updateProjectionMatrix()
  renderer.setSize(width, height)
  if (labelRenderer) labelRenderer.setSize(width, height)
}

// ---------- 暴露 ----------
defineExpose({
  switchFloor,
  flyToView
})

// ---------- 生命周期 ----------
onMounted(() => {
  initScene()
  animate()

  // WebSocket
  try {
    ws = new WebSocket('ws://localhost:8080/ws/slots')
    ws.onopen = () => console.log('WebSocket 连接成功')
    ws.onmessage = (e) => {
      try {
        const data = JSON.parse(e.data)
        if (data.code === 200 && data.data) {
          const slots = data.data.map(s => ({
            id: s.slotCode,
            x: s.xcoordinate || s.xCoordinate || 0,
            y: s.ycoordinate || s.yCoordinate || 0,
            z: s.zcoordinate || s.zCoordinate || 0,
            status: s.status !== undefined ? s.status : 0,
            maxWeight: s.maxWeight || 0,
            currentWeight: s.currentWeight || 0,
            parcelCode: s.parcelCode || '',
            floor: s.floor || 1
          }))
          renderSlots(slots)
        }
      } catch (e) { console.warn('WebSocket 数据解析失败', e) }
    }
    ws.onclose = () => {
      console.log('WebSocket 断开，启用轮询')
      if (!refreshInterval) refreshInterval = setInterval(fetchSlots, 5000)
    }
  } catch (e) {
    console.warn('WebSocket 不可用，启用轮询')
    if (!refreshInterval) refreshInterval = setInterval(fetchSlots, 5000)
  }

  window.addEventListener('resize', onResize)
})

onBeforeUnmount(() => {
    // 1. 🌟 立即取消动画帧，阻止后台持续渲染报错和卡顿
    if (animationFrameId) {
        cancelAnimationFrame(animationFrameId)
    }

    // 2. 移除全局窗口监听
    window.removeEventListener('resize', onResize)

    // 3. 清理定时器
    if (refreshInterval) clearInterval(refreshInterval)
    if (agvPollTimer) clearInterval(agvPollTimer)

    // 4. 🌟 核心防错：切断 WebSocket 的关闭事件监听，防止 ws.close() 重新拉起轮询！
    if (ws) {
        ws.onclose = null // 🌟 先把 onclose 设为 null，再关闭
        ws.close()
    }

    // 5. 移除 DOM 事件监听
    if (container.value) {
        container.value.removeEventListener('click', onPointerClick)
        container.value.removeEventListener('mousemove', onPointerMove)
    }

    // 6. 🌟 手动递归释放场景中所有 Mesh 的几何体和材质，防止 GPU 显存泄漏
    if (scene) {
        scene.traverse((object) => {
            if (object.isMesh) {
                if (object.geometry) object.geometry.dispose()
                if (object.material) {
                    if (Array.isArray(object.material)) {
                        object.material.forEach(material => material.dispose())
                    } else {
                        object.material.dispose()
                    }
                }
            }
        })
    }

    // 7. 销毁控制器
    if (controls) {
        controls.dispose()
    }

    // 8. 销毁 WebGLRenderer，并将其 Canvas 从 DOM 中彻底移除
    if (renderer) {
        renderer.dispose()
        if (renderer.domElement && renderer.domElement.parentNode) {
            renderer.domElement.parentNode.removeChild(renderer.domElement)
        }
    }

    // 9. 🌟 彻底修复：CSS2DRenderer 没有 dispose 方法，直接将其绑定的 DOM 节点从父元素移除即可！
    if (labelRenderer) {
        if (labelRenderer.domElement && labelRenderer.domElement.parentNode) {
            labelRenderer.domElement.parentNode.removeChild(labelRenderer.domElement)
        }
    }
})


</script>

<style scoped>
.three-container {
  width: 100%;
  height: 100vh;
  display: block;
}


</style>