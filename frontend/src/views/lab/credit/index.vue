<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="68px">
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
      <el-form-item label="信用分数" prop="creditScore">
        <el-input
          v-model="queryParams.creditScore"
          placeholder="请输入信用分数"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="违约次数" prop="violation">
        <el-input
          v-model="queryParams.violation"
          placeholder="请输入违约次数"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="守约次数" prop="onTime">
        <el-input
          v-model="queryParams.onTime"
          placeholder="请输入守约次数"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="取消次数" prop="cancel">
        <el-input
          v-model="queryParams.cancel"
          placeholder="请输入取消次数"
          clearable
          @keyup.enter="handleQuery"
        />
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
          v-hasPermi="['lab:credit:add']"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          icon="Edit"
          :disabled="single"
          @click="handleUpdate"
          v-hasPermi="['lab:credit:edit']"
        >修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="Delete"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['lab:credit:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="Download"
          @click="handleExport"
          v-hasPermi="['lab:credit:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="creditList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="ID" align="center" prop="id" width="80" />
      <el-table-column label="用户" align="center" prop="userId" width="150">
        <template #default="scope">
          <span>{{ getUserName(scope.row.userId) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="信用分数" align="center" prop="creditScore" width="120">
        <template #default="scope">
          <el-tag :type="getCreditScoreType(scope.row.creditScore)" size="large">
            {{ scope.row.creditScore }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="信用等级" align="center" prop="creditLevel" width="120">
        <template #default="scope">
          <span>{{ getCreditLevel(scope.row.creditScore) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="守约次数" align="center" prop="onTime" width="100">
        <template #default="scope">
          <el-tag type="success">{{ scope.row.onTime }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="违约次数" align="center" prop="violation" width="100">
        <template #default="scope">
          <el-tag :type="scope.row.violation > 0 ? 'danger' : 'info'">{{ scope.row.violation }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="取消次数" align="center" prop="cancel" width="100">
        <template #default="scope">
          <el-tag :type="scope.row.cancel > 3 ? 'warning' : 'info'">{{ scope.row.cancel }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="创建时间" align="center" prop="createTime" width="170">
        <template #default="scope">
          <span>{{ parseTime(scope.row.createTime) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="创建者" align="center" prop="createBy" width="120" />
      <el-table-column label="更新时间" align="center" prop="updateTime" width="170">
        <template #default="scope">
          <span>{{ scope.row.updateTime ? parseTime(scope.row.updateTime) : '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="更新者" align="center" prop="updateBy" width="120" />
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="180">
        <template #default="scope">
          <el-button link type="primary" icon="View" @click="handleView(scope.row)">查看</el-button>
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['lab:credit:edit']">修改</el-button>
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['lab:credit:remove']">删除</el-button>
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

    <!-- 添加或修改用户信用评分对话框 -->
    <el-dialog :title="title" v-model="open" width="500px" append-to-body>
      <el-form ref="creditRef" :model="form" :rules="rules" label-width="100px">
        <el-row>
          <el-col :span="24">
            <el-form-item label="用户" prop="userId">
              <el-select v-model="form.userId" placeholder="请选择用户" clearable filterable style="width: 100%">
                <el-option
                  v-for="user in userList"
                  :key="user.userId"
                  :label="user.nickName + ' (' + user.userId + ')'"
                  :value="user.userId"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="信用分数" prop="creditScore">
              <el-input v-model="form.creditScore" placeholder="请输入信用分数" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="违约次数" prop="violation">
              <el-input v-model="form.violation" placeholder="请输入违约次数" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="守约次数" prop="onTime">
              <el-input v-model="form.onTime" placeholder="请输入守约次数" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="取消次数" prop="cancel">
              <el-input v-model="form.cancel" placeholder="请输入取消次数" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitForm">确 定</el-button>
          <el-button @click="cancel">取 消</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 查看信用详情对话框 -->
    <el-dialog title="用户信用详情" v-model="viewOpen" width="600px" append-to-body>
      <el-descriptions :column="2" border v-if="viewCredit">
        <el-descriptions-item label="ID">{{ viewCredit.id }}</el-descriptions-item>
        <el-descriptions-item label="用户">{{ getUserName(viewCredit.userId) }}</el-descriptions-item>
        <el-descriptions-item label="信用分数" :span="2">
          <el-tag :type="getCreditScoreType(viewCredit.creditScore)" size="large">
            {{ viewCredit.creditScore }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="信用等级" :span="2">
          {{ getCreditLevel(viewCredit.creditScore) }}
        </el-descriptions-item>
        <el-descriptions-item label="守约次数">
          <el-tag type="success">{{ viewCredit.onTime }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="违约次数">
          <el-tag :type="viewCredit.violation > 0 ? 'danger' : 'info'">{{ viewCredit.violation }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="取消次数">
          <el-tag :type="viewCredit.cancel > 3 ? 'warning' : 'info'">{{ viewCredit.cancel }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="更新时间" :span="2">
          {{ viewCredit.updateTime ? parseTime(viewCredit.updateTime, '{y}-{m}-{d} {h}:{i}:{s}') : '-' }}
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

<script setup name="Credit">
import { listCredit, getCredit, delCredit, addCredit, updateCredit } from "@/api/lab/credit"
import { listUser } from "@/api/system/user"

const { proxy } = getCurrentInstance()

const creditList = ref([])
const open = ref(false)
const viewOpen = ref(false)
const viewCredit = ref(null)
const loading = ref(true)
const showSearch = ref(true)
const ids = ref([])
const single = ref(true)
const multiple = ref(true)
const total = ref(0)
const title = ref("")
const userList = ref([])

const data = reactive({
  form: {},
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    userId: null,
    creditScore: null,
    violation: null,
    onTime: null,
    cancel: null,
  },
  rules: {
    userId: [
      { required: true, message: "用户不能为空", trigger: "change" }
    ],
  }
})

const { queryParams, form, rules } = toRefs(data)

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
  if (score >= 120) return 'success'
  if (score >= 100) return ''
  if (score >= 80) return 'warning'
  return 'danger'
}

/** 获取信用等级 */
function getCreditLevel(score) {
  if (score >= 130) return '优秀 (S)'
  if (score >= 120) return '良好 (A)'
  if (score >= 100) return '合格 (B)'
  if (score >= 80) return '警告 (C)'
  if (score >= 60) return '危险 (D)'
  return '极差 (F)'
}

/** 查看信用详情 */
function handleView(row) {
  viewCredit.value = row
  viewOpen.value = true
}

/** 查询用户信用评分列表 */
function getList() {
  loading.value = true
  listCredit(queryParams.value).then(response => {
    creditList.value = response.rows
    total.value = response.total
    loading.value = false
  })
}

// 取消按钮
function cancel() {
  open.value = false
  reset()
}

// 表单重置
function reset() {
  form.value = {
    id: null,
    userId: null,
    creditScore: null,
    violation: null,
    onTime: null,
    cancel: null,
    updateTime: null
  }
  proxy.resetForm("creditRef")
}

/** 搜索按钮操作 */
function handleQuery() {
  queryParams.value.pageNum = 1
  getList()
}

/** 重置按钮操作 */
function resetQuery() {
  proxy.resetForm("queryRef")
  handleQuery()
}

// 多选框选中数据
function handleSelectionChange(selection) {
  ids.value = selection.map(item => item.id)
  single.value = selection.length != 1
  multiple.value = !selection.length
}

/** 新增按钮操作 */
function handleAdd() {
  reset()
  open.value = true
  title.value = "添加用户信用评分"
}

/** 修改按钮操作 */
function handleUpdate(row) {
  reset()
  const _id = row.id || ids.value
  getCredit(_id).then(response => {
    form.value = response.data
    open.value = true
    title.value = "修改用户信用评分"
  })
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["creditRef"].validate(valid => {
    if (valid) {
      if (form.value.id != null) {
        updateCredit(form.value).then(() => {
          proxy.$modal.msgSuccess("修改成功")
          open.value = false
          getList()
        })
      } else {
        addCredit(form.value).then(() => {
          proxy.$modal.msgSuccess("新增成功")
          open.value = false
          getList()
        })
      }
    }
  })
}

/** 删除按钮操作 */
function handleDelete(row) {
  const _ids = row.id || ids.value
  proxy.$modal.confirm('是否确认删除用户信用评分编号为"' + _ids + '"的数据项？').then(function() {
    return delCredit(_ids)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess("删除成功")
  }).catch(() => {})
}

/** 导出按钮操作 */
function handleExport() {
  proxy.download('lab/credit/export', {
    ...queryParams.value
  }, `credit_${new Date().getTime()}.xlsx`)
}

onMounted(() => {
  loadUserList()
  getList()
})
</script>
