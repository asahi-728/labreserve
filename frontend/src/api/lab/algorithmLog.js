import request from '@/utils/request'

// 算法日志相关接口

export function getList(params) {
  return request({
    url: '/lab/algorithm/log/list',
    method: 'get',
    params
  })
}

export function getInfo(logId) {
  return request({
    url: `/lab/algorithm/log/${logId}`,
    method: 'get'
  })
}

export function del(logId) {
  return request({
    url: `/lab/algorithm/log/${logId}`,
    method: 'delete'
  })
}

export function clean() {
  return request({
    url: '/lab/algorithm/log/clean',
    method: 'delete'
  })
}
