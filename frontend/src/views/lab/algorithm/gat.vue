<template>
  <div class="app-container">
    <!-- 页面标题 -->
    <el-page-header @back="goBack" title="GATv2智能推荐算法">
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
          <el-form :model="gatParams" label-width="120px">
            <el-form-item label="输入维度">
              <el-input-number v-model="gatParams.inChannels" :min="4" :max="32" />
            </el-form-item>
            <el-form-item label="隐藏层维度">
              <el-input-number v-model="gatParams.hiddenChannels" :min="8" :max="64" />
            </el-form-item>
            <el-form-item label="输出维度">
              <el-input-number v-model="gatParams.outChannels" :min="2" :max="16" />
            </el-form-item>
            <el-form-item label="学习率">
              <el-slider v-model="gatParams.lr" :min="0.001" :max="0.1" :step="0.001" :format-tooltip="(val) => val.toFixed(3)" />
              <span class="ml-10">{{ gatParams.lr.toFixed(3) }}</span>
            </el-form-item>
            <el-form-item label="训练轮数">
              <el-input-number v-model="gatParams.epochs" :min="10" :max="500" />
            </el-form-item>
            <el-form-item label="推荐数量">
              <el-input-number v-model="gatParams.topK" :min="1" :max="20" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="handleRecommend" :loading="executing">执行推荐</el-button>
              <el-button type="success" @click="handleTrain" :loading="training">开始训练</el-button>
            </el-form-item>
          </el-form>
        </el-card>

        <!-- 算法说明文档区 -->
        <el-card class="box-card mt-20">
          <template #header>
            <span>算法说明文档</span>
          </template>
          <el-collapse v-model="activeDocs">
            <el-collapse-item title="GATv2算法简介" name="intro">
              <p>GATv2（Graph Attention Network v2）是一种图注意力网络，用于解决图结构数据的表示学习问题。</p>
              <p>在实验室设备推荐场景中，GATv2能够学习用户和设备的嵌入表示，通过注意力机制捕捉用户-设备之间的复杂关系。</p>
            </el-collapse-item>
            <el-collapse-item title="参数说明" name="params">
              <ul>
                <li><strong>输入维度</strong>：用户和设备特征的维度</li>
                <li><strong>隐藏层维度</strong>：图注意力层的隐藏单元数量</li>
                <li><strong>输出维度</strong>：最终嵌入向量的维度</li>
                <li><strong>学习率</strong>：模型训练时的学习速率</li>
                <li><strong>训练轮数</strong>：模型训练的迭代次数</li>
                <li><strong>推荐数量</strong>：每次推荐返回的设备数量</li>
              </ul>
            </el-collapse-item>
            <el-collapse-item title="使用场景" name="scenarios">
              <ul>
                <li>为用户推荐个性化的实验室设备</li>
                <li>基于用户历史行为进行设备推荐</li>
                <li>设备使用模式分析</li>
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
              当前轮数：{{ currentEpoch }} / {{ gatParams.epochs }}
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
        <!-- Tab 切换：推荐结果 / 图表可视化 / 历史记录 -->
        <el-card class="box-card">
          <template #header>
            <div class="card-header">
              <span>结果展示</span>
              <el-radio-group v-model="resultTab" size="small">
                <el-radio-button label="recommend">推荐结果</el-radio-button>
                <el-radio-button label="chart">图表</el-radio-button>
                <el-radio-button label="history">历史记录</el-radio-button>
              </el-radio-group>
            </div>
          </template>

          <!-- 推荐结果详情 -->
          <div v-show="resultTab === 'recommend'" class="result-tab-content">
            <div v-if="recommendResults.length > 0" v-loading="recommendLoading">
              <!-- 推荐结果卡片列表 -->
              <div class="recommend-list">
                <div
                  v-for="(rec, index) in recommendResults"
                  :key="index"
                  class="recommend-item"
                  @click="showRecommendDetail(rec)"
                >
                  <div class="recommend-rank-badge">#{{ rec.rank }}</div>
                  <div class="recommend-info">
                    <div class="recommend-device-name">
                      {{ rec.deviceName || ('设备 #' + rec.deviceId) }}
                    </div>
                    <div class="recommend-meta">
                      <span class="recommend-category" v-if="rec.categoryName">
                        <el-tag size="small" type="info">{{ rec.categoryName }}</el-tag>
                      </span>
                      <span class="recommend-lab" v-if="rec.labName">
                        <el-tag size="small">{{ rec.labName }}</el-tag>
                      </span>
                    </div>
                  </div>
                  <div class="recommend-score-section">
                    <div class="recommend-score-label">匹配度</div>
                    <div class="recommend-score-value" :style="{ 'color': getScoreColor(rec.similarity) }">
                      {{ (rec.similarity * 100).toFixed(1) }}%
                    </div>
                    <el-progress
                      :percentage="rec.similarity * 100"
                      :color="getScoreColor(rec.similarity)"
                      :stroke-width="6"
                      :show-text="false"
                      style="margin-top: 4px;"
                    />
                  </div>
                </div>
              </div>
              <div class="recommend-summary">
                <el-alert
                  :title="'共推荐 ' + recommendResults.length + ' 台设备，推荐时间: ' + (recommendResults[0]?.recommendTime || '-')"
                  type="success"
                  :closable="false"
                  show-icon
                />
              </div>
            </div>
            <div v-else class="chart-empty">
              <el-empty description="请先执行推荐以查看推荐结果" />
            </div>
          </div>

          <!-- 图表可视化 -->
          <div v-show="resultTab === 'chart'" class="result-tab-content">
            <div class="chart-type-bar" v-show="hasResults">
              <el-select v-model="chartDataSource" size="small" style="width: 110px;">
                <el-option label="推荐数据" value="recommend" />
                <el-option label="训练数据" value="train" :disabled="trainMetrics.epochs.length === 0" />
              </el-select>
            </div>
            <div class="chart-container">
              <div ref="chartRef" v-show="hasResults" style="width: 100%; height: 300px;"></div>
              <div v-show="!hasResults" class="chart-empty">
                <el-empty description="请先执行推荐或训练以查看结果" />
              </div>
            </div>
          </div>

          <!-- 历史记录 -->
          <div v-show="resultTab === 'history'" class="result-tab-content history-section">
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
                    <div class="history-item-title">{{ item.taskName || ('GATv2训练 #' + item.trainingId) }}</div>
                    <div class="history-item-meta">
                      <span>轮数: {{ item.completedEpochs || 0 }}/{{ item.totalEpochs || '-' }}</span>
                      <span v-if="item.lossValue">Loss: {{ parseFloat(item.lossValue).toFixed(4) }}</span>
                      <span v-if="item.accuracy">Acc: {{ parseFloat(item.accuracy).toFixed(4) }}</span>
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
                  <el-descriptions-item label="准确率" v-if="selectedRecord.accuracy">{{ parseFloat(selectedRecord.accuracy).toFixed(4) }}</el-descriptions-item>
                  <el-descriptions-item label="训练时间" :span="selectedRecord.accuracy ? 1 : 2">{{ selectedRecord.startTime }} ~ {{ selectedRecord.endTime || '-' }}</el-descriptions-item>
                </el-descriptions>
              </div>
            </div>
          </div>

        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup name="GatAlgorithm">
