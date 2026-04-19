<template>
  <div class="app-container">
    <!-- 欢迎卡片 -->
    <el-row :gutter="20">
      <el-col :span="24">
        <el-card class="welcome-card" shadow="hover">
          <div class="welcome-content">
            <div class="welcome-left">
              <h1 class="welcome-title">欢迎回来，{{ userStore.nickName || userStore.name }}！</h1>
              <p class="welcome-desc">这是您的实验室设备预约系统工作台，快捷高效地管理您的实验室资源</p>
              <div class="welcome-meta">
                <span class="meta-item">
                  <el-icon><Calendar /></el-icon>
                  {{ currentDate }}
                </span>
                <span class="meta-item">
                  <el-icon><Clock /></el-icon>
                  {{ currentTime }}
                </span>
              </div>
            </div>
            <div class="welcome-right">
              <el-avatar :size="100" :src="userStore.avatar" />
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 统计卡片 -->
    <el-row :gutter="20" class="mt20">
      <!-- 全部预约 -->
      <el-col :xs="24" :sm="12" :md="12" v-if="auth.hasPermi('lab:reservation:list')">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-content">
            <div class="stat-info">
              <div class="stat-value">{{ stats.totalReservations }}</div>
              <div class="stat-label">全部预约</div>
            </div>
            <div class="stat-icon">
              <el-icon :size="40"><List /></el-icon>
            </div>
          </div>
        </el-card>
      </el-col>

      <!-- 已通过预约 -->
      <el-col :xs="24" :sm="12" :md="12" v-if="auth.hasPermi('lab:reservation:list')">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-content">
            <div class="stat-info">
              <div class="stat-value">{{ stats.approvedReservations }}</div>
              <div class="stat-label">已通过</div>
            </div>
            <div class="stat-icon">
              <el-icon :size="40"><CircleCheck /></el-icon>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 快捷入口 -->
    <el-row :gutter="20" class="mt20" v-if="quickLinks.length > 0">
      <el-col :span="24">
        <el-card class="section-card">
          <template #header>
            <div class="card-header">
              <span class="card-title"><el-icon><Lightning /></el-icon> 快捷入口</span>
            </div>
          </template>
          <el-row :gutter="20">
            <el-col :xs="12" :sm="8" :md="6" :lg="4" v-for="(item, index) in quickLinks" :key="index">
              <div class="quick-link-item" @click="handleQuickLink(item.path)">
                <div class="quick-icon" :style="{ background: item.bgColor }">
                  <el-icon :size="28">
                    <component :is="item.icon" />
                  </el-icon>
                </div>
                <div class="quick-info">
                  <span class="quick-title">{{ item.title }}</span>
                  <span class="quick-desc">{{ item.desc }}</span>
                </div>
              </div>
            </el-col>
          </el-row>
        </el-card>
      </el-col>
    </el-row>

    <!-- 功能区域 -->
    <el-row :gutter="20" class="mt20">
      <!-- 预约指南 -->
      <el-col :xs="24" :lg="12">
        <el-card class="guide-card">
          <template #header>
            <div class="card-header">
              <span class="card-title"><el-icon><Reading /></el-icon> 预约指南</span>
            </div>
          </template>
          <el-steps :active="5" direction="vertical" finish-status="success" align-center>
            <el-step title="查看设备" description="浏览实验室设备列表，了解设备详情">
              <template #icon><el-icon><Search /></el-icon></template>
            </el-step>
            <el-step title="选择时段" description="查看设备可用时段，选择合适的时间">
              <template #icon><el-icon><Calendar /></el-icon></template>
            </el-step>
            <el-step title="提交申请" description="填写预约信息，提交预约申请">
              <template #icon><el-icon><Document /></el-icon></template>
            </el-step>
            <el-step title="等待审核" description="等待管理员或系统审核您的预约">
              <template #icon><el-icon><Clock /></el-icon></template>
            </el-step>
            <el-step title="使用设备" description="审核通过后按时使用实验室设备">
              <template #icon><el-icon><CircleCheck /></el-icon></template>
            </el-step>
          </el-steps>
        </el-card>
      </el-col>

      <!-- 注意事项 -->
      <el-col :xs="24" :lg="12">
        <el-card class="notice-card">
          <template #header>
            <div class="card-header">
              <span class="card-title"><el-icon><Warning /></el-icon> 注意事项</span>
            </div>
          </template>
          <el-alert title="实验室设备使用须知" type="info" :closable="false">
            <ul class="notice-list">
              <li><el-icon><Right /></el-icon> 请提前规划好实验时间，按时到达实验室使用设备</li>
              <li><el-icon><Right /></el-icon> 使用设备前请仔细阅读设备使用说明书</li>
              <li><el-icon><Right /></el-icon> 如遇设备故障，请及时联系实验室管理员</li>
              <li><el-icon><Right /></el-icon> 预约成功后如需取消，请提前24小时操作</li>
              <li><el-icon><Right /></el-icon> 保持实验室整洁，设备使用完毕请归位</li>
            </ul>
          </el-alert>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup name="LabPortal">
