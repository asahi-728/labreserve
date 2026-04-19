import request from '@/utils/request'

// 查询PPO智能调度决策日志列表
export function listSchedule(query) {
  return request({
    url: '/lab/schedule/list',
    method: 'get',
    params: query
  })
}

// 查询PPO智能调度决策日志详细
export function getSchedule(logId) {
  return request({
    url: '/lab/schedule/' + logId,
    method: 'get'
  })
}

// 新增PPO智能调度决策日志
export function addSchedule(data) {
  return request({
    url: '/lab/schedule',
    method: 'post',
    data: data
  })
}

// 修改PPO智能调度决策日志
export function updateSchedule(data) {
  return request({
    url: '/lab/schedule',
    method: 'put',
    data: data
  })
}

// 删除PPO智能调度决策日志
export function delSchedule(logId) {
  return request({
    url: '/lab/schedule/' + logId,
    method: 'delete'
  })
}

// PPO智能调度决策
export function makeSchedulingDecision(data) {
  return request({
    url: '/lab/schedule/decision',
    method: 'post',
    data: data
  })
}
