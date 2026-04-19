import request from '@/utils/request'

// GATv2算法相关接口
export function gatHealth() {
  return request({
    url: '/lab/algorithm/gat/health',
    method: 'get'
  })
}

export function gatStatus() {
  return request({
    url: '/lab/algorithm/gat/status',
    method: 'get'
  })
}

export function gatRecommend(data) {
  return request({
    url: '/lab/algorithm/gat/recommend',
    method: 'post',
    data: data
  })
}

export function gatRecommendBatch(data) {
  return request({
    url: '/lab/algorithm/gat/recommend/batch',
    method: 'post',
    data: data
  })
}

export function gatEmbedding(data) {
  return request({
    url: '/lab/algorithm/gat/embedding',
    method: 'post',
    data: data
  })
}

export function gatTrain(data) {
  return request({
    url: '/lab/algorithm/gat/train',
    method: 'post',
    data: data
  })
}

export function gatTrainStatus() {
  return request({
    url: '/lab/algorithm/gat/train/status',
    method: 'get'
  })
}

export function gatTrainLogs() {
  return request({
    url: '/lab/algorithm/gat/train/logs',
    method: 'get'
  })
}

export function gatTrainLogsClear() {
  return request({
    url: '/lab/algorithm/gat/train/logs/clear',
    method: 'post'
  })
}

// PPO 算法 API
export function ppoHealth() {
  return request({
    url: '/lab/algorithm/ppo/health',
    method: 'get'
  })
}

export function ppoStatus() {
  return request({
    url: '/lab/algorithm/ppo/status',
    method: 'get'
  })
}

export function ppoTrain(params) {
  return request({
    url: '/lab/algorithm/ppo/train',
    method: 'post',
    data: params
  })
}

export function ppoTrainStatus() {
  return request({
    url: '/lab/algorithm/ppo/train/status',
    method: 'get'
  })
}

export function ppoTrainLogs() {
  return request({
    url: '/lab/algorithm/ppo/train/logs',
    method: 'get'
  })
}

export function ppoTrainLogsClear() {
  return request({
    url: '/lab/algorithm/ppo/train/logs/clear',
    method: 'post'
  })
}

export function ppoDispatchSingle(params) {
  return request({
    url: '/lab/algorithm/ppo/dispatch/single',
    method: 'post',
    data: params
  })
}

export function ppoDispatchBatch(params) {
  return request({
    url: '/lab/algorithm/ppo/dispatch/batch',
    method: 'post',
    data: params
  })
}

export function ppoDispatchStatus(reserveId) {
  return request({
    url: '/lab/algorithm/ppo/dispatch/status/' + reserveId,
    method: 'get'
  })
}

// 算法训练日志相关接口
export function listTrainingLog(query) {
  return request({
    url: '/lab/training/list',
    method: 'get',
    params: query
  })
}

export function getTrainingLog(trainingId) {
  return request({
    url: '/lab/training/' + trainingId,
    method: 'get'
  })
}

export function startTraining(data) {
  return request({
    url: '/lab/training/start',
    method: 'post',
    data: data
  })
}

export function updateTrainingProgress(trainingId, data) {
  return request({
    url: '/lab/training/progress/' + trainingId,
    method: 'put',
    data: data
  })
}

export function completeTraining(trainingId, data) {
  return request({
    url: '/lab/training/complete/' + trainingId,
    method: 'put',
    data: data
  })
}

export function failTraining(trainingId, data) {
  return request({
    url: '/lab/training/fail/' + trainingId,
    method: 'put',
    data: data
  })
}
