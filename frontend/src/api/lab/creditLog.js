import request from '@/utils/request'

// 查询用户信用分日志列表
export function listCreditLog(query) {
  return request({
    url: '/lab/credit/log/list',
    method: 'get',
    params: query
  })
}

// 查询用户信用分日志详细
export function getCreditLog(logId) {
  return request({
    url: '/lab/credit/log/' + logId,
    method: 'get'
  })
}

// 新增用户信用分日志
export function addCreditLog(data) {
  return request({
    url: '/lab/credit/log',
    method: 'post',
    data: data
  })
}

// 修改用户信用分日志
export function updateCreditLog(data) {
  return request({
    url: '/lab/credit/log',
    method: 'put',
    data: data
  })
}

// 删除用户信用分日志
export function delCreditLog(logId) {
  return request({
    url: '/lab/credit/log/' + logId,
    method: 'delete'
  })
}

// 导出用户信用分日志
export function exportCreditLog(query) {
  return request({
    url: '/lab/credit/log/export',
    method: 'get',
    params: query
  })
}
