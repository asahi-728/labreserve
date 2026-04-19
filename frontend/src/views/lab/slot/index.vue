<template>
  <div class="app-container">
    <el-card class="box-card">
      <template #header>
        <div class="card-header">
          <span>设备时段管理</span>
        </div>
      </template>
      
      <el-form :inline="true" :model="queryForm" class="demo-form-inline">
        <el-form-item label="选择设备">
          <el-select v-model="selectedDeviceId" placeholder="请选择设备" clearable @change="handleDeviceChange">
            <el-option
              v-for="item in deviceList"
              :key="item.deviceId"
              :label="item.deviceName"
              :value="item.deviceId"
            />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleAdd" v-hasPermi="['lab:slot:add']">
            <el-icon><Plus /></el-icon>
            批量添加时段
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="box-card" style="margin-top: 20px;">
      <template #header>
        <div class="card-header">
          <span>{{ selectedDeviceName || '请先选择设备' }}</span>
          <span v-if="selectedDeviceId" class="subtitle">
            时段总数：{{ slotList.length }} | 可预约：{{ availableCount }} | 不可用：{{ unavailableCount }}
          </span>
        </div>
      </template>

      <el-empty v-if="!selectedDeviceId" description="请先在上方选择一个设备"></el-empty>

      <div v-else>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-calendar v-model="currentDate">
              <template #date-cell="{ data }">
                <div 
                  class="calendar-day" 
                  :class="getDayCellClass(data.day)"
                >
                  <span class="calendar-day-number">{{ data.day.split('-').slice(2).join('-') }}</span>
                  <div v-if="getSlotsOnDate(data.day).length > 0" class="occupancy-rate">
                  <span class="occupancy-value">{{ getAvailableCount(data.day) }}/{{ getSlotsOnDate(data.day).length }}</span>
                  <span class="occupancy-label">可预约</span>
                </div>
                  <div v-else class="occupancy-rate empty">
                    <span class="occupancy-value">-</span>
                  </div>
                </div>
              </template>
            </el-calendar>
          </el-col>
          <el-col :span="12">
            <el-card shadow="hover">
              <template #header>
                <div class="card-header">
                  <span>{{ formatDateForDisplay(currentDate) }} 时段详情</span>
                </div>
              </template>
              
              <el-table :data="currentDaySlots" style="width: 100%">
                <el-table-column label="时段" width="180">
                  <template #default="scope">
                    <el-tag>{{ scope.row.startTime }} - {{ scope.row.endTime }}</el-tag>
                  </template>
                </el-table-column>
                <el-table-column label="状态" width="120">
                  <template #default="scope">
                    <el-tag :type="getSlotType(scope.row.isAvailable)">
                      {{ scope.row.isAvailable === '0' || scope.row.isAvailable === 0 ? '可预约' : scope.row.isAvailable === '1' || scope.row.isAvailable === 1 ? '已预约' : '不可用' }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column label="操作" fixed="right">
                  <template #default="scope">
                    <el-button link type="primary" @click="handleUpdate(scope.row)" v-hasPermi="['lab:slot:edit']">
                      <el-icon><Edit /></el-icon>
                      编辑
                    </el-button>
                    <el-button link type="danger" @click="handleDelete(scope.row)" v-hasPermi="['lab:slot:remove']">
                      <el-icon><Delete /></el-icon>
                      删除
                    </el-button>
                  </template>
                </el-table-column>
              </el-table>

              <el-empty v-if="currentDaySlots.length === 0" description="当天无时段配置"></el-empty>
            </el-card>

            <el-card shadow="hover" style="margin-top: 20px;">
              <template #header>
                <div class="card-header">
                  <span>所有时段列表</span>
                  <div v-if="selectedDeviceId">
                    <el-button 
                      type="warning" 
                      size="small" 
                      :disabled="selectedSlots.length === 0"
                      @click="handleBatchEdit"
                      v-hasPermi="['lab:slot:edit']"
                    >
                      <el-icon><Edit /></el-icon>
                      批量修改
                    </el-button>
                    <el-button 
                      type="danger" 
                      size="small" 
                      :disabled="selectedSlots.length === 0"
                      @click="handleBatchDelete"
                      v-hasPermi="['lab:slot:remove']"
                    >
                      <el-icon><Delete /></el-icon>
                      批量删除
                    </el-button>
                  </div>
                </div>
              </template>
              
              <el-table 
                :data="sortedSlotList" 
                style="width: 100%"
                @selection-change="handleTableSelectionChange"
              >
                <el-table-column type="selection" width="55" />
                <el-table-column label="日期" width="120">
                  <template #default="scope">
                    {{ parseTime(scope.row.slotDate, '{y}-{m}-{d}') }}
                  </template>
                </el-table-column>
                <el-table-column label="时段" width="180">
                  <template #default="scope">
                    <el-tag>{{ scope.row.startTime }} - {{ scope.row.endTime }}</el-tag>
                  </template>
                </el-table-column>
                <el-table-column label="状态" width="120">
                  <template #default="scope">
                    <el-tag :type="getSlotType(scope.row.isAvailable)">
                      {{ scope.row.isAvailable === '0' || scope.row.isAvailable === 0 ? '可预约' : scope.row.isAvailable === '1' || scope.row.isAvailable === 1 ? '已预约' : '不可用' }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column label="操作" fixed="right" width="150">
                  <template #default="scope">
                    <el-button link type="primary" @click="handleUpdate(scope.row)" v-hasPermi="['lab:slot:edit']">
                      编辑
                    </el-button>
                    <el-button link type="danger" @click="handleDelete(scope.row)" v-hasPermi="['lab:slot:remove']">
                      删除
                    </el-button>
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
          </el-col>
        </el-row>
      </div>
    </el-card>

    <!-- 批量新增设备可预约时段对话框 -->
    <el-dialog :title="title" v-model="open" width="900px" append-to-body>
      <el-form ref="slotRef" :model="form" :rules="rules" label-width="100px">
        <el-row>
          <el-col :span="24">
            <el-form-item label="设备" prop="deviceId">
              <el-select v-model="form.deviceId" placeholder="请选择设备" style="width: 100%" disabled>
                <el-option
                  v-for="item in deviceList"
                  :key="item.deviceId"
                  :label="item.deviceName"
                  :value="item.deviceId"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="快速选择" prop="quickSelect">
              <el-checkbox-group v-model="quickSelectDays">
                <el-checkbox label="monday">周一</el-checkbox>
                <el-checkbox label="tuesday">周二</el-checkbox>
                <el-checkbox label="wednesday">周三</el-checkbox>
                <el-checkbox label="thursday">周四</el-checkbox>
                <el-checkbox label="friday">周五</el-checkbox>
                <el-checkbox label="saturday">周六</el-checkbox>
                <el-checkbox label="sunday">周日</el-checkbox>
              </el-checkbox-group>
              <el-button type="primary" link @click="generateDatesByWeekday" :disabled="!dateRange || quickSelectDays.length === 0">生成日期</el-button>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="日期范围" prop="dateRange">
              <el-date-picker
                v-model="dateRange"
                type="daterange"
                range-separator="至"
                start-placeholder="开始日期"
                end-placeholder="结束日期"
                value-format="YYYY-MM-DD"
                style="width: 100%"
                @change="handleDateRangeChange"
              />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="选择日期" prop="selectedDates">
              <el-select v-model="selectedDates" multiple placeholder="请选择日期" style="width: 100%">
                <el-option
                  v-for="date in allDates"
                  :key="date"
                  :label="date"
                  :value="date"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="选择时段" prop="timeSlots">
              <div style="display: flex; flex-wrap: wrap; gap: 10px;">
                <el-checkbox-group v-model="selectedTimeSlots">
                  <el-checkbox v-for="slot in predefinedTimeSlots" :key="slot.label" :label="slot">
                    {{ slot.label }}
                  </el-checkbox>
                </el-checkbox-group>
              </div>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="自定义开始时间" prop="customStartTime">
              <el-time-picker
                v-model="customStartTime"
                placeholder="请选择开始时间"
                value-format="HH:mm"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="自定义结束时间" prop="customEndTime">
              <el-time-picker
                v-model="customEndTime"
                placeholder="请选择结束时间"
                value-format="HH:mm"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item>
              <el-button type="success" @click="addCustomTimeSlot" :disabled="!customStartTime || !customEndTime">
                <el-icon><Plus /></el-icon>
                添加自定义时段
              </el-button>
              <el-button type="danger" @click="clearTimeSlots">
                <el-icon><Delete /></el-icon>
                清空时段
              </el-button>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="已选时段" prop="selectedTimeSlots">
              <el-tag
                v-for="(slot, index) in selectedTimeSlots"
                :key="index"
                closable
                @close="removeTimeSlot(index)"
                style="margin-right: 8px; margin-bottom: 8px;"
              >
                {{ slot.label }}
              </el-tag>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="是否可用" prop="isAvailable">
              <el-radio-group v-model="form.isAvailable">
                <el-radio :label="'0'">可预约</el-radio>
                <el-radio :label="'2'">不可用</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-alert
              title="即将生成"
              :type="info"
              :description="`将为 ${selectedDates.length} 个日期 × ${selectedTimeSlots.length} 个时段 = ${selectedDates.length * selectedTimeSlots.length} 个时段配置`"
              show-icon
              :closable="false"
            />
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitForm" :loading="submitLoading">
            <el-icon v-if="submitLoading"><Loading /></el-icon>
            确 定
          </el-button>
          <el-button @click="cancel">取 消</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 修改设备可预约时段对话框 -->
    <el-dialog :title="editTitle" v-model="editOpen" width="600px" append-to-body>
      <el-form ref="editSlotRef" :model="editForm" :rules="editRules" label-width="100px">
        <el-row>
          <el-col :span="24">
            <el-form-item label="设备" prop="deviceId">
              <el-select v-model="editForm.deviceId" placeholder="请选择设备" style="width: 100%" disabled>
                <el-option
                  v-for="item in deviceList"
                  :key="item.deviceId"
                  :label="item.deviceName"
                  :value="item.deviceId"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="日期" prop="slotDate">
              <el-date-picker clearable
                v-model="editForm.slotDate"
                type="date"
                value-format="YYYY-MM-DD"
                placeholder="请选择日期"
                style="width: 100%"
                disabled
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="开始时间" prop="startTime">
              <el-time-picker
                v-model="editForm.startTime"
                placeholder="请选择开始时间"
                value-format="HH:mm"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="结束时间" prop="endTime">
              <el-time-picker
                v-model="editForm.endTime"
                placeholder="请选择结束时间"
                value-format="HH:mm"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="是否可用" prop="isAvailable">
              <el-radio-group v-model="editForm.isAvailable">
                <el-radio :label="'0'">可预约</el-radio>
                <el-radio :label="'2'">不可用</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitEditForm">确 定</el-button>
          <el-button @click="cancelEdit">取 消</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 批量编辑设备可预约时段对话框 -->
    <el-dialog :title="'批量编辑 (' + selectedSlots.length + ' 个时段)'" v-model="batchEditOpen" width="500px" append-to-body>
      <el-form ref="batchEditSlotRef" :model="data.batchEditForm" label-width="100px">
        <el-row>
          <el-col :span="24">
            <el-form-item label="修改为">
              <el-radio-group v-model="data.batchEditForm.isAvailable">
                <el-radio :label="'0'">可预约</el-radio>
                <el-radio :label="'2'">不可用</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitBatchEditForm">确 定</el-button>
          <el-button @click="cancelBatchEdit">取 消</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="Slot">
import { listSlot, listAllSlot, getSlot, delSlot, addSlot, updateSlot, batchAddSlot } from "@/api/lab/slot"
import { listDevice } from "@/api/lab/device"
import { Plus, Edit, Delete, Loading } from '@element-plus/icons-vue'

const { proxy } = getCurrentInstance()

const slotList = ref([])
const calendarSlotList = ref([])
const deviceList = ref([])
const open = ref(false)
const editOpen = ref(false)
const batchEditOpen = ref(false)
const selectedDeviceId = ref(null)
const currentDate = ref(new Date())
const submitLoading = ref(false)
const selectedSlots = ref([])
const total = ref(0)
const queryParams = reactive({
  pageNum: 1,
  pageSize: 10
})

const title = ref("")
const editTitle = ref("")

const dateRange = ref([])
const allDates = ref([])
const selectedDates = ref([])
const quickSelectDays = ref([])
const customStartTime = ref("")
const customEndTime = ref("")
const selectedTimeSlots = ref([])

const predefinedTimeSlots = ref([
  { label: "08:00-10:00", startTime: "08:00", endTime: "10:00" },
  { label: "10:00-12:00", startTime: "10:00", endTime: "12:00" },
  { label: "14:00-16:00", startTime: "14:00", endTime: "16:00" },
  { label: "16:00-18:00", startTime: "16:00", endTime: "18:00" },
  { label: "19:00-21:00", startTime: "19:00", endTime: "21:00" }
])

const data = reactive({
  form: {
    deviceId: null,
    isAvailable: '0'
  },
  editForm: {},
  batchEditForm: {
    isAvailable: '0'
  },
  queryForm: {},
  rules: {
    deviceId: [
      { required: true, message: "设备不能为空", trigger: "blur" }
    ],
    selectedDates: [
      { required: true, message: "请至少选择一个日期", trigger: "change" }
    ],
    selectedTimeSlots: [
      { required: true, message: "请至少选择一个时段", trigger: "change" }
    ]
  },
  editRules: {
    deviceId: [
      { required: true, message: "设备ID不能为空", trigger: "blur" }
    ],
    slotDate: [
      { required: true, message: "日期不能为空", trigger: "blur" }
    ],
    startTime: [
      { required: true, message: "开始时段不能为空", trigger: "blur" }
    ],
    endTime: [
      { required: true, message: "结束时段不能为空", trigger: "blur" }
    ],
  }
})

const { form, rules, editForm, editRules } = toRefs(data)

const selectedDeviceName = computed(() => {
  const device = deviceList.value.find(d => d.deviceId === selectedDeviceId.value)
  return device?.deviceName || ''
})

const availableCount = computed(() => {
  return slotList.value.filter(s => s.isAvailable === '0' || s.isAvailable === 0).length
})

const unavailableCount = computed(() => {
  return slotList.value.filter(s => s.isAvailable !== '0' && s.isAvailable !== 0).length
})

const sortedSlotList = computed(() => {
  return [...slotList.value].sort((a, b) => {
    if (a.slotDate !== b.slotDate) {
      return a.slotDate.localeCompare(b.slotDate)
    }
    return a.startTime.localeCompare(b.startTime)
  })
})

const currentDaySlots = computed(() => {
  const dateStr = formatDateForAPI(currentDate.value)
  return getSlotsOnDate(dateStr)
})

const weekdayMap = {
  monday: 1,
  tuesday: 2,
  wednesday: 3,
  thursday: 4,
  friday: 5,
  saturday: 6,
  sunday: 0
}

function formatDateForAPI(date) {
  const d = new Date(date)
  const year = d.getFullYear()
  const month = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

function formatDateForDisplay(date) {
  const d = new Date(date)
  const year = d.getFullYear()
  const month = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${year}年${month}月${day}日`
}

function hasSlotsOnDate(dateStr) {
  return getSlotsOnDate(dateStr).length > 0
}

function getSlotsOnDate(dateStr) {
  return calendarSlotList.value.filter(s => s.slotDate === dateStr)
}

function getSlotType(isAvailable) {
  if (isAvailable === '0' || isAvailable === 0) {
    return 'success' // 可预约
  } else if (isAvailable === '1' || isAvailable === 1) {
    return 'warning' // 已预约
  }
  return 'danger' // 不可用
}

function getDayCellClass(dateStr) {
  const slots = getSlotsOnDate(dateStr)
  if (slots.length === 0) {
    return ['calendar-day-empty']
  }
  
  const availableSlots = slots.filter(s => s.isAvailable === '0' || s.isAvailable === 0)
  
  if (availableSlots.length > 0) {
    return ['calendar-day-available'] // 有可预约时段，显示绿色
  } else {
    return ['calendar-day-unavailable'] // 无可用时段，显示黄色
  }
}

function getAvailableCount(dateStr) {
  const slots = getSlotsOnDate(dateStr)
  return slots.filter(s => s.isAvailable === '0' || s.isAvailable === 0).length
}

function getOccupancyRate(dateStr) {
  const slots = getSlotsOnDate(dateStr)
  if (slots.length === 0) {
    return 0
  }
  
  const availableSlots = slots.filter(s => s.isAvailable === '0' || s.isAvailable === 0)
  const rate = Math.round((availableSlots.length / slots.length) * 100)
  return rate
}

function generateDatesByWeekday() {
  if (!dateRange.value || dateRange.value.length !== 2 || quickSelectDays.value.length === 0) {
    return
  }

  const [startDate, endDate] = dateRange.value
  const dates = []
  const start = new Date(startDate)
  const end = new Date(endDate)

  const targetWeekdays = quickSelectDays.value.map(day => weekdayMap[day])

  while (start <= end) {
    const weekday = start.getDay()
    if (targetWeekdays.includes(weekday)) {
      const dateStr = formatDate(start)
      if (!dates.includes(dateStr)) {
        dates.push(dateStr)
      }
    }
    start.setDate(start.getDate() + 1)
  }

  allDates.value = dates
  selectedDates.value = dates
}

function handleDateRangeChange() {
  if (dateRange.value && dateRange.value.length === 2) {
    const [startDate, endDate] = dateRange.value
    const dates = []
    const start = new Date(startDate)
    const end = new Date(endDate)

    while (start <= end) {
      dates.push(formatDate(start))
      start.setDate(start.getDate() + 1)
    }

    allDates.value = dates
  } else {
    allDates.value = []
    selectedDates.value = []
  }
}

function formatDate(date) {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

function addCustomTimeSlot() {
  if (customStartTime.value && customEndTime.value) {
    const slot = {
      label: `${customStartTime.value}-${customEndTime.value}`,
      startTime: customStartTime.value,
      endTime: customEndTime.value
    }
    if (!selectedTimeSlots.value.find(s => s.label === slot.label)) {
      selectedTimeSlots.value.push(slot)
      customStartTime.value = ""
      customEndTime.value = ""
    }
  }
}

function removeTimeSlot(index) {
  selectedTimeSlots.value.splice(index, 1)
}

function clearTimeSlots() {
  selectedTimeSlots.value = []
}

function loadDeviceList() {
  listDevice({}).then(response => {
    deviceList.value = response.rows || []
  })
}

function loadSlotList() {
  if (!selectedDeviceId.value) {
    slotList.value = []
    calendarSlotList.value = []
    total.value = 0
    return
  }

  // 加载日历数据（全量）
  listAllSlot({ deviceId: selectedDeviceId.value }).then(response => {
    calendarSlotList.value = response.data || []
  })

  // 加载列表数据（分页）
  loadPageList()
}

function loadPageList() {
  if (!selectedDeviceId.value) {
    slotList.value = []
    total.value = 0
    return
  }

  listSlot({ 
    deviceId: selectedDeviceId.value,
    pageNum: queryParams.pageNum,
    pageSize: queryParams.pageSize
  }).then(response => {
    slotList.value = response.rows || []
    total.value = response.total || 0
  })
}

function handlePageChange(pageNum) {
  queryParams.pageNum = pageNum
  loadPageList()
}

function handleSizeChange(pageSize) {
  queryParams.pageSize = pageSize
  queryParams.pageNum = 1
  loadPageList()
}

function getList() {
  loadPageList()
}

function refreshData() {
  loadSlotList()
}

function handleDeviceChange() {
  loadSlotList()
}

function cancel() {
  open.value = false
  reset()
}

function cancelEdit() {
  editOpen.value = false
  resetEdit()
}

function reset() {
  form.value = {
    deviceId: selectedDeviceId.value,
    isAvailable: '1'
  }
  dateRange.value = []
  allDates.value = []
  selectedDates.value = []
  quickSelectDays.value = []
  customStartTime.value = ""
  customEndTime.value = ""
  selectedTimeSlots.value = []
  proxy.resetForm("slotRef")
}

function resetEdit() {
  editForm.value = {}
  proxy.resetForm("editSlotRef")
}

function handleAdd() {
  if (!selectedDeviceId.value) {
    proxy.$modal.msgWarning("请先选择设备")
    return
  }
  reset()
  open.value = true
  title.value = "批量添加设备可预约时段"
}

function handleUpdate(row) {
  resetEdit()
  const _slotId = row.slotId
  getSlot(_slotId).then(response => {
    editForm.value = {
      ...response.data,
      isAvailable: String(response.data.isAvailable)
    }
    editOpen.value = true
    editTitle.value = "修改设备可预约时段"
  })
}

function submitForm() {
  if (selectedDates.value.length === 0) {
    proxy.$modal.msgError("请至少选择一个日期")
    return
  }
  if (selectedTimeSlots.value.length === 0) {
    proxy.$modal.msgError("请至少选择一个时段")
    return
  }

  const batchData = []
  selectedDates.value.forEach(date => {
    selectedTimeSlots.value.forEach(slot => {
      batchData.push({
        deviceId: form.value.deviceId,
        slotDate: date,
        startTime: slot.startTime,
        endTime: slot.endTime,
        isAvailable: form.value.isAvailable
      })
    })
  })

  submitLoading.value = true
  batchAddSlot(batchData).then(() => {
    proxy.$modal.msgSuccess(`成功添加 ${batchData.length} 个时段配置`)
    open.value = false
    submitLoading.value = false
    loadSlotList()
  }).catch(() => {
    submitLoading.value = false
  })
}

function submitEditForm() {
  proxy.$refs["editSlotRef"].validate(valid => {
    if (valid) {
      updateSlot(editForm.value).then(() => {
        proxy.$modal.msgSuccess("修改成功")
        editOpen.value = false
        loadSlotList()
      })
    }
  })
}

function handleDelete(row) {
  const _slotId = row.slotId
  proxy.$modal.confirm('是否确认删除该时段配置？').then(function() {
    return delSlot(_slotId)
  }).then(() => {
    loadSlotList()
    proxy.$modal.msgSuccess("删除成功")
  }).catch(() => {})
}

function handleTableSelectionChange(selection) {
  selectedSlots.value = selection
}

function handleBatchEdit() {
  if (selectedSlots.value.length === 0) {
    proxy.$modal.msgWarning("请先选择要修改的时段")
    return
  }
  data.batchEditForm.isAvailable = '1'
  batchEditOpen.value = true
}

function cancelBatchEdit() {
  batchEditOpen.value = false
}

async function submitBatchEditForm() {
  submitLoading.value = true
  let successCount = 0
  let failCount = 0

  for (const slot of selectedSlots.value) {
    try {
      await updateSlot({
        slotId: slot.slotId,
        isAvailable: data.batchEditForm.isAvailable
      })
      successCount++
    } catch (error) {
      failCount++
    }
  }

  submitLoading.value = false
  batchEditOpen.value = false
  selectedSlots.value = []
  
  if (failCount === 0) {
    proxy.$modal.msgSuccess(`成功修改 ${successCount} 个时段`)
  } else {
    proxy.$modal.msgWarning(`成功修改 ${successCount} 个时段，失败 ${failCount} 个时段`)
  }
  
  loadSlotList()
}

function handleBatchDelete() {
  if (selectedSlots.value.length === 0) {
    proxy.$modal.msgWarning("请先选择要删除的时段")
    return
  }

  proxy.$modal.confirm(`是否确认删除选中的 ${selectedSlots.value.length} 个时段配置？`).then(async function() {
    submitLoading.value = true
    let successCount = 0
    let failCount = 0

    for (const slot of selectedSlots.value) {
      try {
        await delSlot(slot.slotId)
        successCount++
      } catch (error) {
        failCount++
      }
    }

    submitLoading.value = false
    selectedSlots.value = []
    
    if (failCount === 0) {
      proxy.$modal.msgSuccess(`成功删除 ${successCount} 个时段`)
    } else {
      proxy.$modal.msgWarning(`成功删除 ${successCount} 个时段，失败 ${failCount} 个时段`)
    }
    
    loadSlotList()
  }).catch(() => {})
}

function init() {
  loadDeviceList()
}

init()
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.subtitle {
  font-size: 14px;
  color: #909399;
}

.calendar-day {
  padding: 8px 4px;
  text-align: center;
  cursor: pointer;
  min-height: 80px;
  max-height: 80px;
  box-sizing: border-box;
  border-radius: 4px;
  transition: all 0.3s ease;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  overflow: hidden;
  position: relative;
}

.calendar-day:hover {
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.calendar-day-number {
  display: block;
  font-weight: bold;
  margin-bottom: 4px;
  font-size: 14px;
  line-height: 1.2;
}

.calendar-day-empty {
  background-color: transparent;
}

.calendar-day-available {
  background: linear-gradient(135deg, #f0f9eb 0%, #e1f3d8 100%);
  border: 1px solid #67c23a;
}

.calendar-day-unavailable {
  background: linear-gradient(135deg, #fdf6ec 0%, #f5dab1 100%);
  border: 1px solid #e6a23c;
}

.calendar-day-mixed {
  background: linear-gradient(135deg, #fdf6ec 0%, #f5dab1 100%);
  border: 1px solid #e6a23c;
}

.occupancy-rate {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  width: 100%;
}

.occupancy-value {
  font-size: 16px;
  font-weight: bold;
  line-height: 1.1;
}

.calendar-day-empty .occupancy-value {
  color: #c0c4cc;
  font-size: 14px;
}

.calendar-day-available .occupancy-value {
  color: #67c23a;
}

.calendar-day-unavailable .occupancy-value {
  color: #e6a23c;
}

.calendar-day-mixed .occupancy-value {
  color: #e6a23c;
}

.occupancy-label {
  font-size: 10px;
  color: #909399;
  line-height: 1.1;
  margin-top: 1px;
}

.calendar-day-empty .occupancy-label {
  display: none;
}

@media (max-width: 1200px) {
  .calendar-day {
    padding: 6px 3px;
    min-height: 70px;
    max-height: 70px;
  }
  
  .calendar-day-number {
    font-size: 12px;
    margin-bottom: 3px;
  }
  
  .occupancy-value {
    font-size: 14px;
  }
  
  .occupancy-label {
    font-size: 9px;
  }
}

@media (max-width: 768px) {
  .calendar-day {
    padding: 4px 2px;
    min-height: 60px;
    max-height: 60px;
  }
  
  .calendar-day-number {
    font-size: 11px;
    margin-bottom: 2px;
  }
  
  .occupancy-value {
    font-size: 12px;
  }
  
  .occupancy-label {
    font-size: 8px;
  }
}
</style>
