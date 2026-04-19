<template>
  <div class="app-container">
    <el-card class="box-card">
      <template #header>
        <div class="card-header">
          <span>用户信用分日志</span>
        </div>
      </template>
      
      <!-- 搜索表单 -->
      <el-form :model="queryParams" ref="queryRef" :inline="true" class="search-form">
        <el-form-item label="用户" prop="userId">
          <el-select v-model="queryParams.userId" placeholder="请选择用户" clearable filterable>
            <el-option
              v-for="user in userList"
              :key="user.userId"
              :label="user.nickName + ' (' + user.userId + ')'"
              :value="user.userId"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="变化类型" prop="changeType">
          <el-select v-model="queryParams.changeType" placeholder="请选择变化类型" clearable>
            <el-option label="守约" value="ON_TIME" />
            <el-option label="违约" value="VIOLATION" />
            <el-option label="取消已批准" value="CANCEL_APPROVED" />
            <el-option label="取消待审核" value="CANCEL_PENDING" />
            <el-option label="手动调整" value="MANUAL" />
            <el-option label="其他" value="OTHER" />
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
        <el-form-item>
          <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
          <el-button icon="Refresh" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>

      <!-- 操作按钮 -->
      <el-row :gutter="10" class="mb8">
        <el-col :span="1.5">
          <el-button
            type="warning"
            plain
            icon="Download"
            @click="handleExport"
            v-hasPermi="['lab:creditLog:export']"
          >导出</el-button>
        </el-col>
      </el-row>

      <!-- 数据表格 -->
      <el-table v-loading="loading" :data="logList" @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="55" align="center" />
        <el-table-column label="日志ID" align="center" prop="logId" width="80" sortable />
        <el-table-column label="用户" align="center" prop="userId" width="150">
          <template #default="scope">
            <span>{{ getUserName(scope.row.userId) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="变化类型" align="center" prop="changeType" width="130">
          <template #default="scope">
            <el-tag :type="getChangeTypeTagType(scope.row.changeType)">
              {{ getChangeTypeLabel(scope.row.changeType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="变化量" align="center" prop="delta" width="100">
          <template #default="scope">
            <span :style="{ color: scope.row.delta >= 0 ? '#67C23A' : '#F56C6C', fontWeight: 'bold' }">
              {{ scope.row.delta >= 0 ? '+' : '' }}{{ scope.row.delta }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="变化前" align="center" prop="scoreBefore" width="100">
          <template #default="scope">
            <el-tag :type="getCreditScoreType(scope.row.scoreBefore)">
              {{ scope.row.scoreBefore }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="变化后" align="center" prop="scoreAfter" width="100">
          <template #default="scope">
            <el-tag :type="getCreditScoreType(scope.row.scoreAfter)">
              {{ scope.row.scoreAfter }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="预约ID" align="center" prop="reserveId" width="90">
          <template #default="scope">
            <span>{{ scope.row.reserveId || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="备注" align="left" prop="remark" min-width="200" show-overflow-tooltip />
        <el-table-column label="创建时间" align="center" prop="createTime" width="170" sortable>
          <template #default="scope">
            <span>{{ parseTime(scope.row.createTime) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="100">
          <template #default="scope">
            <el-button link type="primary" icon="View" @click="handleView(scope.row)">查看</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <pagination
        v-show="total>0"
        :total="total"
        v-model:page="queryParams.pageNum"
        v-model:limit="queryParams.pageSize"
        @pagination="getList"
      />
    </el-card>

    <!-- 查看详情对话框 -->
    <el-dialog title="信用分日志详情" v-model="viewOpen" width="600px" append-to-body>
      <el-descriptions :column="2" border v-if="viewLog">
        <el-descriptions-item label="日志ID">{{ viewLog.logId }}</el-descriptions-item>
        <el-descriptions-item label="用户">{{ getUserName(viewLog.userId) }}</el-descriptions-item>
        <el-descriptions-item label="变化类型">
          <el-tag :type="getChangeTypeTagType(viewLog.changeType)">
            {{ getChangeTypeLabel(viewLog.changeType) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="变化量">
          <span :style="{ color: viewLog.delta >= 0 ? '#67C23A' : '#F56C6C', fontWeight: 'bold' }">
            {{ viewLog.delta >= 0 ? '+' : '' }}{{ viewLog.delta }}
          </span>
        </el-descriptions-item>
        <el-descriptions-item label="变化前">
          <el-tag :type="getCreditScoreType(viewLog.scoreBefore)">
            {{ viewLog.scoreBefore }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="变化后">
          <el-tag :type="getCreditScoreType(viewLog.scoreAfter)">
            {{ viewLog.scoreAfter }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="预约ID">{{ viewLog.reserveId || '-' }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ parseTime(viewLog.createTime) }}</el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">{{ viewLog.remark || '-' }}</el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="viewOpen = false">关 闭</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="CreditLog">
import { listCreditLog, getCreditLog, delCreditLog, exportCreditLog } from "@/api/lab/creditLog"
import { listUser } from "@/api/system/user"

const { proxy } = getCurrentInstance()

const logList = ref([])
const viewOpen = ref(false)
const viewLog = ref(null)
const loading = ref(true)
const ids = ref([])
const total = ref(0)
const userList = ref([])
const dateRange = ref([])

const data = reactive({
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    userId: null,
    changeType: null,
    startTime: null,
    endTime: null
  }
})

const { queryParams } = toRefs(data)

/** 获取用户名称 */
function getUserName(userId) {
  if (!userId) return '-'
  const user = userList.value.find(u => u.userId === userId)
  return user ? user.nickName : `用户 ${userId}`
}

/** 加载用户列表 */
async function loadUserList() {
  try {
    const res = await listUser({ pageNum: 1, pageSize: 1000 })
    userList.value = res.rows || []
  } catch (error) {
    console.error('加载用户列表失败:', error)
  }
}

/** 获取信用分数标签类型 */
function getCreditScoreType(score) {
  if (!score && score !== 0) return 'info'
  if (score >= 120) return 'success'
  if (score >= 100) return ''
  if (score >= 80) return 'warning'
  return 'danger'
}

/** 获取变化类型标签类型 */
function getChangeTypeTagType(type) {
  const map = {
    'ON_TIME': 'success',
    'VIOLATION': 'danger',
    'CANCEL_APPROVED': 'warning',
    'CANCEL_PENDING': 'info',
    'MANUAL': 'primary',
    'OTHER': 'info'
  }
  return map[type] || 'info'
}

/** 获取变化类型标签 */
function getChangeTypeLabel(type) {
  const map = {
    'ON_TIME': '守约',
    'VIOLATION': '违约',
    'CANCEL_APPROVED': '取消已批准',
    'CANCEL_PENDING': '取消待审核',
    'MANUAL': '手动调整',
    'OTHER': '其他'
  }
  return map[type] || type
}

/** 查看详情 */
function handleView(row) {
  viewLog.value = row
  viewOpen.value = true
}

/** 查询列表 */
function getList() {
  loading.value = true
  listCreditLog(queryParams.value).then(response => {
    logList.value = response.rows
    total.value = response.total
    loading.value = false
  }).catch(() => {
    loading.value = false
  })
}

/** 搜索按钮操作 */
function handleQuery() {
  queryParams.value.pageNum = 1
  getList()
}

/** 重置按钮操作 */
function resetQuery() {
  proxy.resetForm("queryRef")
  dateRange.value = []
  queryParams.value.userId = null
  queryParams.value.changeType = null
  queryParams.value.startTime = null
  queryParams.value.endTime = null
  queryParams.value.pageNum = 1
  getList()
}

/** 时间范围变化 */
function handleDateRangeChange(value) {
  if (value && value.length === 2) {
    queryParams.value.startTime = value[0]
    queryParams.value.endTime = value[1]
  } else {
    queryParams.value.startTime = null
    queryParams.value.endTime = null
  }
}

/** 多选框选中数据 */
function handleSelectionChange(selection) {
  ids.value = selection.map(item => item.logId)
}

/** 导出按钮操作 */
function handleExport() {
  proxy.download('lab/credit/log/export', {
    ...queryParams.value
  }, `credit_log_${new Date().getTime()}.xlsx`)
}

onMounted(() => {
  loadUserList()
  getList()
})
</script>

<style scoped lang="scss">
.app-container {
  padding: 20px;
}

.box-card {
  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    font-weight: bold;
    font-size: 16px;
  }
}

.search-form {
  margin-bottom: 20px;
}

.mb8 {
  margin-bottom: 20px;
}
</style>
