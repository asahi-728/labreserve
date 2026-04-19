<template>
  <div class="app-container">
    <!-- 页面标题 -->
    <el-page-header @back="goBack" title="PPO智能调度算法">
      <template #content>
        <el-tag v-if="serviceStatus === 'online'" type="success">服务在线</el-tag>
        <el-tag v-else type="danger">服务离线</el-tag>
      </template>
    </el-page-header>

    <!-- 主要内容区 -->
    <el-row :gutter="20" class="mt-20">
      <!-- 左侧：算法参数配置区 -->
      <el-col :span="8">
        <el-card class="box-card">
          <template #header>
            <div class="card-header">
              <span>算法参数配置</span>
              <el-button type="primary" size="small" @click="resetParams">重置参数</el-button>
            </div>
          </template>
          <el-form :model="ppoParams" label-width="120px">
            <el-form-item label="隐藏层大小">
              <el-input-number v-model="ppoParams.hiddenSize" :min="16" :max="128" />
            </el-form-item>
            <el-form-item label="学习率">
              <el-slider v-model="ppoParams.lr" :min="0.0001" :max="0.01" :step="0.0001" :format-tooltip="(val) => val.toFixed(4)" />
              <span class="ml-10">{{ ppoParams.lr.toFixed(4) }}</span>
            </el-form-item>
            <el-form-item label="折扣因子">
              <el-slider v-model="ppoParams.gamma" :min="0.8" :max="0.999" :step="0.001" :format-tooltip="(val) => val.toFixed(3)" />
              <span class="ml-10">{{ ppoParams.gamma.toFixed(3) }}</span>
            </el-form-item>
            <el-form-item label="裁剪系数">
              <el-slider v-model="ppoParams.clipEpsilon" :min="0.1" :max="0.3" :step="0.01" :format-tooltip="(val) => val.toFixed(2)" />
              <span class="ml-10">{{ ppoParams.clipEpsilon.toFixed(2) }}</span>
            </el-form-item>
            <el-form-item label="训练轮数">
              <el-input-number v-model="ppoParams.epochs" :min="5" :max="100" />
            </el-form-item>
            <el-form-item label="最大步数">
              <el-input-number v-model="ppoParams.maxSteps" :min="50" :max="500" />
            </el-form-item>
            <el-form-item label="批次大小">
              <el-input-number v-model="ppoParams.batchSize" :min="8" :max="128" />
            </el-form-item>
            <el-form-item>
              <el-button type="warning" @click="handleTrain" :loading="training">开始训练</el-button>
            </el-form-item>
          </el-form>
        </el-card>

        <!-- 算法说明文档区 -->
        <el-card class="box-card mt-20">
          <template #header>
            <span>算法说明文档</span>
          </template>
          <el-collapse v-model="activeDocs">
            <el-collapse-item title="PPO算法简介" name="intro">
              <p>PPO（Proximal Policy Optimization）是一种强化学习算法，用于解决序列决策问题。</p>
              <p>在实验室设备预约调度场景中，PPO能够学习最优的调度策略，根据当前环境状态（设备可用情况、用户优先级等）做出智能决策。</p>
            </el-collapse-item>
            <el-collapse-item title="参数说明" name="params">
              <ul>
                <li><strong>隐藏层大小</strong>：策略网络和价值网络的隐藏单元数量</li>
                <li><strong>学习率</strong>：模型训练时的学习速率</li>
                <li><strong>折扣因子</strong>：未来奖励的衰减系数</li>
                <li><strong>裁剪系数</strong>：PPO算法的裁剪参数，防止策略更新过大</li>
                <li><strong>训练轮数</strong>：模型训练的迭代次数</li>
                <li><strong>最大步数</strong>：每轮训练的最大步数</li>
                <li><strong>批次大小</strong>：每次更新使用的样本数量</li>
              </ul>
            </el-collapse-item>
          </el-collapse>
        </el-card>
      </el-col>

      <!-- 中间：算法执行状态显示区 -->
      <el-col :span="8">
        <el-card class="box-card">
          <template #header>
            <div class="card-header">
              <span>执行状态</span>
              <el-button type="text" @click="clearLogs">清空日志</el-button>
            </div>
          </template>
          
          <!-- 进度指示 -->
          <div v-if="training" class="progress-section">
            <p class="mb-10">训练进度：{{ trainingProgress }}%</p>
            <el-progress :percentage="trainingProgress" :color="getProgressColor(trainingProgress)" />
            <p class="mt-10 text-sm text-gray-500">
              当前轮数：{{ currentEpoch }} / {{ ppoParams.epochs }}
            </p>
          </div>

          <!-- 日志输出 -->
          <div class="log-section">
            <div class="log-header">
              <span>运行日志</span>
              <el-select v-model="logLevel" size="small" style="width: 100px">
                <el-option label="全部" value="all" />
                <el-option label="信息" value="info" />
                <el-option label="警告" value="warn" />
                <el-option label="错误" value="error" />
              </el-select>
            </div>
            <div class="log-content" ref="logContentRef">
              <div v-for="(log, index) in filteredLogs" :key="index" :class="['log-item', log.type]">
                <span class="log-time">[{{ log.time }}]</span>
                <span class="log-type">[{{ log.type.toUpperCase() }}]</span>
                <span class="log-message">{{ log.message }}</span>
              </div>
              <div v-if="filteredLogs.length === 0" class="log-empty">暂无日志</div>
            </div>
          </div>
        </el-card>
      </el-col>

      <!-- 右侧：结果可视化展示区和历史记录查询区 -->
      <el-col :span="8">
        <el-card class="box-card">
          <template #header>
            <div class="card-header">
              <span>结果可视化</span>
              <el-radio-group v-model="resultTab" size="small">
                <el-radio-button label="current">当前训练</el-radio-button>
                <el-radio-button label="history">历史记录</el-radio-button>
              </el-radio-group>
            </div>
          </template>

          <!-- 当前训练图表 -->
          <div v-show="resultTab === 'current'" class="chart-container">
            <div ref="chartRef" v-show="hasResults" style="width: 100%; height: 300px;"></div>
            <div v-show="!hasResults" class="chart-empty">
              <el-empty description="请先执行训练以查看结果" />
            </div>
          </div>

          <!-- 历史记录列表 -->
          <div v-show="resultTab === 'history'" class="history-section">
            <!-- 历史记录表格 -->
            <div v-if="!selectedHistoryId" class="history-list">
              <div v-loading="historyLoading">
                <div v-for="item in historyList" :key="item.trainingId"
                     class="history-item" @click="viewHistoryDetail(item)">
                  <div class="history-item-header">
                    <el-tag :type="item.status === 'COMPLETED' ? 'success' : item.status === 'FAILED' ? 'danger' : 'warning'" size="small">
                      {{ statusText(item.status) }}
                    </el-tag>
                    <span class="history-item-time">{{ item.createTime }}</span>
                  </div>
                  <div class="history-item-body">
                    <div class="history-item-title">{{ item.taskName || ('PPO训练 #' + item.trainingId) }}</div>
                    <div class="history-item-meta">
                      <span>轮数: {{ item.completedEpochs || 0 }}/{{ item.totalEpochs || '-' }}</span>
                      <span v-if="item.lossValue">Loss: {{ parseFloat(item.lossValue).toFixed(4) }}</span>
                    </div>
                  </div>
                  <el-icon class="history-arrow"><ArrowRight /></el-icon>
                </div>
                <div v-if="!historyLoading && historyList.length === 0" class="chart-empty">
                  <el-empty description="暂无历史训练记录" :image-size="80" />
                </div>
              </div>
            </div>

            <!-- 历史详情（图表回放） -->
            <div v-if="selectedHistoryId" class="history-detail">
              <div class="history-detail-header">
                <el-button link type="primary" size="small" @click="selectedHistoryId = null">
                  <el-icon><ArrowLeft /></el-icon> 返回列表
                </el-button>
                <span class="history-detail-title">{{ selectedRecord?.taskName || ('训练 #' + selectedHistoryId) }}</span>
              </div>
              <div ref="historyChartRef" style="width: 100%; height: 280px;"></div>
              <div v-if="selectedRecord" class="history-detail-info">
                <el-descriptions :column="2" border size="small">
                  <el-descriptions-item label="状态">
                    <el-tag :type="selectedRecord.status === 'COMPLETED' ? 'success' : 'danger'" size="small">
                      {{ statusText(selectedRecord.status) }}
                    </el-tag>
                  </el-descriptions-item>
                  <el-descriptions-item label="模型版本">{{ selectedRecord.modelVersion || '-' }}</el-descriptions-item>
                  <el-descriptions-item label="完成轮数">{{ selectedRecord.completedEpochs || 0 }} / {{ selectedRecord.totalEpochs || '-' }}</el-descriptions-item>
                  <el-descriptions-item label="最终 Loss">{{ selectedRecord.lossValue ? parseFloat(selectedRecord.lossValue).toFixed(6) : '-' }}</el-descriptions-item>
                  <el-descriptions-item label="训练时间" :span="2">{{ selectedRecord.startTime }} ~ {{ selectedRecord.endTime || '-' }}</el-descriptions-item>
                </el-descriptions>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

  </div>
