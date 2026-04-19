import request from '@/utils/request'

// 查询实验室设备预约列表
export function listReservation(query) {
  return request({
    url: '/lab/reservation/list',
    method: 'get',
    params: query
  })
}

// 查询实验室设备预约详细
export function getReservation(reserveId) {
  return request({
    url: '/lab/reservation/' + reserveId,
    method: 'get'
  })
}

// 新增实验室设备预约
export function addReservation(data) {
  return request({
    url: '/lab/reservation',
    method: 'post',
    data: data
  })
}

// 修改实验室设备预约
export function updateReservation(data) {
  return request({
    url: '/lab/reservation',
    method: 'put',
    data: data
  })
}

// 删除实验室设备预约
export function delReservation(reserveId) {
  return request({
    url: '/lab/reservation/' + reserveId,
    method: 'delete'
  })
}

// 提交预约申请
export function submitReservation(data) {
  return request({
    url: '/lab/reservation/submit',
    method: 'post',
    data: data
  })
}

// 审核预约
export function auditReservation(data) {
  return request({
    url: '/lab/reservation/audit',
    method: 'put',
    data: data
  })
}

// 取消预约
export function cancelReservation(reserveId) {
  return request({
    url: '/lab/reservation/cancel/' + reserveId,
    method: 'put'
  })
}

// 检测预约冲突
export function checkConflict(data) {
  return request({
    url: '/lab/reservation/checkConflict',
    method: 'post',
    data: data
  })
}

// PPO批量自动审批
export function ppoBatchAudit(data) {
  return request({
    url: '/lab/reservation/ppoBatchAudit',
    method: 'post',
    data: data
  })
}
