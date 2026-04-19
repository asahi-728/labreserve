"""
GATv2 API 模块
"""
from .main import app, create_app
from .schemas import (
    HealthResponse,
    RecommendRequest, RecommendResponse, RecommendationItem,
    EmbeddingRequest, EmbeddingResponse,
    TrainRequest, TrainResponse,
    StatusResponse,
    BatchRecommendRequest, BatchRecommendResponse,
    ErrorResponse
)

__all__ = [
    'app',
    'create_app',
    'HealthResponse',
    'RecommendRequest',
    'RecommendResponse',
    'RecommendationItem',
    'EmbeddingRequest',
    'EmbeddingResponse',
    'TrainRequest',
    'TrainResponse',
    'StatusResponse',
    'BatchRecommendRequest',
    'BatchRecommendResponse',
    'ErrorResponse'
]
