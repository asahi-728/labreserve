import request from '@/utils/request'

export function listRule(query) {
  return request({
    url: '/lab/reservation/rule/list',
    method: 'get',
    params: query
  })
}

export function getRule(ruleId) {
  return request({
    url: '/lab/reservation/rule/' + ruleId,
    method: 'get'
  })
}

export function addRule(data) {
  return request({
    url: '/lab/reservation/rule',
    method: 'post',
    data: data
  })
}

export function updateRule(data) {
  return request({
    url: '/lab/reservation/rule',
    method: 'put',
    data: data
  })
}

export function delRule(ruleId) {
  return request({
    url: '/lab/reservation/rule/' + ruleId,
    method: 'delete'
  })
}
