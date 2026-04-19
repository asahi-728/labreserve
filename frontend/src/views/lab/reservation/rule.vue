<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="规则名称" prop="ruleName">
        <el-input
          v-model="queryParams.ruleName"
          placeholder="请输入规则名称"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="规则类型" prop="ruleType">
        <el-select v-model="queryParams.ruleType" placeholder="请选择规则类型" clearable>
          <el-option label="取消规则" value="cancel" />
          <el-option label="完成规则" value="complete" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="请选择状态" clearable>
          <el-option label="正常" value="0" />
          <el-option label="停用" value="1" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

<!--    <el-row :gutter="10" class="mb8">-->
<!--&lt;!&ndash;      <el-col :span="1.5">&ndash;&gt;-->
<!--&lt;!&ndash;        <el-button&ndash;&gt;-->
<!--&lt;!&ndash;          type="primary"&ndash;&gt;-->
<!--&lt;!&ndash;          plain&ndash;&gt;-->
<!--&lt;!&ndash;          icon="Plus"&ndash;&gt;-->
<!--&lt;!&ndash;          @click="handleAdd"&ndash;&gt;-->
<!--&lt;!&ndash;          v-hasPermi="['lab:rule:add']"&ndash;&gt;-->
<!--&lt;!&ndash;        >新增</el-button>&ndash;&gt;-->
<!--&lt;!&ndash;      </el-col>&ndash;&gt;-->
<!--      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>-->
<!--    </el-row>-->

    <el-table v-loading="loading" :data="ruleList">
      <el-table-column label="规则ID" align="center" prop="ruleId" width="80" />
      <el-table-column label="规则名称" align="center" prop="ruleName" width="200" show-overflow-tooltip />
      <el-table-column label="规则键名" align="center" prop="ruleKey" width="200" />
      <el-table-column label="规则值" align="center" prop="ruleValue" width="120" />
      <el-table-column label="规则类型" align="center" prop="ruleType" width="120">
        <template #default="scope">
          <el-tag v-if="scope.row.ruleType === 'cancel'" type="warning">取消规则</el-tag>
          <el-tag v-else-if="scope.row.ruleType === 'complete'" type="success">完成规则</el-tag>
          <span v-else>{{ scope.row.ruleType }}</span>
        </template>
      </el-table-column>
      <el-table-column label="规则描述" align="center" prop="description" show-overflow-tooltip />
      <el-table-column label="状态" align="center" prop="status" width="80">
        <template #default="scope">
          <el-switch
            v-model="scope.row.status"
            active-value="0"
            inactive-value="1"
            @change="handleStatusChange(scope.row)"
          />
        </template>
      </el-table-column>
      <el-table-column label="创建时间" align="center" prop="createTime" width="180">
        <template #default="scope">
          <span>{{ parseTime(scope.row.createTime) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="150">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['lab:rule:edit']">修改</el-button>
          <el-button link type="danger" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['lab:rule:remove']">删除</el-button>
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

    <!-- 添加或修改规则配置对话框 -->
    <el-dialog :title="title" v-model="open" width="500px" append-to-body>
      <el-form ref="ruleRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="规则名称" prop="ruleName">
          <el-input v-model="form.ruleName" placeholder="请输入规则名称" />
        </el-form-item>
        <el-form-item label="规则键名" prop="ruleKey">
          <el-input v-model="form.ruleKey" placeholder="请输入规则键名" :disabled="form.ruleId != null" />
        </el-form-item>
        <el-form-item label="规则值" prop="ruleValue">
          <el-input v-model="form.ruleValue" placeholder="请输入规则值（数字）" type="number" />
        </el-form-item>
        <el-form-item label="规则类型" prop="ruleType">
          <el-select v-model="form.ruleType" placeholder="请选择规则类型" style="width: 100%">
            <el-option label="取消规则" value="cancel" />
            <el-option label="完成规则" value="complete" />
          </el-select>
        </el-form-item>
        <el-form-item label="规则描述" prop="description">
          <el-input v-model="form.description" type="textarea" placeholder="请输入规则描述" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio label="0">正常</el-radio>
            <el-radio label="1">停用</el-radio>
          </el-radio-group>
        </el-form-item>
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

<script setup name="ReservationRule">
import { listRule, getRule, addRule, updateRule, delRule } from "@/api/lab/reservationRule"
import { parseTime } from '@/utils/ruoyi'

const { proxy } = getCurrentInstance()

const ruleList = ref([])
const loading = ref(true)
const showSearch = ref(true)
const total = ref(0)
const title = ref("")
const open = ref(false)

const data = reactive({
  form: {},
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    ruleName: null,
    ruleKey: null,
    ruleType: null,
    status: null
  },
  rules: {
    ruleName: [{ required: true, message: "规则名称不能为空", trigger: "blur" }],
    ruleKey: [{ required: true, message: "规则键名不能为空", trigger: "blur" }],
    ruleValue: [{ required: true, message: "规则值不能为空", trigger: "blur" }],
    ruleType: [{ required: true, message: "规则类型不能为空", trigger: "change" }]
  }
})

const { queryParams, form, rules } = toRefs(data)

function getList() {
  loading.value = true
  listRule(queryParams.value).then(response => {
    ruleList.value = response.rows
    total.value = response.total
    loading.value = false
  })
}

function handleQuery() {
  queryParams.value.pageNum = 1
  getList()
}

function resetQuery() {
  proxy.resetForm("queryRef")
  handleQuery()
}

function cancel() {
  open.value = false
  reset()
}

function reset() {
  form.value = {
    ruleId: null,
    ruleName: null,
    ruleKey: null,
    ruleValue: null,
    ruleType: null,
    description: null,
    status: "0"
  }
  proxy.resetForm("ruleRef")
}

function handleAdd() {
  reset()
  open.value = true
  title.value = "添加预约规则配置"
}

function handleUpdate(row) {
  reset()
  const ruleId = row.ruleId || ids.value[0]
  getRule(ruleId).then(response => {
    form.value = response.data
    open.value = true
    title.value = "修改预约规则配置"
  })
}

function handleStatusChange(row) {
  let text = row.status === "0" ? "启用" : "停用"
  proxy.$modal.confirm('确认要"' + text + '"该规则吗？').then(function() {
    return updateRule({ ruleId: row.ruleId, status: row.status })
  }).then(() => {
    proxy.$modal.msgSuccess(text + "成功")
  }).catch(function() {
    row.status = row.status === "0" ? "1" : "0"
  })
}

function submitForm() {
  proxy.$refs["ruleRef"].validate(valid => {
    if (valid) {
      if (form.value.ruleId != null) {
        updateRule(form.value).then(() => {
          proxy.$modal.msgSuccess("修改成功")
          open.value = false
          getList()
        })
      } else {
        addRule(form.value).then(() => {
          proxy.$modal.msgSuccess("新增成功")
          open.value = false
          getList()
        })
      }
    }
  })
}

function handleDelete(row) {
  const ruleIds = row.ruleId || ids.value
  proxy.$modal.confirm('是否确认删除规则编号为"' + ruleIds + '"的数据项？').then(function() {
    return delRule(ruleIds)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess("删除成功")
  }).catch(() => {})
}

onMounted(() => {
  getList()
})
</script>
