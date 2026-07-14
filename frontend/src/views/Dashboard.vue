<template>
  <div class="dashboard">
    <ThreeScene ref="threeSceneRef" />
    <!-- 控制面板 -->
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
  </div>
</template>

<script setup>
import { ref } from 'vue'
import ThreeScene from '@/components/ThreeScene.vue'

// 获取 ThreeScene 组件实例
const threeSceneRef = ref(null)

// 切换楼层
const switchFloor = (floor) => {
  threeSceneRef.value?.switchFloor(floor)
}

// 切换视角
const flyToView = (view) => {
  threeSceneRef.value?.flyToView(view)
}
</script>

<style scoped>
.dashboard {
  position: relative;
  width: 100%;
  height: 100vh;
  overflow: hidden;
}
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
</style>