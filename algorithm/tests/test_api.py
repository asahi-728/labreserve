"""
GATv2 API 集成测试
"""

import sys
from pathlib import Path
import pytest
from unittest.mock import MagicMock, patch

# 添加项目根目录到路径
project_root = Path(__file__).parent.parent
sys.path.insert(0, str(project_root))

from fastapi.testclient import TestClient
from src.api.main import create_app


class TestAPI:
    """测试API接口"""
    
    @pytest.fixture
    def client(self):
        """创建测试客户端"""
        app = create_app()
        return TestClient(app)
    
    def test_health_check(self, client):
        """测试健康检查接口"""
        response = client.get("/api/v1/health")
        
        assert response.status_code == 200
        data = response.json()
        assert data['status'] == 'ok'
        assert 'service' in data
        assert 'timestamp' in data
        assert 'version' in data
    
    def test_root_endpoint(self, client):
        """测试根路径"""
        response = client.get("/")
        
        assert response.status_code == 200
        data = response.json()
        assert 'service' in data
        assert 'version' in data
        assert 'status' in data
        assert 'docs' in data
    
    def test_status_endpoint(self, client):
        """测试状态接口"""
        response = client.get("/api/v1/status")
        
        assert response.status_code == 200
        data = response.json()
        assert data['code'] == 200
        assert 'data' in data
        assert 'is_initialized' in data['data']
        assert 'model_config' in data['data']
    
    @patch('src.api.routes.recommender')
    def test_recommend_endpoint(self, mock_recommender, client):
        """测试推荐接口"""
        # 设置mock
        mock_recommender.is_initialized = True
        mock_recommender.recommend.return_value = [
            {
                'user_id': 1,
                'device_id': 10,
                'similarity': 0.85,
                'rank': 1,
                'recommend_time': '2026-04-15 12:00:00'
            }
        ]
        mock_recommender.save_recommendations_to_db = MagicMock()
        
        # 发送请求
        response = client.post(
            "/api/v1/recommend",
            json={
                'user_id': 1,
                'top_k': 5,
                'save_to_db': False
            }
        )
        
        assert response.status_code == 200
        data = response.json()
        assert data['code'] == 200
        assert 'data' in data
        assert len(data['data']) == 1
    
    @patch('src.api.routes.recommender')
    def test_embedding_endpoint(self, mock_recommender, client):
        """测试嵌入向量接口"""
        # 设置mock
        mock_recommender.is_initialized = True
        mock_recommender.get_user_embedding.return_value = [0.1, 0.2, 0.3, 0.4]
        
        # 发送请求
        response = client.post(
            "/api/v1/embedding",
            json={'user_id': 1}
        )
        
        assert response.status_code == 200
        data = response.json()
        assert data['code'] == 200
        assert 'data' in data
        assert 'user_embedding' in data['data']
    
    @patch('src.api.routes.recommender')
    def test_train_endpoint(self, mock_recommender, client):
        """测试训练接口"""
        # 发送请求
        response = client.post(
            "/api/v1/train",
            json={
                'force_reload': False,
                'epochs': 10
            }
        )
        
        assert response.status_code == 200
        data = response.json()
        assert data['code'] == 200
        assert 'data' in data
        assert data['data']['status'] == 'training_started'
    
    def test_training_status_endpoint(self, client):
        """测试训练状态接口"""
        response = client.get("/api/v1/train/status")
        
        assert response.status_code == 200
        data = response.json()
        assert data['code'] == 200
        assert 'data' in data
        assert 'is_training' in data['data']
    
    def test_invalid_recommend_request(self, client):
        """测试无效的推荐请求"""
        response = client.post(
            "/api/v1/recommend",
            json={'user_id': 0, 'top_k': 5}  # user_id应该大于0
        )
        
        assert response.status_code == 422  # 验证失败
    
    def test_embedding_without_id(self, client):
        """测试没有提供ID的嵌入请求"""
        response = client.post(
            "/api/v1/embedding",
            json={}  # 缺少user_id或device_id
        )
        
        assert response.status_code == 422  # 验证失败