</template>

<script setup name="PpoAlgorithm">
import { ref, reactive, computed, onMounted, nextTick, watch, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowRight, ArrowLeft } from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import { ppoHealth, ppoStatus, ppoTrain, ppoTrainStatus, ppoTrainLogs, ppoTrainLogsClear } from '@/api/lab/algorithm'
import { startTraining, completeTraining as completeTrainingApi, listTrainingLog, getTrainingLog } from '@/api/lab/algorithm'

const router = useRouter()
const { proxy } = getCurrentInstance()

// 响应式数据
const serviceStatus = ref('offline')
const training = ref(false)
const trainingProgress = ref(0)
const currentEpoch = ref(0)
const activeDocs = ref(['intro'])
const logLevel = ref('all')
const chartType = ref('line')
const hasResults = ref(false)
const chartRef = ref(null)
const historyChartRef = ref(null)
const logContentRef = ref(null)
// 结果Tab：current=当前训练 | history=历史记录
const resultTab = ref('current')
// 历史记录数据
const historyList = ref([])
const historyLoading = ref(false)
const selectedHistoryId = ref(null)
const selectedRecord = ref(null)
let historyChartInstance = null
// 训练指标数据（从日志中解析）
const trainMetrics = ref({
  epochs: [],
  rewards: [],
  losses: [],
  policyLosses: [],
  valueLosses: []
})
let chartInstance = null
let trainingInterval = null
// 状态文本映射
function statusText(status) {
  const map = { RUNNING: '训练中', COMPLETED: '已完成', FAILED: '失败', PENDING: '等待中' }
  return map[status] || status || '-'
}

