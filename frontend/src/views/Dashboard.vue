<template>
    <div class="dashboard-scene-wrapper">
        <ThreeScene
            v-if="!isMobile"
            ref="threeSceneRef"
            class="three-viewport"
        />
        <div v-else class="mock-pda-bg">
            <span class="pda-tip-text">📱 PDA 极简效能看板模式已启动 (3D引擎已安全卸载)</span>
        </div>

        <div class="dashboard-ui-overlay" v-if="!isMobile">

            <div class="overlay-panel left-panel animate-left">
                <div class="system-title-block">
                    <h3>🛸 智能仓库 3D 数字孪生大屏</h3>
                    <p>集成多源传感器与 AGV 实时位置上报，全仓态势感知就绪。</p>
                </div>

                <div class="panel-section-title">📊 仓储实时核心动态</div>
                <div class="metrics-grid">
                    <div class="metric-card inbound">
                        <div class="m-icon">📥</div>
                        <div class="m-info">
                            <span class="m-label">当日入库落位</span>
                            <span class="m-value">{{ inboundToday }} <small>件</small></span>
                        </div>
                    </div>

                    <div class="metric-card outbound">
                        <div class="m-icon">📤</div>
                        <div class="m-info">
                            <span class="m-label">当日扣减出库</span>
                            <span class="m-value">{{ outboundToday }} <small>件</small></span>
                        </div>
                    </div>

                    <div class="metric-card saturation">
                        <div class="m-icon">🧱</div>
                        <div class="m-info">
                            <span class="m-label">在库货位饱和度</span>
                            <span class="m-value">{{ storageOccupancyRate }} <small>%</small></span>
                        </div>
                        <el-progress
                            :percentage="storageOccupancyRate"
                            :stroke-width="4"
                            color="#38bdf8"
                            :show-text="false"
                            class="m-progress"
                        />
                    </div>
                </div>

                <div class="mini-status-grid" style="margin-top: 12px; display: grid; grid-template-columns: repeat(3, 1fr); gap: 8px;">
                    <div class="mini-card" :style="{ padding: '8px', background: 'rgba(30, 41, 59, 0.5)', border: '1px solid #334155', borderRadius: '4px', textAlign: 'center' }">
                        <div style="font-size: 11px; color: #94a3b8;">🚨 异常拦截告警</div>
                        <div style="font-size: 16px; font-weight: bold; color: #ef4444; margin-top: 2px;">{{ activeAlerts }} <small style="font-size: 10px;">条</small></div>
                    </div>
                    <div class="mini-card" :style="{ padding: '8px', background: 'rgba(30, 41, 59, 0.5)', border: '1px solid #334155', borderRadius: '4px', textAlign: 'center' }">
                        <div style="font-size: 11px; color: #94a3b8;">🤖 AGV利用率</div>
                        <div style="font-size: 16px; font-weight: bold; color: #a855f7; margin-top: 2px;">{{ agvUtilizationRate }} <small style="font-size: 10px;">%</small></div>
                    </div>
                    <div class="mini-card" :style="{ padding: '8px', background: 'rgba(30, 41, 59, 0.5)', border: '1px solid #334155', borderRadius: '4px', textAlign: 'center' }">
                        <div style="font-size: 11px; color: #94a3b8;">✅ 自动化完工率</div>
                        <div style="font-size: 16px; font-weight: bold; color: #10b981; margin-top: 2px;">{{ completionRate }} <small style="font-size: 10px;">%</small></div>
                    </div>
                </div>

                <div class="panel-section-title" style="margin-top: 20px;">📈 库内存货周转时序趋势</div>
                <div class="chart-container" ref="turnoverChartRef"></div>

                <div class="panel-section-title" style="margin-top: 15px;">🍩 调度任务执行状态分布</div>
                <div class="chart-container" ref="taskStatusChartRef" style="height: 160px; min-height: 160px;"></div>
            </div>

            <div class="controls animate-fade">
                <div class="floor-btns">
                    <button v-for="f in [1,2,3]" :key="f" @click="switchFloor(f)">楼层 {{ f }}</button>
                </div>
                <div class="view-btns">
                    <button @click="flyToView('top')">俯视图</button>
                    <button @click="flyToView('front')">正面</button>
                    <button @click="flyToView('side')">侧面</button>
                </div>
            </div>

            <div class="overlay-panel right-panel animate-right">
                <div class="panel-section-title">🤖 智能硬件拣货效能 (件/小时)</div>
                <div class="chart-container" ref="efficiencyChartRef" style="height: 140px; min-height: 140px;"></div>

                <div class="panel-section-title" style="margin-top: 15px;">⚠️ 实时系统风险与告警层级</div>
                <div class="chart-container" ref="alertLevelChartRef" style="height: 150px; min-height: 150px;"></div>

                <div class="panel-section-title" style="margin-top: 15px;">🚨 仓内物联无线实时告警</div>
                <div class="alarm-list-box" style="height: 130px; overflow-y: auto;">
                    <div class="alarm-item" v-for="(alarm, idx) in alarmList" :key="idx" :class="alarm.level">
                        <div class="alarm-dot"></div>
                        <div class="alarm-msg">
                            <div class="alarm-meta">
                                <span class="time">{{ alarm.time }}</span>
                                <span class="loc">{{ alarm.location }}</span>
                            </div>
                            <div class="alarm-text">{{ alarm.text }}</div>
                        </div>
                    </div>
                </div>

                <div class="embedded-video-section" style="margin-top: 15px;">
                    <div class="video-header">
                        <span class="video-title">📹 现场实时视频监控流</span>
                        <div class="video-controls">
                            <button v-if="!isRunning" class="btn-start" @click="startPush" :disabled="videoLoading">
                                {{ videoLoading ? '...' : '启动' }}
                            </button>
                            <button v-else class="btn-stop" @click="stopPush" :disabled="videoLoading">
                                {{ videoLoading ? '...' : '停止' }}
                            </button>
                        </div>
                    </div>
                    <div class="video-wrapper">
                        <VideoPlayer v-if="hlsUrl" :key="hlsUrl" :hls-url="hlsUrl" />
                        <div v-else class="video-placeholder">
                            <span>未启动现场视频推流</span>
                            <span class="status-tag stopped">○ 已断开</span>
                        </div>
                    </div>
                </div>
            </div>
        </div>

        <div class="mobile-pda-layout" v-else>
            <div class="pda-header">📦 WMS 移动工作台态势舱</div>
            <el-row :gutter="12">
                <el-col :span="12">
                    <div class="mobile-metric">
                        <span class="label">当日入库落位</span>
                        <span class="val text-emerald-400">{{ inboundToday }}</span>
                    </div>
                </el-col>
                <el-col :span="12">
                    <div class="mobile-metric">
                        <span class="label">当日扣减出库</span>
                        <span class="val text-amber-400">{{ outboundToday }}</span>
                    </div>
                </el-col>
                <el-col :span="24" style="margin-top: 12px;">
                    <div class="mobile-alarm-card">
                        <div class="m-title">🚨 仓内物联无线实时告警 (PDA同步)</div>
                        <div class="m-alarm-row" v-for="(alarm, idx) in alarmList.slice(0, 3)" :key="idx">
                            <span>[{{ alarm.location }}]</span> {{ alarm.text }}
                        </div>
                    </div>
                </el-col>
            </el-row>
        <div class="video-section">
            <div class="video-header">
                <span class="video-title">📹 实时监控</span>
                <div class="video-controls">
                    <!-- 启动/停止推流 -->
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

                    <!-- ===== AI 检测开关 ===== -->
                    <button
                        class="btn-ai"
                        :class="{ active: enableAI }"
                        @click="toggleAI"
                        :disabled="!hlsUrl"
                    >
                        {{ enableAI ? '🤖 AI检测中' : '📡 启用AI' }}
                    </button>

                    <span class="status-tag" :class="isRunning ? 'running' : 'stopped'">
                        {{ isRunning ? '● 推流中' : '○ 已停止' }}
                    </span>
                </div>
            </div>
            <div class="video-wrapper">
                <!-- ===== VideoPlayer 传入 AI 参数 ===== -->
                <VideoPlayer
                    v-if="hlsUrl"
                    :key="hlsUrl + (enableAI ? aiVideoSource : '')"
                    :hls-url="hlsUrl"
                    :ai-video-source="enableAI ? aiVideoSource : ''"
                    :conf-threshold="confThreshold"
                    :interval-sec="intervalSec"
                />
                <div v-else class="video-placeholder">
                    <span>未启动推流</span>
                    <span class="hint">点击「启动推流」开始播放测试视频</span>
                </div>
            </div>
        </div>
    </div>
    </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, nextTick } from 'vue'
