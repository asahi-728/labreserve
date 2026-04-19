"""
PPO 调度环境模块
实现设备预约调度的强化学习环境

核心 设计：
- 训练模式：加载已完成预约(status=1/3/4/5/6 为通过, status=2 为拒绝)，使用历史审批结果作为奖励信号
- 推理模式：加载待审批预约(status=0)，用于实时调度决策
- 动作空间：2维 [批准(0), 拒绝(1)]
- 奖励函数：基于 动作与真实标签的一致性 + 冲突检测 + 用户信用分

预约状态说明:
| 状态值 | 含义       | 训练标签 | 奖励等级(批准时) |
|--------|-----------|----------|------------------|
| 0      | 待审批     | 不参与训练(推理输入)   | -                |
| 1      | 已批准     | label=1.0 (应该批准)   | ★★★ 中等奖励    |
| 2      | 已拒绝     | label=0.0 (应该拒绝)   | - (负样本)       |
| 3      | 已完成使用  | label=1.0 (应该批准)   | ★★★★ 最高奖励   |
| 4      | 已违约     | label=1.0 (应该批准)   | ★ 低奖励(有瑕疵) |
| 5      | 已取消     | label=1.0 (应该批准)   | ★ 低奖励(有瑕疵) |
| 6      | 已确认完成  | label=1.0 (应该批准)   | ★★★★★ 完美奖励  |

注: 违约/取消虽然原始审批是"批准"(label=1.0)，但最终结果不理想，
    因此在奖励函数中给予较低的正向奖励，引导模型学习更优的审批策略。
"""

import numpy as np
from typing import Tuple, Dict, Any, List, Optional
from dataclasses import dataclass, field

from ...utils import config, logger, db


@dataclass
class State:
    """状态类，描述当前系统状态"""
    device_states: np.ndarray  # 设备状态 (设备数 x 状态维度)
    user_requests: np.ndarray  # 用户请求 (请求数 x 请求维度)
    global_state: np.ndarray  # 全局状态 (全局维度)
    gat_features: Optional[np.ndarray] = None  # GATv2特征 (可选)


@dataclass
class Action:
    """动作类，描述调度决策"""
    action_type: int  # 动作类型: 0=批准, 1=拒绝
    device_id: Optional[int] = None  # 设备ID (批准时)