// PPO参数
const ppoParams = reactive({
  hiddenSize: 32,
  lr: 0.0003,
  gamma: 0.99,
  clipEpsilon: 0.2,
  epochs: 10,
  maxSteps: 100,
  batchSize: 32,
  forceReload: false
})

// 日志数据
const logs = ref([])

// 过滤后的日志
const filteredLogs = computed(() => {
  if (logLevel.value === 'all') {
    return logs.value
  }
  return logs.value.filter(log => log.type === logLevel.value)
})

// 添加日志
function addLog(message, type = 'info') {
  const time = new Date().toLocaleTimeString()
  logs.value.unshift({ time, message, type })
  if (logs.value.length > 100) {
    logs.value.pop()
  }
  nextTick(() => {
    if (logContentRef.value) {
      logContentRef.value.scrollTop = 0
    }
  })
}

// 清空日志
async function clearLogs() {
  try {
    await ppoTrainLogsClear()
    logs.value = []
  } catch (error) {
    logs.value = []
  }
}

// 重置参数
function resetParams() {
  ppoParams.hiddenSize = 32
  ppoParams.lr = 0.0003
  ppoParams.gamma = 0.99
  ppoParams.clipEpsilon = 0.2
  ppoParams.epochs = 10
  ppoParams.maxSteps = 100
  ppoParams.batchSize = 32
  ppoParams.forceReload = false
  addLog('参数已重置为默认值', 'info')
}

// 检查服务状态
async function checkServiceStatus() {
  try {
    const response = await ppoHealth()
    if (response.code === 200) {
      serviceStatus.value = 'online'
      addLog('PPO服务连接成功', 'info')
    } else {
      serviceStatus.value = 'offline'
      addLog('PPO服务连接失败', 'error')
    }
  } catch (error) {
    serviceStatus.value = 'offline'
    addLog('PPO服务连接失败: ' + error.message, 'error')
  }
}

// 当前训练任务ID（用于保存到数据库）
let currentTrainingId = null

// 开始训练
async function handleTrain() {
  if (serviceStatus.value !== 'online') {
    ElMessage.error('PPO服务未连接，请先确保服务正常运行')
    return
  }

  training.value = true
  trainingProgress.value = 0
  currentEpoch.value = 0
  // 重置训练指标
  trainMetrics.value = { epochs: [], rewards: [], losses: [], policyLosses: [], valueLosses: [] }

  try {
    // 先清空服务器日志
    await ppoTrainLogsClear()
    logs.value = []
    
    addLog('开始训练模型...', 'info')
    
    // 创建训练记录到数据库
    const trainRes = await startTraining({
      algorithmType: 'PPO',
      taskName: 'PPO智能调度训练',
      trainingParams: JSON.stringify(ppoParams),
      totalEpochs: ppoParams.epochs,
      status: 'RUNNING'
    })
    currentTrainingId = trainRes.data || null
    addLog(currentTrainingId ? ('训练记录已创建，ID: ' + currentTrainingId) : '训练记录创建（仅本地）', 'info')
    
    const response = await ppoTrain({
      force_reload: ppoParams.forceReload,
      epochs: ppoParams.epochs,
      hidden_size: ppoParams.hiddenSize,
      lr: ppoParams.lr,
      gamma: ppoParams.gamma,
      clip_epsilon: ppoParams.clipEpsilon,
      batch_size: ppoParams.batchSize,
      max_steps: ppoParams.maxSteps
    })

    if (response.code === 200) {
      addLog('训练任务已启动', 'info')
      // 开始轮询训练状态
      trainingInterval = setInterval(checkTrainingStatus, 2000)
    } else {
      addLog('训练任务启动失败: ' + response.msg, 'error')
      training.value = false
    }
  } catch (error) {
    addLog('训练任务启动异常: ' + error.message, 'error')
    training.value = false
  }
}