import request from '@/utils/request.js'
import axios from 'axios'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'
import ThreeScene from '../components/ThreeScene.vue'
import VideoPlayer from '../components/VideoPlayer.vue'

// ===== 基础与双端适配状态 =====
const isMobile = ref(false)
// ===== 3D 场景引用 =====
const threeSceneRef = ref(null)

// ===== 📊 KPI 卡片全量响应式变量 =====
const inboundToday = ref(1428)
const outboundToday = ref(915)
const storageOccupancyRate = ref(68.4)
// TODO：这是模拟数据
const activeAlerts = ref(2)            // 💡 新增：告警数变量
const agvUtilizationRate = ref(12.50)  // 💡 新增：AGV利用率变量
const completionRate = ref(25.00)      // 💡 新增：完工率变量

// ===== ECharts 容器引用与句柄声明 =====
const turnoverChartRef = ref(null)
const efficiencyChartRef = ref(null)
const taskStatusChartRef = ref(null)  // 💡 新增
const alertLevelChartRef = ref(null)  // 💡 新增

let turnoverChart = null
let efficiencyChart = null
let taskStatusChart = null            // 💡 新增
let alertLevelChart = null            // 💡 新增

// ===== 视频推流业务状态 =====

// ===== 视频推流状态 =====
const hlsUrl = ref('')
const isRunning = ref(false)
const videoLoading = ref(false)
const streamId = ref('test')
const HLS_BASE_URL = 'http://localhost:8081/hls/'

