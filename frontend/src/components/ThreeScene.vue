<template>
  <div ref="container" class="three-container"></div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'
import * as THREE from 'three'
import { OrbitControls } from 'three/addons/controls/OrbitControls.js'
// ===== 新增：CSS2DRenderer 用于文本标签 =====
import { CSS2DRenderer, CSS2DObject } from 'three/addons/renderers/CSS2DRenderer.js'
import axios from 'axios'

const container = ref(null)
let scene, camera, renderer, controls, labelRenderer
let slotMeshes = []
// ===== 新增：多辆 AGV 小车数组、轮询定时器、WebSocket =====
let agvs = []
let refreshInterval = null
let ws = null
// ===== 新增：Raycaster 用于点击和高亮交互 =====
const raycaster = new THREE.Raycaster()
const pointer = new THREE.Vector2()
let clickableMeshes = []  // 存储可点击的货位网格
let hoveredObject = null  // 当前高亮的货位

// ===== 新增：状态颜色映射（模块级，供多个方法使用） =====
const statusColors = {
  0: 0x00ff88,   // 空闲 → 绿色
  1: 0xff4444,   // 已占用 → 红色
  2: 0xffaa00,   // 锁定 → 橙色
  3: 0x888888    // 故障 → 灰色
}

// 初始化场景
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
  labelRenderer.domElement.style.pointerEvents = 'none'  // 标签不阻挡点击
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

  // ===== 新增：创建多辆 AGV 小车 =====
  createMultipleAGVs()

  // ===== 新增：点击事件 =====
  container.value.addEventListener('click', onPointerClick)
  // ===== 新增：鼠标移动事件（货位高亮） =====
  container.value.addEventListener('mousemove', onPointerMove)

  // 加载货位数据
  fetchSlots()
}

// ===== 新增：创建多辆 AGV 小车 =====
const createAGV = (color = 0x00aaff) => {
  const group = new THREE.Group()
  
  // 车身
  const bodyGeo = new THREE.BoxGeometry(1.2, 0.4, 0.8)
  const bodyMat = new THREE.MeshStandardMaterial({ color })
  const body = new THREE.Mesh(bodyGeo, bodyMat)
  body.position.y = 0.2
  group.add(body)
  
  // 顶灯（颜色与车身区分）
  const lightGeo = new THREE.SphereGeometry(0.15, 8, 8)
  const lightMat = new THREE.MeshStandardMaterial({ 
    color: 0xffaa00, 
    emissive: 0xffaa00, 
    emissiveIntensity: 0.5 
  })
  const light = new THREE.Mesh(lightGeo, lightMat)
  light.position.set(0, 0.5, 0)
  group.add(light)
  
  return group
}

const createMultipleAGVs = () => {
  const colors = [0x00aaff, 0xff66aa, 0x66ff66, 0xffaa00]
  const basePositions = [
    { x: -4, z: -3 },
    { x: 4, z: -3 },
    { x: -4, z: 3 },
    { x: 4, z: 3 }
  ]

  colors.forEach((color, index) => {
    const agv = createAGV(color)
    const pos = basePositions[index % basePositions.length]
    agv.position.set(pos.x, 0.2, pos.z)
    agv.userData = {
      phase: index * Math.PI / 2,
      speed: 0.3 + index * 0.05,
      radiusX: 5 + index * 0.5,
      radiusZ: 3 + index * 0.3
    }
    scene.add(agv)
    agvs.push(agv)
  })
}

// ===== 新增：添加货位文本标签（样式优化） =====
const addLabel = (slot) => {
  const div = document.createElement('div')
  div.textContent = slot.id
  // ===== 优化：标签样式调整 =====
  div.style.color = '#ffffff'
  div.style.fontSize = '14px'           // 增大字体
  div.style.fontWeight = 'bold'
  div.style.textShadow = '1px 1px 3px rgba(0,0,0,0.9)'
  div.style.background = 'rgba(0,0,0,0.5)'  // 背景半透明
  div.style.padding = '2px 8px'
  div.style.borderRadius = '4px'
  div.style.border = '1px solid rgba(255,255,255,0.2)'
  div.style.fontFamily = 'Arial, sans-serif'
  div.style.pointerEvents = 'none'
  div.style.backdropFilter = 'blur(2px)'  // 背景模糊
  
  const label = new CSS2DObject(div)
  label.position.set(slot.x, slot.y + 1.8, slot.z)  // 货位上方
  scene.add(label)
  slotMeshes.push(label)
}

