"""
PPO API 路由模块
定义PPO算法相关的所有API端点
"""

import asyncio
from datetime import datetime
from fastapi import APIRouter, HTTPException, BackgroundTasks
from typing import Dict, Any, List

from ...utils import config, logger
from ...algorithms.ppo import dispatcher
from ..schemas import (
    HealthResponse,
    StatusResponse,
    TrainResponse,
    ErrorResponse,
    DispatchSingleRequest,
    DispatchBatchRequest,
    DispatchResponse,
    PPOTrainRequest
)

router = APIRouter()

# 训练状态管理
training_status = {
    'is_training': False,
    'last_train_time': None,
    'current_epoch': 0,
    'total_epochs': 0,
    'progress': 0.0,
    # 每轮训练指标（实时累积）
    'metrics': {
        'epochs': [],       # 每轮 episode 编号
        'rewards': [],      # 每轮 reward
        'losses': [],       # 每轮总 loss
        'accuracies': [],   # 每轮 accuracy
        'policy_losses': [], # 每轮 policy_loss（可选）
        'value_losses': [],  # 每轮 value_loss（可选）
    }
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
        service="PPO Scheduling Service",
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
        'is_initialized': dispatcher.is_initialized,
        'training_status': training_status.copy(),
        'model_config': {
            'hidden_size': config.ppo_hidden_size,
            'lr': config.ppo_lr,
            'gamma': config.ppo_gamma,
            'clip_epsilon': config.ppo_clip_epsilon,
            'epochs': config.ppo_epochs
        },
        'scheduler_config': {
            'max_steps': config.ppo_max_steps,
            'batch_size': config.ppo_batch_size
        }
    }
    
    if dispatcher.is_initialized:
        status_data['data_info'] = {
            'device_count': dispatcher.env.device_count,
            'reservation_count': dispatcher.env.reservation_count
        }
    
    return StatusResponse(code=200, message="success", data=status_data)


@router.post("/dispatch/single", response_model=DispatchResponse, tags=["调度"],
             responses={400: {"model": ErrorResponse}, 500: {"model": ErrorResponse}})
async def dispatch_single(request: DispatchSingleRequest):
    """
    调度单个预约请求
    
    Args:
        request: 单个调度请求参数
    
    Returns:
        DispatchResponse: 调度结果
    """
    try:
        if not dispatcher.is_initialized:
            logger.info("调度器未初始化，正在初始化...")
            success = dispatcher.initialize()
            if not success:
                raise HTTPException(status_code=500, detail="调度器初始化失败")
        
        result = dispatcher.dispatch_single(request.reserve_id)
        
        return DispatchResponse(
            code=200,
            message="success",
            data=result
        )
    
    except HTTPException:
        raise
    except Exception as e:
        logger.exception(f"调度失败: {str(e)}")
        raise HTTPException(status_code=500, detail=f"调度失败: {str(e)}")


@router.post("/dispatch/batch", response_model=DispatchResponse, tags=["调度"],
             responses={400: {"model": ErrorResponse}, 500: {"model": ErrorResponse}})
async def dispatch_batch(request: DispatchBatchRequest):
    """
    批量调度预约请求
    
    Args:
        request: 批量调度请求参数
    
    Returns:
        DispatchResponse: 批量调度结果
    """
    try:
        if not dispatcher.is_initialized:
            logger.info("调度器未初始化，正在初始化...")
            success = dispatcher.initialize()
            if not success:
                raise HTTPException(status_code=500, detail="调度器初始化失败")
        
        results = dispatcher.dispatch_batch(request.reserve_ids)
        
        return DispatchResponse(
            code=200,
            message="success",
            data=results
        )
    
    except HTTPException:
        raise
    except Exception as e:
        logger.exception(f"批量调度失败: {str(e)}")
        raise HTTPException(status_code=500, detail=f"批量调度失败: {str(e)}")


