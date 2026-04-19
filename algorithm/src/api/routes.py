"""
GATv2 API 路由模块
定义所有API端点
"""

import asyncio
from datetime import datetime
from fastapi import APIRouter, HTTPException, BackgroundTasks
from typing import Dict, Any

from ..utils import config, logger
from ..algorithms import recommender
from .schemas import (
    HealthResponse,
    RecommendRequest, RecommendResponse, RecommendationItem,
    EmbeddingRequest, EmbeddingResponse,
    TrainRequest, TrainResponse,
    StatusResponse,
    BatchRecommendRequest, BatchRecommendResponse,
    ErrorResponse
)

router = APIRouter()

# 训练状态管理
training_status = {
    'is_training': False,
    'last_train_time': None,
    'current_epoch': 0,
    'total_epochs': 0,
    'progress': 0.0
}


@router.get("/health", response_model=HealthResponse, tags=["系统"])
async def health_check():
    """
    健康检查接口
    
    Returns:
        HealthResponse: 服务健康状态
    """
    return HealthResponse(
        status="ok",
        service="GATv2 Recommendation Service",
        timestamp=datetime.now(),
        version="1.0.0"
    )


@router.get("/status", response_model=StatusResponse, tags=["系统"])
async def get_status():
    """
    获取系统状态
    
    Returns:
        StatusResponse: 系统状态信息
    """
    status_data = {
        'is_initialized': recommender.is_initialized,
        'training_status': training_status.copy(),
        'model_config': {
            'in_channels': config.model_in_channels,
            'hidden_channels': config.model_hidden_channels,
            'out_channels': config.model_out_channels,
            'lr': config.model_lr,
            'epochs': config.model_epochs
        },
        'recommend_config': {
            'top_k': config.recommend_top_k,
            'exclude_reserved': config.recommend_exclude_reserved,
            'exclude_maintenance': config.recommend_exclude_maintenance
        }
    }
    
    if recommender.is_initialized:
        status_data['data_info'] = {
            'user_count': recommender.data_loader.user_count,
            'device_count': recommender.data_loader.device_count,
            'edge_count': recommender.data_loader.edge_index.shape[1] if recommender.data_loader.edge_index.numel() > 0 else 0
        }
    
    return StatusResponse(code=200, message="success", data=status_data)


@router.post("/recommend", response_model=RecommendResponse, tags=["推荐"], 
             responses={400: {"model": ErrorResponse}, 500: {"model": ErrorResponse}})
async def get_recommendation(request: RecommendRequest):
    """
    获取单个用户的个性化推荐
    
    Args:
        request: 推荐请求参数
    
    Returns:
        RecommendResponse: 推荐结果
    """
    try:
        if not recommender.is_initialized:
            logger.info("推荐器未初始化，正在初始化...")
            success = recommender.initialize()
            if not success:
                raise HTTPException(status_code=500, detail="推荐器初始化失败")
        
        recommendations = recommender.recommend(
            user_id=request.user_id,
            top_k=request.top_k
        )
        
        if request.save_to_db and recommendations:
            recommender.save_recommendations_to_db(recommendations)
        
        # 转换为响应模型
        recommendation_items = [
            RecommendationItem(**rec) for rec in recommendations
        ]
        
        return RecommendResponse(
            code=200,
            message="success",
            data=recommendation_items
        )
    
    except HTTPException:
        raise
    except Exception as e:
        logger.exception(f"获取推荐失败: {str(e)}")
        raise HTTPException(status_code=500, detail=f"获取推荐失败: {str(e)}")


@router.post("/recommend/batch", response_model=BatchRecommendResponse, tags=["推荐"],
             responses={400: {"model": ErrorResponse}, 500: {"model": ErrorResponse}})
async def get_batch_recommendation(request: BatchRecommendRequest):
    """
    批量获取用户的个性化推荐
    
    Args:
        request: 批量推荐请求参数
    
    Returns:
        BatchRecommendResponse: 批量推荐结果
    """
    try:
        if not recommender.is_initialized:
            logger.info("推荐器未初始化，正在初始化...")
            success = recommender.initialize()
            if not success:
                raise HTTPException(status_code=500, detail="推荐器初始化失败")
        
        batch_results = {}
        all_recommendations = []
        
        for user_id in request.user_ids:
            recommendations = recommender.recommend(
                user_id=user_id,
                top_k=request.top_k
            )
            
            batch_results[user_id] = [
                RecommendationItem(**rec) for rec in recommendations
            ]
            
            if request.save_to_db:
                all_recommendations.extend(recommendations)
        
        if request.save_to_db and all_recommendations:
            recommender.save_recommendations_to_db(all_recommendations)
        
        return BatchRecommendResponse(
            code=200,
            message="success",
            data=batch_results
        )
    
    except HTTPException:
        raise
    except Exception as e:
        logger.exception(f"批量获取推荐失败: {str(e)}")
        raise HTTPException(status_code=500, detail=f"批量获取推荐失败: {str(e)}")