// 检查训练状态
async function checkTrainingStatus() {
  try {
    // 同时获取训练状态和日志
    const [statusResponse, logsResponse] = await Promise.all([
      ppoTrainStatus(),
      ppoTrainLogs()
    ])
    
    if (statusResponse.code === 200) {
      let status = statusResponse.data
      // 处理双重包装情况：{code:200, message:"success", data: {...}}
      if (status && status.data && typeof status.data === 'object' && !Array.isArray(status.data)) {
        status = status.data
      }
      if (status) {
        if (status.is_training) {
          currentEpoch.value = status.current_epoch
          trainingProgress.value = Math.round((status.current_epoch / status.total_epochs) * 100)
          // 从状态中提取训练指标（如果后端返回了）
          parseTrainMetrics(status, logsResponse)
        } else {
          // 训练完成或未在训练
          clearInterval(trainingInterval)
          training.value = false
          // 确保进度显示100%
          if (status.progress >= 1.0 || status.current_epoch >= status.total_epochs) {
            trainingProgress.value = 100
            currentEpoch.value = status.total_epochs
            ElMessage.success('模型训练完成')
            // 训练完成后，标记有结果并展示训练图表
            hasResults.value = true
            // 最终一次从 status.metrics 获取完整训练指标（优先），否则解析日志
            if (status && status.metrics && Array.isArray(status.metrics.epochs) && status.metrics.epochs.length > 0) {
              trainMetrics.value = {
                epochs: [...status.metrics.epochs],
                rewards: [...(status.metrics.rewards || [])],
                losses: [...(status.metrics.losses || [])],
                policyLosses: [...(status.metrics.policy_losses || [])],
                valueLosses: [...(status.metrics.value_losses || [])],
              }
            } else {
              parseAllLogMetrics(logsResponse)
            }
            renderTrainChart()
            
            // 将训练指标保存到数据库
            saveTrainingMetricsToDB()
          }
        }
      }
    }
    
    // 更新日志显示 - 处理双重包装的响应
    if (logsResponse.code === 200 && logsResponse.data) {
      let serverLogs = logsResponse.data
      // 处理双重包装情况：{code:200, message:"success", data: [...]}
      if (serverLogs && serverLogs.data && Array.isArray(serverLogs.data)) {
        serverLogs = serverLogs.data
      }
      // 确保是数组
      if (Array.isArray(serverLogs)) {
        logs.value = serverLogs.map(log => ({
          time: log.time,
          message: log.message,
          type: log.type
        }))
      }
    }
  } catch (error) {
    clearInterval(trainingInterval)
    training.value = false
    addLog('训练状态查询失败: ' + error.message, 'error')
  }
}

// 从训练状态中解析训练指标（优先从 metrics 字段获取真实数据）
function parseTrainMetrics(status, logsResponse) {
  if (!status) return
  
  // 优先从后端 /train/status 返回的 metrics 字段获取每轮真实指标
  if (status.metrics && Array.isArray(status.metrics.epochs) && status.metrics.epochs.length > 0) {
    trainMetrics.value = {
      epochs: [...status.metrics.epochs],
      rewards: [...(status.metrics.rewards || [])],
      losses: [...(status.metrics.losses || [])],
      policyLosses: [...(status.metrics.policy_losses || [])],
      valueLosses: [...(status.metrics.value_losses || [])],
    }
    return
  }
  
  // 兼容旧格式：从状态顶层字段提取
  const epoch = status.current_epoch || 0
  const reward = status.avg_reward || status.reward || null
  const loss = status.loss || null
  const pLoss = status.policy_loss || null
  const vLoss = status.value_loss || null
  
  if (reward !== null || loss !== null || pLoss !== null || vLoss !== null) {
    const lastEpoch = trainMetrics.value.epochs[trainMetrics.value.epochs.length - 1]
    if (lastEpoch !== epoch) {
      trainMetrics.value.epochs.push(epoch)
      trainMetrics.value.rewards.push(reward !== null ? parseFloat(reward) : null)
      trainMetrics.value.losses.push(loss !== null ? parseFloat(loss) : null)
      trainMetrics.value.policyLosses.push(pLoss !== null ? parseFloat(pLoss) : null)
      trainMetrics.value.valueLosses.push(vLoss !== null ? parseFloat(vLoss) : null)
    }
  }
}

