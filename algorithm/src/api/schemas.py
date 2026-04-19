"""
算法 API 数据模型
使用 Pydantic 定义请求和响应数据结构
"""

from pydantic import BaseModel, Field, field_validator
from typing import List, Optional, Dict, Any, Union
from datetime import datetime


class HealthResponse(BaseModel):
    """健康检查响应"""
    status: str = "ok"
    service: str = "Algorithm Service"
    timestamp: datetime
    version: str = "1.0.0"


# GATv2 算法相关模型
class RecommendRequest(BaseModel):
    """推荐请求"""
    user_id: int = Field(..., description="用户ID", gt=0)
    top_k: Optional[int] = Field(None, description="推荐数量", gt=0, le=20)
    save_to_db: Optional[bool] = Field(True, description="是否保存到数据库")


class RecommendationItem(BaseModel):
    """推荐项"""
    user_id: int = Field(..., description="用户ID")
    device_id: int = Field(..., description="设备ID")
    similarity: float = Field(..., description="相似度分数", ge=0.0, le=1.0)
    rank: int = Field(..., description="推荐排名", gt=0)
    recommend_time: str = Field(..., description="推荐时间")


class RecommendResponse(BaseModel):
    """推荐响应"""
    code: int = 200
    message: str = "success"
    data: List[RecommendationItem]


class EmbeddingRequest(BaseModel):
    """嵌入向量请求"""
    user_id: Optional[int] = Field(None, description="用户ID", gt=0)
    device_id: Optional[int] = Field(None, description="设备ID", gt=0)

    @field_validator('user_id', 'device_id')
    @classmethod
    def check_at_least_one(cls, v, info):
        if info.data.get('user_id') is None and info.data.get('device_id') is None:
            raise ValueError('必须提供 user_id 或 device_id 中的至少一个')
        return v


class EmbeddingResponse(BaseModel):
    """嵌入向量响应"""
    code: int = 200
    message: str = "success"
    data: Dict[str, Any]


class BatchRecommendRequest(BaseModel):
    """批量推荐请求"""
    user_ids: List[int] = Field(..., description="用户ID列表", min_length=1)
    top_k: Optional[int] = Field(None, description="每个用户的推荐数量", gt=0, le=20)
    save_to_db: Optional[bool] = Field(True, description="是否保存到数据库")


class BatchRecommendResponse(BaseModel):
    """批量推荐响应"""
    code: int = 200
    message: str = "success"
    data: Dict[int, List[RecommendationItem]]


# GATv2 训练参数模型
class GATTrainRequest(BaseModel):
    """GATv2训练请求"""
    force_reload: Optional[bool] = Field(False, description="是否强制重新加载数据")
    in_channels: Optional[int] = Field(None, description="输入维度", ge=4, le=32)
    hidden_channels: Optional[int] = Field(None, description="隐藏层维度", ge=8, le=64)
    out_channels: Optional[int] = Field(None, description="输出维度", ge=2, le=16)
    lr: Optional[float] = Field(None, description="学习率", ge=0.001, le=0.1)
    epochs: Optional[int] = Field(None, description="训练轮数", ge=10, le=500)
    top_k: Optional[int] = Field(None, description="推荐数量", ge=1, le=20)


TrainRequest = GATTrainRequest


# PPO 算法相关模型
class DispatchSingleRequest(BaseModel):
    """单个预约调度请求"""
    reserve_id: int = Field(..., description="预约ID", gt=0)


class DispatchBatchRequest(BaseModel):
    """批量预约调度请求"""
    reserve_ids: List[int] = Field(..., description="预约ID列表", min_length=1)


class DispatchResponse(BaseModel):
    """调度响应"""
    code: int = 200
    message: str = "success"
    data: Union[Dict[str, Any], List[Any]]


# PPO 训练参数模型
class PPOTrainRequest(BaseModel):
    """PPO训练请求"""
    force_reload: Optional[bool] = Field(False, description="是否强制重新加载数据")
    hidden_size: Optional[int] = Field(None, description="隐藏层大小", ge=16, le=128)
    lr: Optional[float] = Field(None, description="学习率", ge=0.0001, le=0.01)
    gamma: Optional[float] = Field(None, description="折扣因子", ge=0.8, le=0.999)
    clip_epsilon: Optional[float] = Field(None, description="裁剪系数", ge=0.1, le=0.3)
    epochs: Optional[int] = Field(None, description="训练轮数", ge=5, le=100)
    max_steps: Optional[int] = Field(None, description="最大步数", ge=50, le=500)
    batch_size: Optional[int] = Field(None, description="批次大小", ge=8, le=128)


class TrainResponse(BaseModel):
    """训练响应"""
    code: int = 200
    message: str = "success"
    data: Dict[str, Any]


class StatusResponse(BaseModel):
    """状态响应"""
    code: int = 200
    message: str = "success"
    data: Dict[str, Any]


class ErrorResponse(BaseModel):
    """错误响应"""
    code: int
    message: str
    detail: Optional[str] = None