@router.get("/dispatch/status/{reserve_id}", tags=["调度"])
async def get_dispatch_status(reserve_id: int):
    """
    获取单个预约的调度状态
    
    Args:
        reserve_id: 预约ID
    
    Returns:
        dict: 调度状态
    """
    try:
        if not dispatcher.is_initialized:
            raise HTTPException(status_code=400, detail="调度器未初始化")
        
        status = dispatcher.get_dispatch_status(reserve_id)
        if status is None:
            raise HTTPException(status_code=404, detail=f"预约 {reserve_id} 不存在")
        
        return {
            'code': 200,
            'message': 'success',
            'data': status
        }
    
    except HTTPException:
        raise
    except Exception as e:
        logger.exception(f"获取调度状态失败: {str(e)}")
        raise HTTPException(status_code=500, detail=f"获取调度状态失败: {str(e)}")


def _update_training_progress(current_epoch: int, total_epochs: int, metrics: dict = None):
    """
    更新训练进度回调函数
    
    Args:
        current_epoch: 当前episode
        total_epochs: 总episode数
        metrics: 本轮训练指标字典，如 {'reward': 12.3, 'loss': 0.56, 'accuracy': 0.87}
    """
    global training_status
    training_status['current_epoch'] = current_epoch
    training_status['progress'] = current_epoch / total_epochs
    
    # 累积每轮训练指标
    if metrics:
        training_status['metrics']['epochs'].append(current_epoch)
        if 'reward' in metrics:
            training_status['metrics']['rewards'].append(metrics['reward'])
        if 'accuracy' in metrics:
            training_status['metrics']['accuracies'].append(metrics['accuracy'])
        if 'loss' in metrics:
            training_status['metrics']['losses'].append(metrics['loss'])
        if 'policy_loss' in metrics:
            training_status['metrics']['policy_losses'].append(metrics['policy_loss'])
        if 'value_loss' in metrics:
            training_status['metrics']['value_losses'].append(metrics['value_loss'])


def _log_training_mode(has_saved_model: bool, force_reload: bool, is_initialized: bool):
    """记录当前训练模式日志"""
    if force_reload:
        logger.info("🔄 [训练模式] 强制重载 — 全新模型从头训练")
    elif has_saved_model and is_initialized:
        logger.info("📈 [训练模式] 增量训练 — 在已有模型基础上继续优化")
    elif has_saved_model and not is_initialized:
        logger.info("📦 [训练模式] 加载已保存模型后继续训练")
    else:
        logger.info("🆕 [训练模式] 首次训练 — 创建全新模型")