// 从全部日志中解析训练指标（训练完成后调用）
function parseAllLogMetrics(logsResponse) {
  if (!logsResponse || !logsResponse.data) return
  let serverLogs = logsResponse.data
  if (serverLogs && serverLogs.data && Array.isArray(serverLogs.data)) {
    serverLogs = serverLogs.data
  }
  if (!Array.isArray(serverLogs)) return
  
  const parsedEpochs = []
  const parsedRewards = []
  const parsedLosses = []
  const parsedPLosses = []
  const parsedVLosses = []
  
  for (const log of serverLogs) {
    const msg = log.message || ''
    // 匹配 PPO 日志格式，如 "Epoch 3/10 | Reward: 12.34 | Loss: 0.5678 | Policy Loss: 0.1234 | Value Loss: 0.3456"
    const epochMatch = msg.match(/(?:Epoch|epoch|Ep)\s+(\d+)\s*\/\s*(\d+)/i)
    const rewardMatch = msg.match(/[Rr]eward[:\s]+([\d.-]+)/)
    const lossMatch = msg.match(/[Ll]oss[:\s]+([\d.]+)/)
    const pLossMatch = msg.match(/[Pp]olicy[_\s]?[Ll]oss[:\s]+([\d.]+)/)
    const vLossMatch = msg.match(/[Vv]alue[_\s]?[Ll]oss[:\s]+([\d.]+)/)
    
    if (epochMatch) {
      parsedEpochs.push(parseInt(epochMatch[1]))
      parsedRewards.push(rewardMatch ? parseFloat(rewardMatch[1]) : null)
      parsedLosses.push(lossMatch ? parseFloat(lossMatch[1]) : null)
      parsedPLosses.push(pLossMatch ? parseFloat(pLossMatch[1]) : null)
      parsedVLosses.push(vLossMatch ? parseFloat(vLossMatch[1]) : null)
    }
  }
  
  if (parsedEpochs.length > 0) {
    trainMetrics.value = {
      epochs: parsedEpochs,
      rewards: parsedRewards,
      losses: parsedLosses,
      policyLosses: parsedPLosses,
      valueLosses: parsedVLosses
    }
  }
}

// 将训练指标保存到数据库
async function saveTrainingMetricsToDB() {
  if (!currentTrainingId) {
    console.warn('[PPO] 无 trainingId，跳过保存指标')
    return
  }
  const metrics = trainMetrics.value
  if (metrics.epochs.length === 0) {
    addLog('无训练指标数据，跳过保存', 'warn')
    return
  }
  
  // 构建每轮指标的 JSON
  const lastLoss = metrics.losses[metrics.losses.length - 1]
  const metricsJson = JSON.stringify({
    epochs: metrics.epochs,
    rewards: metrics.rewards,
    losses: metrics.losses,
    policy_losses: metrics.policyLosses,
    value_losses: metrics.valueLosses,
    algorithm_type: 'PPO'
  })
  
  try {
    await completeTrainingApi(currentTrainingId, {
      lossValue: lastLoss !== null && lastLoss !== undefined ? parseFloat(lastLoss) : null,
      accuracy: null,
      modelVersion: 'v' + new Date().getTime(),
      metrics: metricsJson
    })
    addLog('训练指标已保存到数据库（ID: ' + currentTrainingId + '）', 'info')
  } catch (error) {
    addLog('保存训练指标失败: ' + error.message, 'error')
  }
}

