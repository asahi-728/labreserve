import request from '@/utils/request'

// 查询实验室设备列表
export function listDevice(query) {
  return request({
    url: '/lab/device/list',
    method: 'get',
    params: query
  })
}

// 查询实验室设备详细
export function getDevice(deviceId) {
  return request({
    url: '/lab/device/' + deviceId,
    method: 'get'
  })
}

// 新增实验室设备
export function addDevice(data) {
  return request({
    url: '/lab/device',
    method: 'post',
    data: data
  })
}

// 修改实验室设备
export function updateDevice(data) {
  return request({
    url: '/lab/device',
    method: 'put',
    data: data
  })
}

// 删除实验室设备
export function delDevice(deviceId) {
  return request({
    url: '/lab/device/' + deviceId,
    method: 'delete'
  })
}