// ===== 定时器句柄 =====
let statusTimer = null
let alarmTimer = null
let dataTimer = null

// TODO: 改为真读实数据
// ===== 物联网告警模拟数据源 =====
const alarmList = ref([
    { time: '16:45:22', location: '货位 A-02-01', text: '货位超载 (当前 520kg / 上限 500kg)', level: 'danger' },
    { time: '16:42:10', location: 'AGV #03号', text: '检测到激光防撞雷达前方物理阻挡，已挂起', level: 'warning' },
    { time: '16:30:15', location: 'B区重货仓', text: '温湿度报高 (当前 31.5℃ / 上限 30.0℃)', level: 'warning' },
    { time: '16:12:04', location: '货位 C-01-12', text: '货物条码发生视觉复验不匹配异常', level: 'info' }
])

// ===== 🔌 一键抓取中台控制塔全局多维聚合KPI与4张图表数据 =====
const fetchDashboardOverview = async () => {
    try {
        const res = await request.get('/wms/dashboard/overview')

        // 针对规范过后的 Result 统一返回体进行安全数据抽取
        let finalData = null
        if (res && res.kpi) {
            finalData = res
        } else if (res && res.data && res.data.kpi) {
            finalData = res.data
        } else if (res && res.data && res.data.data) {
            finalData = res.data.data
        }

        if (finalData) {
            const { kpi, charts } = finalData
            console.log("成功提取大盘多维核心数据矩阵:", { kpi, charts })

// ===== AI 检测控制 =====
const enableAI = ref(false)
const aiVideoSource = ref('C:/Users/12256/Downloads/Test Jellyfin 1080p AVC 3M.mp4')
const confThreshold = ref(0.3)
const intervalSec = ref(1.0)

// ---------- AI 切换 ----------
const toggleAI = () => {
    enableAI.value = !enableAI.value
    if (!enableAI.value) {
        // 关闭 AI 检测时，清空视频源
        aiVideoSource.value = ''
    } else {
        // 开启时如果视频源为空，设置默认测试视频
        if (!aiVideoSource.value) {
            aiVideoSource.value = 'C:/Users/12256/Downloads/Test Jellyfin 1080p AVC 3M.mp4'
        }
    }
}

// ===== 获取推流状态 =====
            // 🌟 1. 动态刷新顶部及副指标全部 6 个核心数字卡片
            if (kpi) {
                inboundToday.value = kpi.inboundToday ?? 0
                outboundToday.value = kpi.outboundToday ?? 0
                storageOccupancyRate.value = kpi.storageOccupancyRate ? Number(kpi.storageOccupancyRate) : 0
                activeAlerts.value = kpi.activeAlerts ?? 0
                agvUtilizationRate.value = kpi.agvUtilizationRate ? Number(kpi.agvUtilizationRate) : 0
                completionRate.value = kpi.completionRate ? Number(kpi.completionRate) : 0
            }

            // 🌟 2. 动态向 4 张 ECharts 实例灌入对应的后端探库深度聚合指标
            if (charts) {
                // A. 更新时序折线图
                if (turnoverChart && charts.turnoverTrend) {
                    turnoverChart.setOption({
                        xAxis: { data: charts.turnoverTrend.xAxis },
                        series: [{ data: charts.turnoverTrend.yData }]
                    })
                }
                // B. 更新效能条形图
                if (efficiencyChart && charts.pickingEfficiency) {
                    efficiencyChart.setOption({
                        yAxis: { data: charts.pickingEfficiency.categories },
                        series: [{ data: charts.pickingEfficiency.values }]
                    })
                }
                // C. 💡 新增：更新任务状态分布环形图
                if (taskStatusChart && charts.taskStatusDistribution) {
                    taskStatusChart.setOption({
                        series: [{
                            data: charts.taskStatusDistribution.map(item => ({
                                name: item.statusName || item.key || '其他',
                                value: item.taskCount || item.value || 0
                            }))
                        }]
                    })
                }
                // D. 💡 新增：更新告警级别分布玫瑰图
                if (alertLevelChart && charts.alertLevelDistribution) {
                    alertLevelChart.setOption({
                        series: [{
                            data: charts.alertLevelDistribution.map(item => ({
                                name: item.levelName || item.key || '正常',
                                value: item.alertCount || item.value || 0
                            }))
                        }]
                    })
                }
            }
        }
    } catch (error) {
        console.error('WMS中台驾驶舱多维核心数据抓取异常:', error)
    }
}