// 渲染训练图表
function renderTrainChart() {
  // 使用 nextTick + 延迟确保 DOM 完全就绪
  nextTick(() => {
    setTimeout(() => {
      if (!chartRef.value) {
        console.warn('[PPO] chartRef 不存在，无法渲染图表')
        return
      }
      if (chartInstance) {
        chartInstance.dispose()
      }
      chartInstance = echarts.init(chartRef.value)
      console.log('[PPO] ECharts 初始化成功，容器尺寸:', chartRef.value.offsetWidth, 'x', chartRef.value.offsetHeight)
    
    let option = {}
    const metrics = trainMetrics.value
    
    if (metrics.epochs.length === 0) {
      // 无真实数据时显示模拟训练曲线作为占位
      const totalEp = ppoParams.epochs
      option = {
        title: { text: 'PPO 训练奖励曲线（模拟）', left: 'center' },
        tooltip: { trigger: 'axis' },
        legend: { data: ['平均奖励', '总损失'], bottom: 0 },
        dataZoom: [
          { type: 'inside', xAxisIndex: 0, filterMode: 'none' },
          {
            type: 'slider',
            xAxisIndex: 0,
            bottom: 25,
            height: 18,
            borderColor: '#ccc',
            fillerColor: 'rgba(144, 147, 153, 0.15)',
            handleStyle: { color: '#409EFF' },
            textStyle: { fontSize: 10 }
          }
        ],
        xAxis: { type: 'category', data: Array.from({ length: totalEp }, (_, i) => 'Epoch ' + (i + 1)), name: '训练轮次' },
        yAxis: [
          { type: 'value', name: 'Reward', position: 'left' },
          { type: 'value', name: 'Loss', position: 'right' }
        ],
        grid: { top: 45, bottom: 40, left: 48, right: 72 },
        series: [
          {
            name: '平均奖励',
            type: 'line',
            yAxisIndex: 0,
            data: Array.from({ length: totalEp }, (_, i) => (+(10 + i * 3 + Math.random() * 5).toFixed(2))),
            smooth: true,
            lineStyle: { color: '#E6A23C', width: 2 },
            itemStyle: { color: '#E6A23C' },
            areaStyle: { color: 'rgba(230, 162, 60, 0.1)' }
          },
          {
            name: '总损失',
            type: 'line',
            yAxisIndex: 1,
            data: Array.from({ length: totalEp }, (_, i) => +(Math.max(0.05, 2.0 * Math.exp(-i / 3) + 0.05 + Math.random() * 0.1).toFixed(4))),
            smooth: true,
            lineStyle: { color: '#F56C6C', width: 2 },
            itemStyle: { color: '#F56C6C' }
          }
        ]
      }
    } else {
      // 有真实训练数据
      const seriesData = []
      const legendData = []
      
      // 奖励曲线
      const validRewardIdx = []
      const rewardValues = []
      metrics.rewards.forEach((r, i) => {
        if (r !== null && r !== undefined) {
          validRewardIdx.push(i)
          rewardValues.push(parseFloat(r))
        }
      })
      if (rewardValues.length > 0) {
        legendData.push('平均奖励')
        seriesData.push({
          name: '平均奖励',
          type: 'line',
          data: rewardValues.map((val, i) => [metrics.epochs[validRewardIdx[i]], val]),
          smooth: true,
          lineStyle: { color: '#E6A23C', width: 2 },
          itemStyle: { color: '#E6A23C' },
          areaStyle: { color: 'rgba(230, 162, 60, 0.1)' }
        })
      }
      
      // 总 Loss 曲线
      const validLossIdx = []
      const lossValues = []
      metrics.losses.forEach((l, i) => {
        if (l !== null && l !== undefined) {
          validLossIdx.push(i)
          lossValues.push(parseFloat(l))
        }
      })
      if (lossValues.length > 0) {
        legendData.push('总损失')
        seriesData.push({
          name: '总损失',
          type: 'line',
          yAxisIndex: seriesData.length > 0 ? 1 : 0,
          data: lossValues.map((val, i) => [metrics.epochs[validLossIdx[i]], val]),
          smooth: true,
          lineStyle: { color: '#F56C6C', width: 2 },
          itemStyle: { color: '#F56C6C' }
        })
      }
      
      // Policy Loss 曲线
      const validPLossIdx = []
      const pLossValues = []
      metrics.policyLosses.forEach((pl, i) => {
        if (pl !== null && pl !== undefined) {
          validPLossIdx.push(i)
          pLossValues.push(parseFloat(pl))
        }
      })
      if (pLossValues.length > 0) {
        legendData.push('策略损失')
        seriesData.push({
          name: '策略损失',
          type: 'line',
          data: pLossValues.map((val, i) => [metrics.epochs[validPLossIdx[i]], val]),
          smooth: true,
          lineStyle: { color: '#409EFF', width: 2, type: 'dashed' },
          itemStyle: { color: '#409EFF' }
        })
      }
      
      // Value Loss 曲线
      const validVLossIdx = []
      const vLossValues = []
      metrics.valueLosses.forEach((vl, i) => {
        if (vl !== null && vl !== undefined) {
          validVLossIdx.push(i)
          vLossValues.push(parseFloat(vl))
        }
      })
      if (vLossValues.length > 0) {
        legendData.push('价值损失')
        seriesData.push({
          name: '价值损失',
          type: 'line',
          data: vLossValues.map((val, i) => [metrics.epochs[validVLossIdx[i]], val]),
          smooth: true,
          lineStyle: { color: '#67C23A', width: 2, type: 'dotted' },
          itemStyle: { color: '#67C23A' }
        })
      }
      
      const yAxes = []
      if (rewardValues.length > 0) {
        yAxes.push({ type: 'value', name: 'Reward', position: 'left' })
      }
      if (lossValues.length > 0) {
        yAxes.push({ type: 'value', name: 'Loss', position: yAxes.length > 0 ? 'right' : 'left' })
      }
      if (yAxes.length === 0) {
        yAxes.push({ type: 'value' })
      }
      
      option = {
        title: { text: 'PPO 训练曲线', left: 'center' },
        tooltip: { trigger: 'axis' },
        legend: { data: legendData, bottom: 0, type: 'scroll' },
        dataZoom: [
          {
            type: 'inside',
            xAxisIndex: 0,
            filterMode: 'none'
          },
          {
            type: 'slider',
            xAxisIndex: 0,
            bottom: legendData.length > 3 ? 35 : 25,
            height: 18,
            borderColor: '#ccc',
            fillerColor: 'rgba(144, 147, 153, 0.15)',
            handleStyle: { color: '#409EFF' },
            textStyle: { fontSize: 10 }
          }
        ],
        xAxis: { type: 'category', name: 'Epoch' },
        yAxis: yAxes,
        grid: { top: 45, bottom: legendData.length > 3 ? 65 : 55, left: 48, right: yAxes.length > 1 ? 72 : 18 },
        series: seriesData
      }
    }
    
    chartInstance.setOption(option)
    
    // 确保渲染后 resize 到正确尺寸（特别是从隐藏状态切换过来时）
    setTimeout(() => {
      if (chartInstance && !chartInstance.isDisposed()) {
        chartInstance.resize()
      }
    }, 100)
    }, 50)
  })
}


