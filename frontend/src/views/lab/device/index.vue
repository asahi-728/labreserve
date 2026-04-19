<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="设备名称" prop="deviceName">
        <el-input
          v-model="queryParams.deviceName"
          placeholder="请输入设备名称"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="设备分类" prop="categoryId">
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
      <el-form-item label="所属实验室" prop="labId">
        <el-select v-model="queryParams.labId" placeholder="请选择实验室" clearable>
          <el-option
            v-for="item in laboratoryList"
            :key="item.labId"
            :label="item.labName"
            :value="item.labId"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="设备状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="请选择设备状态" clearable>
          <el-option
            v-for="dict in lab_device_status"
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
          v-hasPermi="['lab:device:add']"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          icon="Edit"
          :disabled="single"
          @click="handleUpdate"
          v-hasPermi="['lab:device:edit']"
        >修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="Delete"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['lab:device:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="Download"
          @click="handleExport"
          v-hasPermi="['lab:device:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="deviceList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="设备ID" align="center" prop="deviceId" width="80" />
      <el-table-column label="设备名称" align="center" prop="deviceName" width="150" />
      <el-table-column label="设备分类" align="center" prop="categoryName" width="120" />
      <el-table-column label="所属实验室" align="center" prop="labName" width="150" />
      <el-table-column label="设备位置" align="center" prop="location" width="150" />
      <el-table-column label="设备状态" align="center" prop="status" width="100">
        <template #default="scope">
          <dict-tag :options="lab_device_status" :value="scope.row.status"/>
        </template>
      </el-table-column>
      <el-table-column label="设备规格" align="center" prop="specs" show-overflow-tooltip />
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
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['lab:device:edit']">修改</el-button>
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['lab:device:remove']">删除</el-button>
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

    <!-- 添加或修改实验室设备对话框 -->
    <el-dialog :title="title" v-model="open" width="800px" append-to-body>
      <el-form ref="deviceRef" :model="form" :rules="rules" label-width="100px">
        <el-row>
          <el-col :span="12">
            <el-form-item label="设备名称" prop="deviceName">
              <el-input v-model="form.deviceName" placeholder="请输入设备名称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="设备分类" prop="categoryId">
              <el-tree-select
                v-model="form.categoryId"
                :data="categoryTreeList"
                :props="{ value: 'id', label: 'label', children: 'children' }"
                value-key="id"
                placeholder="请选择设备分类"
                check-strictly
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="所属实验室" prop="labId">
              <el-select v-model="form.labId" placeholder="请选择实验室" style="width: 100%">
                <el-option
                  v-for="item in laboratoryList"
                  :key="item.labId"
                  :label="item.labName"
                  :value="item.labId"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="设备状态" prop="status">
              <el-radio-group v-model="form.status">
                <el-radio
                  v-for="dict in lab_device_status"
                  :key="dict.value"
                  :label="dict.value"
                >{{dict.label}}</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="设备规格" prop="specs">
              <el-input v-model="form.specs" type="textarea" placeholder="请输入设备规格说明" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="备注" prop="remark">
              <el-input v-model="form.remark" type="textarea" placeholder="请输入备注信息" />
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

    <!-- 查看设备详情对话框 -->
    <el-dialog title="设备详情" v-model="viewOpen" width="600px" append-to-body>
      <el-descriptions :column="1" border v-if="viewDevice">
        <el-descriptions-item label="设备ID">{{ viewDevice.deviceId }}</el-descriptions-item>
        <el-descriptions-item label="设备名称">{{ viewDevice.deviceName }}</el-descriptions-item>
        <el-descriptions-item label="设备分类">{{ viewDevice.categoryName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="所属实验室">{{ viewDevice.labName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="设备位置">{{ viewDevice.location || '-' }}</el-descriptions-item>
        <el-descriptions-item label="设备状态">
          <dict-tag :options="lab_device_status" :value="viewDevice.status"/>
        </el-descriptions-item>
        <el-descriptions-item label="设备规格">{{ viewDevice.specs || '-' }}</el-descriptions-item>
        <el-descriptions-item label="备注">{{ viewDevice.remark || '-' }}</el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="viewOpen = false">关 闭</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="Device">
import { listDevice, getDevice, delDevice, addDevice, updateDevice } from "@/api/lab/device"
import { listCategory, treeSelectCategory } from "@/api/lab/category"
import { listLaboratory } from "@/api/lab/laboratory"

const { proxy } = getCurrentInstance()
const { lab_device_status } = useDict('lab_device_status')

const deviceList = ref([])
const categoryList = ref([])
const categoryTreeList = ref([])
const laboratoryList = ref([])
const open = ref(false)
const viewOpen = ref(false)
const viewDevice = ref(null)
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
    deviceName: null,
    categoryId: null,
    labId: null,
    status: null,
  },
  rules: {
    deviceName: [
      { required: true, message: "设备名称不能为空", trigger: "blur" }
    ],
  }
})

const { queryParams, form, rules } = toRefs(data)

/** 扁平树形结构为列表 */
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

/** 加载设备分类列表 */
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

/** 加载实验室列表 */
async function loadLaboratoryList() {
  try {
    const res = await listLaboratory({ status: '0' })
    laboratoryList.value = res.rows || []
  } catch (error) {
    console.error('加载实验室列表失败:', error)
  }
}

/** 查询实验室设备列表 */
function getList() {
  loading.value = true
  listDevice(queryParams.value).then(response => {
    deviceList.value = response.rows.map(item => {
      const category = categoryList.value.find(c => c.categoryId === item.categoryId)
      const lab = laboratoryList.value.find(l => l.labId === item.labId)
      return {
        ...item,
        categoryName: category?.categoryName || '-',
        labName: lab?.labName || '-',
        location: lab?.location || '-'
      }
    })
    total.value = response.total
    loading.value = false
  })
}

/** 查看设备详情 */
function handleView(row) {
  viewDevice.value = {
    ...row,
    categoryName: categoryList.value.find(c => c.categoryId === row.categoryId)?.categoryName || '-',
    labName: laboratoryList.value.find(l => l.labId === row.labId)?.labName || '-',
    location: laboratoryList.value.find(l => l.labId === row.labId)?.location || '-'
  }
  viewOpen.value = true
}

// 取消按钮
function cancel() {
  open.value = false
  reset()
}

// 表单重置
function reset() {
  form.value = {
    deviceId: null,
    deviceName: null,
    categoryId: null,
    labId: null,
    status: null,
    specs: null,
    createBy: null,
    createTime: null,
    updateBy: null,
    updateTime: null,
    remark: null
  }
  proxy.resetForm("deviceRef")
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
  ids.value = selection.map(item => item.deviceId)
  single.value = selection.length != 1
  multiple.value = !selection.length
}

/** 新增按钮操作 */
function handleAdd() {
  reset()
  open.value = true
  title.value = "添加实验室设备"
}

/** 修改按钮操作 */
function handleUpdate(row) {
  reset()
  const _deviceId = row.deviceId || ids.value
  getDevice(_deviceId).then(response => {
    form.value = response.data
    open.value = true
    title.value = "修改实验室设备"
  })
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["deviceRef"].validate(valid => {
    if (valid) {
      if (form.value.deviceId != null) {
        updateDevice(form.value).then(() => {
          proxy.$modal.msgSuccess("修改成功")
          open.value = false
          getList()
        })
      } else {
        addDevice(form.value).then(() => {
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
  const _deviceIds = row.deviceId || ids.value
  proxy.$modal.confirm('是否确认删除实验室设备编号为"' + _deviceIds + '"的数据项？').then(function() {
    return delDevice(_deviceIds)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess("删除成功")
  }).catch(() => {})
}

/** 导出按钮操作 */
function handleExport() {
  proxy.download('lab/device/export', {
    ...queryParams.value
  }, `device_${new Date().getTime()}.xlsx`)
}

async function init() {
  await Promise.all([
    loadCategoryList(),
    loadLaboratoryList()
  ])
  getList()
}

init()
</script>