// ===== 获取视频推流状态 =====
const fetchStatus = async () => {
    try {
        const res = await axios.get('/api/video/status', { params: { streamId: streamId.value } })
        const responseData = res.data.code !== undefined ? res.data.data : res.data
        if (res.data.code === 0 || res.data.code === 200 || responseData) {
            isRunning.value = responseData.status === 'running'
            hlsUrl.value = isRunning.value ? `${HLS_BASE_URL}${streamId.value}.m3u8` : ''
        }
    } catch (error) {
        console.error('获取视频状态失败:', error)
    }
}

// ===== 启动与停止推流业务 =====
const startPush = async () => {
    videoLoading.value = true
    try {
        const res = await axios.post('/api/video/start2', null, {
            params: { streamId: streamId.value, rtspUrl: '' }
        })
        if (res.data.code === 0 || res.data.code === 200) {
            isRunning.value = true
            hlsUrl.value = `${HLS_BASE_URL}${streamId.value}.m3u8`
            ElMessage.success('监控视频推流启动成功')
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
        const res = await axios.post('/api/video/stop', null, { params: { streamId: streamId.value } })
        if (res.data.code === 0 || res.data.code === 200) {
            isRunning.value = false
            hlsUrl.value = ''
            // 停止推流时自动关闭 AI 检测
            if (enableAI.value) {
                enableAI.value = false
                aiVideoSource.value = ''
            }
            ElMessage.success('推流已停止')
        }
    } catch (error) {
        console.error('停止推流失败:', error)
    } finally {
        videoLoading.value = false
    }
}

// ===== 3D 孪生相机控制层 =====
const switchFloor = (floor) => {
    threeSceneRef.value?.switchFloor(floor)
}
const flyToView = (view) => {
    threeSceneRef.value?.flyToView(view)
}

// ===== 分辨率检测与多图表动态实例化 =====
const checkDevice = () => {
    isMobile.value = window.innerWidth <= 1024
    if (!isMobile.value) {
        nextTick(() => {
            initCharts()
            fetchDashboardOverview() // 图表骨架挂载完毕，立即同步灌入后端真实数据
        })
    }
}

// ===== 📊 集中初始化 4 张 ECharts 图表骨架配置 =====
const initCharts = () => {
    // 1. 周转趋势折线图
    if (turnoverChartRef.value) {
        turnoverChart = echarts.init(turnoverChartRef.value)
        turnoverChart.setOption({
            backgroundColor: 'transparent',
            tooltip: { trigger: 'axis', backgroundColor: '#1e293b', borderColor: '#334155', textStyle: { color: '#f8fafc' } },
            grid: { top: '15%', left: '3%', right: '4%', bottom: '5%', containLabel: true },
            xAxis: {
                type: 'category',
                data: ['-','-','-','-','-','-','-'],
                axisLine: { lineStyle: { color: '#475569' } },
                axisLabel: { color: '#94a3b8', fontSize: 10 }
            },
            yAxis: {
                type: 'value',
                splitLine: { lineStyle: { color: '#334155', type: 'dashed' } },
                axisLabel: { color: '#94a3b8', fontSize: 10 }
            },
            series: [{
                name: '周转效能',
                data: [0, 0, 0, 0, 0, 0, 0],
                type: 'line',
                smooth: true,
                color: '#38bdf8',
                lineStyle: { width: 2.5 },
                areaStyle: {
                    color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                        { offset: 0, color: 'rgba(56, 189, 248, 0.25)' },
                        { offset: 1, color: 'rgba(56, 189, 248, 0)' }
                    ])
                }
            }]
        })
    }

    // 2. 智能硬件效能条形图
    if (efficiencyChartRef.value) {
        efficiencyChart = echarts.init(efficiencyChartRef.value)
        efficiencyChart.setOption({
            backgroundColor: 'transparent',
            tooltip: { trigger: 'axis', backgroundColor: '#1e293b', borderColor: '#334155', textStyle: { color: '#f8fafc' } },
            grid: { top: '10%', left: '3%', right: '8%', bottom: '5%', containLabel: true },
            xAxis: {
                type: 'value',
                splitLine: { lineStyle: { color: '#334155', type: 'dashed' } },
                axisLabel: { color: '#94a3b8', fontSize: 10 }
            },
            yAxis: {
                type: 'category',
                data: [],
                axisLine: { lineStyle: { color: '#475569' } },
                axisLabel: { color: '#94a3b8', fontSize: 10 }
            },
            series: [{
                name: '综合吞吐量',
                type: 'bar',
                color: '#10b981',
                data: [],
                itemStyle: { borderRadius: [0, 4, 4, 0] }
            }]
        })
    }

    // 3. 💡 新增：任务状态环形饼图骨架
    if (taskStatusChartRef.value) {
        taskStatusChart = echarts.init(taskStatusChartRef.value)
        taskStatusChart.setOption({
            backgroundColor: 'transparent',
            tooltip: { trigger: 'item', backgroundColor: '#1e293b', borderColor: '#334155', textStyle: { color: '#f8fafc' } },
            legend: { orient: 'vertical', left: 'right', top: 'center', textStyle: { color: '#94a3b8', fontSize: 10 } },
            series: [{
                name: '调度任务',
                type: 'pie',
                radius: ['45%', '70%'],
                center: ['40%', '50%'],
                avoidLabelOverlap: false,
                label: { show: false },
                labelLine: { show: false },
                data: []
            }]
        })
    }

    // 4. 💡 新增：告警层级南丁格尔玫瑰图骨架
    if (alertLevelChartRef.value) {
        alertLevelChart = echarts.init(alertLevelChartRef.value)
        alertLevelChart.setOption({
            backgroundColor: 'transparent',
            tooltip: { trigger: 'item', backgroundColor: '#1e293b', borderColor: '#334155', textStyle: { color: '#f8fafc' } },
            series: [{
                name: '告警层级',
                type: 'pie',
                radius: [15, 60],
                center: ['50%', '50%'],
                roseType: 'radius',
                itemStyle: { borderRadius: 4 },
                label: { color: '#94a3b8', fontSize: 10 },
                data: []
            }]
        })
    }
}

