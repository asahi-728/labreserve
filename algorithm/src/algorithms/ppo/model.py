"""
PPO 模型定义模块
实现基于近端策略优化的设备调度模型
"""

import torch
import torch.nn as nn
import torch.optim as optim
import torch.nn.functional as F
from typing import Dict, List, Tuple, Optional
from collections import deque
import numpy as np
import random

from ...utils import config, logger


class ActorCriticNetwork(nn.Module):
    """Actor-Critic 网络"""
    
    def __init__(
        self,
        state_dim: int,
        action_dim: int,
        hidden_dims: List[int] = [128, 64],
        activation: nn.Module = nn.ReLU()
    ):
        """
        初始化Actor-Critic网络
        
        Args:
            state_dim: 状态维度
            action_dim: 动作维度
            hidden_dims: 隐藏层维度列表
            activation: 激活函数
        """
        super().__init__()
        
        self.state_dim = state_dim
        self.action_dim = action_dim
        
        # 构建共享层
        layers = []
        input_dim = state_dim
        
        for hidden_dim in hidden_dims:
            layers.append(nn.Linear(input_dim, hidden_dim))
            layers.append(activation)
            layers.append(nn.LayerNorm(hidden_dim))
            input_dim = hidden_dim
        
        self.shared_layers = nn.Sequential(*layers)
        
        # Actor 头 (策略网络)
        self.actor_head = nn.Sequential(
            nn.Linear(hidden_dims[-1], action_dim),
            nn.Softmax(dim=-1)
        )
        
        # Critic 头 (价值网络)
        self.critic_head = nn.Linear(hidden_dims[-1], 1)
        
        logger.info(f"Actor-Critic网络初始化完成: state_dim={state_dim}, action_dim={action_dim}")
    
    def forward(self, state: torch.Tensor) -> Tuple[torch.Tensor, torch.Tensor]:
        """
        前向传播
        
        Args:
            state: 状态张量 [batch_size, state_dim]
        
        Returns:
            Tuple[torch.Tensor, torch.Tensor]: (动作概率, 状态价值)
        """
        shared_features = self.shared_layers(state)
        action_probs = self.actor_head(shared_features)
        state_value = self.critic_head(shared_features)
        
        return action_probs, state_value
    
    def get_action(self, state: torch.Tensor, deterministic: bool = False) -> Tuple[int, torch.Tensor, torch.Tensor]:
        """
        获取动作
        
        Args:
            state: 状态张量
            deterministic: 是否确定性选择动作
        
        Returns:
            Tuple[int, torch.Tensor, torch.Tensor]: (动作, 动作概率, 状态价值)
        """
        self.eval()
        with torch.no_grad():
            action_probs, state_value = self.forward(state.unsqueeze(0))
            
            if deterministic:
                action = torch.argmax(action_probs).item()
            else:
                dist = torch.distributions.Categorical(action_probs)
                action = dist.sample().item()
            
            action_prob = action_probs[0, action]
        
        return action, action_prob, state_value[0]


