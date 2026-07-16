<template>
  <div ref="container" class="three-container"></div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'
import * as THREE from 'three'
import { OrbitControls } from 'three/addons/controls/OrbitControls.js'
// ===== 新增：CSS2DRenderer 用于文本标签 =====
import { CSS2DRenderer, CSS2DObject } from 'three/addons/renderers/CSS2DRenderer.js'
// ===== 新增：TWEEN 用于视角动画 =====
import TWEEN from 'three/addons/libs/tween.module.js'
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

// ===== 新增：楼层分组 =====
let floorGroups = {}          // { floor: [mesh1, mesh2, ...] }
let currentFloor = 1
let labelMeshes = []          // 存储所有标签，便于楼层切换时隐藏

// ===== 新增：AGV 网格数组 =====
let agvMeshes = []

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

// ===== 新增：创建 AGV 小车（可指定颜色） =====
const createAGV = (color = 0x00aaff) => {
  const group = new THREE.Group()
  
  // 车身
  const bodyGeo = new THREE.BoxGeometry(1.2, 0.4, 0.8)
  const bodyMat = new THREE.MeshStandardMaterial({ color })
  const body = new THREE.Mesh(bodyGeo, bodyMat)
  body.position.y = 0.2
  group.add(body)
  
  // 顶灯
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
  label.userData = { floor: slot.floor || 1 }       // 存储楼层信息
  scene.add(label)
  slotMeshes.push(label)
  labelMeshes.push(label)  // 存入标签数组
  return label
}

// ===== 新增：点击货位弹出详情（含承载量、包裹编号） =====
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

// ===== 新增：获取 AGV 位置数据 =====
const fetchAgvData = async () => {
  try {
    const res = await axios.get('/api/agv/positions')
    if (res.data.code === 200) {
      updateAgvs(res.data.data)
    }
  } catch (error) {
    console.warn('AGV 数据加载失败，使用模拟数据:', error)
    // 使用模拟数据
    const mockAgv = generateMockAgvData()
    updateAgvs(mockAgv)
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
      if (data.angle !== undefined) {
        mesh.rotation.y = data.angle
      }
    }
  })
}

// ===== 新增：启动 AGV 轮询（若 WebSocket 未连接） =====
let agvPollTimer = null
const startAgvPolling = () => {
  if (agvPollTimer) clearInterval(agvPollTimer)
  agvPollTimer = setInterval(fetchAgvData, 3000)  // 每3秒刷新
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
        status: s.status !== undefined ? s.status : 0,
        maxWeight: s.maxWeight || 0,
        currentWeight: s.currentWeight || 0,
        parcelCode: s.parcelCode || '',
        floor: s.floor || 1
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
          status: Math.floor(Math.random() * 4),  // 0~3 随机状态
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
  clickableMeshes = []  // 清空可点击列表
  // ===== 新增：清除楼层分组 =====
  floorGroups = {}
  labelMeshes = []

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
    // ===== 优化：存储完整数据（含承载量、包裹编号） =====
    cube.userData = { 
      id: slot.id, 
      status: status,
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
    clickableMeshes.push(cube)  // 加入可点击列表

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

// ===== 新增：楼层过滤 =====
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

// ===== 新增：视角动画（使用 TWEEN） =====
const flyTo = (targetPos, targetLookAt) => {
  const startPos = camera.position.clone()
  const startTarget = controls.target.clone()
  
  new TWEEN.Tween({
    x: startPos.x, y: startPos.y, z: startPos.z,
    tx: startTarget.x, ty: startTarget.y, tz: startTarget.z
  })
  .to({
    x: targetPos.x, y: targetPos.y, z: targetPos.z,
    tx: targetLookAt.x, ty: targetLookAt.y, tz: targetLookAt.z
  }, 1000)
  .easing(TWEEN.Easing.Quadratic.InOut)
  .onUpdate((obj) => {
    camera.position.set(obj.x, obj.y, obj.z)
    controls.target.set(obj.tx, obj.ty, obj.tz)
    controls.update()
  })
  .start()
}

// ===== 新增：快捷视角 =====
const flyToView = (view) => {
  const views = {
    top:    { pos: {x:0, y:30, z:0.1}, look: {x:0, y:0, z:0} },
    front:  { pos: {x:0, y:5, z:25},   look: {x:0, y:0, z:0} },
    side:   { pos: {x:25, y:5, z:0},   look: {x:0, y:0, z:0} }
  }
  const v = views[view]
  if (v) flyTo(v.pos, v.look)
}

// 动画循环
const animate = () => {
  requestAnimationFrame(animate)
  
  // ===== 更新 TWEEN =====
  TWEEN.update()
  
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

// ===== 暴露方法给父组件 =====
defineExpose({
  switchFloor,
  flyToView
})

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
  if (agvPollTimer) clearInterval(agvPollTimer)
  // ===== 新增：关闭 WebSocket =====
  if (ws) {
    ws.close()
  }
  // ===== 新增：移除事件监听 =====
  if (container.value) {
    container.value.removeEventListener('click', onPointerClick)
    container.value.removeEventListener('mousemove', onPointerMove)
  }
    if (renderer) {
        renderer.dispose();
        renderer.domElement?.remove();
    }

    // 💡 正确清理 CSS2D 渲染器的方法：直接移除它的 DOM 节点即可
    if (labelRenderer && labelRenderer.domElement) {
        labelRenderer.domElement.remove();
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