"""
GATv2 推荐器模块
实现基于图注意力网络的个性化设备推荐
"""

import numpy as np
import torch
from typing import List, Dict, Any, Optional
from datetime import datetime
from sklearn.metrics.pairwise import cosine_similarity

from ...utils import config, logger, db
from .data_loader import GraphDataLoader
from .model import GATv2Model, GATv2Trainer


class GATv2Recommender:
    """GATv2 个性化推荐器"""
    
    def __init__(self):
        self.data_loader = GraphDataLoader()
        self.model: Optional[GATv2Model] = None
        self.trainer: Optional[GATv2Trainer] = None
        self.graph_data = None
        self.user_embeddings: np.ndarray = np.array([])
        self.device_embeddings: np.ndarray = np.array([])
        self.is_initialized = False
        self._progress_callback = None
    
    def set_progress_callback(self, callback):
        """
        设置训练进度回调函数
        
        Args:
            callback: 回调函数
        """
        self._progress_callback = callback
        if self.trainer is not None:
            self.trainer.set_progress_callback(callback)
    
    def initialize(self, force_reload: bool = False,
                   in_channels: int = None,
                   hidden_channels: int = None,
                   out_channels: int = None,
                   lr: float = None,
                   epochs: int = None) -> bool:
        """
        初始化推荐器

        Args:
            force_reload: 是否强制重新加载数据
            in_channels: 输入维度
            hidden_channels: 隐藏层维度
            out_channels: 输出维度
            lr: 学习率
            epochs: 训练轮数

        Returns:
            bool: 初始化是否成功
        """
        logger.info(f"正在初始化GATv2推荐器... (in_channels={in_channels}, hidden_channels={hidden_channels}, out_channels={out_channels}, lr={lr}, epochs={epochs})")

        try:
            if force_reload or not self.data_loader.data_exists():
                self.graph_data = self.data_loader.load_from_database()
            else:
                try:
                    self.graph_data = self.data_loader.load_from_file()
                except Exception as e:
                    logger.warning(f"从文件加载失败，将从数据库重新加载: {str(e)}")
                    self.graph_data = self.data_loader.load_from_database()

            model_path = config.model_dir / "gatv2_model.pth"
            if model_path.exists() and not force_reload:
                logger.info("加载已训练的模型...")
                self.trainer, _ = GATv2Trainer.load_model(str(model_path))
                self.model = self.trainer.model
            else:
                logger.info("训练新模型...")
                self.model = GATv2Model(
                    in_channels=in_channels,
                    hidden_channels=hidden_channels,
                    out_channels=out_channels
                )
                self.trainer = GATv2Trainer(self.model, lr=lr)
                if self._progress_callback:
                    self.trainer.set_progress_callback(self._progress_callback)
                self.trainer.train(self.graph_data.x, self.graph_data.edge_index, epochs=epochs or config.model_epochs)
                self.trainer.save_model()

            self._extract_embeddings()

            self.is_initialized = True
            logger.info("GATv2推荐器初始化成功！")
            return True

        except Exception as e:
            logger.exception(f"GATv2推荐器初始化失败: {str(e)}")
            return False
    
    def _extract_embeddings(self):
        """提取用户和设备的嵌入向量"""
        if self.model is None or self.graph_data is None:
            raise ValueError("模型或图数据未初始化")
        
        logger.info("提取节点嵌入向量...")
        
        # 提取所有节点嵌入
        all_embeddings = self.model.encode(self.graph_data.x, self.graph_data.edge_index).numpy()
        
        # 分离用户和设备嵌入
        user_count = self.data_loader.user_count
        self.user_embeddings = all_embeddings[:user_count]
        self.device_embeddings = all_embeddings[user_count:]
        
        # 保存嵌入向量
        np.save(config.data_dir / "user_embeddings.npy", self.user_embeddings)
        np.save(config.data_dir / "device_embeddings.npy", self.device_embeddings)
        
        logger.debug(f"用户嵌入维度: {self.user_embeddings.shape}")
        logger.debug(f"设备嵌入维度: {self.device_embeddings.shape}")
    
    def _get_available_devices(self, user_id: int) -> List[int]:
        """
        获取对用户可用的设备列表
        
        Args:
            user_id: 用户ID
        
        Returns:
            List[int]: 可用设备ID列表
        """
        device_ids = list(self.data_loader.device_id_map.keys())
        
        if not (config.recommend_exclude_reserved or config.recommend_exclude_maintenance):
            return device_ids
        
        available_device_ids = []
        
        # 查询设备状态和预约情况
        sql = """
            SELECT d.device_id, d.status, 
                   CASE WHEN r.device_id IS NOT NULL THEN 1 ELSE 0 END as is_reserved
            FROM lab_device d
            LEFT JOIN (
                SELECT DISTINCT device_id 
                FROM lab_reservation 
                WHERE user_id = %s AND status IN ('0', '1')
            ) r ON d.device_id = r.device_id
            WHERE d.device_id IN (%s)
        """
        
        # 构建IN子句的占位符
        placeholders = ','.join(['%s'] * len(device_ids))
        sql = sql % ('%s', placeholders)
        
        params = [user_id] + device_ids
        results = db.execute_query(sql, tuple(params), dict_cursor=True)
        
        for row in results:
            device_id = row['device_id']
            status = row['status']
            is_reserved = row['is_reserved']
            
            include = True
            
            if config.recommend_exclude_reserved and is_reserved:
                include = False
            
            if config.recommend_exclude_maintenance and status != '0':
                include = False
            
            if include:
                available_device_ids.append(device_id)
        
        return available_device_ids
    
    def recommend(self, user_id: int, top_k: int = None) -> List[Dict[str, Any]]:
        """
        为用户生成个性化推荐列表
        
        Args:
            user_id: 用户ID
            top_k: 推荐数量，默认为配置中的值
        
        Returns:
            List[Dict]: 推荐设备列表，包含设备ID、相似度分数等信息
        """
        if not self.is_initialized:
            success = self.initialize()
            if not success:
                raise RuntimeError("推荐器初始化失败")
        
        top_k = top_k or config.recommend_top_k
        
        logger.info(f"为用户 {user_id} 生成Top-{top_k}推荐...")
        
        # 检查用户是否存在
        if user_id not in self.data_loader.user_id_map:
            logger.warning(f"用户 {user_id} 不存在于图数据中")
            return []
        
        user_idx = self.data_loader.user_id_map[user_id]
        
        # 获取可用设备
        available_device_ids = self._get_available_devices(user_id)
        if not available_device_ids:
            logger.warning(f"没有可用设备推荐给用户 {user_id}")
            return []
        
        # 计算可用设备的索引
        available_device_indices = []
        for dev_id in available_device_ids:
            if dev_id in self.data_loader.device_id_map:
                available_device_indices.append(self.data_loader.device_id_map[dev_id])
        
        if not available_device_indices:
            return []
        
        # 计算相似度
        user_emb = self.user_embeddings[user_idx].reshape(1, -1)
        available_dev_emb = self.device_embeddings[available_device_indices]
        
        similarities = cosine_similarity(user_emb, available_dev_emb)[0]
        
        # 排序并选择Top-K
        top_indices = np.argsort(similarities)[::-1][:top_k]
        
        # 构建推荐结果
        recommendations = []
        for rank, idx in enumerate(top_indices):
            device_idx_in_available = available_device_indices[idx]
            device_id = list(self.data_loader.device_id_map.keys())[list(self.data_loader.device_id_map.values()).index(device_idx_in_available)]
            
            # 余弦相似度范围是 [-1, 1]，归一化到 [0, 1]
            normalized_similarity = (float(similarities[idx]) + 1) / 2
            
            recommendations.append({
                'user_id': user_id,
                'device_id': device_id,
                'similarity': normalized_similarity,
                'rank': rank + 1,
                'recommend_time': datetime.now().strftime('%Y-%m-%d %H:%M:%S')
            })
        
        logger.info(f"为用户 {user_id} 生成了 {len(recommendations)} 条推荐")
        
        return recommendations
    
    def save_recommendations_to_db(self, recommendations: List[Dict[str, Any]]):
        """
        将推荐结果保存到数据库
        
        Args:
            recommendations: 推荐结果列表
        """
        if not recommendations:
            return
        
        logger.info(f"保存 {len(recommendations)} 条推荐到数据库...")
        
        # 构建插入参数
        params_list = []
        for rec in recommendations:
            params_list.append((
                rec['user_id'],
                rec['device_id'],
                rec['similarity']
            ))
        
        # 批量插入或更新
        sql = """
            INSERT INTO lab_recommend (user_id, device_id, similarity)
            VALUES (%s, %s, %s)
            ON DUPLICATE KEY UPDATE 
                similarity = VALUES(similarity)
        """
        
        try:
            db.execute_batch(sql, params_list)
            logger.info(f"推荐结果已保存到数据库")
        except Exception as e:
            logger.exception(f"保存推荐到数据库失败: {str(e)}")
    
    def get_user_embedding(self, user_id: int) -> Optional[List[float]]:
        """
        获取用户嵌入向量
        
        Args:
            user_id: 用户ID
        
        Returns:
            Optional[List[float]]: 用户嵌入向量
        """
        if not self.is_initialized:
            return None
        
        if user_id not in self.data_loader.user_id_map:
            return None
        
        user_idx = self.data_loader.user_id_map[user_id]
        return self.user_embeddings[user_idx].tolist()
    
    def get_device_embedding(self, device_id: int) -> Optional[List[float]]:
        """
        获取设备嵌入向量
        
        Args:
            device_id: 设备ID
        
        Returns:
            Optional[List[float]]: 设备嵌入向量
        """
        if not self.is_initialized:
            return None
        
        if device_id not in self.data_loader.device_id_map:
            return None
        
        device_idx = self.data_loader.device_id_map[device_id]
        return self.device_embeddings[device_idx].tolist()
    
    def retrain(self, in_channels: int = None, hidden_channels: int = None, out_channels: int = None, lr: float = None, epochs: int = None):
        """
        重新训练模型（支持增量训练：优先加载已有模型继续优化）
        """
        from .model import GATv2Trainer

        logger.info(f"开始重新训练GATv2模型... (in_channels={in_channels}, hidden_channels={hidden_channels}, out_channels={out_channels}, lr={lr}, epochs={epochs})")

        if self.trainer is None or self.graph_data is None:
            # 从未初始化过 → 完全新建
            logger.info("🆕 [GATv2 训练模式] 首次训练 — 创建全新模型")
            self.initialize(force_reload=True, in_channels=in_channels, hidden_channels=hidden_channels, out_channels=out_channels, lr=lr, epochs=epochs)
        else:
            # 已初始化过 → 检查是否有已保存的模型，支持增量训练
            model_path = config.model_dir / "gatv2_model.pth"
            has_saved_model = model_path.exists()

            # 重新加载数据（数据可能更新了）
            self.graph_data = self.data_loader.load_from_database()

            if has_saved_model:
                # ===== 增量训练：加载已有模型权重继续优化 =====
                try:
                    logger.info(f"📈 [GATv2 训练模式] 增量训练 — 加载已有模型 {model_path} 继续优化")
                    loaded_trainer, _ = GATv2Trainer.load_model(str(model_path))
                    self.model = loaded_trainer.model
                    self.trainer = loaded_trainer

                    # 更新学习率（如果用户指定了新 lr）
                    if lr is not None:
                        for param_group in self.trainer.optimizer.param_groups:
                            param_group['lr'] = lr
                        logger.info(f"学习率已更新为: {lr}")

                    if self._progress_callback:
                        self.trainer.set_progress_callback(self._progress_callback)

                    self.trainer.train(self.graph_data.x, self.graph_data.edge_index, epochs=epochs or config.model_epochs)
                    self.trainer.save_model()
                    self._extract_embeddings()
                    logger.info("✅ GATv2 增量训练完成，模型已更新保存")
                except Exception as e:
                    logger.warning(f"⚠️ 加载已有模型失败 ({e})，回退为全新训练")
                    # 回退：创建全新模型
                    self._create_and_train_new(in_channels, hidden_channels, out_channels, lr, epochs)
            else:
                # 无已保存模型 → 全新训练
                logger.info("🔄 [GATv2 训练模式] 无已保存模型 — 创建全新模型从头训练")
                self._create_and_train_new(in_channels, hidden_channels, out_channels, lr, epochs)

        logger.info("模型重新训练完成")

    def _create_and_train_new(self, in_channels: int = None, hidden_channels: int = None, out_channels: int = None, lr: float = None, epochs: int = None):
        """创建全新模型并训练"""
        self.model = GATv2Model(
            in_channels=in_channels,
            hidden_channels=hidden_channels,
            out_channels=out_channels
        )
        self.trainer = GATv2Trainer(self.model, lr=lr)

        if self._progress_callback:
            self.trainer.set_progress_callback(self._progress_callback)

        self.trainer.train(self.graph_data.x, self.graph_data.edge_index, epochs=epochs or config.model_epochs)
        self.trainer.save_model()

        self._extract_embeddings()


# 全局推荐器实例
recommender = GATv2Recommender()
