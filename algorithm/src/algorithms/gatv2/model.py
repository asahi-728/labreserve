"""
GATv2 模型定义模块
实现基于图注意力网络的个性化推荐模型
"""

import torch
import torch.nn.functional as F
from torch_geometric.nn import GATv2Conv
from typing import Tuple

from ...utils import config, logger


class GATv2Model(torch.nn.Module):
    """GATv2 图注意力网络模型"""
    
    def __init__(
        self,
        in_channels: int = None,
        hidden_channels: int = None,
        out_channels: int = None,
        heads: int = 1,
        dropout: float = 0.0
    ):
        """
        初始化GATv2模型
        
        Args:
            in_channels: 输入特征维度，默认为配置中的值
            hidden_channels: 隐藏层维度，默认为配置中的值
            out_channels: 输出嵌入维度，默认为配置中的值
            heads: 注意力头数
            dropout: Dropout率
        """
        super().__init__()
        
        self.in_channels = in_channels or config.model_in_channels
        self.hidden_channels = hidden_channels or config.model_hidden_channels
        self.out_channels = out_channels or config.model_out_channels
        self.heads = heads
        self.dropout = dropout
        
        # 第一层GATv2卷积
        self.conv1 = GATv2Conv(
            in_channels=self.in_channels,
            out_channels=self.hidden_channels,
            heads=heads,
            dropout=dropout,
            add_self_loops=True
        )
        
        # 第二层GATv2卷积
        self.conv2 = GATv2Conv(
            in_channels=self.hidden_channels * heads,
            out_channels=self.out_channels,
            heads=1,
            dropout=dropout,
            concat=False,
            add_self_loops=True
        )
        
        # 特征重构层（用于自监督学习）
        self.recon_layer = torch.nn.Linear(self.out_channels, self.in_channels)
        
        logger.info(f"GATv2模型初始化完成: in={self.in_channels}, hidden={self.hidden_channels}, out={self.out_channels}, heads={heads}")
    
    def forward(self, x: torch.Tensor, edge_index: torch.Tensor) -> torch.Tensor:
        """
        前向传播
        
        Args:
            x: 节点特征张量 [num_nodes, in_channels]
            edge_index: 边索引张量 [2, num_edges]
        
        Returns:
            torch.Tensor: 节点嵌入向量 [num_nodes, out_channels]
        """
        # 第一层图注意力卷积
        x = self.conv1(x, edge_index)
        x = F.elu(x)
        x = F.dropout(x, p=self.dropout, training=self.training)
        
        # 第二层图注意力卷积
        x = self.conv2(x, edge_index)
        
        return x
    
    def encode(self, x: torch.Tensor, edge_index: torch.Tensor) -> torch.Tensor:
        """
        编码节点特征（推理模式）
        
        Args:
            x: 节点特征张量
            edge_index: 边索引张量
        
        Returns:
            torch.Tensor: 节点嵌入向量
        """
        self.eval()
        with torch.no_grad():
            return self.forward(x, edge_index)
    
    def get_reconstruction(self, x: torch.Tensor, edge_index: torch.Tensor) -> torch.Tensor:
        """
        获取特征重构（用于自监督学习）
        
        Args:
            x: 节点特征张量
            edge_index: 边索引张量
        
        Returns:
            torch.Tensor: 重构的节点特征
        """
        embeddings = self.forward(x, edge_index)
        recon_x = self.recon_layer(embeddings)
        return recon_x
    
    def get_num_parameters(self) -> int:
        """
        获取模型参数量
        
        Returns:
            int: 模型参数量
        """
        return sum(p.numel() for p in self.parameters())
    
    def get_attention_weights(self, x: torch.Tensor, edge_index: torch.Tensor) -> Tuple[torch.Tensor, torch.Tensor]:
        """
        获取注意力权重（用于可视化）
        
        Args:
            x: 节点特征张量
            edge_index: 边索引张量
        
        Returns:
            Tuple[torch.Tensor, torch.Tensor]: (第一层注意力权重, 第二层注意力权重)
        """
        self.eval()
        with torch.no_grad():
            # 获取第一层注意力
            _, attn1 = self.conv1(x, edge_index, return_attention_weights=True)
            
            # 获取第二层注意力
            x1 = F.elu(self.conv1(x, edge_index))
            _, attn2 = self.conv2(x1, edge_index, return_attention_weights=True)
            
            return attn1[1], attn2[1]


