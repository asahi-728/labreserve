<template>
  <div class="app-container">
    <el-card class="box-card">
      <template #header>
        <div class="card-header">
          <span>发起设备预约</span>
          <div style="display: flex; align-items: center; gap: 10px;">
            <span style="font-size: 14px;">推荐数量：</span>
            <el-input-number v-model="recommendTopK" :min="1" :max="20" size="small" style="width: 100px;" />
            <el-button type="primary" :loading="recommendLoading" @click="loadRecommendations" icon="MagicStick">
              智能推荐设备
            </el-button>
          </div>
        </div>
      </template>

      <el-form :model="queryForm" :inline="true" class="search-form">
        <el-form-item label="设备分类">
          <el-tree-select
            v-model="queryForm.categoryId"
            :data="categoryTreeList"
            :props="{ value: 'id', label: 'label', children: 'children' }"
            value-key="id"
            placeholder="请选择设备分类"
            clearable
            check-strictly
            @change="loadDeviceList"
          />
        </el-form-item>
        <el-form-item label="所属实验室">
          <el-select v-model="queryForm.labId" placeholder="请选择实验室" clearable @change="loadDeviceList">
            <el-option
              v-for="item in laboratoryList"
              :key="item.labId"
              :label="item.labName"
              :value="item.labId"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="设备状态">
          <el-select v-model="queryForm.status" placeholder="请选择设备状态" clearable @change="loadDeviceList">
            <el-option
              v-for="dict in lab_device_status"
              :key="dict.value"
              :label="dict.label"
              :value="dict.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="Search" @click="loadDeviceList">搜索</el-button>
          <el-button icon="Refresh" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 推荐设备展示区域 -->
    <el-card v-if="showRecommendations" class="box-card" style="margin-top: 20px;">
      <template #header>
        <div class="card-header">
          <span>智能推荐设备</span>
          <el-button link type="primary" @click="showRecommendations = false">收起</el-button>
        </div>
      </template>
      <el-alert
        title="基于GAT算法为您智能推荐的设备"
        type="info"
        :closable="false"
        show-icon
        style="margin-bottom: 16px;"
      />
      <el-row :gutter="20">
        <el-col :span="6" v-for="(rec, index) in recommendations" :key="index">
          <el-card 
            shadow="hover" 
            class="recommend-card"
            :class="{ 'recommend-card-selected': selectedDevice?.deviceId === rec.deviceId }"
            @click="selectRecommendedDevice(rec)"
          >
            <div class="recommend-rank">#{{ rec.rank }}</div>
            <div class="recommend-name">{{ getDeviceName(rec.deviceId) }}</div>
            <div class="recommend-similarity">
              匹配度: {{ (rec.similarity * 100).toFixed(1) }}%
            </div>
            <el-progress 
              :percentage="rec.similarity * 100" 
              :color="getProgressColor(rec.similarity)"
              :stroke-width="8"
              style="margin-top: 8px;"
            />
          </el-card>
        </el-col>
      </el-row>
    </el-card>

    <el-row :gutter="20" style="margin-top: 20px;">
      <el-col :span="12">
        <el-card class="box-card">
          <template #header>
            <div class="card-header">
              <span>选择设备</span>
            </div>
          </template>
          
          <el-table 
            v-loading="deviceLoading" 
            :data="filteredDeviceList" 
            style="width: 100%"
            @row-click="handleSelectDevice"
            highlight-current-row
            height="500"
          >
            <el-table-column label="设备名称" align="center" prop="deviceName" show-overflow-tooltip />
            <el-table-column label="设备分类" align="center" prop="categoryName" width="120" />
            <el-table-column label="所属实验室" align="center" prop="labName" width="140" />
            <el-table-column label="设备状态" align="center" prop="status" width="100">
              <template #default="scope">
                <dict-tag :options="lab_device_status" :value="scope.row.status"/>
              </template>
            </el-table-column>
          </el-table>
          
          <pagination
            v-show="deviceTotal > 0"
            :total="deviceTotal"
            v-model:page="deviceQuery.pageNum"
            v-model:limit="deviceQuery.pageSize"
            @pagination="loadDeviceList"
          />
        </el-card>

        <el-card v-if="selectedDevice && selectedSlots.length > 0" class="box-card" style="margin-top: 20px;">
          <template #header>
            <div class="card-header">
              <span>填写预约信息</span>
            </div>
          </template>

          <el-form
            ref="reservationFormRef"
            :model="form"
            :rules="rules"
            label-width="100px"
          >
            <el-row :gutter="20">
              <el-col :xs="24" :sm="12">
                <el-form-item label="预约人" prop="userName">
                  <el-input
                    v-model="form.userName"
                    placeholder="请输入预约人姓名"
                    clearable
                    disabled
                  />
                </el-form-item>
              </el-col>
              <el-col :xs="24" :sm="12">
                <el-form-item label="联系电话" prop="phone">
                  <el-input
                    v-model="form.phone"
                    placeholder="请输入联系电话"
                    clearable
                    autocomplete="on"
                  />
                </el-form-item>
              </el-col>
            </el-row>

            <el-row :gutter="20">
              <el-col :span="24">
                <el-form-item label="已选时段">
                  <el-tag
                    v-for="(slot, index) in selectedSlots"
                    :key="index"
                    closable
                    @close="removeSlot(index)"
                    style="margin-right: 8px; margin-bottom: 8px;"
                  >
                    {{ parseTime(slot.slotDate, '{y}-{m}-{d}') }} {{ slot.startTime }}-{{ slot.endTime }}
                  </el-tag>
                </el-form-item>
              </el-col>
            </el-row>

            <el-row :gutter="20">
              <el-col :span="24">
                <el-form-item label="预约理由" prop="reason">
                  <el-input
                    v-model="form.reason"
                    type="textarea"
                    :rows="4"
                    placeholder="请输入预约理由，说明使用目的等信息"
                  />
                </el-form-item>
              </el-col>
            </el-row>

            <el-row :gutter="20">
              <el-col :span="24">
                <el-form-item label="常用理由">
                  <el-space wrap>
                    <el-button
                      v-for="(template, index) in reasonTemplates"
                      :key="index"
                      size="small"
                      @click="applyReasonTemplate(template)"
                    >
                      {{ template }}
                    </el-button>
                  </el-space>
                </el-form-item>
              </el-col>
            </el-row>

            <el-form-item>
              <div class="button-group">
                <el-button
                  type="primary"
                  :loading="submitLoading"
                  @click="handleSubmit"
                  icon="Check"
                >
                  提交预约
                </el-button>
                <el-button
                  @click="handleReset"
                  icon="Refresh"
                >
                  重置
                </el-button>
                <el-button
                  @click="handleBack"
                  icon="Back"
                >
                  返回列表
                </el-button>
              </div>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>

      <el-col :span="12">
        <el-card v-if="selectedDevice" class="box-card">
          <template #header>
            <div class="card-header">
              <span>设备信息</span>
            </div>
          </template>
          
          <el-descriptions :column="1" border>
            <el-descriptions-item label="设备ID">{{ selectedDevice.deviceId }}</el-descriptions-item>
            <el-descriptions-item label="设备名称">{{ selectedDevice.deviceName }}</el-descriptions-item>
            <el-descriptions-item label="设备分类">{{ selectedDevice.categoryName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="所属实验室">{{ selectedDevice.labName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="设备位置">{{ selectedDevice.location || '-' }}</el-descriptions-item>
            <el-descriptions-item label="设备状态">
              <dict-tag :options="lab_device_status" :value="selectedDevice.status"/>
            </el-descriptions-item>
            <el-descriptions-item label="设备规格">{{ selectedDevice.specs || '-' }}</el-descriptions-item>
          </el-descriptions>
        </el-card>

        <el-card v-if="selectedDevice" class="box-card" style="margin-top: 20px;">
          <template #header>
            <div class="card-header">
              <span>选择预约时段</span>
            </div>
          </template>
          
          <el-calendar v-model="currentDate" style="margin-bottom: 20px;">
            <template #date-cell="{ data }">
              <div 
                class="calendar-day" 
                :class="getDayCellClass(data.day)"
                @click.stop="selectDate(data.day)"
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

          <el-card shadow="hover">
            <template #header>
              <div class="card-header">
                <span>{{ formatDateForDisplay(currentDate) }} 可预约时段</span>
              </div>
            </template>
            
            <el-table :data="availableSlots" style="width: 100%" @selection-change="handleSlotSelectionChange" height="200">
              <el-table-column type="selection" width="55" />
              <el-table-column label="时段" width="180">
                <template #default="scope">
                  <el-tag>{{ scope.row.startTime }} - {{ scope.row.endTime }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column label="状态" width="100">
                <template #default="scope">
                  <el-tag :type="getSlotType(scope.row.isAvailable)">
                    {{ scope.row.isAvailable === '0' || scope.row.isAvailable === 0 ? '可预约' : scope.row.isAvailable === '1' || scope.row.isAvailable === 1 ? '已预约' : '不可用' }}
                  </el-tag>
                </template>
              </el-table-column>
            </el-table>
            
            <el-empty v-if="availableSlots.length === 0" description="当天无可预约时段"></el-empty>
          </el-card>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup name="CreateReservation">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { listDevice, getDevice } from '@/api/lab/device'
import { listCategory, treeSelectCategory } from '@/api/lab/category'
import { listLaboratory } from '@/api/lab/laboratory'
import { listAllSlot } from '@/api/lab/slot'
import { submitReservation, checkConflict } from '@/api/lab/reservation'
import { gatRecommend } from '@/api/lab/algorithm'
import useUserStore from '@/store/modules/user'

const router = useRouter()
const { proxy } = getCurrentInstance()
const userStore = useUserStore()

const { lab_device_status } = useDict('lab_device_status')

const reservationFormRef = ref()
const submitLoading = ref(false)
const deviceLoading = ref(false)
const recommendLoading = ref(false)
const showRecommendations = ref(false)
const recommendations = ref([])
const recommendedDeviceMap = ref({}) // 存储推荐设备的详细信息
const recommendTopK = ref(5) // 推荐数量

const deviceList = ref([])
const categoryList = ref([])
const categoryTreeList = ref([])
const laboratoryList = ref([])
const calendarSlotList = ref([])
const selectedDevice = ref(null)
const selectedSlots = ref([])
const currentDate = ref(new Date())

const deviceTotal = ref(0)
const deviceQuery = reactive({
  pageNum: 1,
  pageSize: 10
})

const queryForm = reactive({
  categoryId: null,
  labId: null,
  status: null
})

const form = reactive({
  userName: '',
  phone: '',
  reason: ''
})

const validatePhone = (rule, value, callback) => {
  const phoneReg = /^1[3-9]\d{9}$/
  if (!value) {
    callback(new Error('请输入联系电话'))
  } else if (!phoneReg.test(value)) {
    callback(new Error('请输入正确的手机号码'))
  } else {
    callback()
  }
}

const rules = {
  userName: [
    { required: true, message: '预约人不能为空', trigger: 'blur' },
    { min: 2, max: 20, message: '姓名长度在 2 到 20 个字符', trigger: 'blur' }
  ],
  phone: [
    { required: true, validator: validatePhone, trigger: 'blur' }
  ],
  reason: [
    { required: true, message: '请输入预约理由', trigger: 'blur' }
  ]
}

const reasonTemplates = [
  '课程实验使用',
  '项目研发使用',
  '毕业设计使用',
  '学科竞赛使用',
  '教学演示使用',
  '科研测试使用'
]

const filteredDeviceList = computed(() => {
  return deviceList.value.map(device => {
    const category = categoryList.value.find(c => c.categoryId === device.categoryId)
    const lab = laboratoryList.value.find(l => l.labId === device.labId)
    return {
      ...device,
      categoryName: category?.categoryName || '-',
      labName: lab?.labName || '-',
      location: lab?.location || '-'
    }
  })
})

const availableSlots = computed(() => {
  const dateStr = formatDateForAPI(currentDate.value)
  return getSlotsOnDate(dateStr).filter(s => s.isAvailable === '0' || s.isAvailable === 0)
})

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
  deviceLoading.value = true
  try {
    const res = await listDevice({
      ...deviceQuery,
      ...queryForm
    })
    deviceList.value = res.rows || []
    deviceTotal.value = res.total || 0
  } catch (error) {
    console.error('加载设备列表失败:', error)
  } finally {
    deviceLoading.value = false
  }
}

function handleSelectDevice(row) {
  selectedDevice.value = row
  selectedSlots.value = []
  loadSlotList(row.deviceId)
}

async function loadSlotList(deviceId) {
  try {
    const res = await listAllSlot({ deviceId: deviceId })
    calendarSlotList.value = res.data || []
  } catch (error) {
    console.error('加载时段列表失败:', error)
  }
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

function selectDate(dateStr) {
  currentDate.value = new Date(dateStr)
}

function handleSlotSelectionChange(selection) {
  selection.forEach(slot => {
    const exists = selectedSlots.value.find(s => 
      s.slotId === slot.slotId
    )
    if (!exists) {
      selectedSlots.value.push(slot)
    }
  })
}

function removeSlot(index) {
  selectedSlots.value.splice(index, 1)
}

function applyReasonTemplate(template) {
  form.reason = template
}

function disabledDate(time) {
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  return time.getTime() < today.getTime()
}

async function handleSubmit() {
  if (!reservationFormRef.value) return
  
  await reservationFormRef.value.validate(async (valid) => {
    if (valid) {
      if (selectedSlots.value.length === 0) {
        proxy.$modal.msgWarning('请至少选择一个时段')
        return
      }

      try {
        proxy.$modal.confirm(
          '确认提交预约申请吗？提交后将进入审核流程。',
          '提示',
          {
            confirmButtonText: '确定',
            cancelButtonText: '取消',
            type: 'info'
          }
        ).then(async () => {
          submitLoading.value = true
          
          let hasConflict = false
          for (const slot of selectedSlots.value) {
            const conflictData = {
              deviceId: selectedDevice.value.deviceId,
              startTime: `${slot.slotDate} ${slot.startTime}`,
              endTime: `${slot.slotDate} ${slot.endTime}`
            }
            
            const conflictRes = await checkConflict(conflictData)
            if (conflictRes.hasConflict) {
              hasConflict = true
              proxy.$modal.msgWarning(`时段 ${parseTime(slot.slotDate, '{y}-{m}-{d}')} ${slot.startTime}-${slot.endTime} 已被预约，请重新选择`)
              break
            }
          }
          
          if (hasConflict) {
            submitLoading.value = false
            return
          }
          
          for (const slot of selectedSlots.value) {
            // 读取当前管理员设置的审批模式（人工/PPO自动）
            const currentApprovalMode = localStorage.getItem('approvalMode') || 'manual'
            const submitData = {
              userId: userStore.id,
              deviceId: selectedDevice.value.deviceId,
              startTime: `${slot.slotDate} ${slot.startTime}`,
              endTime: `${slot.slotDate} ${slot.endTime}`,
              remark: form.reason,
              approvalMode: currentApprovalMode
            }
            
            await submitReservation(submitData)
          }
          
          proxy.$modal.msgSuccess('预约申请提交成功，请等待审核')
          
          // setTimeout(() => {
          //   router.push('/lab/reservation')
          // }, 1500)
        }).catch(() => {
        }).finally(() => {
          submitLoading.value = false
        })
      } catch (error) {
        submitLoading.value = false
        proxy.$modal.msgError(error.message || '提交预约失败，请稍后重试')
      }
    }
  })
}

function handleReset() {
  if (!reservationFormRef.value) return
  reservationFormRef.value.resetFields()
  selectedDevice.value = null
  selectedSlots.value = []
  form.userName = userStore.nickName || userStore.name
  proxy.$modal.msgInfo('表单已重置')
}

function handleBack() {
  router.push('/lab/reservation/my')
}

function resetQuery() {
  queryForm.categoryId = null
  queryForm.labId = null
  queryForm.status = null
  loadDeviceList()
}

// GAT 推荐相关函数
async function loadRecommendations() {
  recommendLoading.value = true
  try {
    const res = await gatRecommend({
      user_id: userStore.id,
      top_k: recommendTopK.value
    })
    if (res.code === 200 && res.data?.data) {
      // 转换字段名：下划线 -> 驼峰
      recommendations.value = res.data.data.map(rec => ({
        userId: rec.user_id,
        deviceId: rec.device_id,
        similarity: rec.similarity,
        rank: rec.rank,
        recommendTime: rec.recommend_time
      }))
      showRecommendations.value = true
      
      // 加载推荐设备的详细信息
      recommendedDeviceMap.value = {}
      for (const rec of recommendations.value) {
        try {
          console.log('正在加载设备信息, deviceId:', rec.deviceId)
          const deviceRes = await getDevice(rec.deviceId)
          if (deviceRes.code === 200 && deviceRes.data) {
            recommendedDeviceMap.value[rec.deviceId] = deviceRes.data
          }
        } catch (error) {
          console.error(`加载设备 ${rec.deviceId} 信息失败:`, error)
        }
      }
      
      proxy.$modal.msgSuccess('智能推荐已加载')
    }
  } catch (error) {
    console.error('加载推荐失败:', error)
    proxy.$modal.msgError('加载智能推荐失败，请稍后重试')
  } finally {
    recommendLoading.value = false
  }
}

function selectRecommendedDevice(rec) {
  let device = recommendedDeviceMap.value[rec.deviceId]
  
  // 如果在推荐设备 map 中找不到，再从 deviceList 中查找
  if (!device) {
    device = deviceList.value.find(d => d.deviceId === rec.deviceId)
  }
  
  if (device) {
    // 确保该设备在 deviceList 中，如果不在，先添加进去
    const existsInList = deviceList.value.some(d => d.deviceId === device.deviceId)
    if (!existsInList) {
      deviceList.value.push(device)
    }
    handleSelectDevice(device)
  } else {
    proxy.$modal.msgWarning('该设备信息加载失败，请刷新页面重试')
  }
}

function getDeviceName(deviceId) {
  let device = recommendedDeviceMap.value[deviceId]
  
  // 如果在推荐设备 map 中找不到，再从 deviceList 中查找
  if (!device) {
    device = deviceList.value.find(d => d.deviceId === deviceId)
  }
  
  return device?.deviceName || `设备 #${deviceId}`
}

function getProgressColor(similarity) {
  if (similarity >= 0.8) return '#67c23a'
  if (similarity >= 0.6) return '#e6a23c'
  return '#909399'
}

onMounted(async () => {
  await Promise.all([
    loadCategoryList(),
    loadLaboratoryList()
  ])
  form.userName = userStore.nickName || userStore.name
  loadDeviceList()
})
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.search-form {
  margin-bottom: 0;
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

.button-group {
  display: flex;
  gap: 12px;
  padding-top: 10px;
}

:deep(.el-descriptions__label) {
  width: 100px;
  font-weight: 500;
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

.recommend-card {
  cursor: pointer;
  transition: all 0.3s ease;
  margin-bottom: 20px;
}

.recommend-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 16px rgba(0, 0, 0, 0.1);
}

.recommend-card-selected {
  border: 2px solid #409eff;
  background: linear-gradient(135deg, #ecf5ff 0%, #d9ecff 100%);
}

.recommend-rank {
  font-size: 24px;
  font-weight: bold;
  color: #409eff;
  margin-bottom: 8px;
}

.recommend-name {
  font-size: 16px;
  font-weight: 500;
  color: #303133;
  margin-bottom: 8px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.recommend-similarity {
  font-size: 14px;
  color: #606266;
}
</style>