@router.post("/embedding", response_model=EmbeddingResponse, tags=["嵌入"],
             responses={400: {"model": ErrorResponse}, 500: {"model": ErrorResponse}})
async def get_embedding(request: EmbeddingRequest):
    """
    获取用户或设备的嵌入向量
    
    Args:
        request: 嵌入向量请求参数
    
    Returns:
        EmbeddingResponse: 嵌入向量结果
    """
    try:
        if not recommender.is_initialized:
            raise HTTPException(status_code=400, detail="推荐器未初始化，请先调用 /train 或 /recommend")
        
        result_data = {
            'timestamp': datetime.now().isoformat()
        }
        
        if request.user_id is not None:
            user_emb = recommender.get_user_embedding(request.user_id)
            if user_emb is None:
                raise HTTPException(status_code=404, detail=f"用户 {request.user_id} 不存在")
            result_data['user_id'] = request.user_id
            result_data['user_embedding'] = user_emb
        
        if request.device_id is not None:
            device_emb = recommender.get_device_embedding(request.device_id)
            if device_emb is None:
                raise HTTPException(status_code=404, detail=f"设备 {request.device_id} 不存在")
            result_data['device_id'] = request.device_id
            result_data['device_embedding'] = device_emb
        
        return EmbeddingResponse(
            code=200,
            message="success",
            data=result_data
        )
    
    except HTTPException:
        raise
    except Exception as e:
        logger.exception(f"获取嵌入向量失败: {str(e)}")
        raise HTTPException(status_code=500, detail=f"获取嵌入向量失败: {str(e)}")


def _train_model_task(force_reload: bool, epochs: int):
    """
    后台训练任务
    
    Args:
        force_reload: 是否强制重新加载数据
        epochs: 训练轮数
    """
    global training_status
    
    try:
        training_status['is_training'] = True
        training_status['current_epoch'] = 0
        training_status['total_epochs'] = epochs or config.model_epochs
        training_status['progress'] = 0.0
        
        logger.info("开始后台训练任务...")
        
        if force_reload or not recommender.is_initialized:
            recommender.initialize(force_reload=True)
        else:
            recommender.retrain()
        
        training_status['last_train_time'] = datetime.now().isoformat()
        training_status['progress'] = 1.0
        
        logger.info("后台训练任务完成")
    
    except Exception as e:
        logger.exception(f"后台训练任务失败: {str(e)}")
    
    finally:
        training_status['is_training'] = False


@router.post("/train", response_model=TrainResponse, tags=["训练"],
             responses={400: {"model": ErrorResponse}, 500: {"model": ErrorResponse}})
async def train_model(request: TrainRequest, background_tasks: BackgroundTasks):
    """
    触发模型训练（异步）
    
    Args:
        request: 训练请求参数
        background_tasks: 后台任务
    
    Returns:
        TrainResponse: 训练响应
    """
    try:
        if training_status['is_training']:
            raise HTTPException(status_code=400, detail="训练正在进行中，请稍后再试")
        
        # 启动后台训练任务
        background_tasks.add_task(
            _train_model_task,
            force_reload=request.force_reload,
            epochs=request.epochs
        )
        
        return TrainResponse(
            code=200,
            message="训练任务已启动",
            data={
                'status': 'training_started',
                'timestamp': datetime.now().isoformat()
            }
        )
    
    except HTTPException:
        raise
    except Exception as e:
        logger.exception(f"启动训练失败: {str(e)}")
        raise HTTPException(status_code=500, detail=f"启动训练失败: {str(e)}")


@router.get("/train/status", tags=["训练"])
async def get_training_status():
    """
    获取训练状态
    
    Returns:
        dict: 训练状态
    """
    return {
        'code': 200,
        'message': 'success',
        'data': training_status.copy()
    }
