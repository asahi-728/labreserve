"""
PPO 智能调度器模块
实现GATv2+PPO联合调度

核心改进：
- 训练时使用已完成预约(status=1/2/5)作为有标签数据
- 奖励基于"动作与历史真实审批结果的一致性"
- 动作空间简化为2维: 批准(0) / 拒绝(1)
- 推理时对待审批预约(status=0)进行决策
"""

import numpy as np
import torch
from typing import List, Dict, Any, Optional, Tuple
from datetime import datetime

from ...utils import config, logger, db
from .environment import ReservationEnvironment, Action, State
from .model import ActorCriticNetwork, PPOTrainer
from ..gatv2 import GATv2Recommender, recommender


class PPODispatcher:
    """PPO智能调度器"""

    def __init__(self):
        self.env = ReservationEnvironment()
        self.model: Optional[ActorCriticNetwork] = None
        self.trainer: Optional[PPOTrainer] = None
        self.gat_recommender: Optional[GATv2Recommender] = None
        self.is_initialized = False
        self.is_training = False

        # 调度结果缓存
        self.dispatch_results = []

        # 训练进度回调
        self._progress_callback = None

        # 当前训练参数
        self._current_train_params = {}

        logger.info("PPO智能调度器初始化完成 (动作空间=2: 批准/拒绝)")

    def set_progress_callback(self, callback):
        """
        设置训练进度回调函数
        
        Args:
            callback: 回调函数，接收 (current_episode, total_episodes, metrics_dict)
                       metrics_dict 包含本轮的 reward, loss, accuracy 等指标
        """
        self._progress_callback = callback

    def initialize(self, force_reload: bool = False,
                   hidden_size: int = 64,
                   lr: float = 3e-4,
                   gamma: float = 0.99,
                   clip_epsilon: float = 0.2,
                   batch_size: int = 64,
                   update_epochs: int = 10) -> bool:
        """
        初始化调度器

        Returns:
            bool: 初始化是否成功
        """
        logger.info(f"正在初始化PPO智能调度器... (hidden_size={hidden_size}, lr={lr})")

        self._current_train_params = {
            'hidden_size': hidden_size,
            'lr': lr,
            'gamma': gamma,
            'clip_epsilon': clip_epsilon,
            'batch_size': batch_size,
            'update_epochs': update_epochs
        }

        try:
            # 1. 初始化环境（默认用推理模式，加载待审批数据）
            self.env.reset(force_reload=force_reload, training_mode=False)

            # 2. 初始化GAT推荐器
            self.gat_recommender = recommender
            if not self.gat_recommender.is_initialized:
                self.gat_recommender.initialize(force_reload=force_reload)

            # 3. 初始化PPO模型
            state_dim = self.env.observation_space
            if self.gat_recommender.is_initialized:
                state_dim += config.model_out_channels

            action_dim = self.env.action_space  # 现在是 2（批准/拒绝）

            model_path = config.model_dir / "ppo_model.pth"
            if model_path.exists() and not force_reload:
                logger.info("加载已训练的PPO模型...")
                self.trainer = PPOTrainer.load_model(str(model_path))
                self.model = self.trainer.model
                # 验证模型action_dim是否匹配
                if self.model.action_dim != action_dim:
                    logger.warning(f"模型action_dim({self.model.action_dim})不匹配当前({action_dim}), 重新创建")
                    raise ValueError("action_dim mismatch")
            else:
                logger.info(f"创建新的PPO模型... (state_dim={state_dim}, action_dim={action_dim})")
                self.model = ActorCriticNetwork(
                    state_dim=state_dim,
                    hidden_dims=[hidden_size, hidden_size // 2],
                    action_dim=action_dim
                )
                self.trainer = PPOTrainer(
                    model=self.model,
                    lr=lr,
                    gamma=gamma,
                    clip_ratio=clip_epsilon,
                    batch_size=batch_size,
                    update_epochs=update_epochs
                )

            self.is_initialized = True
            logger.info("PPO智能调度器初始化成功！")
            return True

        except Exception as e:
            logger.exception(f"PPO智能调度器初始化失败: {str(e)}")
            return False

    def dispatch_single(self, reserve_id: int) -> Dict[str, Any]:
        """
        单个预约调度（推理模式）

        使用训练好的PPO模型对待审批预约进行自动决策
        """
        if not self.is_initialized:
            success = self.initialize()
            if not success:
                raise RuntimeError("调度器初始化失败")

        logger.info(f"开始调度预约: {reserve_id}")

        # 查找请求索引
        request_idx = self._find_request_index(reserve_id)
        if request_idx == -1:
            # 尝试动态加载
            if not self._load_single_reservation(reserve_id):
                return {
                    'reserve_id': reserve_id,
                    'status': 'error',
                    'reason': '预约加载失败'
                }
            request_idx = self._find_request_index(reserve_id)
            if request_idx == -1:
                return {
                    'reserve_id': reserve_id,
                    'status': 'error',
                    'reason': '预约不在列表中'
                }

        # 模型未充分训练时降级为规则
        if self.model is None or self.trainer is None or self.trainer.total_steps < 10:
            logger.info("PPO模型未充分训练，使用简单规则进行审批")
            return self._simple_rule_approval(reserve_id, request_idx)

        # === PPO 模型推理 ===
        # 1. 获取GAT特征
        gat_features = self._get_gat_features(request_idx)
        if gat_features is not None:
            self.env.set_gat_features(gat_features)

        # 2. 构建观察向量
        observation = self.env.get_observation(request_idx)
        observation_tensor = torch.tensor(observation, dtype=torch.float32)

        # 3. 模型前向推理
        action_idx, action_prob, value = self.model.get_action(
            observation_tensor,
            deterministic=True
        )

        action_name = '批准' if action_idx == 0 else '拒绝'
        # 将 Tensor 转为 Python 标量后再格式化（Tensor 不支持 f-string 的 format spec）
        prob_val = action_prob.item() if hasattr(action_prob, 'item') else float(action_prob)
        val_val = value.item() if hasattr(value, 'item') else float(value)
        logger.info(f"PPO推理: reserve_id={reserve_id}, action={action_idx}({action_name}), "
                   f"prob={prob_val:.4f}, value={val_val:.4f}")

        # 4. 构建结果
        action = Action(action_type=action_idx)
        if action_idx == 0:
            action.device_id = self.env.request_info[request_idx].get('device_id')

        result = {
            'reserve_id': reserve_id,
            'request_idx': request_idx,
            'action': {
                'type': action_idx,
                'type_name': action_name,
                'device_id': action.device_id,
            },
            'confidence': float(action_prob),
            'value': float(value),
            'dispatch_time': datetime.now().strftime('%Y-%m-%d %H:%M:%S'),
            'status': 'approved' if action_idx == 0 else 'rejected'
        }

        self.dispatch_results.append(result)
        logger.info(f"预约 {reserve_id} 调度完成: {result['status']}")
        return result

    def _find_request_index(self, reserve_id: int) -> int:
        """查找预约在当前列表中的索引"""
        for idx, info in enumerate(self.env.request_info):
            if info['reserve_id'] == reserve_id:
                return idx
        return -1

    def _simple_rule_approval(self, reserve_id: int, request_idx: int) -> Dict[str, Any]:
        """使用简单规则进行审批（模型未充分训练时的降级方案）"""
        logger.info(f"使用简单规则审批预约 {reserve_id}")

        request_info = self.env.request_info[request_idx]
        device_id = request_info['device_id']
        credit_score = request_info.get('credit_score', 50)

        has_conflict = self.env.check_conflict(request_info, exclude_reserve_id=reserve_id)

        # 规则：有冲突→拒绝, 信用分>=60→批准, 否则→拒绝
        if has_conflict:
            status = 'rejected'
            reason = '预约时段存在冲突'
        elif credit_score >= 60:
            status = 'approved'
            reason = f'信用分良好({credit_score})，自动批准'
        else:
            status = 'rejected'
            reason = f'信用分较低({credit_score})，自动拒绝'

        logger.info(f"简单规则审批: {status}, 原因: {reason}")

        result = {
            'reserve_id': reserve_id,
            'request_idx': request_idx,
            'action': {
                'type': 0 if status == 'approved' else 1,
                'type_name': '批准' if status == 'approved' else '拒绝',
                'device_id': device_id if status == 'approved' else None,
            },
            'confidence': 1.0,
            'reason': reason,
            'dispatch_time': datetime.now().strftime('%Y-%m-%d %H:%M:%S'),
            'status': status
        }

        self.dispatch_results.append(result)
        return result

    def dispatch_batch(self, reserve_ids: Optional[List[int]] = None) -> List[Dict[str, Any]]:
        """批量预约调度"""
        if not self.is_initialized:
            success = self.initialize()
            if not success:
                raise RuntimeError("调度器初始化失败")

        if reserve_ids is None:
            reserve_ids = [info['reserve_id'] for info in self.env.request_info]

        logger.info(f"开始批量调度: {len(reserve_ids)} 个预约")

        results = []
        for reserve_id in reserve_ids:
            result = self.dispatch_single(reserve_id)
            results.append(result)

        logger.info(f"批量调度完成: {len(results)} 个预约")
        return results

    def train(self, num_episodes: int = 10, max_steps: int = 100) -> Dict[str, Any]:
        """
        训练PPO模型（核心改进版）

        改进点：
        1. 使用 training_mode=True 加载已完成预约（有真实标签）
        2. 奖励基于标签一致性，模型能学到真正的审批策略
        3. 每个 episode 随机打乱数据顺序，增强泛化能力
        """
        if self.is_training:
            return {'status': 'error', 'reason': '训练已在进行中'}

        self.is_training = True
        logger.info(f"开始PPO模型训练: {num_episodes} 轮, max_steps={max_steps}")

        try:
            episode_rewards = []
            episode_accuracies = []  # 新增：记录每轮准确率
            loss_history = []

            for episode in range(num_episodes):
                # === 以训练模式重置环境（加载已完成预约+标签）===
                state = self.env.reset(force_reload=(episode == 0), training_mode=True)

                # 检查是否有训练数据
                if self.env.request_count == 0:
                    logger.warning("没有已完成的预约数据用于训练！请先有一些审批通过的预约记录。")
                    # 用待审批数据代替（无标签，仅靠启发式奖励）
                    state = self.env.reset(force_reload=False, training_mode=False)

                episode_reward = 0.0
                step_count = 0

                # 生成随机遍历顺序（每个episode打乱，增强泛化）
                indices_order = np.random.permutation(self.env.request_count)

                for step in range(max_steps):
                    request_idx = int(indices_order[step % len(indices_order)])

                    # 获取GAT特征
                    gat_features = self._get_gat_features(request_idx)
                    if gat_features is not None:
                        self.env.set_gat_features(gat_features)

                    # 获取观察
                    observation = self.env.get_observation(request_idx)
                    observation_tensor = torch.tensor(observation, dtype=torch.float32)

                    # 模型采样动作（训练时用随机策略探索）
                    action_idx, action_prob, value = self.model.get_action(
                        observation_tensor,
                        deterministic=False
                    )

                    action = Action(action_type=action_idx)
                    if action_idx == 0 and request_idx < len(self.env.request_info):
                        action.device_id = self.env.request_info[request_idx].get('device_id')

                    # 执行动作，获取基于标签的奖励
                    next_state, reward, done, info = self.env.step(action, request_idx)

                    # 存储经验到回放池
                    next_idx = int(indices_order[(step + 1) % len(indices_order)])
                    next_observation = self.env.get_observation(next_idx)

                    self.trainer.store_transition(
                        state=observation,
                        action=action_idx,
                        action_prob=float(action_prob),
                        reward=float(reward),
                        next_state=next_observation,
                        done=done,
                        value=float(value)
                    )

                    episode_reward += reward
                    step_count += 1

                    if done:
                        break

                # PPO更新策略网络
                losses = self.trainer.update()

                episode_rewards.append(episode_reward)
                if losses:
                    loss_history.append(losses['loss'])

                # 记录准确率
                total = self.env.total_correct + self.env.total_wrong
                acc = self.env.total_correct / max(1, total)
                episode_accuracies.append(acc)

                # 更新进度回调（附带每轮训练指标）
                if self._progress_callback:
                    episode_metrics = {
                        'reward': round(float(episode_reward), 4),
                        'accuracy': round(float(acc), 6),
                    }
                    if losses:
                        episode_metrics['loss'] = round(float(losses['loss']), 6)
                        if 'policy_loss' in losses:
                            episode_metrics['policy_loss'] = round(float(losses['policy_loss']), 6)
                        if 'value_loss' in losses:
                            episode_metrics['value_loss'] = round(float(losses['value_loss']), 6)
                    self._progress_callback(episode + 1, num_episodes, episode_metrics)

                # 定期输出日志
                if (episode + 1) % 5 == 0:
                    avg_reward = np.mean(episode_rewards[-5:])
                    avg_acc = np.mean(episode_accuracies[-5:]) * 100
                    avg_loss = np.mean(loss_history[-5:]) if loss_history else 0.0
                    logger.info(f"Episode {episode+1}/{num_episodes}, "
                               f"Avg Reward: {avg_reward:.2f}, "
                               f"Accuracy: {avg_acc:.1f}%, "
                               f"Avg Loss: {avg_loss:.4f}, "
                               f"Steps/ep: ~{step_count}")

            # 保存模型
            self.trainer.save_model()

            # 训练完成后切回推理模式
            self.env.reset(force_reload=False, training_mode=False)

            # 进度回调设为100%
            if self._progress_callback:
                self._progress_callback(num_episodes, num_episodes)

            self.is_training = False

            return {
                'status': 'success',
                'num_episodes': num_episodes,
                'avg_reward': float(np.mean(episode_rewards)),
                'final_reward': float(episode_rewards[-1]),
                'accuracy': float(np.mean(episode_accuracies)) if episode_accuracies else 0.0,
                'total_steps': self.trainer.total_steps,
                'episode_rewards': episode_rewards
            }

        except Exception as e:
            self.is_training = False
            # 确保切回推理模式
            try:
                self.env.reset(force_reload=False, training_mode=False)
            except:
                pass
            logger.exception(f"PPO训练失败: {str(e)}")
            return {'status': 'error', 'reason': str(e)}

    def _get_gat_features(self, request_idx: int) -> Optional[np.ndarray]:
        """获取GATv2用户嵌入特征"""
        if self.gat_recommender is None or not self.gat_recommender.is_initialized:
            return None

        if request_idx >= len(self.env.request_info):
            return None

        user_id = self.env.request_info[request_idx].get('user_id')
        if user_id is None:
            return None

        user_embedding = self.gat_recommender.get_user_embedding(user_id)
        if user_embedding is not None:
            return np.array(user_embedding, dtype=np.float32)
        return None

    def get_status(self) -> Dict[str, Any]:
        """获取调度器状态"""
        return {
            'is_initialized': self.is_initialized,
            'is_training': self.is_training,
            'device_count': self.env.device_count,
            'request_count': self.env.request_count,
            'dispatch_count': len(self.dispatch_results),
            'total_steps': self.trainer.total_steps if self.trainer else 0,
            'train_params': self._current_train_params,
            'action_space': ['批准', '拒绝']
        }

    def save_results_to_db(self):
        """保存调度结果到数据库"""
        if not self.dispatch_results:
            return

        logger.info(f"保存 {len(self.dispatch_results)} 条调度结果到数据库...")

        for result in self.dispatch_results:
            if result['status'] in ['approved', 'rejected']:
                reserve_id = result['reserve_id']
                status = '1' if result['status'] == 'approved' else '2'

                sql = """
                    UPDATE lab_reservation
                    SET status = %s,
                        audit_time = NOW()
                    WHERE reserve_id = %s
                """

                try:
                    db.execute_update(sql, (status, reserve_id))
                except Exception as e:
                    logger.warning(f"更新预约 {reserve_id} 失败: {str(e)}")

        logger.info("调度结果已保存到数据库")

    def data_exists(self) -> bool:
        """检查是否有可用于训练或调度的数据"""
        try:
            sql_train = "SELECT COUNT(*) FROM lab_reservation WHERE status IN ('1', '2', '5')"
            result_train = db.execute_query(sql_train)
            train_count = result_train[0][0] if result_train else 0

            sql_pending = "SELECT COUNT(*) FROM lab_reservation WHERE status = '0'"
            result_pending = db.execute_query(sql_pending)
            pending_count = result_pending[0][0] if result_pending else 0

            return train_count > 0 or pending_count > 0
        except Exception as e:
            logger.warning(f"检查数据存在性失败: {str(e)}")
            return False

    def _load_single_reservation(self, reserve_id: int) -> bool:
        """动态加载单个预约"""
        try:
            return self.env.load_single_reservation(reserve_id)
        except Exception as e:
            logger.exception(f"动态加载预约 {reserve_id} 失败: {str(e)}")
            return False

    def get_dispatch_status(self, reserve_id: int) -> Optional[Dict[str, Any]]:
        """获取单个预约的调度状态"""
        for result in self.dispatch_results:
            if result['reserve_id'] == reserve_id:
                return result
        return None


# 全局调度器实例
dispatcher = PPODispatcher()

# 兼容导出
scheduler = dispatcher
is_initialized = dispatcher.is_initialized
