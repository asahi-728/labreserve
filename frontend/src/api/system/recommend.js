import request from '@/utils/request'

// 查询GATv2智能推荐结果列表
export function listRecommend(query) {
  return request({
    url: '/system/recommend/list',
    method: 'get',
    params: query
  })
}

// 查询GATv2智能推荐结果详细
export function getRecommend(recId) {
  return request({
    url: '/system/recommend/' + recId,
    method: 'get'
  })
}

// 新增GATv2智能推荐结果
export function addRecommend(data) {
  return request({
    url: '/system/recommend',
    method: 'post',
    data: data
  })
}

// 修改GATv2智能推荐结果
export function updateRecommend(data) {
  return request({
    url: '/system/recommend',
    method: 'put',
    data: data
  })
}

// 删除GATv2智能推荐结果
export function delRecommend(recId) {
  return request({
    url: '/system/recommend/' + recId,
    method: 'delete'
  })
}

// 获取用户个性化推荐设备列表
export function getPersonalizedRecommendations(userId, topN = 5) {
  return request({
    url: '/system/recommend/personalized/' + userId,
    method: 'get',
    params: { topN }
  })
}

// 记录推荐反馈
export function recordFeedback(data) {
  return request({
    url: '/system/recommend/feedback',
    method: 'post',
    data: data
  })
}
