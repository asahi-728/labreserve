"""
GATv2 模型单元测试
"""

import sys
from pathlib import Path
import pytest
import torch
import numpy as np

# 添加项目根目录到路径
project_root = Path(__file__).parent.parent
sys.path.insert(0, str(project_root))

from src.algorithms.gatv2.model import GATv2Model, GATv2Trainer


class TestGATv2Model:
    """测试GATv2模型"""
    
    def test_model_initialization(self):
        """测试模型初始化"""
        model = GATv2Model(in_channels=8, hidden_channels=16, out_channels=4)
        
        assert model is not None
        assert model.in_channels == 8
        assert model.hidden_channels == 16
        assert model.out_channels == 4
    
    def test_forward_pass(self):
        """测试前向传播"""
        model = GATv2Model(in_channels=8, hidden_channels=16, out_channels=4)
        
        # 创建测试数据
        num_nodes = 10
        x = torch.randn(num_nodes, 8)
        
        # 创建简单的边索引
        edge_index = torch.tensor([[0, 1, 2, 3], [1, 0, 3, 2]], dtype=torch.long)
        
        # 前向传播
        output = model(x, edge_index)
        
        # 验证输出形状
        assert output.shape == (num_nodes, 4)
    
    def test_encode(self):
        """测试编码函数"""
        model = GATv2Model(in_channels=8, hidden_channels=16, out_channels=4)
        
        num_nodes = 10
        x = torch.randn(num_nodes, 8)
        edge_index = torch.tensor([[0, 1], [1, 0]], dtype=torch.long)
        
        # 编码
        embeddings = model.encode(x, edge_index)
        
        assert embeddings.shape == (num_nodes, 4)
        assert not embeddings.requires_grad  # 应该不需要梯度
    
    def test_get_num_parameters(self):
        """测试获取参数量"""
        model = GATv2Model(in_channels=8, hidden_channels=16, out_channels=4)
        
        num_params = model.get_num_parameters()
        
        assert isinstance(num_params, int)
        assert num_params > 0


class TestGATv2Trainer:
    """测试GATv2训练器"""
    
    def test_trainer_initialization(self):
        """测试训练器初始化"""
        model = GATv2Model(in_channels=8, hidden_channels=16, out_channels=4)
        trainer = GATv2Trainer(model)
        
        assert trainer is not None
        assert trainer.model is model
    
    def test_train_epoch(self):
        """测试单个epoch训练"""
        model = GATv2Model(in_channels=8, hidden_channels=16, out_channels=4)
        trainer = GATv2Trainer(model)
        
        # 创建测试数据
        num_nodes = 20
        x = torch.randn(num_nodes, 8)
        
        # 创建随机边索引
        edge_list = []
        for i in range(num_nodes):
            for j in range(i+1, num_nodes):
                if np.random.rand() < 0.1:
                    edge_list.append([i, j])
                    edge_list.append([j, i])
        
        edge_index = torch.tensor(edge_list, dtype=torch.long).t() if edge_list else torch.empty((2, 0), dtype=torch.long)
        
        # 训练一个epoch
        loss = trainer.train_epoch(x, edge_index)
        
        assert isinstance(loss, float)
        assert not np.isnan(loss)
    
    def test_train(self):
        """测试完整训练"""
        model = GATv2Model(in_channels=8, hidden_channels=16, out_channels=4)
        trainer = GATv2Trainer(model)
        
        # 创建测试数据
        num_nodes = 20
        x = torch.randn(num_nodes, 8)
        
        # 创建简单的边索引
        edge_index = torch.tensor([[i, (i+1) % num_nodes] for i in range(num_nodes)], dtype=torch.long).t()
        
        # 训练
        history = trainer.train(x, edge_index, epochs=5)
        
        assert 'losses' in history
        assert len(history['losses']) == 5
        assert 'best_epoch' in history
        assert 'best_loss' in history
    
    def test_save_and_load_model(self, test_model_dir):
        """测试模型保存和加载"""
        model = GATv2Model(in_channels=8, hidden_channels=16, out_channels=4)
        trainer = GATv2Trainer(model)
        
        # 先简单训练一下
        num_nodes = 10
        x = torch.randn(num_nodes, 8)
        edge_index = torch.tensor([[0, 1], [1, 0]], dtype=torch.long)
        trainer.train(x, edge_index, epochs=2)
        
        # 保存模型
        model_path = test_model_dir / "test_model.pth"
        trainer.save_model(str(model_path))
        
        assert model_path.exists()
        
        # 加载模型
        loaded_trainer, model_config = GATv2Trainer.load_model(str(model_path))
        
        assert loaded_trainer is not None
        assert loaded_trainer.model is not None
        assert model_config['in_channels'] == 8
        assert model_config['hidden_channels'] == 16
        assert model_config['out_channels'] == 4