// ===== 新增：点击货位弹出详情 =====
const onPointerClick = (event) => {
  const rect = container.value.getBoundingClientRect()
  pointer.x = ((event.clientX - rect.left) / rect.width) * 2 - 1
  pointer.y = -((event.clientY - rect.top) / rect.height) * 2 + 1

  raycaster.setFromCamera(pointer, camera)
  const intersects = raycaster.intersectObjects(clickableMeshes)

  if (intersects.length > 0) {
    const clicked = intersects[0].object
    const data = clicked.userData
    if (data && data.id) {
      const statusText = ['空闲', '已占用', '锁定', '故障'][data.status] || '未知'
      alert(`📦 货位: ${data.id}\n📊 状态: ${statusText}\n📍 坐标: (${data.x.toFixed(1)}, ${data.y.toFixed(1)}, ${data.z.toFixed(1)})`)
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

// 从后端获取货位数据
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
        status: s.status !== undefined ? s.status : 0
      }))
      renderSlots(slots)
    } else {
      console.warn('API 返回错误，使用模拟数据')
      const mockData = generateMockSlots()
      renderSlots(mockData)
    }
  } catch (error) {
    console.error('加载货位数据失败，使用模拟数据:', error)
    const mockData = generateMockSlots()
    renderSlots(mockData)
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
            status: s.status !== undefined ? s.status : 0
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
      console.log('WebSocket 断开，5秒后重连...')
      setTimeout(connectWebSocket, 5000)
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
          status: Math.floor(Math.random() * 4)  // 0~3 随机状态
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
  clickableMeshes = []  // 清空可点击列表

  // ===== 优化：货位尺寸调整 =====
  const geometry = new THREE.BoxGeometry(1.5, 2.0, 1.5)  // 原为 1.8, 2, 1.8

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
    // ===== 优化：存储完整数据 =====
    cube.userData = { 
      id: slot.id, 
      status: status,
      x: slot.x,
      y: slot.y,
      z: slot.z
    }
    scene.add(cube)
    slotMeshes.push(cube)
    clickableMeshes.push(cube)  // 加入可点击列表

    // 边框
    const edges = new THREE.EdgesGeometry(geometry)
    const line = new THREE.LineSegments(edges, new THREE.LineBasicMaterial({ color: 0xffffff }))
    line.position.copy(cube.position)
    scene.add(line)
    slotMeshes.push(line)

    // ===== 新增：添加文本标签 =====
    addLabel(slot)
  })
}

// 动画循环
const animate = () => {
  requestAnimationFrame(animate)
  
  // ===== 优化：多辆 AGV 协同移动 =====
  agvs.forEach((agv) => {
    const time = Date.now() / 1000
    const data = agv.userData
    const rX = data.radiusX || 5
    const rZ = data.radiusZ || 3
    const speed = data.speed || 0.3
    const phase = data.phase || 0
    
    agv.position.x = Math.sin(time * speed + phase) * rX
    agv.position.z = Math.cos(time * speed + phase) * rZ
    agv.rotation.y = -Math.atan2(
      Math.cos(time * speed + phase) * rZ,
      Math.sin(time * speed + phase) * rX
    )
  })
  
  controls.update()
  renderer.render(scene, camera)
  // ===== 新增：渲染 CSS2D 标签 =====
  if (labelRenderer) {
    labelRenderer.render(scene, camera)
  }
}

// 窗口自适应
const onResize = () => {
  const width = container.value.clientWidth
  const height = container.value.clientHeight
  camera.aspect = width / height
  camera.updateProjectionMatrix()
  renderer.setSize(width, height)
  if (labelRenderer) {
    labelRenderer.setSize(width, height)
  }
}

// 生命周期
onMounted(() => {
  initScene()
  animate()
  // ===== 优化：优先使用 WebSocket，降级为轮询 =====
  connectWebSocket()
  // 如果 WebSocket 连接成功，停止定时轮询；否则轮询已启动
  // 但为了保险，设置一个超时检测
  setTimeout(() => {
    if (!ws || ws.readyState !== WebSocket.OPEN) {
      console.warn('WebSocket 连接失败，启用定时轮询')
      startAutoRefresh()
    }
  }, 3000)
  
  window.addEventListener('resize', onResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', onResize)
  // ===== 新增：清理定时器 =====
  if (refreshInterval) clearInterval(refreshInterval)
  // ===== 新增：关闭 WebSocket =====
  if (ws) {
    ws.close()
  }
  // ===== 新增：移除事件监听 =====
  if (container.value) {
    container.value.removeEventListener('click', onPointerClick)
    container.value.removeEventListener('mousemove', onPointerMove)
  }
  renderer.dispose()
  if (labelRenderer) {
    labelRenderer.dispose()
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