class ReservationEnvironment:
    """设备预约调度环境"""

    # 状态常量（完整定义）
    # 标签含义：只有 status='2'(已拒绝) 是负样本(label=0.0)，
    #           其余 1/3/4/5/6 原始审批决策都是"批准"(label=1.0)，
    #           但在奖励函数中根据最终结果给予差异化奖励
    STATUS_PENDING = '0'      # 待审批     → 不参与训练(推理输入)
    STATUS_APPROVED = '1'     # 已批准     → label=1.0, 奖励=+8.0
    STATUS_REJECTED = '2'     # 已拒绝     → label=0.0 (唯一负样本)
    STATUS_COMPLETED = '3'    # 已完成使用  → label=1.0, 奖励=+10.0
    STATUS_BREACH = '4'       # 已违约     → label=1.0, 奖励=+2.0 (低，有瑕疵)
    STATUS_CANCELLED = '5'    # 已取消     → label=1.0, 奖励=+3.0 (低，有瑕疵)
    STATUS_FINISHED = '6'     # 已确认完成  → label=1.0, 奖励=+12.0 (最高)

    def __init__(self):
        self.device_count = 0
        self.request_count = 0
        self.current_step = 0
        self.max_steps = 100
        self.state: Optional[State] = None
        self.total_conflicts = 0
        self.total_correct = 0  # 正确决策计数
        self.total_wrong = 0    # 错误决策计数

        # 动作空间大小: 2种动作 (批准/拒绝)
        self.action_space = 2

        # 状态空间维度
        self.device_state_dim = 8   # 设备状态维度
        self.request_state_dim = 8  # 请求维度（增加了标签位）
        self.global_state_dim = 4   # 全局状态维度
        self.observation_space = (
            self.device_state_dim +
            self.request_state_dim +
            self.global_state_dim
        )

        # 标签数据：每条预约的真实审批结果 (用于训练)
        # 1.0 = 应该批准, 0.0 = 应该拒绝
        self.labels: np.ndarray = np.array([])

        # 模式标志
        self.training_mode = True  # True=训练(用已完成预约), False=推理(用待审批)

        logger.info("PPO调度环境初始化完成 (动作空间=2: 批准/拒绝)")

    def reset(self, force_reload: bool = False,
              training_mode: bool = True) -> State:
        """
        重置环境

        Args:
            force_reload: 是否强制重新加载数据
            training_mode: True=训练模式(加载已完成预约), False=推理模式(加载待审批)

        Returns:
            State: 初始状态
        """
        self.training_mode = training_mode
        logger.info(f"重置PPO调度环境... (mode={'训练' if training_mode else '推理'})")

        self.current_step = 0
        self.total_conflicts = 0
        self.total_correct = 0
        self.total_wrong = 0

        # 加载数据（根据模式选择不同数据源）
        self._load_data(force_reload)

        # 构建初始状态
        self.state = self._build_initial_state()

        logger.info(f"环境重置完成: 设备数={self.device_count}, 请求数={self.request_count}, "
                   f"模式={'训练' if self.training_mode else '推理'}")
        if self.training_mode and len(self.labels) > 0:
            approved_count = int(np.sum(self.labels))
            rejected_count = len(self.labels) - approved_count
            logger.info(f"标签分布: 批准={approved_count}, 拒绝={rejected_count}")

        return self.state

    def _load_data(self, force_reload: bool = False):
        """从数据库加载数据"""
        logger.info("加载调度环境数据...")

        # 加载设备数据
        self._load_devices()

        # 根据模式加载不同类型的预约
        if self.training_mode:
            self._load_completed_requests()  # 训练：已完成的预约（有标签）
        else:
            self._load_pending_requests()   # 推理：待审批的预约（无标签）

        logger.debug(f"数据加载完成: 设备={self.device_count}, 请求={self.request_count}")

    def _load_devices(self):
        """加载设备数据"""
        sql = """
            SELECT device_id, category_id, lab_id, status,
                   device_name, create_time
            FROM lab_device
            ORDER BY device_id
        """

        device_data = db.execute_query(sql, dict_cursor=True)
        self.device_count = len(device_data)

        # 初始化设备状态
        self.device_states = np.zeros((self.device_count, self.device_state_dim), dtype=np.float32)

        # 设备ID到索引的映射
        self.device_id_to_idx = {}
        self.device_idx_to_id = {}

        for idx, row in enumerate(device_data):
            device_id = row['device_id']
            self.device_id_to_idx[device_id] = idx
            self.device_idx_to_id[idx] = device_id

            category_id = row['category_id'] or 0
            lab_id = row['lab_id'] or 0
            status = row['status'] or '0'
            create_time = row['create_time']

            year_diff = 1.0
            if create_time:
                from datetime import datetime
                try:
                    if isinstance(create_time, str):
                        create_year = datetime.strptime(create_time[:10], '%Y-%m-%d').year
                    else:
                        create_year = create_time.year
                    year_diff = max(0.0, min(1.0, (2026 - create_year) / 5.0))
                except:
                    pass

            self.device_states[idx] = [
                category_id / 20.0 if category_id > 0 else 0.0,
                lab_id / 10.0 if lab_id > 0 else 0.0,
                int(status) / 2.0 if status else 0.0,
                1.0 if status == '0' else 0.0,  # 是否可用
                year_diff,
                0.0,  # 剩余可用时长
                0.0,  # 预约次数
                0.0   # 冲突次数
            ]

    def _load_completed_requests(self):
        """
        加载已完成预约作为训练数据（有标签）

        标签规则：
        - 正样本 (label=1.0, 原始审批决策为"批准"):
          status='1'(已批准), '3'(已完成使用), '4'(已违约), '5'(已取消), '6'(已确认完成)
          注：违约/取消虽然原始审批是"批准"，但最终结果不理想，
              因此在奖励函数中给予较低的正向奖励（差异化奖励）
        - 负样本 (label=0.0, 原始审批决策为"拒绝"):
          status='2'(已拒绝)
        - 推理输入:
          status='0'(待审批) 不参与训练

        奖励梯度设计（批准时）：
        - status='6' 已确认完成 → 最高奖励（完美结果）
        - status='3' 已完成使用 → 高奖励（正常完成）
        - status='1' 已批准     → 中等奖励（待执行）
        - status='4' 已违约     → 低奖励（有瑕疵，但原始决策是批准的）
        - status='5' 已取消     → 低奖励（有瑕疵，但原始决策是批准的）
        """
        sql = """
            SELECT r.reserve_id, r.user_id, r.device_id, r.start_time, r.end_time,
                   r.status as audit_status,
                   uc.credit_score
            FROM lab_reservation r
            LEFT JOIN lab_user_credit uc ON r.user_id = uc.user_id
            WHERE r.status IN ('1', '2', '3', '4', '5', '6')
            ORDER BY r.audit_time DESC, r.create_time DESC
            LIMIT 200
        """

        request_data = db.execute_query(sql, dict_cursor=True)
        self.request_count = len(request_data)

        self.request_states = np.zeros((self.request_count, self.request_state_dim), dtype=np.float32)
        self.request_info = []
        self.labels = np.zeros(self.request_count, dtype=np.float32)

        # 唯一的负样本状态：被明确拒绝
        REJECTED_STATUS = self.STATUS_REJECTED  # '2'

        for idx, row in enumerate(request_data):
            reserve_id = row['reserve_id']
            user_id = row['user_id']
            device_id = row['device_id']
            credit_score = row['credit_score'] or 50
            audit_status = row['audit_status']

            # 构建标签：只有被明确拒绝的才是负样本(label=0.0)
            # 其余所有状态(1/3/4/5/6)原始审批决策都是"批准"→ label=1.0
            if audit_status == REJECTED_STATUS:
                self.labels[idx] = 0.0
            else:
                # '1'已批准, '3'已完成, '4'已违约, '5'已取消, '6'已确认完成
                self.labels[idx] = 1.0

            # 计算需求时长
            duration_hours = self._calc_duration(row.get('start_time'), row.get('end_time'))

            # 构建请求特征（8维）
            self.request_states[idx] = [
                user_id / 1000.0 if user_id > 0 else 0.0,
                device_id / 1000.0 if device_id > 0 else 0.0,
                credit_score / 100.0 if credit_score > 0 else 0.0,
                min(duration_hours / 8.0, 1.0),
                1.0,  # 优先级
                0.0,  # 是否冲突（动态检测）
                1.0 if credit_score >= 60 else 0.0,  # 信用良好标记
                self.labels[idx]  # 真实标签（训练时可观测）
            ]

            self.request_info.append({
                'reserve_id': reserve_id,
                'user_id': user_id,
                'device_id': device_id,
                'start_time': row['start_time'],
                'end_time': row['end_time'],
                'credit_score': credit_score,
                'label': self.labels[idx],
                'audit_status': audit_status,
                'reason': ''
            })

        logger.info(f"加载已完成预约 {self.request_count} 条作为训练数据 "
                   f"(批准={int(np.sum(self.labels))}, 拒绝={int(np.sum(1 - self.labels))})")

    def _load_pending_requests(self):
        """加载待处理的预约请求（推理/调度时使用）"""
        sql = """
            SELECT r.reserve_id, r.user_id, r.device_id, r.start_time, r.end_time,
                   uc.credit_score
            FROM lab_reservation r
            LEFT JOIN lab_user_credit uc ON r.user_id = uc.user_id
            WHERE r.status = '0'
            ORDER BY r.create_time
            LIMIT 50
        """

        request_data = db.execute_query(sql, dict_cursor=True)
        self.request_count = len(request_data)

        self.request_states = np.zeros((self.request_count, self.request_state_dim), dtype=np.float32)
        self.request_info = []
        self.labels = np.zeros(self.request_count, dtype=np.float32)  # 推理时无标签

        for idx, row in enumerate(request_data):
            reserve_id = row['reserve_id']
            user_id = row['user_id']
            device_id = row['device_id']
            credit_score = row['credit_score'] or 50

            duration_hours = self._calc_duration(row.get('start_time'), row.get('end_time'))

            self.request_states[idx] = [
                user_id / 1000.0 if user_id > 0 else 0.0,
                device_id / 1000.0 if device_id > 0 else 0.0,
                credit_score / 100.0 if credit_score > 0 else 0.0,
                min(duration_hours / 8.0, 1.0),
                1.0,
                0.0,
                1.0 if credit_score >= 60 else 0.0,
                0.5  # 推理时标签未知，设为中性值
            ]

            self.request_info.append({
                'reserve_id': reserve_id,
                'user_id': user_id,
                'device_id': device_id,
                'start_time': row['start_time'],
                'end_time': row['end_time'],
                'credit_score': credit_score,
                'label': 0.5,  # 未知
                'audit_status': '0',
                'reason': ''
            })

    @staticmethod
    def _calc_duration(start_time, end_time) -> float:
        """计算预约时长（小时）"""
        duration_hours = 2.0
        try:
            from datetime import datetime
            if start_time and end_time:
                if isinstance(start_time, str):
                    start = datetime.strptime(start_time[:19], '%Y-%m-%d %H:%M:%S')
                    end = datetime.strptime(end_time[:19], '%Y-%m-%d %H:%M:%S')
                else:
                    start = start_time
                    end = end_time
                duration_hours = (end - start).total_seconds() / 3600.0
        except:
            pass
        return duration_hours

    def _build_initial_state(self) -> State:
        """构建初始状态"""
        global_state = np.array([
            self.device_count / 100.0,
            self.request_count / 50.0,
            0.0,  # 当前冲突率
            0.5   # 平均设备利用率
        ], dtype=np.float32)

        return State(
            device_states=self.device_states.copy(),
            user_requests=self.request_states.copy(),
            global_state=global_state
        )

    def set_gat_features(self, gat_features: np.ndarray):
        """设置GATv2特征"""
        if self.state:
            self.state.gat_features = gat_features
        logger.debug("GATv2特征已设置")

    def step(self, action: Action, request_idx: int = 0) -> Tuple[State, float, bool, Dict[str, Any]]:
        """
        执行一步动作

        Args:
            action: 执行的动作
            request_idx: 当前处理的请求索引

        Returns:
            Tuple[State, float, bool, Dict]: (下一个状态, 奖励, 是否结束, 额外信息)
        """
        self.current_step += 1

        reward, info = self._execute_action(action, request_idx)
        next_state = self._update_state(request_idx)
        done = self.current_step >= self.max_steps or request_idx >= self.request_count - 1

        return next_state, reward, done, info

    def _execute_action(self, action: Action, request_idx: int) -> Tuple[float, Dict[str, Any]]:
        """
        执行动作并计算奖励（核心改进）

        奖励函数设计（混合信号）：
        1. 标签一致性奖励（主要）：动作与历史真实结果一致则高奖励
        2. 冲突惩罚（辅助）：有时段冲突时惩罚批准操作
        3. 信用分调节（辅助）：高信用用户被批准给予额外奖励
        """
        reward = 0.0
        info = {
            'action_type': action.action_type,
            'request_idx': request_idx
        }

        if request_idx >= len(self.request_info):
            return reward, info

        request_info = self.request_info[request_idx]
        true_label = request_info.get('label', 0.5)
        credit_score = request_info.get('credit_score', 50)

        # 检查冲突
        has_conflict = self.check_conflict(request_info)

        if action.action_type == 0:  # 批准
            reward = self._reward_approve(has_conflict, true_label, credit_score, request_info)
            info['result'] = 'approved'
            if has_conflict:
                self.total_conflicts += 1
        elif action.action_type == 1:  # 拒绝
            reward = self._reward_reject(has_conflict, true_label, credit_score)
            info['result'] = 'rejected'

        # 统计正确率
        predicted_label = 1.0 if action.action_type == 0 else 0.0
        if self.training_mode and true_label != 0.5:
            if abs(predicted_label - true_label) < 0.5:
                self.total_correct += 1
            else:
                self.total_wrong += 1

        logger.debug(f"动作执行: 类型={'批准' if action.action_type == 0 else '拒绝'}, "
                    f"奖励={reward:.3f}, 真实标签={true_label}, 冲突={has_conflict}")
        return reward, info

    def _reward_approve(self, has_conflict: bool, true_label: float,
                        credit_score: int, request_info: Dict) -> float:
        """
        计算"批准"动作的奖励（差异化奖励设计）

        核心思想：所有非拒绝状态的原始审批决策都是"批准"(label=1.0)，
        但根据最终执行结果给予不同等级的正向奖励，形成梯度：

        奖励梯度（从高到低）：
        | 最终状态     | 含义         | 基础奖励 | 说明                     |
        |-------------|-------------|---------|--------------------------|
        | '6' 已确认完成 | 完美结果     | +12.0   | 用户正常使用并确认归还     |
        | '3' 已完成使用 | 正常完成     | +10.0   | 系统自动标记为已完成       |
        | '1' 已批准     | 待/在执行   | +8.0    | 批准通过，尚未到最终状态    |
        | '5' 已取消     | 有瑕疵       | +3.0    | 用户主动取消，资源浪费      |
        | '4' 已违约     | 有瑕疵       | +2.0    | 违规使用，最不理想的结果    |
        | '2' 已拒绝     | 负样本       | -8.0    | 历史上被拒绝，我们却批准了  |
        """
        reward = 0.0

        # 获取该条预约的实际最终状态，用于差异化奖励
        audit_status = request_info.get('audit_status', '')

        if true_label == 1.0:
            # === 正样本：原始审批是"批准"，根据最终状态差异化奖励 ===
            if audit_status == self.STATUS_FINISHED:       # '6' 已确认完成 → 最完美
                base_reward = 12.0
            elif audit_status == self.STATUS_COMPLETED:    # '3' 已完成使用 → 很好
                base_reward = 10.0
            elif audit_status == self.STATUS_APPROVED:     # '1' 已批准 → 中等
                base_reward = 8.0
            elif audit_status == self.STATUS_CANCELLED:    # '5' 已取消 → 低（有瑕疵）
                base_reward = 3.0
            elif audit_status == self.STATUS_BREACH:       # '4' 已违约 → 最低（有瑕疵）
                base_reward = 2.0
            else:
                # 其他未知正样本状态（兜底）
                base_reward = 6.0

            reward += base_reward

            # 无冲突额外加成（对所有正样本一视同仁）
            if not has_conflict:
                reward += 2.0

        elif true_label == 0.0:
            # === 负样本：历史上被拒绝了，我们却批准 → 大错误！===
            reward -= 8.0
        else:
            # === 无标签（推理模式）：使用启发式 ===
            if not has_conflict:
                reward += 3.0
            else:
                reward -= 8.0

        # === 辅助信号：冲突惩罚（硬约束）===
        if has_conflict:
            reward -= 6.0

        # === 辅助信号：信用分调节 ===
        if credit_score >= 80:
            reward += 1.5
        elif credit_score >= 60:
            reward += 0.8
        elif credit_score < 30:
            reward -= 1.0

        return reward

    def _reward_reject(self, has_conflict: bool, true_label: float,
                       credit_score: int) -> float:
        """
        计算"拒绝"动作的奖励

        设计思路：
        - 与真实标签一致(应该拒绝且拒绝了): 正奖励 +3~6
        - 与真实标签不一致(应该批准却拒绝了): 负惩罚 -3~-8
        - 有冲突时拒绝是合理的，给额外奖励
        - 低信用分用户拒绝给小奖励
        """
        reward = 0.0

        # === 主要信号：标签一致性 ===
        if true_label == 0.0:
            # 历史上被拒绝了，我们也拒绝 → 正确！
            reward += 5.0
        elif true_label == 1.0:
            # 历史上被批准了，我们却拒绝 → 错误！
            reward -= 8.0
        else:
            # 无标签（推理模式），使用启发式
            reward -= 1.0  # 默认轻微惩罚拒绝（倾向于批准）

        # === 辅助信号：冲突时的拒绝是明智的 ===
        if has_conflict:
            reward += 4.0  # 有冲突时拒绝避免了问题

        # === 辅助信号：信用分调节 ===
        if credit_score < 30:
            reward += 2.0  # 低信用用户拒绝是合理的
        elif credit_score >= 80:
            reward -= 1.5  # 拒绝高信用用户代价较高

        return reward

    def check_conflict(self, request_info: Dict, exclude_reserve_id: Optional[int] = None) -> bool:
        """检查是否存在预约冲突"""
        device_id = request_info.get('device_id')
        start_time = request_info.get('start_time')
        end_time = request_info.get('end_time')

        if not device_id or not start_time or not end_time:
            return False

        sql = """
            SELECT COUNT(*) as conflict_count
            FROM lab_reservation
            WHERE device_id = %s
              AND status IN ('0', '1')
              AND (
                  (start_time < %s AND end_time > %s) OR
                  (start_time < %s AND end_time > %s) OR
                  (start_time >= %s AND end_time <= %s)
              )
        """

        params = [device_id, end_time, start_time, end_time, start_time, start_time, end_time]

        if exclude_reserve_id is not None:
            sql += " AND reserve_id != %s"
            params.append(exclude_reserve_id)

        result = db.execute_query(sql, tuple(params), dict_cursor=True)
        conflict_count = result[0]['conflict_count'] if result else 0
        return conflict_count > 0

    def _update_state(self, request_idx: int) -> State:
        """更新环境状态"""
        total_decisions = self.total_correct + self.total_wrong
        accuracy = self.total_correct / max(1, total_decisions)
        conflict_rate = min(1.0, self.total_conflicts / max(1, request_idx + 1))

        self.state.global_state = np.array([
            self.device_count / 100.0,
            max(0.0, (self.request_count - request_idx - 1) / 50.0),
            conflict_rate,
            accuracy
        ], dtype=np.float32)

        return self.state

    def get_observation(self, request_idx: int = 0) -> np.ndarray:
        """
        获取当前观察向量（神经网络输入）

        观察空间组成：
        - 设备状态均值 (8维): 所有设备状态的平均
        - 当前请求特征 (8维): 用户/设备/信用分/时长/冲突/标签等
        - 全局状态 (4维): 设备数/剩余请求数/冲突率/准确率
        - [可选] GAT嵌入 (4维): 用户图嵌入向量
        """
        if self.state is None:
            raise ValueError("环境未初始化，请先调用reset()")

        # 设备状态：取所有设备的平均
        device_obs = self.state.device_states.mean(axis=0)

        # 当前请求的特征
        request_obs = (self.state.user_requests[request_idx]
                       if request_idx < self.request_count
                       else np.zeros(self.request_state_dim))

        # 合并基础观察
        observation = np.concatenate([
            device_obs,
            request_obs,
            self.state.global_state
        ])

        # 拼接GAT特征（如果存在）
        if self.state.gat_features is not None:
            observation = np.concatenate([observation, self.state.gat_features])

        return observation

    def load_single_reservation(self, reserve_id: int) -> bool:
        """动态加载单个预约到环境中（推理/调度时使用）"""
        logger.info(f"动态加载预约: {reserve_id}")

        sql = """
            SELECT r.reserve_id, r.user_id, r.device_id, r.start_time, r.end_time,
                   r.status, uc.credit_score
            FROM lab_reservation r
            LEFT JOIN lab_user_credit uc ON r.user_id = uc.user_id
            WHERE r.reserve_id = %s
        """

        result = db.execute_query(sql, (reserve_id,), dict_cursor=True)

        if not result or len(result) == 0:
            logger.warning(f"预约 {reserve_id} 不存在")
            return False

        row = result[0]
        device_id = row['device_id']

        # 确保设备已加载
        if device_id not in self.device_id_to_idx:
            self._load_devices()
            if device_id not in self.device_id_to_idx:
                logger.error(f"设备 {device_id} 不存在")
                return False

        user_id = row['user_id']
        credit_score = row['credit_score'] or 50
        duration_hours = self._calc_duration(row.get('start_time'), row.get('end_time'))

        new_request_state = np.array([
            user_id / 1000.0 if user_id > 0 else 0.0,
            device_id / 1000.0 if device_id > 0 else 0.0,
            credit_score / 100.0 if credit_score > 0 else 0.0,
            min(duration_hours / 8.0, 1.0),
            1.0,
            0.0,
            1.0 if credit_score >= 60 else 0.0,
            0.5  # 推理时标签未知
        ], dtype=np.float32)

        self.request_states = np.vstack([self.request_states, new_request_state])
        self.labels = np.append(self.labels, 0.5)

        self.request_info.append({
            'reserve_id': row['reserve_id'],
            'user_id': user_id,
            'device_id': device_id,
            'start_time': row['start_time'],
            'end_time': row['end_time'],
            'credit_score': credit_score,
            'label': 0.5,
            'audit_status': row['status'],
            'reason': ''
        })

        self.request_count += 1

        if self.state:
            self.state.user_requests = self.request_states.copy()
            self.state.global_state = np.array([
                self.device_count / 100.0,
                self.request_count / 50.0,
                0.0,
                0.5
            ], dtype=np.float32)

        logger.info(f"成功动态加载预约 {reserve_id}，当前请求数: {self.request_count}")
        return True

    def close(self):
        """关闭环境"""
        logger.info("关闭PPO调度环境")
        self.state = None
