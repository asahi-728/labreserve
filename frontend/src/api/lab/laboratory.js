import request from '@/utils/request'

// 查询实验室信息列表
export function listLaboratory(query) {
  return request({
    url: '/lab/laboratory/list',
    method: 'get',
    params: query
  })
}

// 查询实验室信息详细
export function getLaboratory(labId) {
  return request({
    url: '/lab/laboratory/' + labId,
    method: 'get'
  })
}

// 新增实验室信息
export function addLaboratory(data) {
  return request({
    url: '/lab/laboratory',
    method: 'post',
    data: data
  })
}

// 修改实验室信息
export function updateLaboratory(data) {
  return request({
    url: '/lab/laboratory',
    method: 'put',
    data: data
  })
}

// 删除实验室信息
export function delLaboratory(labId) {
  return request({
    url: '/lab/laboratory/' + labId,
    method: 'delete'
  })
}
