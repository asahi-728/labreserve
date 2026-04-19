import request from '@/utils/request'

// 查询设备分类列表
export function listCategory(query) {
  return request({
    url: '/lab/category/list',
    method: 'get',
    params: query
  })
}

// 查询设备分类下拉树结构
export function treeSelectCategory(query) {
  return request({
    url: '/lab/category/treeSelect',
    method: 'get',
    params: query
  })
}

// 查询设备分类详细
export function getCategory(categoryId) {
  return request({
    url: '/lab/category/' + categoryId,
    method: 'get'
  })
}

// 新增设备分类
export function addCategory(data) {
  return request({
    url: '/lab/category',
    method: 'post',
    data: data
  })
}

// 修改设备分类
export function updateCategory(data) {
  return request({
    url: '/lab/category',
    method: 'put',
    data: data
  })
}

// 删除设备分类
export function delCategory(categoryId) {
  return request({
    url: '/lab/category/' + categoryId,
    method: 'delete'
  })
}
