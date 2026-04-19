import request from '@/utils/request'

// 查询用户信用评分列表
export function listCredit(query) {
  return request({
    url: '/lab/credit/list',
    method: 'get',
    params: query
  })
}

// 查询用户信用评分详细
export function getCredit(id) {
  return request({
    url: '/lab/credit/' + id,
    method: 'get'
  })
}

// 根据用户ID查询信用记录
export function getCreditByUserId(userId) {
  return request({
    url: '/lab/credit/user/' + userId,
    method: 'get'
  })
}

// 新增用户信用评分
export function addCredit(data) {
  return request({
    url: '/lab/credit',
    method: 'post',
    data: data
  })
}

// 修改用户信用评分
export function updateCredit(data) {
  return request({
    url: '/lab/credit',
    method: 'put',
    data: data
  })
}

// 删除用户信用评分
export function delCredit(id) {
  return request({
    url: '/lab/credit/' + id,
    method: 'delete'
  })
}

// 更新用户信用评分（守约）
export function updateCreditOnTime(userId) {
  return request({
    url: '/lab/credit/onTime/' + userId,
    method: 'put'
  })
}

// 更新用户信用评分（违约）
export function updateCreditViolation(userId) {
  return request({
    url: '/lab/credit/violation/' + userId,
    method: 'put'
  })
}

// 更新用户信用评分（取消预约）
export function updateCreditCancel(userId) {
  return request({
    url: '/lab/credit/cancel/' + userId,
    method: 'put'
  })
}
