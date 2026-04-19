<template>
  <div class="app-container">
    <el-card class="box-card">
      <template #header>
        <div class="card-header">
          <span>算法日志管理</span>
          <div class="header-actions">
            <el-button type="danger" icon="Delete" @click="handleClean" v-hasPermi="['lab:algorithmLog:clean']">
              清空日志
            </el-button>
          </div>
        </div>
      </template>

      <el-form :model="queryParams" ref="queryRef" :inline="true" class="search-form">
        <el-form-item label="算法类型" prop="algorithmType">
          <el-select v-model="queryParams.algorithmType" placeholder="请选择算法类型" clearable>
            <el-option label="GAT推荐算法" value="GAT" />
            <el-option label="PPO调度算法" value="PPO" />
          </el-select>
        </el-form-item>
        <el-form-item label="操作类型" prop="operationType">
          <el-select v-model="queryParams.operationType" placeholder="请选择操作类型" clearable>
            <el-option label="训练" value="TRAIN" />
            <el-option label="推荐" value="RECOMMEND" />
            <el-option label="调度" value="SCHEDULE" />
            <el-option label="查询" value="QUERY" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-select v-model="queryParams.status" placeholder="请选择状态" clearable>
            <el-option label="成功" value="0" />
            <el-option label="失败" value="1" />
          </el-select>
        </el-form-item>
        <el-form-item label="时间范围" prop="dateRange">
          <el-date-picker
            v-model="dateRange"
            type="datetimerange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD HH:mm:ss"
            @change="handleDateRangeChange"
          />
        </el-form-item>
        <el-form-item label="用户ID" prop="userId">
          <el-input
            v-model="queryParams.userId"
            placeholder="请输入用户ID"
            clearable
            @keyup.enter="handleQuery"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
          <el-button icon="Refresh" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table v-loading="loading" :data="logList" @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="55" align="center" />
        <el-table-column label="日志ID" align="center" prop="logId" width="80" sortable />
        <el-table-column label="算法类型" align="center" prop="algorithmType" width="120">
          <template #default="scope">
            <el-tag :type="getAlgorithmTypeTagType(scope.row.algorithmType)">
              {{ getAlgorithmTypeLabel(scope.row.algorithmType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作类型" align="center" prop="operationType" width="100">
          <template #default="scope">
            <el-tag :type="getOperationTypeTagType(scope.row.operationType)">
              {{ getOperationTypeLabel(scope.row.operationType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" align="center" prop="status" width="100">
          <template #default="scope">
            <el-tag :type="scope.row.status === '0' ? 'success' : 'danger'">
              {{ scope.row.status === '0' ? '成功' : '失败' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="用户ID" align="center" prop="userId" width="80" />
        <el-table-column label="执行时间(ms)" align="center" prop="durationMs" width="120" sortable />
        <el-table-column label="错误信息" align="left" prop="errorMessage" min-width="200" show-overflow-tooltip />
        <el-table-column label="创建时间" align="center" prop="createTime" width="170" sortable>
          <template #default="scope">
            <span>{{ parseTime(scope.row.createTime) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="120">
          <template #default="scope">
            <el-button link type="primary" icon="View" @click="handleView(scope.row)">查看</el-button>
            <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['lab:algorithmLog:remove']">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <pagination
        v-show="total > 0"
        :total="total"
        v-model:page="queryParams.pageNum"
        v-model:limit="queryParams.pageSize"
        @pagination="getList"
      />
    </el-card>

    <el-dialog title="日志详情" v-model="viewOpen" width="700px" append-to-body>
      <el-descriptions :column="1" border v-if="viewLog">
        <el-descriptions-item label="日志ID">{{ viewLog.logId }}</el-descriptions-item>
        <el-descriptions-item label="算法类型">
          <el-tag :type="getAlgorithmTypeTagType(viewLog.algorithmType)">
            {{ getAlgorithmTypeLabel(viewLog.algorithmType) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="操作类型">
          <el-tag :type="getOperationTypeTagType(viewLog.operationType)">
            {{ getOperationTypeLabel(viewLog.operationType) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="viewLog.status === '0' ? 'success' : 'danger'">
            {{ viewLog.status === '0' ? '成功' : '失败' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="用户ID">{{ viewLog.userId || '-' }}</el-descriptions-item>
        <el-descriptions-item label="执行时间">{{ viewLog.durationMs ? viewLog.durationMs + ' ms' : '-' }}</el-descriptions-item>
        <el-descriptions-item label="请求参数">
          <pre v-if="viewLog.params" class="json-preview">{{ formatJson(viewLog.params) }}</pre>
          <span v-else>-</span>
        </el-descriptions-item>
        <el-descriptions-item label="响应结果">
          <pre v-if="viewLog.result" class="json-preview">{{ formatJson(viewLog.result) }}</pre>
          <span v-else>-</span>
        </el-descriptions-item>
        <el-descriptions-item label="错误信息">
          <div class="log-message-detail">{{ viewLog.errorMessage || '-' }}</div>
        </el-descriptions-item>
        <el-descriptions-item label="备注">{{ viewLog.remark || '-' }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ parseTime(viewLog.createTime) }}</el-descriptions-item>
        <el-descriptions-item label="更新时间">{{ viewLog.updateTime ? parseTime(viewLog.updateTime) : '-' }}</el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="viewOpen = false">关 闭</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="AlgorithmLog">
import { getList as listAlgorithmLog, getInfo, del, clean } from '@/api/lab/algorithmLog'

const { proxy } = getCurrentInstance()

const logList = ref([])
const open = ref(false)
const viewOpen = ref(false)
const viewLog = ref(null)
const loading = ref(true)
const ids = ref([])
const single = ref(true)
const multiple = ref(true)
const total = ref(0)
const dateRange = ref([])

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  algorithmType: null,
  operationType: null,
  status: null,
  userId: null,
  startTime: null,
  endTime: null
})

function getAlgorithmTypeLabel(type) {
  const map = {
    'GAT': 'GAT推荐算法',
    'PPO': 'PPO调度算法'
  }
  return map[type] || type
}

function getAlgorithmTypeTagType(type) {
  const map = {
    'GAT': 'primary',
    'PPO': 'success'
  }
  return map[type] || 'info'
}

function getOperationTypeLabel(type) {
  const map = {
    'TRAIN': '训练',
    'RECOMMEND': '推荐',
    'SCHEDULE': '调度',
    'QUERY': '查询'
  }
  return map[type] || type
}

function getOperationTypeTagType(type) {
  const map = {
    'TRAIN': 'warning',
    'RECOMMEND': 'primary',
    'SCHEDULE': 'success',
    'QUERY': 'info'
  }
  return map[type] || 'info'
}



function formatJson(str) {
  try {
    return JSON.stringify(JSON.parse(str), null, 2)
  } catch {
    return str
  }
}

async function getList() {
  loading.value = true
  try {
    const response = await listAlgorithmLog(queryParams)
    console.log('API返回原始数据:', response)
    logList.value = response.rows || []
    total.value = response.total || 0
  } catch (error) {
    console.error('获取日志列表失败:', error)
    proxy.$modal.msgError('获取日志列表失败')
  } finally {
    loading.value = false
  }
}

function handleQuery() {
  queryParams.pageNum = 1
  getList()
}

function resetQuery() {
  proxy.resetForm('queryRef')
  dateRange.value = []
  queryParams.algorithmType = null
  queryParams.operationType = null
  queryParams.status = null
  queryParams.userId = null
  queryParams.startTime = null
  queryParams.endTime = null
  queryParams.pageNum = 1
  getList()
}

function handleDateRangeChange(value) {
  if (value && value.length === 2) {
    queryParams.startTime = value[0]
    queryParams.endTime = value[1]
  } else {
    queryParams.startTime = null
    queryParams.endTime = null
  }
}

function handleSelectionChange(selection) {
  ids.value = selection.map(item => item.logId)
  single.value = selection.length != 1
  multiple.value = !selection.length
}

function handleView(row) {
  getInfo(row.logId).then(response => {
    viewLog.value = response.data
    viewOpen.value = true
  })
}

function handleDelete(row) {
  const logIds = row.logId || ids.value
  proxy.$modal.confirm('是否确认删除日志编号为"' + logIds + '"的数据项？').then(function() {
    return del(logIds)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess('删除成功')
  }).catch(() => {})
}

function handleClean() {
  proxy.$modal.confirm('是否确认清空所有算法日志数据？此操作不可恢复！', '警告', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(function() {
    return clean()
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess('清空成功')
  }).catch(() => {})
}

onMounted(() => {
  getList()
})
</script>

<style scoped>
.app-container {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.search-form {
  margin-bottom: 20px;
}

.json-preview {
  margin: 0;
  padding: 10px;
  background-color: #f5f7fa;
  border-radius: 4px;
  max-height: 300px;
  overflow: auto;
  white-space: pre-wrap;
  word-break: break-all;
}

.log-message-detail {
  max-height: 200px;
  overflow-y: auto;
  padding: 10px;
  background-color: #f5f7fa;
  border-radius: 4px;
  line-height: 1.6;
}
</style>