import { ref, onMounted, onUnmounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { Search, Calendar, Document, Clock, CircleCheck, List, Box, Location, Menu, Lightning, Warning, Reading, Right } from '@element-plus/icons-vue'
import { listReservation } from '@/api/lab/reservation'
import useUserStore from '@/store/modules/user'
import auth from '@/plugins/auth'

const router = useRouter()
const userStore = useUserStore()

// 当前时间
const currentDate = ref('')
const currentTime = ref('')
let timer = null

// 统计数据
const stats = ref({
  totalReservations: 0,
  approvedReservations: 0
})



// 快捷入口 - 使用纯色而非渐变色
const allQuickLinks = [
  {
    title: '设备列表',
    desc: '查看所有设备',
    icon: Box,
    bgColor: '#667eea',
    path: '/lab/device',
    permission: 'lab:device:list'
  },
  {
    title: '发起预约',
    desc: '快速预约设备',
    icon: Calendar,
    bgColor: '#f093fb',
    path: '/lab/reservation/create',
    permission: 'lab:reservation:add'
  },
  {
    title: '我的预约',
    desc: '查看预约记录',
    icon: List,
    bgColor: '#4facfe',
    path: '/lab/reservation',
    permission: 'lab:reservation:list'
  },
  {
    title: '实验室管理',
    desc: '管理实验室信息',
    icon: Location,
    bgColor: '#43e97b',
    path: '/lab/laboratory',
    permission: 'lab:laboratory:list'
  },
  {
    title: '算法日志',
    desc: '查看算法运行',
    icon: Menu,
    bgColor: '#a18cd1',
    path: '/lab/algorithm/log',
    permission: 'lab:algorithm:list'
  }
]

// 根据权限过滤快捷入口
const quickLinks = computed(() => {
  return allQuickLinks.filter(item => auth.hasPermi(item.permission))
})

// 处理快捷链接跳转
function handleQuickLink(path) {
  if (path) {
    router.push(path)
  }
}

// 更新时间
function updateTime() {
  const now = new Date()
  currentDate.value = now.toLocaleDateString('zh-CN', { 
    year: 'numeric', 
    month: 'long', 
    day: 'numeric',
    weekday: 'long'
  })
  currentTime.value = now.toLocaleTimeString('zh-CN', {
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  })
}

// 加载统计数据
async function loadStats() {
  try {
    // 加载预约统计
    const reservationRes = await listReservation({ 
      userId: userStore.id, 
      pageNum: 1, 
      pageSize: 100 
    })
    if (reservationRes.rows) {
      stats.value.totalReservations = reservationRes.total
      stats.value.approvedReservations = reservationRes.rows.filter(r => r.status === 'approved').length
    }
  } catch (error) {
    console.error('加载统计数据失败:', error)
  }
}

// 初始化
onMounted(() => {
  updateTime()
  timer = setInterval(updateTime, 1000)
  loadStats()
})

// 清理定时器
onUnmounted(() => {
  if (timer) {
    clearInterval(timer)
    timer = null
  }
})
</script>

<style scoped lang="scss">
.app-container {
  padding: 20px;
}

// 欢迎卡片
.welcome-card {
  border-radius: 12px;
  
  .welcome-content {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 20px 0;
  }
  
  .welcome-left {
    flex: 1;
  }
  
  .welcome-title {
    font-size: 28px;
    color: #303133;
    margin-bottom: 12px;
    font-weight: 600;
  }
  
  .welcome-desc {
    font-size: 15px;
    color: #909399;
    margin-bottom: 16px;
  }
  
  .welcome-meta {
    display: flex;
    gap: 24px;
    
    .meta-item {
      display: flex;
      align-items: center;
      gap: 6px;
      color: #606266;
      font-size: 14px;
    }
  }
  
  .welcome-right {
    display: flex;
    justify-content: center;
    align-items: center;
  }
}

// 统计卡片 - 使用纯色而非渐变色
.stat-card {
  border-radius: 12px;
  background: #f5f7fa;
  
  .stat-content {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 10px 0;
  }
  
  .stat-info {
    .stat-value {
      font-size: 32px;
      font-weight: 700;
      color: #303133;
      margin-bottom: 6px;
    }
    
    .stat-label {
      font-size: 14px;
      color: #909399;
    }
  }
  
  .stat-icon {
    color: #667eea;
    display: flex;
    justify-content: center;
    align-items: center;
  }
}

// 通用卡片
.section-card,
.guide-card,
.notice-card {
  border-radius: 12px;
  margin-bottom: 0;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: 600;
  font-size: 16px;
  
  .card-title {
    display: flex;
    align-items: center;
    gap: 8px;
    color: #303133;
  }
}

// 快捷入口
.quick-link-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.3s;
  background: #f5f7fa;
  
  &:hover {
    background: #e6f7ff;
    transform: translateY(-2px);
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  }
  
  .quick-icon {
    width: 56px;
    height: 56px;
    border-radius: 12px;
    display: flex;
    justify-content: center;
    align-items: center;
    color: #fff;
  }
  
  .quick-info {
    flex: 1;
    
    .quick-title {
      display: block;
      font-size: 15px;
      font-weight: 500;
      color: #303133;
      margin-bottom: 4px;
    }
    
    .quick-desc {
      display: block;
      font-size: 13px;
      color: #909399;
    }
  }
}

// 注意事项
.notice-list {
  margin: 0;
  padding-left: 0;
  list-style: none;
  
  li {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 8px 0;
    font-size: 14px;
    color: #606266;
    
    .el-icon {
      color: #409eff;
    }
  }
}

.mt20 {
  margin-top: 20px;
}
</style>