import { ref, reactive, computed, onMounted, nextTick, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowRight, ArrowLeft } from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import { gatHealth, gatStatus, gatRecommend, gatTrain, gatTrainStatus, gatTrainLogs, gatTrainLogsClear } from '@/api/lab/algorithm'
import { startTraining, completeTraining as completeTrainingApi, listTrainingLog } from '@/api/lab/algorithm'
import { getDevice } from '@/api/lab/device'
import useUserStore from '@/store/modules/user'

const router = useRouter()
const userStore = useUserStore()
const { proxy } = getCurrentInstance()

// 响应式数据
const serviceStatus = ref('offline')
const executing = ref(false)
const training = ref(false)
const trainingProgress = ref(0)
const currentEpoch = ref(0)
const activeDocs = ref(['intro'])
const logLevel = ref('all')
const chartType = ref('line')
const resultTab = ref('recommend')
const hasResults = ref(false)
const chartRef = ref(null)
const historyChartRef = ref(null)
const logContentRef = ref(null)
// 历史记录数据
const historyList = ref([])
const historyLoading = ref(false)
const selectedHistoryId = ref(null)
const selectedRecord = ref(null)
let historyChartInstance = null
const recommendResults = ref([])
const recommendLoading = ref(false)
// 训练指标数据（从日志中解析）
const trainMetrics = ref({
  epochs: [],
  losses: [],
  accuracies: []
})
// 当前图表数据来源：'recommend' 推荐 | 'train' 训练
const chartDataSource = ref('recommend')
let chartInstance = null
let trainingInterval = null
// 状态文本映射
function statusText(status) {
  const map = { RUNNING: '训练中', COMPLETED: '已完成', FAILED: '失败', PENDING: '等待中' }
  return map[status] || status || '-'
}