// 处理视口缩放与 ECharts 自适应重绘
const handleResize = () => {
    checkDevice()
    turnoverChart?.resize()
    efficiencyChart?.resize()
    taskStatusChart?.resize()  // 💡 新增
    alertLevelChart?.resize()  // 💡 新增
}

// ===== 生命周期安全控制栏 =====
onMounted(() => {
    // 1. 视频流推流状态短轮询
    fetchStatus()
    statusTimer = setInterval(fetchStatus, 3000)

    // 2. 初始化双端分辨率适配检测与图表装载
    checkDevice()
    window.addEventListener('resize', handleResize)

    // 3. ⚡ 中台大盘聚合控制塔核心数据：每隔 12 秒向后端要一次最新动态
    dataTimer = setInterval(fetchDashboardOverview, 12000)

    // 4. 模拟无线传感网突发物联网数据刷入
    alarmTimer = setInterval(() => {
        const mockLocs = ['A区冷链', 'AGV #05', '分拣滑道B', '出库月台']
        const mockTexts = ['堆垛托盘发生无线离线告警', '电量低于 15% 申请回充', '主履带红外传感器受阻', 'PDA扫码枪异常中断']
        const levels = ['danger', 'warning', 'info']

        alarmList.value.unshift({
            time: new Date().toTimeString().split(' ')[0],
            location: mockLocs[Math.floor(Math.random() * mockLocs.length)],
            text: mockTexts[Math.floor(Math.random() * mockTexts.length)],
            level: levels[Math.floor(Math.random() * levels.length)]
        })
        if (alarmList.value.length > 5) alarmList.value.pop()
    }, 14000)
})

