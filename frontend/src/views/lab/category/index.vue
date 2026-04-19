<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch">
      <el-form-item label="分类名称" prop="categoryName">
        <el-input
          v-model="queryParams.categoryName"
          placeholder="请输入分类名称"
          clearable
          style="width: 200px"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="分类状态" clearable style="width: 200px">
          <el-option
            v-for="dict in lab_device_category_status"
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
          v-hasPermi="['lab:category:add']"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="info"
          plain
          icon="Sort"
          @click="toggleExpandAll"
        >展开/折叠</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table
      v-if="refreshTable"
      v-loading="loading"
      :data="categoryList"
      row-key="categoryId"
      :default-expand-all="isExpandAll"
      :tree-props="{ children: 'children', hasChildren: 'hasChildren' }"
    >
      <el-table-column prop="categoryName" label="分类名称" width="260"></el-table-column>
      <el-table-column prop="sort" label="排序" width="150">
        <template #default="scope">
          <el-input-number v-model="scope.row.sort" controls-position="right" :min="0" style="width: 120px" />
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="100">
        <template #default="scope">
          <dict-tag :options="lab_device_category_status" :value="scope.row.status" />
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
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['lab:category:edit']">修改</el-button>
          <el-button link type="primary" icon="Plus" @click="handleAdd(scope.row)" v-hasPermi="['lab:category:add']">新增</el-button>
          <el-button v-if="scope.row.parentId != 0" link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['lab:category:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 添加或修改设备分类对话框 -->
    <el-dialog :title="title" v-model="open" width="600px" append-to-body>
      <el-form ref="categoryRef" :model="form" :rules="rules" label-width="100px">
        <el-row>
          <el-col :span="24" v-if="form.parentId !== 0">
            <el-form-item label="上级分类" prop="parentId">
              <el-tree-select
                v-model="form.parentId"
                :data="categoryOptions"
                :props="{ value: 'categoryId', label: 'categoryName', children: 'children' }"
                value-key="categoryId"
                placeholder="选择上级分类"
                check-strictly
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="分类名称" prop="categoryName">
              <el-input v-model="form.categoryName" placeholder="请输入分类名称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="显示排序" prop="sort">
              <el-input-number v-model="form.sort" controls-position="right" :min="0" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="分类状态">
              <el-radio-group v-model="form.status">
                <el-radio
                  v-for="dict in lab_device_category_status"
                  :key="dict.value"
                  :value="dict.value"
                >{{ dict.label }}</el-radio>
              </el-radio-group>
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

<script setup name="Category">
import { listCategory, getCategory, delCategory, addCategory, updateCategory } from "@/api/lab/category"

const { proxy } = getCurrentInstance()
const { lab_device_category_status } = useDict('lab_device_category_status')

const categoryList = ref([])
const categoryOptions = ref([])
const open = ref(false)
const loading = ref(true)
const showSearch = ref(true)
const isExpandAll = ref(true)
const refreshTable = ref(true)
const title = ref("")

const data = reactive({
  form: {},
  queryParams: {
    categoryName: null,
    status: null,
  },
  rules: {
    categoryName: [
      { required: true, message: "分类名称不能为空", trigger: "blur" }
    ],
  }
})

const { queryParams, form, rules } = toRefs(data)

/** 查询设备分类列表 */
function getList() {
  loading.value = true
  listCategory(queryParams.value).then(response => {
    categoryList.value = response.data
    categoryOptions.value = response.data
    loading.value = false
  })
}

/** 展开/折叠操作 */
function toggleExpandAll() {
  refreshTable.value = false
  isExpandAll.value = !isExpandAll.value
  nextTick(() => {
    refreshTable.value = true
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
    categoryId: null,
    categoryName: null,
    parentId: 0,
    sort: 0,
    status: "0",
    createBy: null,
    createTime: null,
    updateBy: null,
    updateTime: null,
    remark: null
  }
  proxy.resetForm("categoryRef")
}

/** 搜索按钮操作 */
function handleQuery() {
  getList()
}

/** 重置按钮操作 */
function resetQuery() {
  proxy.resetForm("queryRef")
  handleQuery()
}

/** 新增按钮操作 */
function handleAdd(row) {
  reset()
  if (row != null) {
    form.value.parentId = row.categoryId
  }
  open.value = true
  title.value = "添加设备分类"
}

/** 修改按钮操作 */
function handleUpdate(row) {
  reset()
  const _categoryId = row.categoryId
  getCategory(_categoryId).then(response => {
    form.value = response.data
    open.value = true
    title.value = "修改设备分类"
  })
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["categoryRef"].validate(valid => {
    if (valid) {
      if (form.value.categoryId != null) {
        updateCategory(form.value).then(() => {
          proxy.$modal.msgSuccess("修改成功")
          open.value = false
          getList()
        })
      } else {
        addCategory(form.value).then(() => {
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
  const _categoryIds = row.categoryId
  proxy.$modal.confirm('是否确认删除设备分类编号为"' + _categoryIds + '"的数据项？').then(function() {
    return delCategory(_categoryIds)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess("删除成功")
  }).catch(() => {})
}

getList()
</script>