// GATv2参数
const gatParams = reactive({
  inChannels: 8,
  hiddenChannels: 16,
  outChannels: 4,
  lr: 0.01,
  epochs: 100,
  topK: 5,
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
    await gatTrainLogsClear()
    logs.value = []
  } catch (error) {
    logs.value = []
    addLog('清空日志失败: ' + error.message, 'error')
  }
}

// 重置参数
function resetParams() {
  gatParams.inChannels = 8
  gatParams.hiddenChannels = 16
  gatParams.outChannels = 4
  gatParams.lr = 0.01
  gatParams.epochs = 100
  gatParams.topK = 5
  gatParams.forceReload = false
  addLog('参数已重置为默认值', 'info')
}

// 检查服务状态
async function checkServiceStatus() {
  try {
    const response = await gatHealth()
    if (response.code === 200) {
      serviceStatus.value = 'online'
      addLog('GATv2服务连接成功', 'info')
    } else {
      serviceStatus.value = 'offline'
      addLog('GATv2服务连接失败', 'error')
    }
  } catch (error) {
    serviceStatus.value = 'offline'
    addLog('GATv2服务连接失败: ' + error.message, 'error')
  }
}

// 执行推荐
async function handleRecommend() {
  if (serviceStatus.value !== 'online') {
    ElMessage.error('GATv2服务未连接，请先确保服务正常运行')
    return
  }

  executing.value = true
  recommendLoading.value = true
  recommendResults.value = []
  addLog('开始执行推荐...', 'info')

  try {
    // 使用当前登录用户ID进行推荐
    const userId = userStore.id || 1
    addLog('正在为用户 ' + userId + ' (' + (userStore.nickName || userStore.name || '未知') + ') 执行推荐...', 'info')
    const response = await gatRecommend({
      user_id: userId,
      top_k: gatParams.topK,
      save_to_db: true
    })

    if (response.code === 200) {
      // 处理双重包装的响应数据
      let recommendData = response.data
      // 后端 proxyToAlgorithm 将算法服务的响应整体放入 AjaxResult.data 中
      // 算法服务返回 { code:200, message:"success", data:[...] }
      // 所以 response.data 可能是 { code:200, message:"success", data:[...] }
      if (recommendData && recommendData.data && Array.isArray(recommendData.data)) {
        recommendData = recommendData.data
      }

      // 确保 recommendData 是数组
      if (!Array.isArray(recommendData)) {
        addLog('推荐返回数据格式异常: ' + JSON.stringify(recommendData).substring(0, 200), 'error')
        recommendData = []
      }

      if (recommendData.length > 0) {
        addLog('推荐执行成功，获取到 ' + recommendData.length + ' 条推荐结果', 'info')
        hasResults.value = true
        resultTab.value = 'recommend'

        // 转换字段名并加载设备详细信息
        const results = []
        for (const item of recommendData) {
          const rec = {
            userId: item.user_id,
            deviceId: item.device_id,
            similarity: item.similarity,
            rank: item.rank,
            recommendTime: item.recommend_time,
            deviceName: null,
            categoryName: null,
            labName: null,
            location: null,
            status: null,
            specs: null
          }
          // 异步加载设备详情
          loadDeviceInfo(rec)
          results.push(rec)
        }
        recommendResults.value = results

          // 渲染图表
        renderChart(recommendData)
      } else {
        addLog('推荐执行成功，但未获取到推荐结果（可能用户或设备数据不足）', 'warn')
      }
    } else {
      addLog('推荐执行失败: ' + (response.msg || '未知错误'), 'error')
    }
  } catch (error) {
    addLog('推荐执行异常: ' + error.message, 'error')
  } finally {
    executing.value = false
    recommendLoading.value = false
  }
}