def _train_model_task(force_reload: bool, epochs: int, hidden_size: int, lr: float, gamma: float, clip_epsilon: float, batch_size: int, max_steps: int):
    """
    后台训练任务

    Args:
        force_reload: 是否强制重新加载数据
        epochs: 训练轮数
        hidden_size: 隐藏层大小
        lr: 学习率
        gamma: 折扣因子
        clip_epsilon: PPO裁剪系数
        batch_size: 批次大小
        max_steps: 每轮最大步数
    """
    global training_status

    try:
        training_status['is_training'] = True
        training_status['current_epoch'] = 0
        training_status['total_epochs'] = epochs or config.ppo_epochs
        training_status['progress'] = 0.0

        logger.info("开始后台训练任务...")

        # 重置训练指标
        training_status['metrics'] = {
            'epochs': [], 'rewards': [], 'losses': [],
            'accuracies': [], 'policy_losses': [], 'value_losses': []
        }

        dispatcher.set_progress_callback(_update_training_progress)

        # ===== 增量训练逻辑 =====
        model_path = config.model_dir / "ppo_model.pth"
        has_saved_model = model_path.exists()

        if force_reload or not dispatcher.is_initialized:
            # 首次训练 或 用户强制重载 → 完全重新开始
            if has_saved_model and not force_reload:
                # 有旧模型文件但调度器未初始化（如服务重启后）→ 加载旧模型继续训
                logger.info(f"检测到已有模型文件 {model_path}，尝试加载以继续训练...")
                init_ok = dispatcher.initialize(
                    force_reload=False,  # 不强制重载，让 initialize 内部加载模型
                    hidden_size=hidden_size if hidden_size is not None else config.ppo_hidden_size,
                    lr=lr if lr is not None else config.ppo_lr,
                    gamma=gamma if gamma is not None else config.ppo_gamma,
                    clip_epsilon=clip_epsilon if clip_epsilon is not None else config.ppo_clip_epsilon,
                    batch_size=batch_size if batch_size is not None else config.ppo_batch_size
                )
                if not init_ok:
                    # 加载失败则回退到全新初始化
                    logger.warning("加载旧模型失败，使用全新模型从头训练")
                    dispatcher.initialize(
                        force_reload=True,
                        hidden_size=hidden_size if hidden_size is not None else config.ppo_hidden_size,
                        lr=lr if lr is not None else config.ppo_lr,
                        gamma=gamma if gamma is not None else config.ppo_gamma,
                        clip_epsilon=clip_epsilon if clip_epsilon is not None else config.ppo_clip_epsilon,
                        batch_size=batch_size if batch_size is not None else config.ppo_batch_size
                    )
            else:
                # force_reload=True 或无旧模型 → 全新初始化
                mode = "强制重载" if force_reload else "首次训练"
                logger.info(f"{mode}，创建全新PPO模型...")
                dispatcher.initialize(
                    force_reload=True,
                    hidden_size=hidden_size if hidden_size is not None else config.ppo_hidden_size,
                    lr=lr if lr is not None else config.ppo_lr,
                    gamma=gamma if gamma is not None else config.ppo_gamma,
                    clip_epsilon=clip_epsilon if clip_epsilon is not None else config.ppo_clip_epsilon,
                    batch_size=batch_size if batch_size is not None else config.ppo_batch_size
                )
        else:
            # 调度器已初始化且非 force_reload → 在当前模型基础上继续训练
            logger.info("调度器已初始化，在现有模型基础上继续训练（增量模式）")

        _log_training_mode(has_saved_model, force_reload, dispatcher.is_initialized)

        dispatcher.train(num_episodes=epochs or config.ppo_epochs, max_steps=max_steps or config.ppo_max_steps)

        training_status['last_train_time'] = datetime.now().isoformat()
        training_status['progress'] = 1.0

        logger.info("后台训练任务完成")

    except Exception as e:
        logger.exception(f"后台训练任务失败: {str(e)}")

    finally:
        training_status['is_training'] = False
        dispatcher.set_progress_callback(None)


@router.post("/train", response_model=TrainResponse, tags=["训练"],
             responses={400: {"model": ErrorResponse}, 500: {"model": ErrorResponse}})
async def train_model(request: PPOTrainRequest, background_tasks: BackgroundTasks):
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

        background_tasks.add_task(
            _train_model_task,
            force_reload=request.force_reload,
            epochs=request.epochs,
            hidden_size=request.hidden_size,
            lr=request.lr,
            gamma=request.gamma,
            clip_epsilon=request.clip_epsilon,
            batch_size=request.batch_size,
            max_steps=request.max_steps
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
    获取训练状态（包含每轮训练指标）
    
    Returns:
        dict: 训练状态，含 metrics 字段存储每轮 reward/loss/accuracy 等指标
    """
    import copy
    status_copy = copy.deepcopy(training_status)
    return {
        'code': 200,
        'message': 'success',
        'data': status_copy
    }


@router.get("/train/logs", tags=["训练"])
async def get_training_logs():
    """
    获取训练日志
    
    Returns:
        dict: 训练日志列表
    """
    from ...utils import logger as log_util
    logs = log_util.get_memory_logs()
    return {
        'code': 200,
        'message': 'success',
        'data': logs
    }


@router.post("/train/logs/clear", tags=["训练"])
async def clear_training_logs():
    """
    清空训练日志
    
    Returns:
        dict: 操作结果
    """
    from ...utils import logger as log_util
    log_util.clear_memory_logs()
    return {
        'code': 200,
        'message': 'success'
    }
