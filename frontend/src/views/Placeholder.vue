<template>
    <div class="placeholder-module">
        <el-card class="placeholder-card">
            <h2>🛠️ {{ route.meta.title }} 正在加载</h2>
            <p>该功能页面正在进行敏捷开发迭代中...</p>
            <el-empty description="开发组正在全力拼搏中" />
        </el-card>
    </div>
</template>

<script setup>
import { useRoute } from 'vue-router'
const route = useRoute()
</script>

<style scoped>
.placeholder-module {
    height: 100%;
    display: flex;
    justify-content: center;
    align-items: center;
}
.placeholder-card {
    background-color: #1e293b !important;
    border: 1px solid #334155 !important;
    width: 500px;
    text-align: center;
    padding: 40px;
}
.placeholder-card h2 {
    color: #f8fafc;
    margin-bottom: 8px;
}
.placeholder-card p {
    color: #64748b;
    margin-bottom: 24px;
}
</style>