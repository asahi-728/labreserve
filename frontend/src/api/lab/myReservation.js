import request from '@/utils/request'

export function listMyReservation(query) {
  return request({
    url: '/lab/reservation/my/list',
    method: 'get',
    params: query
  })
}

export function getReservation(reserveId) {
  return request({
    url: '/lab/reservation/' + reserveId,
    method: 'get'
  })
}

export function cancelReservation(reserveId) {
  return request({
    url: '/lab/reservation/cancel/' + reserveId,
    method: 'put'
  })
}

export function completeReservation(reserveId) {
  return request({
    url: '/lab/reservation/complete/' + reserveId,
    method: 'put'
  })
}

export function startUsing(reserveId) {
  return request({
    url: '/lab/reservation/start/' + reserveId,
    method: 'put'
  })
}
