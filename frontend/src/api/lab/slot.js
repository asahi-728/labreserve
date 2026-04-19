import request from '@/utils/request'

// 查询设备可预约时段列表
export function listSlot(query) {
  return request({
    url: '/lab/slot/list',
    method: 'get',
    params: query
  })
}

// 查询设备可预约时段列表（不分页）
export function listAllSlot(query) {
  return request({
    url: '/lab/slot/listAll',
    method: 'get',
    params: query
  })
}

// 查询设备可预约时段详细
export function getSlot(slotId) {
  return request({
    url: '/lab/slot/' + slotId,
    method: 'get'
  })
}

// 新增设备可预约时段
export function addSlot(data) {
  return request({
    url: '/lab/slot',
    method: 'post',
    data: data
  })
}

// 修改设备可预约时段
export function updateSlot(data) {
  return request({
    url: '/lab/slot',
    method: 'put',
    data: data
  })
}

// 删除设备可预约时段
export function delSlot(slotId) {
  return request({
    url: '/lab/slot/' + slotId,
    method: 'delete'
  })
}

// 批量新增设备可预约时段
export function batchAddSlot(data) {
  return request({
    url: '/lab/slot/batch',
    method: 'post',
    data: data
  })
}
