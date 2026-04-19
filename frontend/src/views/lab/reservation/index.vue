<template>
  <div class="app-container">
    <el-card class="mode-card">
      <div class="mode-switch-container">
        <div class="mode-info">
          <el-icon v-if="approvalMode === 'manual'" class="mode-icon"><User /></el-icon>
          <el-icon v-else class="mode-icon"><Cpu /></el-icon>
          <span class="mode-label">当前模式：{{ approvalMode === 'manual' ? '人工审批' : 'PPO算法自动审批' }}</span>
        </div>
        <el-switch
          v-model="approvalMode"
          active-value="auto"
          inactive-value="manual"
          active-text="PPO自动"
          inactive-text="人工"
          style="--el-switch-on-color: #67c23a; --el-switch-off-color: #409eff"
        />
      </div>
    </el-card>

    <el-card class="search-card">
      <template #header>
        <div class="card-header">
          <span>高级筛选</span>
        </div>
      </template>
      <el-form :model="queryParams" ref="queryRef" :inline="true" label-width="100px">
        <el-form-item label="设备分类">
          <el-tree-select
            v-model="queryParams.categoryId"
            :data="categoryTreeList"
            :props="{ value: 'id', label: 'label', children: 'children' }"
            value-key="id"
            placeholder="请选择设备分类"
            clearable
            check-strictly
          />
        </el-form-item>
        <el-form-item label="所属实验室">
          <el-select v-model="queryParams.labId" placeholder="请选择实验室" clearable>
            <el-option
              v-for="item in laboratoryList"
              :key="item.labId"
              :label="item.labName"
              :value="item.labId"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="申请人">
          <el-input
            v-model="queryParams.userName"
            placeholder="请输入申请人"
            clearable
          />
        </el-form-item>
        <el-form-item label="预约时间段">
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            @change="handleDateRangeChange"
          />
        </el-form-item>
        <el-form-item label="审批状态">
          <el-select v-model="queryParams.status" placeholder="请选择审批状态" clearable>
            <el-option
              v-for="dict in lab_reservation_status"
              :key="dict.value"
              :label="dict.label"
              :value="dict.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
          <el-button icon="Refresh" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5" v-if="approvalMode === 'manual'">
        <el-button
          type="success"
          :disabled="selectedRows.length === 0"
          @click="handleBatchApprove"
          v-hasPermi="['lab:reservation:audit']"
          icon="Check"
        >批量通过</el-button>
      </el-col>
      <el-col :span="1.5" v-if="approvalMode === 'manual'">
        <el-button
          type="warning"
          :disabled="selectedRows.length === 0"
          @click="handleBatchReject"
          v-hasPermi="['lab:reservation:audit']"
          icon="Close"
        >批量拒绝</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="primary"
          :disabled="selectedRows.length === 0"
          @click="handlePpoBatchAudit"
          v-hasPermi="['lab:reservation:audit']"
          icon="Cpu"
        >PPO审批</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          icon="Download"
          @click="handleExport"
          v-hasPermi="['lab:reservation:export']"
        >导出审批记录</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table 
      v-loading="loading" 
      :data="reservationList" 
      @selection-change="handleSelectionChange"
      row-key="reserveId"
      :row-class-name="getRowClassName"
    >
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="预约ID" align="center" prop="reserveId" width="80" />
      <el-table-column label="申请人" align="center" prop="userName" width="120" />
      <el-table-column label="设备名称" align="center" prop="deviceName" width="150" />
      <el-table-column label="设备分类" align="center" prop="categoryName" width="120" />
      <el-table-column label="所属实验室" align="center" prop="labName" width="140" />
      <el-table-column label="开始时间" align="center" prop="startTime" width="170">
        <template #default="scope">
          <span>{{ parseTime(scope.row.startTime, '{y}-{m}-{d} {h}:{i}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="结束时间" align="center" prop="endTime" width="170">
        <template #default="scope">
          <span>{{ parseTime(scope.row.endTime, '{y}-{m}-{d} {h}:{i}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="审批状态" align="center" prop="status" width="120">
        <template #default="scope">
          <dict-tag :options="lab_reservation_status" :value="scope.row.status">
            <template #default="{ option }">
              <el-tag :type="getStatusType(scope.row.status)" effect="dark">
                <el-icon v-if="isPendingStatus(scope.row.status)" class="status-icon"><Clock /></el-icon>
                <el-icon v-else-if="isApprovedStatus(scope.row.status)" class="status-icon"><CircleCheck /></el-icon>
                <el-icon v-else-if="isRejectedStatus(scope.row.status)" class="status-icon"><CircleClose /></el-icon>
                <el-icon v-else class="status-icon"><CircleClose /></el-icon>
                {{ option.label }}
              </el-tag>
            </template>
          </dict-tag>
        </template>
      </el-table-column>
      <el-table-column label="审批人" align="center" prop="auditBy" width="100">
        <template #default="scope">
          <span>{{ scope.row.auditBy || '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="审批时间" align="center" prop="auditTime" width="170">
        <template #default="scope">
          <span>{{ scope.row.auditTime ? parseTime(scope.row.auditTime, '{y}-{m}-{d} {h}:{i}:{s}') : '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="审批意见" align="center" prop="auditRemark" show-overflow-tooltip>
        <template #default="scope">
          <span>{{ scope.row.auditRemark || '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="创建时间" align="center" prop="createTime" width="170">
        <template #default="scope">
          <span>{{ parseTime(scope.row.createTime) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="280">
        <template #default="scope">
          <el-button link type="primary" icon="View" @click="handleView(scope.row)">查看</el-button>
          <el-button 
            v-if="isPendingStatus(scope.row.status) && approvalMode === 'manual'" 
            link type="success" 
            icon="Check" 
            @click="handleAudit(scope.row, STATUS_APPROVED)" 
            v-hasPermi="['lab:reservation:audit']"
          >通过</el-button>
          <el-button 
            v-if="isPendingStatus(scope.row.status) && approvalMode === 'manual'" 
            link type="warning" 
            icon="Close" 
            @click="handleAudit(scope.row, STATUS_REJECTED)" 
            v-hasPermi="['lab:reservation:audit']"
          >拒绝</el-button>
          <el-button 
            v-if="isPendingStatus(scope.row.status) && approvalMode === 'auto'" 
            link type="primary" 
            icon="Edit" 
            @click="handleManualIntervention(scope.row)"
          >人工干预</el-button>
        </template>
      </el-table-column>
    </el-table>
    
    <pagination
      v-show="total>0"
      :total="total"
      v-model:page="queryParams.pageNum"
      v-model:limit="queryParams.pageSize"
      @pagination="getList"
    />

    <el-dialog title="预约详情" v-model="viewOpen" width="900px" append-to-body>
      <el-descriptions :column="2" border v-if="viewReservation">
        <el-descriptions-item label="预约ID">{{ viewReservation.reserveId }}</el-descriptions-item>
        <el-descriptions-item label="申请人">{{ viewReservation.userName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="设备名称">{{ viewReservation.deviceName }}</el-descriptions-item>
        <el-descriptions-item label="设备分类">{{ viewReservation.categoryName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="所属实验室">{{ viewReservation.labName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="设备位置">{{ viewReservation.location || '-' }}</el-descriptions-item>
        <el-descriptions-item label="审批状态" :span="2">
          <dict-tag :options="lab_reservation_status" :value="viewReservation.status">
            <template #default="{ option }">
              <el-tag :type="getStatusType(viewReservation.status)" effect="dark">
                {{ option.label }}
              </el-tag>
            </template>
          </dict-tag>
        </el-descriptions-item>
        <el-descriptions-item label="开始时间" :span="2">
          {{ parseTime(viewReservation.startTime, '{y}-{m}-{d} {h}:{i}') }}
        </el-descriptions-item>
        <el-descriptions-item label="结束时间" :span="2">
          {{ parseTime(viewReservation.endTime, '{y}-{m}-{d} {h}:{i}') }}
        </el-descriptions-item>
        <el-descriptions-item label="审批人">{{ viewReservation.auditBy || '-' }}</el-descriptions-item>
        <el-descriptions-item label="审批时间">
          {{ viewReservation.auditTime ? parseTime(viewReservation.auditTime, '{y}-{m}-{d} {h}:{i}:{s}') : '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="审批意见" :span="2">
          {{ viewReservation.auditRemark || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="预约备注" :span="2">
          {{ viewReservation.remark || '-' }}
        </el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="viewOpen = false">关 闭</el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog 
      :title="auditDialogTitle" 
      v-model="auditOpen" 
      width="600px" 
      append-to-body
      :close-on-click-modal="false"
    >
      <el-form ref="auditFormRef" :model="auditForm" :rules="auditRules" label-width="100px">
        <el-form-item label="预约ID">
          <el-input v-model="auditForm.reserveId" disabled />
        </el-form-item>
        <el-form-item label="设备名称">
          <el-input v-model="auditForm.deviceName" disabled />
        </el-form-item>
        <el-form-item label="预约时间">
          <el-input :value="auditTimeRange" disabled />
        </el-form-item>
        <el-form-item label="审批意见" prop="auditRemark">
          <el-input
            v-model="auditForm.auditRemark"
            type="textarea"
            :rows="4"
            :placeholder="auditAction === STATUS_APPROVED ? '请输入审批通过的意见（可选）' : '请输入拒绝理由（必填）'"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" :loading="auditLoading" @click="submitAudit">
            {{ auditAction === STATUS_APPROVED ? '确认通过' : '确认拒绝' }}
          </el-button>
          <el-button @click="cancelAudit">取 消</el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog title="PPO算法自动审批规则说明" v-model="ruleOpen" width="800px" append-to-body>
      <el-alert
        title="PPO算法自动审批接口设计"
        type="info"
        :closable="false"
        style="margin-bottom: 20px;"
      >
        <template #default>
          <p><strong>接口地址：</strong>POST /api/lab/reservation/ppoAutoAudit</p>
          <p><strong>请求格式：</strong></p>
          <pre>{
  "reserveIds": [1, 2, 3],
  "modelParameters": {
    "learningRate": 0.001,
    "gamma": 0.99,
    "clipRatio": 0.2
  }
}</pre>
          <p><strong>响应格式：</strong></p>
          <pre>{
  "code": 200,
  "data": [
    {
      "reserveId": 1,
      "status": "approved",
      "confidence": 0.95,
      "reason": "符合所有审批规则"
    }
  ]
}</pre>
        </template>
      </el-alert>
      <el-divider content-position="left">审批规则说明</el-divider>
      <el-timeline>
        <el-timeline-item
          v-for="(activity, index) in ppoRules"
          :key="index"
          :timestamp="activity.rule"
          :type="activity.type"
        >
          {{ activity.description }}
        </el-timeline-item>
      </el-timeline>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="ruleOpen = false">关 闭</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="ReservationApproval">
import { listReservation, getReservation, auditReservation, ppoBatchAudit } from "@/api/lab/reservation"
import { listDevice } from "@/api/lab/device"
import { listCategory, treeSelectCategory } from "@/api/lab/category"
import { listLaboratory } from "@/api/lab/laboratory"
import { User, Cpu, Clock, CircleCheck, CircleClose, Check, Close, View, Edit, Search, Refresh, Download } from '@element-plus/icons-vue'
import { parseTime } from '@/utils/ruoyi'

const { proxy } = getCurrentInstance()
const { lab_reservation_status } = useDict('lab_reservation_status')

// 从 localStorage 读取持久化的审批模式，默认人工
const savedMode = localStorage.getItem('approvalMode') || 'manual'
const approvalMode = ref(savedMode)
const ppoLoading = ref(false)

// 监听模式变化，持久化到 localStorage
watch(approvalMode, (newVal) => {
  localStorage.setItem('approvalMode', newVal)
})
const reservationList = ref([])
const deviceList = ref([])
const categoryList = ref([])
const categoryTreeList = ref([])
const laboratoryList = ref([])
const viewOpen = ref(false)
const viewReservation = ref(null)
const auditOpen = ref(false)
const ruleOpen = ref(false)
const loading = ref(true)
const showSearch = ref(true)
const ids = ref([])
const selectedRows = ref([])
const total = ref(0)
const dateRange = ref([])
const auditLoading = ref(false)
const auditAction = ref('approved')

const ppoRules = [
  {
    rule: '规则1：用户信用检查',
    type: 'primary',
    description: '检查用户历史预约记录，信用评分高于80分优先通过'
  },
  {
    rule: '规则2：设备冲突检测',
    type: 'success',
    description: '检测预约时间段内设备是否被占用，无冲突优先通过'
  },
  {
    rule: '规则3：预约时长限制',
    type: 'warning',
    description: '单次预约不超过8小时，连续预约不超过3天'
  },
  {
    rule: '规则4：资源利用率优化',
    type: 'danger',
    description: '基于历史使用数据，优化设备资源分配效率'
  }
]

const data = reactive({
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    categoryId: null,
    labId: null,
    userName: null,
    startTimeStr: null,
    endTimeStr: null,
    status: null,
  },
})

const { queryParams } = toRefs(data)

const auditForm = reactive({
  reserveId: null,
  deviceName: '',
  startTime: '',
  endTime: '',
  auditRemark: ''
})

const auditRules = {
  auditRemark: [
    { 
      validator: (rule, value, callback) => {
        if (auditAction.value === STATUS_REJECTED && !value) {
          callback(new Error('拒绝时必须填写理由'))
        } else {
          callback()
        }
      }, 
      trigger: 'blur' 
    }
  ]
}

const auditDialogTitle = computed(() => {
  return auditAction.value === STATUS_APPROVED ? '审批通过' : '审批拒绝'
})

const auditTimeRange = computed(() => {
  if (auditForm.startTime && auditForm.endTime) {
    return `${parseTime(auditForm.startTime, '{y}-{m}-{d} {h}:{i}')} 至 ${parseTime(auditForm.endTime, '{y}-{m}-{d} {h}:{i}')}`
  }
  return ''
})

const STATUS_PENDING = '0'
const STATUS_APPROVED = '1'
const STATUS_REJECTED = '2'
const STATUS_COMPLETED = '3'
const STATUS_BREACH = '4'

function isPendingStatus(status) {
  return status === STATUS_PENDING
}

function isApprovedStatus(status) {
  return status === STATUS_APPROVED
}

function isRejectedStatus(status) {
  return status === STATUS_REJECTED
}

function flattenTree(tree, result = []) {
  tree.forEach(item => {
    result.push({
      categoryId: item.categoryId,
      categoryName: item.categoryName,
      parentId: item.parentId,
      sort: item.sort,
      status: item.status
    })
    if (item.children && item.children.length > 0) {
      flattenTree(item.children, result)
    }
  })
  return result
}

async function loadCategoryList() {
  try {
    const [listRes, treeRes] = await Promise.all([
      listCategory({ status: '0' }),
      treeSelectCategory({ status: '0' })
    ])
    const treeData = listRes.data || []
    categoryList.value = flattenTree(treeData)
    categoryTreeList.value = treeRes.data || []
  } catch (error) {
    console.error('加载分类列表失败:', error)
  }
}

async function loadLaboratoryList() {
  try {
    const res = await listLaboratory({ status: '0' })
    laboratoryList.value = res.rows || []
  } catch (error) {
    console.error('加载实验室列表失败:', error)
  }
}

async function loadDeviceList() {
  try {
    const res = await listDevice({})
    deviceList.value = res.rows || []
  } catch (error) {
    console.error('加载设备列表失败:', error)
  }
}

function getStatusType(status) {
  const typeMap = {}
  typeMap[STATUS_PENDING] = 'warning'
  typeMap[STATUS_APPROVED] = 'success'
  typeMap[STATUS_REJECTED] = 'danger'
  typeMap[STATUS_COMPLETED] = 'info'
  typeMap[STATUS_BREACH] = 'danger'
  return typeMap[status] || 'info'
}

function getRowClassName({ row }) {
  if (isPendingStatus(row.status)) {
    return 'row-pending'
  }
  return ''
}

function getList() {
  loading.value = true
  listReservation(queryParams.value).then(response => {
    reservationList.value = response.rows.map(item => {
      const device = deviceList.value.find(d => d.deviceId === item.deviceId)
      const category = categoryList.value.find(c => c.categoryId === device?.categoryId)
      const lab = laboratoryList.value.find(l => l.labId === device?.labId)
      return {
        ...item,
        deviceName: device?.deviceName || '-',
        categoryName: category?.categoryName || '-',
        labName: lab?.labName || '-',
        location: lab?.location || '-',
        userName: item.createBy || '-'
      }
    })
    total.value = response.total
    loading.value = false
  })
}

function handleView(row) {
  const device = deviceList.value.find(d => d.deviceId === row.deviceId)
  const category = categoryList.value.find(c => c.categoryId === device?.categoryId)
  const lab = laboratoryList.value.find(l => l.labId === device?.labId)
  viewReservation.value = {
    ...row,
    deviceName: device?.deviceName || '-',
    categoryName: category?.categoryName || '-',
    labName: lab?.labName || '-',
    location: lab?.location || '-',
    userName: row.createBy || '-'
  }
  viewOpen.value = true
}

function handleAudit(row, status) {
  auditAction.value = status
  const device = deviceList.value.find(d => d.deviceId === row.deviceId)
  auditForm.reserveId = row.reserveId
  auditForm.deviceName = device?.deviceName || '-'
  auditForm.startTime = row.startTime
  auditForm.endTime = row.endTime
  auditForm.auditRemark = status === STATUS_APPROVED ? '' : ''
  auditOpen.value = true
}

function handleManualIntervention(row) {
  approvalMode.value = 'manual'
  handleView(row)
  proxy.$modal.msgInfo('已切换至人工审批模式，请在详情页进行操作')
}

function submitAudit() {
  proxy.$refs["auditFormRef"].validate(valid => {
    if (valid) {
      auditLoading.value = true
      const auditData = {
        reserveId: auditForm.reserveId,
        status: auditAction.value,
        auditRemark: auditForm.auditRemark || (auditAction.value === STATUS_APPROVED ? '预约审核通过' : '预约审核未通过')
      }
      auditReservation(auditData).then(() => {
        proxy.$modal.msgSuccess(`审核${auditAction.value === STATUS_APPROVED ? '通过' : '拒绝'}成功`)
        auditOpen.value = false
        auditLoading.value = false
        getList()
      }).catch(() => {
        auditLoading.value = false
      })
    }
  })
}

function cancelAudit() {
  auditOpen.value = false
  proxy.resetForm("auditFormRef")
}

function handleBatchApprove() {
  console.log('handleBatchApprove 被调用')
  console.log('selectedRows:', selectedRows.value)
  console.log('STATUS_PENDING:', STATUS_PENDING)

  const pendingRows = selectedRows.value.filter(row => isPendingStatus(row.status))
  
  console.log('pendingRows:', pendingRows)

  if (selectedRows.value.length > 0 && pendingRows.length === 0) {
    proxy.$modal.msgWarning(`已选择 ${selectedRows.value.length} 条记录，但没有待审批的记录，请重新选择`)
    return
  }
  
  if (pendingRows.length === 0) {
    proxy.$modal.msgWarning('请选择待审批的记录')
    return
  }
  
  proxy.$modal.confirm(`确认批量通过 ${pendingRows.length} 条预约申请吗？`).then(function() {
    let successCount = 0
    let failCount = 0
    const promises = pendingRows.map(row => {
      const auditData = {
        reserveId: row.reserveId,
        status: STATUS_APPROVED,
        auditRemark: '批量审批通过'
      }
      return auditReservation(auditData).then(() => {
        successCount++
      }).catch(() => {
        failCount++
      })
    })
    Promise.all(promises).finally(() => {
      getList()
      if (failCount === 0) {
        proxy.$modal.msgSuccess(`成功批量通过 ${successCount} 条预约申请`)
      } else {
        proxy.$modal.msgWarning(`成功通过 ${successCount} 条，失败 ${failCount} 条`)
      }
    })
  }).catch(() => {})
}

function handleBatchReject() {
  console.log('handleBatchReject 被调用')
  console.log('selectedRows:', selectedRows.value)
  console.log('STATUS_PENDING:', STATUS_PENDING)

  const pendingRows = selectedRows.value.filter(row => isPendingStatus(row.status))
  
  console.log('pendingRows:', pendingRows)

  if (selectedRows.value.length > 0 && pendingRows.length === 0) {
    proxy.$modal.msgWarning(`已选择 ${selectedRows.value.length} 条记录，但没有待审批的记录，请重新选择`)
    return
  }
  
  if (pendingRows.length === 0) {
    proxy.$modal.msgWarning('请选择待审批的记录')
    return
  }
  
  proxy.$modal.confirm(`确认批量拒绝 ${pendingRows.length} 条预约申请吗？`).then(function() {
    let successCount = 0
    let failCount = 0
    const promises = pendingRows.map(row => {
      const auditData = {
        reserveId: row.reserveId,
        status: STATUS_REJECTED,
        auditRemark: '批量审批拒绝'
      }
      return auditReservation(auditData).then(() => {
        successCount++
      }).catch(() => {
        failCount++
      })
    })
    Promise.all(promises).finally(() => {
      getList()
      if (failCount === 0) {
        proxy.$modal.msgSuccess(`成功批量拒绝 ${successCount} 条预约申请`)
      } else {
        proxy.$modal.msgWarning(`成功拒绝 ${successCount} 条，失败 ${failCount} 条`)
      }
    })
  }).catch(() => {})
}

function handleAutoApprove() {
  // 切换到 PPO 自动模式后，使用 PPO 审批按钮进行审批
  proxy.$modal.msgInfo('已切换至PPO自动审批模式，请选中预约后点击「PPO审批」按钮进行自动审批')
}

function handlePpoBatchAudit() {
  const pendingRows = selectedRows.value.filter(row => isPendingStatus(row.status))
  
  if (selectedRows.value.length > 0 && pendingRows.length === 0) {
    proxy.$modal.msgWarning(`已选择 ${selectedRows.value.length} 条记录，但没有待审批的记录，请重新选择`)
    return
  }
  
  if (pendingRows.length === 0) {
    proxy.$modal.msgWarning('请选择待审批的预约记录')
    return
  }
  
  const pendingIds = pendingRows.map(row => row.reserveId)
  
  proxy.$modal.confirm(`确认使用 PPO 算法对选中的 ${pendingRows.length} 条预约申请进行自动审批吗？<br/><br/>` +
    `<span style='color:#E6A23C'>PPO算法将根据用户信用分、设备冲突检测、资源利用率等因素自动决策批准或拒绝。</span>`).then(function() {
    proxy.$modal.loading('正在调用PPO算法进行智能审批...')
    
    ppoBatchAudit({ reserveIds: pendingIds }).then(response => {
      proxy.$modal.closeLoading()
      
      const results = response.data || []
      let approvedCount = 0
      let rejectedCount = 0
      let errorCount = 0
      let pendingCount = 0
      
      results.forEach(item => {
        if (item.status === 'approved') approvedCount++
        else if (item.status === 'rejected') rejectedCount++
        else if (item.status === 'error') errorCount++
        else pendingCount++
      })
      
      // 显示审批结果详情
      let summary = `PPO审批完成！共 ${results.length} 条：`
      let details = []
      if (approvedCount > 0) details.push(`<span style='color:#67C23A'>批准 ${approvedCount} 条</span>`)
      if (rejectedCount > 0) details.push(`<span style='color:#F56C6C'>拒绝 ${rejectedCount} 条</span>`)
      if (errorCount > 0) details.push(`<span style='color:#E6A23C'>异常 ${errorCount} 条</span>`)
      if (pendingCount > 0) details.push(`待定 ${pendingCount} 条`)
      
      proxy.$modal.alertHtml(summary + '<br/>' + details.join('、'), 'PPO审批结果')
    }).catch((error) => {
      proxy.$modal.closeLoading()
      // 只有在真正的网络/服务器错误时才显示错误提示
      // 不要在这里额外弹错误了，因为 request.js 的响应拦截器已经会自动弹出 code=500 的错误消息
      console.error('PPO审批请求异常:', error)
    }).finally(() => {
      // 无论成功或失败，都清除选中状态并刷新数据
      selectedRows.value = []
      ids.value = []
      getList()
    })
  }).catch(() => {})
}

function handleDateRangeChange(val) {
  if (val && val.length === 2) {
    queryParams.value.startTimeStr = val[0]
    queryParams.value.endTimeStr = val[1]
  } else {
    queryParams.value.startTimeStr = null
    queryParams.value.endTimeStr = null
  }
}

function handleQuery() {
  queryParams.value.pageNum = 1
  getList()
}

function resetQuery() {
  dateRange.value = []
  queryParams.value = {
    pageNum: 1,
    pageSize: 10,
    categoryId: null,
    labId: null,
    userName: null,
    startTimeStr: null,
    endTimeStr: null,
    status: null,
  }
  handleQuery()
}

function handleSelectionChange(selection) {
  console.log('handleSelectionChange 被调用, selection:', selection)
  ids.value = selection.map(item => item.reserveId)
  selectedRows.value = selection
}

function handleExport() {
  proxy.download('lab/reservation/export', {
    ...queryParams.value
  }, `approval_${new Date().getTime()}.xlsx`)
}

async function init() {
  await Promise.all([
    loadCategoryList(),
    loadLaboratoryList(),
    loadDeviceList()
  ])
  getList()
}

init()
</script>

<style scoped>
.mode-card {
  margin-bottom: 20px;
}

.mode-switch-container {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.mode-info {
  display: flex;
  align-items: center;
  gap: 8px;
}

.mode-icon {
  font-size: 20px;
  color: #409eff;
}

.mode-label {
  font-size: 16px;
  font-weight: 500;
}

.search-card {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.status-icon {
  margin-right: 4px;
}

:deep(.row-pending) {
  background-color: #fdf6ec !important;
}

:deep(.row-pending:hover) {
  background-color: #faecd8 !important;
}
</style>