class PPOTrainer:
    """PPO 训练器"""
    
    def __init__(
        self,
        model: ActorCriticNetwork,
        device: str = 'cpu',
        lr: float = 3e-4,
        gamma: float = 0.99,
        gae_lambda: float = 0.95,
        clip_ratio: float = 0.2,
        value_coef: float = 0.5,
        entropy_coef: float = 0.01,
        max_grad_norm: float = 0.5,
        batch_size: int = 64,
        update_epochs: int = 10
    ):
        """
        初始化PPO训练器
        
        Args:
            model: Actor-Critic模型
            device: 训练设备
            lr: 学习率
            gamma: 折扣因子
            gae_lambda: GAE lambda
            clip_ratio: PPO裁剪比例
            value_coef: 价值损失系数
            entropy_coef: 熵损失系数
            max_grad_norm: 最大梯度范数
            batch_size: 批次大小
            update_epochs: 更新轮数
        """
        self.model = model
        self.device = torch.device(device if torch.cuda.is_available() else 'cpu')
        self.model.to(self.device)
        
        self.optimizer = optim.Adam(model.parameters(), lr=lr)
        self.gamma = gamma
        self.gae_lambda = gae_lambda
        self.clip_ratio = clip_ratio
        self.value_coef = value_coef
        self.entropy_coef = entropy_coef
        self.max_grad_norm = max_grad_norm
        self.batch_size = batch_size
        self.update_epochs = update_epochs
        
        # 经验回放池
        self.buffer = {
            'states': [],
            'actions': [],
            'action_probs': [],
            'rewards': [],
            'next_states': [],
            'dones': [],
            'values': []
        }
        
        # 训练统计
        self.total_steps = 0
        self.episode_rewards = []
        
        logger.info(f"PPO训练器初始化完成: device={self.device}, lr={lr}")
    
    def store_transition(
        self,
        state: np.ndarray,
        action: int,
        action_prob: float,
        reward: float,
        next_state: np.ndarray,
        done: bool,
        value: float
    ):
        """
        存储转换到经验池
        
        Args:
            state: 当前状态
            action: 执行的动作
            action_prob: 动作概率
            reward: 获得的奖励
            next_state: 下一个状态
            done: 是否结束
            value: 状态价值
        """
        self.buffer['states'].append(state)
        self.buffer['actions'].append(action)
        self.buffer['action_probs'].append(action_prob)
        self.buffer['rewards'].append(reward)
        self.buffer['next_states'].append(next_state)
        self.buffer['dones'].append(done)
        self.buffer['values'].append(value)

        # 递增总训练步数
        self.total_steps += 1
    
    def compute_advantages_and_returns(self) -> Tuple[np.ndarray, np.ndarray]:
        """
        计算优势函数和回报
        
        Returns:
            Tuple[np.ndarray, np.ndarray]: (优势函数, 回报)
        """
        rewards = np.array(self.buffer['rewards'])
        values = np.array(self.buffer['values'])
        dones = np.array(self.buffer['dones'])
        
        next_values = np.concatenate([values[1:], [0.0]])
        
        # 计算TD误差
        deltas = rewards + self.gamma * next_values * (1 - dones) - values
        
        # 计算GAE优势
        advantages = np.zeros_like(rewards)
        last_advantage = 0.0
        
        for t in reversed(range(len(rewards))):
            advantages[t] = deltas[t] + self.gamma * self.gae_lambda * (1 - dones[t]) * last_advantage
            last_advantage = advantages[t]
        
        # 计算回报
        returns = advantages + values
        
        # 归一化优势
        advantages = (advantages - advantages.mean()) / (advantages.std() + 1e-8)
        
        return advantages, returns
    
    def update(self) -> Dict[str, float]:
        """
        更新策略网络
        
        Returns:
            Dict[str, float]: 损失统计
        """
        if len(self.buffer['states']) == 0:
            return {}
        
        # 计算优势和回报
        advantages, returns = self.compute_advantages_and_returns()
        
        # 转换为张量
        states = torch.tensor(np.array(self.buffer['states']), dtype=torch.float32).to(self.device)
        actions = torch.tensor(np.array(self.buffer['actions']), dtype=torch.long).to(self.device)
        old_action_probs = torch.tensor(np.array(self.buffer['action_probs']), dtype=torch.float32).to(self.device)
        advantages = torch.tensor(advantages, dtype=torch.float32).to(self.device)
        returns = torch.tensor(returns, dtype=torch.float32).to(self.device)
        
        total_loss = 0.0
        total_policy_loss = 0.0
        total_value_loss = 0.0
        total_entropy_loss = 0.0
        num_updates = 0
        
        # 多轮更新
        for epoch in range(self.update_epochs):
            # 打乱数据
            indices = torch.randperm(len(states))
            
            # 分批更新
            for start in range(0, len(states), self.batch_size):
                end = start + self.batch_size
                batch_indices = indices[start:end]
                
                batch_states = states[batch_indices]
                batch_actions = actions[batch_indices]
                batch_old_probs = old_action_probs[batch_indices]
                batch_advantages = advantages[batch_indices]
                batch_returns = returns[batch_indices]
                
                # 前向传播
                self.model.train()
                action_probs, state_values = self.model(batch_states)
                
                # 获取当前动作概率
                current_probs = action_probs.gather(1, batch_actions.unsqueeze(1)).squeeze(1)
                
                # 计算概率比
                ratio = current_probs / (batch_old_probs + 1e-8)
                
                # 计算裁剪的策略损失
                surr1 = ratio * batch_advantages
                surr2 = torch.clamp(ratio, 1 - self.clip_ratio, 1 + self.clip_ratio) * batch_advantages
                policy_loss = -torch.min(surr1, surr2).mean()
                
                # 计算价值损失
                value_loss = F.mse_loss(state_values.squeeze(), batch_returns)
                
                # 计算熵损失
                dist = torch.distributions.Categorical(action_probs)
                entropy_loss = -dist.entropy().mean()
                
                # 总损失
                loss = policy_loss + self.value_coef * value_loss + self.entropy_coef * entropy_loss
                
                # 反向传播
                self.optimizer.zero_grad()
                loss.backward()
                torch.nn.utils.clip_grad_norm_(self.model.parameters(), self.max_grad_norm)
                self.optimizer.step()
                
                total_loss += loss.item()
                total_policy_loss += policy_loss.item()
                total_value_loss += value_loss.item()
                total_entropy_loss += entropy_loss.item()
                num_updates += 1
        
        # 清空缓冲区
        self._clear_buffer()
        
        # 返回平均损失
        avg_loss = total_loss / num_updates if num_updates > 0 else 0.0
        avg_policy_loss = total_policy_loss / num_updates if num_updates > 0 else 0.0
        avg_value_loss = total_value_loss / num_updates if num_updates > 0 else 0.0
        avg_entropy_loss = total_entropy_loss / num_updates if num_updates > 0 else 0.0
        
        return {
            'loss': avg_loss,
            'policy_loss': avg_policy_loss,
            'value_loss': avg_value_loss,
            'entropy_loss': avg_entropy_loss
        }
    
    def _clear_buffer(self):
        """清空经验缓冲区"""
        for key in self.buffer:
            self.buffer[key].clear()
    
    def save_model(self, filepath: str = None) -> str:
        """
        保存模型
        
        Args:
            filepath: 保存路径
        
        Returns:
            str: 保存的文件路径
        """
        if filepath is None:
            filepath = str(config.model_dir / "ppo_model.pth")
        
        torch.save({
            'model_state_dict': self.model.state_dict(),
            'optimizer_state_dict': self.optimizer.state_dict(),
            'total_steps': self.total_steps,
            'episode_rewards': self.episode_rewards,
            'config': {
                'state_dim': self.model.state_dim,
                'action_dim': self.model.action_dim,
                'hidden_dims': [layer.out_features for layer in self.model.shared_layers if isinstance(layer, nn.Linear)],
                'gamma': self.gamma,
                'clip_ratio': self.clip_ratio
            }
        }, filepath)
        
        logger.info(f"PPO模型已保存到: {filepath}")
        return filepath
    
    @classmethod
    def load_model(cls, filepath: str = None, device: str = 'cpu') -> 'PPOTrainer':
        """
        加载模型
        
        Args:
            filepath: 模型文件路径
            device: 加载设备
        
        Returns:
            PPOTrainer: 加载的训练器
        """
        if filepath is None:
            filepath = str(config.model_dir / "ppo_model.pth")
        
        checkpoint = torch.load(filepath, map_location=device, weights_only=False)
        model_config = checkpoint['config']
        
        # 从 checkpoint 恢复 hidden_dims，确保网络结构一致
        hidden_dims = model_config.get('hidden_dims')
        if hidden_dims is not None:
            # 兼容旧格式：如果保存的是单个 int（第一层维度），推导完整 hidden_dims
            if isinstance(hidden_dims, int):
                hidden_dims = [hidden_dims, max(hidden_dims // 2, 16)]
        else:
            # 完全没有 hidden_dims 信息，使用默认值
            hidden_dims = [128, 64]
        
        model = ActorCriticNetwork(
            state_dim=model_config['state_dim'],
            action_dim=model_config['action_dim'],
            hidden_dims=hidden_dims
        )
        
        trainer = cls(model, device=device)
        trainer.model.load_state_dict(checkpoint['model_state_dict'])
        trainer.optimizer.load_state_dict(checkpoint['optimizer_state_dict'])
        trainer.total_steps = checkpoint['total_steps']
        trainer.episode_rewards = checkpoint['episode_rewards']
        
        logger.info(f"PPO模型已从 {filepath} 加载")
        return trainer