// 获取进度条颜色
function getProgressColor(percent) {
  if (percent < 30) return '#F56C6C'
  if (percent < 70) return '#E6A23C'
  return '#67C23A'
}

// 返回
function goBack() {
  router.back()
}

// 训练完成后自动展示图表（无需手动触发重新渲染）

// 加载历史训练记录
async function loadHistoryList() {
  historyLoading.value = true
  try {
    const res = await listTrainingLog({ algorithmType: 'PPO', pageNum: 1, pageSize: 20 })
    historyList.value = res.rows || []
  } catch (error) {
    console.error('加载历史记录失败:', error)
  } finally {
    historyLoading.value = false
  }
}

// 查看历史详情（图表回放）
async function viewHistoryDetail(item) {
  selectedHistoryId.value = item.trainingId
  selectedRecord.value = item
  
  // 解析 metrics JSON 并渲染图表
  if (item.metrics) {
    try {
      const metricsData = typeof item.metrics === 'string' ? JSON.parse(item.metrics) : item.metrics
      renderHistoryChart(metricsData)
    } catch (e) {
      console.error('解析metrics失败:', e)
    }
  }
}

// 渲染历史图表
function renderHistoryChart(metricsData) {
  nextTick(() => {
    setTimeout(() => {
      if (!historyChartRef.value) return
      if (historyChartInstance) {
        historyChartInstance.dispose()
      }
      historyChartInstance = echarts.init(historyChartRef.value)
      
      const seriesData = []
      const legendData = []
      const epochs = metricsData.epochs || []
      
      // 奖励曲线
      if (metricsData.rewards && metricsData.rewards.length > 0) {
        legendData.push('平均奖励')
        seriesData.push({
          name: '平均奖励', type: 'line',
          data: metricsData.rewards.map((v, i) => [epochs[i] || (i + 1), v]),
          smooth: true, lineStyle: { color: '#E6A23C', width: 2 }, itemStyle: { color: '#E6A23C' },
          areaStyle: { color: 'rgba(230, 162, 60, 0.1)' }
        })
      }
      
      // Loss 曲线
      if (metricsData.losses && metricsData.losses.length > 0) {
        legendData.push('总损失')
        seriesData.push({
          name: '总损失', type: 'line',
          yAxisIndex: seriesData.length > 0 ? 1 : 0,
          data: metricsData.losses.map((v, i) => [epochs[i] || (i + 1), v]),
          smooth: true, lineStyle: { color: '#F56C6C', width: 2 }, itemStyle: { color: '#F56C6C' }
        })
      }
      
      // Policy Loss
      if (metricsData.policy_losses && metricsData.policy_losses.length > 0) {
        legendData.push('策略损失')
        seriesData.push({
          name: '策略损失', type: 'line',
          data: metricsData.policy_losses.map((v, i) => [epochs[i] || (i + 1), v]),
          smooth: true, lineStyle: { color: '#409EFF', width: 2, type: 'dashed' }, itemStyle: { color: '#409EFF' }
        })
      }
      
      // Value Loss
      if (metricsData.value_losses && metricsData.value_losses.length > 0) {
        legendData.push('价值损失')
        seriesData.push({
          name: '价值损失', type: 'line',
          data: metricsData.value_losses.map((v, i) => [epochs[i] || (i + 1), v]),
          smooth: true, lineStyle: { color: '#67C23A', width: 2, type: 'dotted' }, itemStyle: { color: '#67C23A' }
        })
      }
      
      const yAxes = []
      if (metricsData.rewards?.length > 0) yAxes.push({ type: 'value', name: 'Reward', position: 'left' })
      if (metricsData.losses?.length > 0) yAxes.push({ type: 'value', name: 'Loss', position: yAxes.length > 0 ? 'right' : 'left' })
      if (yAxes.length === 0) yAxes.push({ type: 'value' })
      
      const option = {
        title: { text: 'PPO 训练曲线（历史回放）', left: 'center', textStyle: { fontSize: 13 } },
        tooltip: { trigger: 'axis' },
        legend: { data: legendData, bottom: 0, type: 'scroll', textStyle: { fontSize: 10 } },
        dataZoom: [
          { type: 'inside', xAxisIndex: 0, filterMode: 'none' },
          { type: 'slider', xAxisIndex: 0, bottom: legendData.length > 3 ? 35 : 25, height: 16,
            borderColor: '#ccc', fillerColor: 'rgba(144,147,153,0.15)', handleStyle: { color: '#409EFF' }, textStyle: { fontSize: 10 } }
        ],
        xAxis: { type: 'category', name: 'Epoch' },
        yAxis: yAxes,
        grid: { top: 45, bottom: legendData.length > 3 ? 70 : 55, left: 55, right: yAxes.length > 1 ? 55 : 15 },
        series: seriesData
      }
      
      historyChartInstance.setOption(option)
      setTimeout(() => { if (historyChartInstance && !historyChartInstance.isDisposed()) historyChartInstance.resize() }, 100)
    }, 50)
  })
}