class GATv2Trainer:
    """GATv2 模型训练器"""

    def __init__(self, model: GATv2Model, device: str = 'cpu', lr: float = None):
        """
        初始化训练器

        Args:
            model: GATv2模型
            device: 训练设备 ('cpu' 或 'cuda')
            lr: 学习率，默认为配置中的值
        """
        self.model = model
        self.device = torch.device(device if torch.cuda.is_available() else 'cpu')
        self.model.to(self.device)

        self.optimizer = torch.optim.Adam(
            model.parameters(),
            lr=lr if lr is not None else config.model_lr
        )

        self.best_loss = float('inf')
        self.best_model_state = None

        self.progress_callback = None

        logger.info(f"训练器初始化完成，使用设备: {self.device}, lr: {lr or config.model_lr}")
    
    def set_progress_callback(self, callback):
        """
        设置训练进度回调函数
        
        Args:
            callback: 回调函数，接收 (current_epoch, total_epochs, metrics_dict)
                       metrics_dict 包含本轮的 loss 等指标
        """
        self.progress_callback = callback
    
    def train_epoch(self, x: torch.Tensor, edge_index: torch.Tensor) -> tuple:
        """
        训练一个epoch
        
        Args:
            x: 节点特征张量
            edge_index: 边索引张量
        
        Returns:
            tuple: (本epoch的平均损失, 本epoch的重构准确率)
        """
        self.model.train()
        
        # 将数据移到设备
        x = x.to(self.device)
        edge_index = edge_index.to(self.device)
        
        self.optimizer.zero_grad()
        
        # 前向传播：特征重构
        recon_x = self.model.get_reconstruction(x, edge_index)
        
        # 计算损失：重构损失
        loss = F.mse_loss(recon_x, x)
        
        # 反向传播
        loss.backward()
        self.optimizer.step()
        
        # 计算重构准确率：基于余弦相似度，范围 [0, 1]
        with torch.no_grad():
            # 将 recon_x 和 x 展平为 (N*D,) 向量计算全局相似度
            x_flat = x.reshape(-1)
            r_flat = recon_x.reshape(-1)
            cos_sim = F.cosine_similarity(x_flat.unsqueeze(0), r_flat.unsqueeze(0)).item()
            # 映射到 [0, 1] 范围作为准确率指标
            accuracy = max(0.0, min(1.0, (cos_sim + 1.0) / 2.0))
        
        return loss.item(), accuracy
    
    def train(self, x: torch.Tensor, edge_index: torch.Tensor, epochs: int = None) -> dict:
        """
        训练模型
        
        Args:
            x: 节点特征张量
            edge_index: 边索引张量
            epochs: 训练轮数，默认为配置中的值
        
        Returns:
            dict: 训练历史信息
        """
        epochs = epochs or config.model_epochs
        
        logger.info(f"开始训练GATv2模型，总轮数: {epochs}")
        
        train_history = {
            'losses': [],
            'best_epoch': 0,
            'best_loss': float('inf')
        }
        
        train_history = {
            'losses': [],
            'accuracies': [],
            'best_epoch': 0,
            'best_loss': float('inf')
        }
        
        for epoch in range(epochs):
            loss, acc = self.train_epoch(x, edge_index)
            train_history['losses'].append(loss)
            train_history['accuracies'].append(acc)
            
            # 保存最佳模型
            if loss < self.best_loss:
                self.best_loss = loss
                self.best_model_state = {
                    k: v.cpu().clone() for k, v in self.model.state_dict().items()
                }
                train_history['best_epoch'] = epoch + 1
                train_history['best_loss'] = loss
            
            # 更新进度回调（附带每轮训练指标）
            if self.progress_callback:
                self.progress_callback(epoch + 1, epochs, {'loss': loss, 'accuracy': acc})
            
            # 定期输出日志
            if (epoch + 1) % 10 == 0:
                logger.info(f"Epoch {epoch+1}/{epochs}, Loss: {loss:.6f}, Acc: {acc:.4f} (Best: {self.best_loss:.6f} @ Epoch {train_history['best_epoch']})")
        
        # 加载最佳模型
        if self.best_model_state is not None:
            self.model.load_state_dict(self.best_model_state)
        
        logger.info(f"训练完成！最佳损失: {self.best_loss:.6f}, 最终准确率: {train_history['accuracies'][-1]:.4f} @ Epoch {train_history['best_epoch']}")
        
        return train_history
    
    def save_model(self, filepath: str = None) -> str:
        """
        保存模型
        
        Args:
            filepath: 保存路径，默认为配置中的路径
        
        Returns:
            str: 实际保存的文件路径
        """
        if filepath is None:
            filepath = str(config.model_dir / "gatv2_model.pth")
        
        torch.save({
            'model_state_dict': self.model.state_dict(),
            'optimizer_state_dict': self.optimizer.state_dict(),
            'best_loss': self.best_loss,
            'model_config': {
                'in_channels': self.model.in_channels,
                'hidden_channels': self.model.hidden_channels,
                'out_channels': self.model.out_channels,
                'heads': self.model.heads,
                'dropout': self.model.dropout
            }
        }, filepath)
        
        logger.info(f"模型已保存到: {filepath}")
        return filepath
    
    @classmethod
    def load_model(cls, filepath: str = None, device: str = 'cpu') -> Tuple['GATv2Trainer', dict]:
        """
        加载模型
        
        Args:
            filepath: 模型文件路径
            device: 加载设备
        
        Returns:
            Tuple[GATv2Trainer, dict]: (训练器, 模型配置)
        """
        if filepath is None:
            filepath = str(config.model_dir / "gatv2_model.pth")
        
        checkpoint = torch.load(filepath, map_location=device)
        
        model_config = checkpoint['model_config']
        model = GATv2Model(**model_config)
        model.load_state_dict(checkpoint['model_state_dict'])
        
        trainer = cls(model, device=device)
        trainer.best_loss = checkpoint['best_loss']
        
        logger.info(f"模型已从 {filepath} 加载")
        
        return trainer, model_config