onBeforeUnmount(() => {
    // 全局句柄与事件强力卸载，杜绝线程驻留与内存溢出
    if (statusTimer) clearInterval(statusTimer)
    if (alarmTimer) clearInterval(alarmTimer)
    if (dataTimer) clearInterval(dataTimer)
    window.removeEventListener('resize', handleResize)

    turnoverChart?.dispose()
    efficiencyChart?.dispose()
    taskStatusChart?.dispose() // 💡 新增
    alertLevelChart?.dispose() // 💡 新增
})
</script>

<style scoped>
/* 整个大屏视窗画布基座 */
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
    position: absolute;
    top: 0;
    left: 0;
    z-index: 1;
}

/* 2D 透明图表大屏核心控制浮层层级 */
.dashboard-ui-overlay {
    position: absolute;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
    z-index: 10;
    pointer-events: none; /* 穿透机制：确保用户依然可以拖拽转动底层的3D货位模型 */
    display: flex;
    justify-content: space-between;
    padding: 20px;
    box-sizing: border-box;
}

/* 左右两侧控制舱面板公共样式 - 工业暗黑磨砂玻璃 */
.overlay-panel {
    pointer-events: auto; /* 允许浮层内的按钮、图表交互 */
    width: 390px;
    height: 100%;
    background: rgba(15, 23, 42, 0.82);
    backdrop-filter: blur(12px);
    -webkit-backdrop-filter: blur(12px);
    border: 1px solid rgba(51, 65, 85, 0.7);
    border-radius: 12px;
    padding: 18px;
    box-sizing: border-box;
    display: flex;
    flex-direction: column;
    box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.6);
}