// 监听 Tab 切换到历史记录时加载数据
watch(resultTab, (tab) => {
  if (tab === 'history' && historyList.value.length === 0) {
    loadHistoryList()
  }
})

// 组件挂载时
onMounted(() => {
  checkServiceStatus()
  
  // 窗口大小变化时重新渲染图表
  window.addEventListener('resize', () => {
    if (chartInstance) {
      chartInstance.resize()
    }
  })
})

// 组件卸载时
onUnmounted(() => {
  if (trainingInterval) {
    clearInterval(trainingInterval)
  }
  if (chartInstance) {
    chartInstance.dispose()
  }
  if (historyChartInstance) {
    historyChartInstance.dispose()
  }
  window.removeEventListener('resize', () => {})
})
</script>

<style scoped lang="scss">
.app-container {
  padding: 20px;
}

.mt-20 {
  margin-top: 20px;
}

.ml-10 {
  margin-left: 10px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.progress-section {
  margin-bottom: 20px;
}

.log-section {
  border: 1px solid #ebeef5;
  border-radius: 4px;
  
  .log-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 10px;
    background-color: #f5f7fa;
    border-bottom: 1px solid #ebeef5;
  }
  
  .log-content {
    height: 300px;
    overflow-y: auto;
    padding: 10px;
    background-color: #1e1e1e;
  }
  
  .log-item {
    margin-bottom: 5px;
    font-family: 'Consolas', 'Monaco', monospace;
    font-size: 12px;
    
    &.info {
      color: #67C23A;
    }
    
    &.warn {
      color: #E6A23C;
    }
    
    &.error {
      color: #F56C6C;
    }
    
    .log-time {
      color: #909399;
      margin-right: 10px;
    }
    
    .log-type {
      color: #409EFF;
      margin-right: 10px;
      font-weight: bold;
    }
    
    .log-message {
      color: #ffffff;
    }
  }
  
  .log-empty {
    color: #909399;
    text-align: center;
    line-height: 200px;
  }
}

.chart-container {
  height: 300px;
  
  .chart-empty {
    height: 100%;
    display: flex;
    align-items: center;
    justify-content: center;
  }
}

.text-sm {
  font-size: 14px;
}

.text-gray-500 {
  color: #909399;
}

.chart-type-bar {
  display: flex;
  justify-content: flex-end;
}

/* 历史记录样式 */
.history-section {
  min-height: 300px;
}

.history-list {
  max-height: 520px;
  overflow-y: auto;
}

.history-item {
  display: flex;
  align-items: center;
  padding: 10px 12px;
  border-bottom: 1px solid #f0f0f0;
  cursor: pointer;
  transition: all 0.2s ease;
  position: relative;

  &:hover {
    background-color: #ecf5ff;
  }

  &:last-child {
    border-bottom: none;
  }
}

.history-item-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}

.history-item-time {
  font-size: 11px;
  color: #909399;
}

.history-item-body {
  flex: 1;
  min-width: 0;
}

.history-item-title {
  font-size: 13px;
  font-weight: 600;
  color: #303133;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.history-item-meta {
  font-size: 11px;
  color: #909399;
  margin-top: 2px;
  display: flex;
  gap: 12px;
}

.history-arrow {
  color: #c0c4cc;
  flex-shrink: 0;
  margin-left: 8px;
}

.history-detail {
  overflow-y: auto;
  max-height: 540px;
}

.history-detail-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;
  padding-bottom: 8px;
  border-bottom: 1px solid #ebeef5;
}

.history-detail-title {
  font-size: 13px;
  font-weight: 600;
  color: #303133;
}

.history-detail-info {
  margin-top: 12px;
}
</style>