// 加载设备详细信息
async function loadDeviceInfo(rec) {
  try {
    const deviceRes = await getDevice(rec.deviceId)
    if (deviceRes.code === 200 && deviceRes.data) {
      const dev = deviceRes.data
      rec.deviceName = dev.deviceName
      rec.categoryName = dev.categoryName
      rec.labName = dev.labName
      rec.location = dev.location
      rec.status = dev.status
      rec.specs = dev.specs
    }
  } catch (error) {
    console.error('加载设备 ' + rec.deviceId + ' 信息失败:', error)
  }
}

// 显示推荐详情
function showRecommendDetail(rec) {
  // 可以扩展为弹窗展示更详细的设备信息
  ElMessage.info('设备: ' + (rec.deviceName || ('#' + rec.deviceId)) + ', 匹配度: ' + (rec.similarity * 100).toFixed(1) + '%')
}

// 根据匹配度获取颜色
function getScoreColor(similarity) {
  if (similarity >= 0.8) return '#67C23A'
  if (similarity >= 0.6) return '#E6A23C'
  if (similarity >= 0.4) return '#409EFF'
  return '#F56C6C'
}

// 当前训练任务ID（用于保存到数据库）
let currentTrainingId = null

// 开始训练
async function handleTrain() {
  if (serviceStatus.value !== 'online') {
    ElMessage.error('GATv2服务未连接，请先确保服务正常运行')
    return
  }

  training.value = true
  trainingProgress.value = 0
  currentEpoch.value = 0
  // 重置训练指标
  trainMetrics.value = { epochs: [], losses: [], accuracies: [] }
  
  try {
    // 先清空服务器日志
    await gatTrainLogsClear()
    logs.value = []
    
    addLog('开始训练模型...', 'info')
    
    // 创建训练记录到数据库
    const trainRes = await startTraining({
      algorithmType: 'GATv2',
      taskName: 'GATv2推荐模型训练',
      trainingParams: JSON.stringify(gatParams),
      totalEpochs: gatParams.epochs,
      status: 'RUNNING'
    })
    currentTrainingId = trainRes.data || null
    addLog(currentTrainingId ? ('训练记录已创建，ID: ' + currentTrainingId) : '训练记录创建（仅本地）', 'info')
    
    const response = await gatTrain({
      force_reload: gatParams.forceReload,
      epochs: gatParams.epochs,
      in_channels: gatParams.inChannels,
      hidden_channels: gatParams.hiddenChannels,
      out_channels: gatParams.outChannels,
      lr: gatParams.lr
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

// 检查训练状态和日志
async function checkTrainingStatus() {
  try {
    // 同时获取训练状态和日志
    const [statusResponse, logsResponse] = await Promise.all([
      gatTrainStatus(),
      gatTrainLogs()
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
            // 训练完成后，标记有结果并切换到图表Tab展示训练曲线
            hasResults.value = true
            resultTab.value = 'chart'
            chartDataSource.value = 'train'
            // 最终一次从 status.metrics 获取完整训练指标（优先），否则解析日志
            if (status && status.metrics && Array.isArray(status.metrics.epochs) && status.metrics.epochs.length > 0) {
              trainMetrics.value = {
                epochs: [...status.metrics.epochs],
                losses: [...(status.metrics.losses || [])],
                accuracies: [...(status.metrics.accuracies || [])]
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
      losses: [...(status.metrics.losses || [])],
      accuracies: [...(status.metrics.accuracies || [])]
    }
    return
  }
  
  // 兼容旧格式：从状态顶层字段提取
  const epoch = status.current_epoch || 0
  const loss = status.loss || status.train_loss || null
  const acc = status.accuracy || status.train_acc || null
  
  if (loss !== null || acc !== null) {
    const lastEpoch = trainMetrics.value.epochs[trainMetrics.value.epochs.length - 1]
    if (lastEpoch !== epoch) {
      trainMetrics.value.epochs.push(epoch)
      trainMetrics.value.losses.push(loss !== null ? parseFloat(loss) : null)
      trainMetrics.value.accuracies.push(acc !== null ? parseFloat(acc) : null)
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
  
  // 清空之前的指标，用完整的日志重新解析
  const parsedEpochs = []
  const parsedLosses = []
  const parsedAccs = []
  
  for (const log of serverLogs) {
    const msg = log.message || ''
    // 匹配类似 "Epoch 5/100 - Loss: 0.2345 - Acc: 0.8765" 的日志格式
    const epochMatch = msg.match(/Epoch\s+(\d+)\s*\/\s*(\d+)/i)
    const lossMatch = msg.match(/[Ll]oss[:\s]+([\d.]+)/)
    const accMatch = msg.match(/(?:[Aa]cc|[Aa]ccuracy)[:\s]+([\d.]+)/)
    
    if (epochMatch) {
      const ep = parseInt(epochMatch[1])
      const lossVal = lossMatch ? parseFloat(lossMatch[1]) : null
      const accVal = accMatch ? parseFloat(accMatch[1]) : null
      parsedEpochs.push(ep)
      parsedLosses.push(lossVal)
      parsedAccs.push(accVal)
    }
  }
  
  if (parsedEpochs.length > 0) {
    trainMetrics.value = {
      epochs: parsedEpochs,
      losses: parsedLosses,
      accuracies: parsedAccs
    }
  }
}

// 将训练指标保存到数据库
async function saveTrainingMetricsToDB() {
  if (!currentTrainingId) {
    console.warn('[GATv2] 无 trainingId，跳过保存指标')
    return
  }
  const metrics = trainMetrics.value
  if (metrics.epochs.length === 0) {
    addLog('无训练指标数据，跳过保存', 'warn')
    return
  }
  
  // 构建每轮指标的 JSON
  const lastLoss = metrics.losses[metrics.losses.length - 1]
  const lastAcc = metrics.accuracies.length > 0 ? metrics.accuracies[metrics.accuracies.length - 1] : null
  const metricsJson = JSON.stringify({
    epochs: metrics.epochs,
    losses: metrics.losses,
    accuracies: metrics.accuracies,
    algorithm_type: 'GATv2'
  })
  
  try {
    await completeTrainingApi(currentTrainingId, {
      lossValue: lastLoss !== null && lastLoss !== undefined ? parseFloat(lastLoss) : null,
      accuracy: lastAcc !== null && lastAcc !== undefined ? parseFloat(lastAcc) : null,
      modelVersion: 'v' + new Date().getTime(),
      metrics: metricsJson
    })
    addLog('训练指标已保存到数据库（ID: ' + currentTrainingId + '）', 'info')
  } catch (error) {
    addLog('保存训练指标失败: ' + error.message, 'error')
  }
}

// 渲染训练图表（根据当前数据源选择）
function renderTrainChart() {
  // 使用 nextTick + 延迟确保 DOM 完全就绪
  nextTick(() => {
    setTimeout(() => {
      if (!chartRef.value) {
        console.warn('[GATv2] chartRef 不存在，无法渲染图表')
        return
      }
      if (chartInstance) {
        chartInstance.dispose()
      }
      chartInstance = echarts.init(chartRef.value)
      console.log('[GATv2] ECharts 初始化成功，容器尺寸:', chartRef.value.offsetWidth, 'x', chartRef.value.offsetHeight)
    
    let option = {}
    
    if (chartDataSource.value === 'train') {
      // ====== 训练数据图表 ======
      const metrics = trainMetrics.value
      if (metrics.epochs.length === 0) {
        // 无真实数据时显示模拟曲线作为占位
        const totalEp = gatParams.epochs
        option = {
          title: { text: '训练损失曲线（模拟）', left: 'center', textStyle: { fontSize: 14 } },
          tooltip: { trigger: 'axis', confine: true },
          legend: { data: ['损失值'], bottom: 0, textStyle: { fontSize: 11 } },
          dataZoom: [
            { type: 'inside', xAxisIndex: 0, filterMode: 'none' },
            {
              type: 'slider',
              xAxisIndex: 0,
              bottom: 22,
              height: 16,
              borderColor: '#ccc',
              fillerColor: 'rgba(144, 147, 153, 0.15)',
              handleStyle: { color: '#409EFF' },
              textStyle: { fontSize: 10 }
            }
          ],
          xAxis: {
            type: 'category', data: Array.from({ length: totalEp }, (_, i) => i + 1), name: 'Epoch',
            axisLabel: { interval: Math.max(1, Math.floor(totalEp / 10)), fontSize: 11 }
          },
          yAxis: { type: 'value', name: 'Loss', axisLabel: { fontSize: 11 }, nameTextStyle: { fontSize: 11 } },
          grid: { top: 48, bottom: 55, left: 40, right: 12, containLabel: false },
          series: [{
            name: '损失值',
            type: 'line',
            data: Array.from({ length: totalEp }, (_, i) => (0.9 * Math.exp(-i / 15) + 0.05 + Math.random() * 0.03).toFixed(4)),
            smooth: true,
            lineStyle: { color: '#F56C6C', width: 2 },
            itemStyle: { color: '#F56C6C' },
            areaStyle: { color: 'rgba(245, 108, 108, 0.1)' }
          }]
        }
      } else {
        // 有真实训练数据，绘制 Loss 和 Accuracy 双曲线
        const seriesData = []
        const legendData = []
        
        // 过滤出有效loss数据
        const validLossIdx = []
        const lossValues = []
        metrics.losses.forEach((l, i) => {
          if (l !== null && l !== undefined) {
            validLossIdx.push(i)
            lossValues.push(parseFloat(l))
          }
        })
        if (lossValues.length > 0) {
          legendData.push('训练损失 (Loss)')
          seriesData.push({
            name: '训练损失 (Loss)',
            type: 'line',
            data: lossValues.map((val, i) => [metrics.epochs[validLossIdx[i]], val]),
            smooth: true,
            lineStyle: { color: '#F56C6C', width: 2 },
            itemStyle: { color: '#F56C6C' },
            areaStyle: { color: 'rgba(245, 108, 108, 0.1)' }
          })
        }
        
        // 过滤出有效accuracy数据
        const validAccIdx = []
        const accValues = []
        metrics.accuracies.forEach((a, i) => {
          if (a !== null && a !== undefined) {
            validAccIdx.push(i)
            accValues.push(parseFloat(a))
          }
        })
        if (accValues.length > 0) {
          legendData.push('准确率 (Accuracy)')
          seriesData.push({
            name: '准确率 (Accuracy)',
            type: 'line',
            yAxisIndex: seriesData.length > 1 ? 1 : 0,
            data: accValues.map((val, i) => [metrics.epochs[validAccIdx[i]], val]),
            smooth: true,
            lineStyle: { color: '#67C23A', width: 2 },
            itemStyle: { color: '#67C23A' }
          })
        }
        
        const yAxes = [{ type: 'value', name: 'Loss', position: 'left' }]
        if (accValues.length > 0) {
          yAxes.push({ type: 'value', name: 'Accuracy', min: 0, max: 1, position: 'right' })
        }
        
        option = {
          title: { text: 'GATv2 训练曲线', left: 'center', textStyle: { fontSize: 14 } },
          tooltip: { trigger: 'axis', confine: true },
          legend: { data: legendData, bottom: 0, textStyle: { fontSize: 11 } },
          dataZoom: [
            { type: 'inside', xAxisIndex: 0, filterMode: 'none' },
            {
              type: 'slider',
              xAxisIndex: 0,
              bottom: 22,
              height: 16,
              borderColor: '#ccc',
              fillerColor: 'rgba(144, 147, 153, 0.15)',
              handleStyle: { color: '#409EFF' },
              textStyle: { fontSize: 10 }
            }
          ],
          xAxis: {
            type: 'category', name: 'Epoch',
            axisLabel: { interval: Math.max(1, Math.floor((metrics.epochs?.length || 100) / 10)), fontSize: 11 }
          },
          yAxis: yAxes.map((y, i) => ({ ...y, axisLabel: { fontSize: 11 }, nameTextStyle: { fontSize: 11 } })),
          grid: { top: 48, bottom: accValues.length > 0 ? 58 : 55, left: 40, right: accValues.length > 0 ? 72 : 12, containLabel: true },
          series: seriesData
        }
      }
    } else {
      // ====== 推荐数据图表（固定为柱状图） ======
      const data = recommendResults.value.map(item => ({
        device_id: item.deviceId,
        similarity: item.similarity
      }))
      
      option = {
        title: { text: '推荐设备相似度', left: 'center' },
        tooltip: { trigger: 'axis' },
        xAxis: { type: 'category', data: data.map(item => '设备' + item.device_id) },
        yAxis: { type: 'value', max: 1, name: '相似度' },
        grid: { top: 50, bottom: 30, left: 55, right: 20 },
        series: [{
          type: 'bar',
          data: data.map(item => (+item.similarity).toFixed(4)),
          itemStyle: { color: '#409EFF' },
          label: { show: true, position: 'top', fontSize: 10 }
        }]
      }
    }
    
    chartInstance.setOption(option)
      
      // 确保渲染后 resize 到正确尺寸（特别是从 v-show 隐藏状态切换过来时）
      setTimeout(() => {
        if (chartInstance && !chartInstance.isDisposed()) {
          chartInstance.resize()
        }
      }, 100)
    }, 50)
  })
}

// 兼容旧接口：渲染推荐图表
function renderChart(data) {
  // 直接调用统一的渲染函数
  renderTrainChart()
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

// 加载历史训练记录
async function loadHistoryList() {
  historyLoading.value = true
  try {
    const res = await listTrainingLog({ algorithmType: 'GATv2', pageNum: 1, pageSize: 20 })
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
  
  if (item.metrics) {
    try {
      const metricsData = typeof item.metrics === 'string' ? JSON.parse(item.metrics) : item.metrics
      renderHistoryChart(metricsData)
    } catch (e) {
      console.error('解析metrics失败:', e)
    }
  }
}

// 渲染历史图表（GATv2 版本：Loss + Accuracy）
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
      
      // Loss 曲线
      if (metricsData.losses && metricsData.losses.length > 0) {
        legendData.push('训练损失 (Loss)')
        seriesData.push({
          name: '训练损失 (Loss)', type: 'line',
          data: metricsData.losses.map((v, i) => [epochs[i] || (i + 1), v]),
          smooth: true, lineStyle: { color: '#F56C6C', width: 2 }, itemStyle: { color: '#F56C6C' },
          areaStyle: { color: 'rgba(245, 108, 108, 0.1)' }
        })
      }
      
      // Accuracy 曲线
      if (metricsData.accuracies && metricsData.accuracies.length > 0) {
        legendData.push('准确率 (Accuracy)')
        seriesData.push({
          name: '准确率 (Accuracy)', type: 'line',
          yAxisIndex: seriesData.length > 0 ? 1 : 0,
          data: metricsData.accuracies.map((v, i) => [epochs[i] || (i + 1), v]),
          smooth: true, lineStyle: { color: '#67C23A', width: 2 }, itemStyle: { color: '#67C23A' }
        })
      }
      
      const yAxes = [{ type: 'value', name: 'Loss', position: 'left' }]
      if (metricsData.accuracies?.length > 0) {
        yAxes.push({ type: 'value', name: 'Accuracy', min: 0, max: 1, position: 'right' })
      }
      
      const option = {
        title: { text: 'GATv2 训练曲线（历史回放）', left: 'center', textStyle: { fontSize: 13 } },
        tooltip: { trigger: 'axis' },
        legend: { data: legendData, bottom: 0, textStyle: { fontSize: 10 } },
        dataZoom: [
          { type: 'inside', xAxisIndex: 0, filterMode: 'none' },
          { type: 'slider', xAxisIndex: 0, bottom: 25, height: 16,
            borderColor: '#ccc', fillerColor: 'rgba(144,147,153,0.15)', handleStyle: { color: '#409EFF' }, textStyle: { fontSize: 10 } }
        ],
        xAxis: { type: 'category', name: 'Epoch' },
        yAxis: yAxes,
        grid: { top: 45, bottom: 55, left: 55, right: yAxes.length > 1 ? 72 : 15 },
        series: seriesData
      }
      
      historyChartInstance.setOption(option)
      setTimeout(() => { if (historyChartInstance && !historyChartInstance.isDisposed()) historyChartInstance.resize() }, 100)
    }, 50)
  })
}

// 监听 Tab 切换
watch(resultTab, (newTab) => {
  if (newTab === 'chart' && hasResults.value) {
    // v-show 从 none 切换到 block 后需要等待浏览器完成布局重算
    setTimeout(() => {
      renderTrainChart()
    }, 100)
  }
  if (newTab === 'history' && historyList.value.length === 0) {
    loadHistoryList()
  }
})

// 监听数据源变化
watch(chartDataSource, () => {
  if (hasResults.value) {
    renderTrainChart()
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

.result-tab-content {
  min-height: 300px;
}

.chart-type-bar {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 10px;
}

/* 推荐结果列表样式 */
.recommend-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  max-height: 360px;
  overflow-y: auto;
  padding-right: 4px;
}

.recommend-item {
  display: flex;
  align-items: center;
  padding: 12px 16px;
  background: linear-gradient(135deg, #f8f9ff 0%, #f0f5ff 100%);
  border-radius: 10px;
  border: 1px solid #e8ecf5;
  cursor: pointer;
  transition: all 0.3s ease;
  position: relative;

  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 4px 12px rgba(64, 158, 255, 0.15);
    border-color: #409EFF;
  }
}

.recommend-rank-badge {
  position: absolute;
  top: -8px;
  left: 12px;
  background: linear-gradient(135deg, #409EFF, #66b1ff);
  color: #fff;
  font-size: 11px;
  font-weight: bold;
  padding: 2px 8px;
  border-radius: 10px;
  box-shadow: 0 2px 6px rgba(64, 158, 255, 0.3);
}

.recommend-info {
  flex: 1;
  margin-left: 24px;
  margin-right: 16px;
  min-width: 0;
}

.recommend-device-name {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.recommend-meta {
  display: flex;
  gap: 6px;
  margin-top: 6px;
  flex-wrap: wrap;
}

.recommend-score-section {
  text-align: center;
  min-width: 70px;
}

.recommend-score-label {
  font-size: 11px;
  color: #909399;
  margin-bottom: 2px;
}

.recommend-score-value {
  font-size: 18px;
  font-weight: bold;
}

.recommend-summary {
  margin-top: 12px;
}

.text-sm {
  font-size: 14px;
}

.text-gray-500 {
  color: #909399;
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