/* 左侧标题及说明块 */
.system-title-block {
    margin-bottom: 16px;
}
.system-title-block h3 {
    margin: 0 0 6px 0;
    color: #38bdf8;
    font-size: 16px;
    letter-spacing: 0.5px;
}
.system-title-block p {
    margin: 0;
    font-size: 12px;
    color: #94a3b8;
    line-height: 1.4;
}

.panel-section-title {
    font-size: 14px;
    font-weight: bold;
    color: #38bdf8;
    border-left: 3px solid #38bdf8;
    padding-left: 8px;
    margin-bottom: 12px;
}

/* 左侧：指标卡片样式 */
.metrics-grid {
    display: flex;
    flex-direction: column;
    gap: 10px;
}
.metric-card {
    background: rgba(30, 41, 59, 0.45);
    border: 1px solid rgba(71, 85, 105, 0.4);
    border-radius: 8px;
    padding: 10px 14px;
    display: flex;
    align-items: center;
    position: relative;
    overflow: hidden;
}
.metric-card .m-icon {
    font-size: 22px;
    margin-right: 14px;
    background: rgba(15, 23, 42, 0.5);
    padding: 6px;
    border-radius: 6px;
}
.metric-card .m-info {
    display: flex;
    flex-direction: column;
}
.metric-card .m-label { font-size: 11px; color: #94a3b8; }
.metric-card .m-value { font-size: 20px; font-weight: 800; color: #f8fafc; margin-top: 2px;}
.metric-card .m-value small { font-size: 11px; font-weight: normal; color: #64748b; }
.metric-card.inbound .m-value { color: #10b981; }
.metric-card.outbound .m-value { color: #fbbf24; }
.metric-card.saturation .m-progress { position: absolute; bottom: 0; left: 0; width: 100%; }

.chart-container {
    flex: 1;
    width: 100%;
    min-height: 160px;
}

/* 右侧：相机视角与楼层控制按钮悬浮盒 (位置微调至左栏右侧，绝不冲突) */
.controls {
    position: absolute;
    top: 20px;
    left: 430px;
    display: flex;
    flex-direction: column;
    gap: 10px;
    z-index: 20;
    background: rgba(15, 23, 42, 0.85);
    padding: 12px;
    border-radius: 8px;
    border: 1px solid #334155;
    pointer-events: auto;
}
.floor-btns, .view-btns { display: flex; gap: 8px; }
.controls button {
    background: #1e293b;
    border: 1px solid #334155;
    color: #f8fafc;
    padding: 6px 12px;
    border-radius: 4px;
    cursor: pointer;
    font-size: 12px;
    transition: all 0.2s;
}
.controls button:hover {
    background: #38bdf8;
    color: #0f172a;
    border-color: #38bdf8;
}

/* 右侧：物联网告警列表流 */
.alarm-list-box {
    height: 150px;
    overflow-y: auto;
    display: flex;
    flex-direction: column;
    gap: 8px;
    margin-bottom: 16px;
}
.alarm-list-box::-webkit-scrollbar { width: 0; }
.alarm-item {
    background: rgba(30, 41, 59, 0.25);
    border-left: 3px solid #64748b;
    padding: 8px 10px;
    display: flex;
    gap: 8px;
}
.alarm-dot { width: 5px; height: 5px; border-radius: 50%; margin-top: 5px; background-color: #64748b; }
.alarm-meta { display: flex; gap: 10px; font-size: 10px; color: #64748b; }
.alarm-text { font-size: 11.5px; color: #e2e8f0; margin-top: 2px; }
.alarm-item.danger { background: rgba(244, 63, 94, 0.08); border-color: #f43f5e; }
.alarm-item.danger .alarm-dot { background-color: #f43f5e; box-shadow: 0 0 6px #f43f5e; }
.alarm-item.warning { background: rgba(245, 158, 11, 0.08); border-color: #f59e0b; }
.alarm-item.warning .alarm-dot { background-color: #f59e0b; box-shadow: 0 0 6px #f59e0b; }

/* 右侧最下方：视频流推流收纳舱 */
.embedded-video-section {
    background: rgba(30, 41, 59, 0.4);
    border: 1px solid #334155;
    border-radius: 8px;
    padding: 10px;
}
.video-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 6px;
}
.video-controls {
    display: flex;
    align-items: center;
    gap: 6px;
    flex-wrap: wrap;
}
.video-title { color: #f8fafc; font-size: 12.5px; font-weight: 600; }
.video-controls button {
    padding: 4px 10px;
    padding: 3px 10px;
    border: none;
    border-radius: 4px;
    font-size: 11px;
    cursor: pointer;
    font-weight: bold;
}
.btn-start { background: #38bdf8; color: #0f172a; }
.btn-stop { background: #ef4444; color: white; }
.video-wrapper { width: 100%; aspect-ratio: 16 / 9; background: #000; border-radius: 4px; overflow: hidden; }
.btn-stop {
    background: #ef4444;
    color: white;
}
.btn-ai {
    background: transparent;
    border: 1px solid #8b5cf6 !important;
    color: #8b5cf6;
    font-weight: 500;
}
.btn-ai.active {
    background: #8b5cf6;
    color: #fff;
}
.btn-ai:disabled {
    border-color: #475569 !important;
    color: #475569;
}
.status-tag {
    font-size: 11px;
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
    color: #64748b;
    font-size: 12px;
    gap: 6px;
}
.status-tag { font-size: 10px; padding: 1px 6px; border-radius: 10px; color: #fff; background: #475569; }

/* ==================== 响应式移动端样式 (PDA降级) ==================== */
.mock-pda-bg {
    width: 100%;
    height: 100%;
    background-color: #0f172a;
    display: flex;
    justify-content: center;
    align-items: center;
}
.pda-tip-text { color: #64748b; font-size: 13px; }
.mobile-pda-layout {
    position: absolute;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
    z-index: 30;
    background-color: #0b0f19;
    padding: 16px;
    box-sizing: border-box;
    overflow-y: auto;
}
.pda-header { color: #38bdf8; font-weight: bold; font-size: 15px; margin-bottom: 16px; border-bottom: 1px solid #1e293b; padding-bottom: 8px;}
.mobile-metric { background: #1e293b; border: 1px solid #334155; padding: 12px; border-radius: 6px; display: flex; flex-direction: column; }
.mobile-metric .label { font-size: 11px; color: #64748b; }
.mobile-metric .val { font-size: 22px; font-weight: bold; margin-top: 4px; }
.mobile-alarm-card { background: #1e293b; border: 1px solid #334155; border-radius: 6px; padding: 12px; }
.mobile-alarm-card .m-title { font-size: 13px; font-weight: bold; color: #f43f5e; margin-bottom: 8px; }
.m-alarm-row { font-size: 12px; color: #94a3b8; padding: 6px 0; border-bottom: 1px dashed #334155; }
.m-alarm-row span { color: #38bdf8; }

/* 炫酷的动态飞入动画 */
.animate-left { animation: slideLeft 0.4s ease-out forwards; }
.animate-right { animation: slideRight 0.4s ease-out forwards; }
@keyframes slideLeft { from { transform: translateX(-60px); opacity: 0; } to { transform: translateX(0); opacity: 1; } }
@keyframes slideRight { from { transform: translateX(60px); opacity: 0; } to { transform: translateX(0); opacity: 1; } }
</style>