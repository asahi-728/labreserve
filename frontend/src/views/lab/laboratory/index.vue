<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="实验室名称" prop="labName">
        <el-input
          v-model="queryParams.labName"
          placeholder="请输入实验室名称"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="实验室位置" prop="location">
        <el-input
          v-model="queryParams.location"
          placeholder="请输入实验室位置"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="负责人" prop="leader">
        <el-input
          v-model="queryParams.leader"
          placeholder="请输入负责人"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="负责人电话" prop="leaderPhone">
        <el-input
          v-model="queryParams.leaderPhone"
          placeholder="请输入负责人电话"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="开放时间" prop="openTime">
        <el-date-picker clearable
          v-model="queryParams.openTime"
          type="date"
          value-format="YYYY-MM-DD"
          placeholder="请选择开放时间">
        </el-date-picker>
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="请选择状态" clearable>
          <el-option
            v-for="dict in lab_laboratory_status"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="关闭时间" prop="closeTime">
        <el-date-picker clearable
          v-model="queryParams.closeTime"
          type="date"
          value-format="YYYY-MM-DD"
          placeholder="请选择关闭时间">
        </el-date-picker>
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
          v-hasPermi="['lab:laboratory:add']"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          icon="Edit"
          :disabled="single"
          @click="handleUpdate"
          v-hasPermi="['lab:laboratory:edit']"
        >修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="Delete"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['lab:laboratory:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="Download"
          @click="handleExport"
          v-hasPermi="['lab:laboratory:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="laboratoryList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="实验室ID" align="center" prop="labId" />
      <el-table-column label="实验室名称" align="center" prop="labName" />
      <el-table-column label="实验室位置" align="center" prop="location" />
      <el-table-column label="负责人" align="center" prop="leader" />
      <el-table-column label="负责人电话" align="center" prop="leaderPhone" />
      <el-table-column label="开放时间" align="center" prop="openTime" width="120">
        <template #default="scope">
          <span>{{ scope.row.openTime || '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" align="center" prop="status">
        <template #default="scope">
          <dict-tag :options="lab_laboratory_status" :value="scope.row.status"/>
        </template>
      </el-table-column>
      <el-table-column label="备注" align="center" prop="remark" />
      <el-table-column label="关闭时间" align="center" prop="closeTime" width="120">
        <template #default="scope">
          <span>{{ scope.row.closeTime || '-' }}</span>
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
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['lab:laboratory:edit']">修改</el-button>
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['lab:laboratory:remove']">删除</el-button>
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

    <!-- 添加或修改实验室信息对话框 -->
    <el-dialog :title="title" v-model="open" width="500px" append-to-body>
      <el-form ref="laboratoryRef" :model="form" :rules="rules" label-width="100px">
        <el-row>
          <el-col :span="24">
            <el-form-item label="实验室名称" prop="labName">
              <el-input v-model="form.labName" placeholder="请输入实验室名称" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="实验室位置" prop="location">
              <el-input v-model="form.location" placeholder="请输入实验室位置" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="负责人" prop="leader">
              <el-input v-model="form.leader" placeholder="请输入负责人" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="负责人电话" prop="leaderPhone">
              <el-input v-model="form.leaderPhone" placeholder="请输入负责人电话" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="开放/关闭时间" prop="timeRange">
              <el-time-picker
                v-model="form.timeRange"
                is-range
                range-separator="至"
                start-placeholder="开放时间"
                end-placeholder="关闭时间"
                value-format="HH:mm:ss"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="状态" prop="status">
              <el-radio-group v-model="form.status">
                <el-radio
                  v-for="dict in lab_laboratory_status"
                  :key="dict.value"
                  :label="dict.value"
                >{{dict.label}}</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="备注" prop="remark">
              <el-input v-model="form.remark" type="textarea" placeholder="请输入内容" />
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
  </div>
</template>

<script setup name="Laboratory">
import { listLaboratory, getLaboratory, delLaboratory, addLaboratory, updateLaboratory } from "@/api/lab/laboratory"

const { proxy } = getCurrentInstance()
const { lab_laboratory_status } = useDict('lab_laboratory_status')

const laboratoryList = ref([])
const open = ref(false)
const loading = ref(true)
const showSearch = ref(true)
const ids = ref([])
const single = ref(true)
const multiple = ref(true)
const total = ref(0)
const title = ref("")

const data = reactive({
  form: {},
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    labName: null,
    location: null,
    leader: null,
    leaderPhone: null,
    openTime: null,
    status: null,
    closeTime: null
  },
  rules: {
    labName: [
      { required: true, message: "实验室名称不能为空", trigger: "blur" }
    ],
  }
})

const { queryParams, form, rules } = toRefs(data)

/** 查询实验室信息列表 */
function getList() {
  loading.value = true
  listLaboratory(queryParams.value).then(response => {
    laboratoryList.value = response.rows
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
    labId: null,
    labName: null,
    location: null,
    leader: null,
    leaderPhone: null,
    openTime: null,
    closeTime: null,
    timeRange: null,
    status: null,
    createBy: null,
    createTime: null,
    updateBy: null,
    updateTime: null,
    remark: null
  }
  proxy.resetForm("laboratoryRef")
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
  ids.value = selection.map(item => item.labId)
  single.value = selection.length != 1
  multiple.value = !selection.length
}

/** 新增按钮操作 */
function handleAdd() {
  reset()
  open.value = true
  title.value = "添加实验室信息"
}

/** 修改按钮操作 */
function handleUpdate(row) {
  reset()
  const _labId = row.labId || ids.value
  getLaboratory(_labId).then(response => {
    form.value = response.data
    if (form.value.openTime && form.value.closeTime) {
      form.value.timeRange = [form.value.openTime, form.value.closeTime]
    }
    open.value = true
    title.value = "修改实验室信息"
  })
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["laboratoryRef"].validate(valid => {
    if (valid) {
      const submitData = { ...form.value }
      if (submitData.timeRange && submitData.timeRange.length === 2) {
        submitData.openTime = submitData.timeRange[0]
        submitData.closeTime = submitData.timeRange[1]
      }
      delete submitData.timeRange
      
      if (form.value.labId != null) {
        updateLaboratory(submitData).then(() => {
          proxy.$modal.msgSuccess("修改成功")
          open.value = false
          getList()
        })
      } else {
        addLaboratory(submitData).then(() => {
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
  const _labIds = row.labId || ids.value
  proxy.$modal.confirm('是否确认删除实验室信息编号为"' + _labIds + '"的数据项？').then(function() {
    return delLaboratory(_labIds)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess("删除成功")
  }).catch(() => {})
}

/** 导出按钮操作 */
function handleExport() {
  proxy.download('lab/laboratory/export', {
    ...queryParams.value
  }, `laboratory_${new Date().getTime()}.xlsx`)
}

getList()
</script>
