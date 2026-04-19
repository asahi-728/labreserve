<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="实验室" prop="labId">
        <el-select v-model="queryParams.labId" placeholder="请选择实验室" clearable>
          <el-option
            v-for="item in laboratoryList"
            :key="item.labId"
            :label="item.labName"
            :value="item.labId"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="预约日期">
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
      <el-form-item label="预约状态">
        <el-select v-model="queryParams.status" placeholder="请选择状态" clearable>
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

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button
          type="primary"
          plain
          icon="Plus"
          @click="handleAdd"
        >发起预约</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="reservationList">
      <el-table-column label="预约ID" align="center" prop="reserveId" width="80" />
      <el-table-column label="实验室" align="center" prop="labName" width="150" show-overflow-tooltip />
      <el-table-column label="设备名称" align="center" prop="deviceName" width="150" show-overflow-tooltip />
      <el-table-column label="预约日期" align="center" prop="startTime" width="120">
        <template #default="scope">
          {{ parseTime(scope.row.startTime, '{y}-{m}-{d}') }}
        </template>
      </el-table-column>
      <el-table-column label="开始时间" align="center" prop="startTime" width="100">
        <template #default="scope">
          {{ parseTime(scope.row.startTime, '{h}:{i}') }}
        </template>
      </el-table-column>
      <el-table-column label="结束时间" align="center" prop="endTime" width="100">
        <template #default="scope">
          {{ parseTime(scope.row.endTime, '{h}:{i}') }}
        </template>
      </el-table-column>
      <el-table-column label="预约状态" align="center" prop="status" width="120">
        <template #default="scope">
          <dict-tag :options="lab_reservation_status" :value="scope.row.status" />
        </template>
      </el-table-column>
      <el-table-column label="审核意见" align="center" prop="auditRemark" show-overflow-tooltip>
        <template #default="scope">
          {{ scope.row.auditRemark || '-' }}
        </template>
      </el-table-column>
      <el-table-column label="预约理由" align="center" prop="remark" show-overflow-tooltip>
        <template #default="scope">
          {{ scope.row.remark || '-' }}
        </template>
      </el-table-column>
      <el-table-column label="创建时间" align="center" prop="createTime" width="170">
        <template #default="scope">
          {{ parseTime(scope.row.createTime) }}
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="260">
        <template #default="scope">
          <el-button
            link
            type="primary"
            icon="View"
            @click="handleView(scope.row)"
          >查看</el-button>
          <el-button
            v-if="scope.row.status === '0' || scope.row.status === '1'"
            link
            type="warning"
            icon="Close"
            @click="handleCancel(scope.row)"
          >取消预约</el-button>
          <el-button
            v-if="scope.row.status === '1'"
            link
            type="primary"
            icon="VideoPlay"
            @click="handleStartUsing(scope.row)"
          >开始使用</el-button>
          <el-button
            v-if="scope.row.status === '6'"
            link
            type="success"
            icon="Check"
            @click="handleComplete(scope.row)"
          >完成使用</el-button>
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

    <!-- 查看预约详情对话框 -->
    <el-dialog title="预约详情" v-model="viewOpen" width="700px" append-to-body>
      <el-descriptions :column="2" border v-if="viewReservation">
        <el-descriptions-item label="预约ID">{{ viewReservation.reserveId }}</el-descriptions-item>
        <el-descriptions-item label="实验室">{{ viewReservation.labName }}</el-descriptions-item>
        <el-descriptions-item label="设备名称">{{ viewReservation.deviceName }}</el-descriptions-item>
        <el-descriptions-item label="设备位置">{{ viewReservation.location || '-' }}</el-descriptions-item>
        <el-descriptions-item label="预约日期" :span="2">
          {{ parseTime(viewReservation.startTime, '{y}-{m}-{d}') }}
        </el-descriptions-item>
        <el-descriptions-item label="预约时段">
          {{ parseTime(viewReservation.startTime, '{h}:{i}') }} - {{ parseTime(viewReservation.endTime, '{h}:{i}') }}
        </el-descriptions-item>
        <el-descriptions-item label="预约状态">
          <dict-tag :options="lab_reservation_status" :value="viewReservation.status" />
        </el-descriptions-item>
        <el-descriptions-item label="审核意见" :span="2">
          {{ viewReservation.auditRemark || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="预约理由" :span="2">
          {{ viewReservation.remark || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="创建时间">
          {{ parseTime(viewReservation.createTime) }}
        </el-descriptions-item>
        <el-descriptions-item label="审核时间">
          {{ viewReservation.auditTime ? parseTime(viewReservation.auditTime, '{y}-{m}-{d} {h}:{i}:{s}') : '-' }}
        </el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="viewOpen = false">关 闭</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="MyReservation">
import { useRouter } from 'vue-router'
import { listMyReservation, getReservation, cancelReservation, completeReservation, startUsing } from "@/api/lab/myReservation"
import { listLaboratory } from "@/api/lab/laboratory"
import { parseTime } from '@/utils/ruoyi'
import useUserStore from '@/store/modules/user'

const router = useRouter()
const { proxy } = getCurrentInstance()
const { lab_reservation_status } = useDict('lab_reservation_status')

const userStore = useUserStore()
const reservationList = ref([])
const laboratoryList = ref([])
const loading = ref(true)
const showSearch = ref(true)
const total = ref(0)
const dateRange = ref([])
const viewOpen = ref(false)
const viewReservation = ref(null)

const STATUS_CANCELLED = '5'
const STATUS_COMPLETED = '3'

const data = reactive({
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    userId: null,
    labId: null,
    startTimeStr: null,
    endTimeStr: null,
    status: null
  }
})

const { queryParams } = toRefs(data)

async function loadLaboratoryList() {
  try {
    const res = await listLaboratory({ status: '0' })
    laboratoryList.value = res.rows || []
  } catch (error) {
    console.error('加载实验室列表失败:', error)
  }
}

function getList() {
  loading.value = true
  queryParams.value.userId = userStore.id
  listMyReservation(queryParams.value).then(response => {
    reservationList.value = response.rows || []
    total.value = response.total
    loading.value = false
  }).catch(() => {
    loading.value = false
  })
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
    userId: userStore.id,
    labId: null,
    startTimeStr: null,
    endTimeStr: null,
    status: null
  }
  handleQuery()
}

function handleView(row) {
  viewReservation.value = row
  viewOpen.value = true
}

function handleCancel(row) {
  proxy.$modal.confirm('确认取消该预约吗？取消后将无法恢复。').then(() => {
    cancelReservation(row.reserveId).then(() => {
      proxy.$modal.msgSuccess('预约已成功取消')
      getList()
    }).catch((error) => {
      proxy.$modal.msgError(error.message || '取消预约失败，请稍后重试')
    })
  }).catch(() => {})
}

function handleComplete(row) {
  proxy.$modal.confirm('确认已完成该设备的使用吗？').then(() => {
    completeReservation(row.reserveId).then(() => {
      proxy.$modal.msgSuccess('已完成确认，感谢您的使用')
      getList()
    }).catch((error) => {
      proxy.$modal.msgError(error.message || '操作失败，请稍后重试')
    })
  }).catch(() => {})
}

function handleStartUsing(row) {
  proxy.$modal.confirm('确认要开始使用该设备吗？').then(() => {
    startUsing(row.reserveId).then(() => {
      proxy.$modal.msgSuccess('已开始使用设备')
      getList()
    }).catch((error) => {
      proxy.$modal.msgError(error.message || '操作失败，请稍后重试')
    })
  }).catch(() => {})
}

function handleAdd() {
  router.push('/lab/reservation/create')
}

onMounted(() => {
  queryParams.value.userId = userStore.id
  loadLaboratoryList()
  getList()
})
</script>

<style scoped>
.mb8 {
  margin-bottom: 8px;
}
</style>
