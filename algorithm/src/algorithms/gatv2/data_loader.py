"""
GATv2 数据加载模块
负责从数据库读取数据并构建图结构
"""

import numpy as np
import torch
from typing import Tuple, Dict, Any
from pathlib import Path
from torch_geometric.data import Data

from ...utils import config, logger, db


class GraphDataLoader:
    """图数据加载器"""
    
    def __init__(self):
        self.user_count = 0
        self.device_count = 0
        self.user_id_map: Dict[int, int] = {}
        self.device_id_map: Dict[int, int] = {}
        self.user_features: np.ndarray = np.array([])
        self.device_features: np.ndarray = np.array([])
        self.edge_index: torch.Tensor = torch.tensor([])
    
    def load_from_database(self) -> Data:
        """
        从数据库加载数据并构建图结构
        
        Returns:
            Data: PyTorch Geometric图数据对象
        """
        logger.info("开始从数据库加载图数据...")
        
        try:
            # 1. 加载用户数据
            self._load_user_data()
            
            # 2. 加载设备数据
            self._load_device_data()
            
            # 3. 加载边关系
            self._load_edge_data()
            
            # 4. 构建图数据
            graph_data = self._build_graph_data()
            
            # 5. 保存数据到文件
            self._save_to_file()
            
            logger.info(f"图数据加载完成: 用户数={self.user_count}, 设备数={self.device_count}, 边数={self.edge_index.shape[1]}")
            
            return graph_data
            
        except Exception as e:
            logger.exception(f"从数据库加载数据失败: {str(e)}")
            raise
    
    def _load_user_data(self):
        """加载用户数据并构建特征"""
        logger.info("加载用户数据...")
        
        sql = """
            SELECT u.user_id, u.dept_id, uc.credit_score, uc.reserve_count,
                   u.sex, u.phonenumber, u.email 
            FROM sys_user u
            LEFT JOIN lab_user_credit uc ON u.user_id = uc.user_id
            ORDER BY u.user_id
        """
        
        user_data = db.execute_query(sql, dict_cursor=True)
        self.user_count = len(user_data)
        
        # 构建用户ID映射
        for idx, row in enumerate(user_data):
            self.user_id_map[row['user_id']] = idx
        
        # 初始化用户特征（8维）
        self.user_features = np.zeros((self.user_count, 8), dtype=np.float32)
        
        for idx, row in enumerate(user_data):
            user_id = row['user_id']
            dept_id = row['dept_id'] or 0
            credit_score = row['credit_score'] or 0
            reserve_count = row['reserve_count'] or 0
            sex = row['sex'] or '2'
            phonenumber = row['phonenumber'] or ''
            email = row['email'] or ''
            
            # 特征归一化到 [0, 1]
            self.user_features[idx] = [
                dept_id / 100.0 if dept_id > 0 else 0.0,  # 部门ID
                credit_score / 100.0 if credit_score > 0 else 0.0,  # 信用分
                reserve_count / 50.0 if reserve_count > 0 else 0.0,  # 预约次数
                1.0 if sex == '0' else 0.0,  # 性别 (0=男 1=女 2=未知)
                0.5,  # 年龄（默认值，因为数据库没有该字段）
                len(phonenumber) / 11.0 if phonenumber else 0.0,  # 手机号长度
                len(email) / 30.0 if email else 0.0,  # 邮箱长度
                1.0 if reserve_count > 0 else 0.0  # 是否有预约历史
            ]
        
        logger.debug(f"用户特征维度: {self.user_features.shape}")
    
    def _load_device_data(self):
        """加载设备数据并构建特征"""
        logger.info("加载设备数据...")
        
        sql = """
            SELECT d.device_id, d.category_id, d.lab_id, d.status, 
                   d.device_name, d.specs, d.create_time, d.remark
            FROM lab_device d
            ORDER BY d.device_id
        """
        
        device_data = db.execute_query(sql, dict_cursor=True)
        self.device_count = len(device_data)
        
        # 构建设备ID映射
        for idx, row in enumerate(device_data):
            self.device_id_map[row['device_id']] = idx
        
        # 初始化设备特征（8维）
        self.device_features = np.zeros((self.device_count, 8), dtype=np.float32)
        
        for idx, row in enumerate(device_data):
            device_id = row['device_id']
            category_id = row['category_id'] or 0
            lab_id = row['lab_id'] or 0
            status = row['status'] or '0'
            device_name = row['device_name'] or ''
            specs = row['specs'] or ''
            remark = row['remark'] or ''
            create_time = row['create_time']
            
            # 计算设备使用时间
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
            
            # 特征归一化到 [0, 1]
            self.device_features[idx] = [
                category_id / 20.0 if category_id > 0 else 0.0,  # 分类ID
                lab_id / 10.0 if lab_id > 0 else 0.0,  # 实验室ID
                int(status) / 2.0 if status else 0.0,  # 状态
                len(device_name) / 50.0 if device_name else 0.0,  # 名称长度
                len(specs) / 100.0 if specs else 0.0,  # 规格长度
                year_diff,  # 使用年份
                len(remark) / 50.0 if remark else 0.0,  # 备注长度
                1.0 if status == '0' else 0.0  # 是否可用
            ]
        
        logger.debug(f"设备特征维度: {self.device_features.shape}")
    
    def _load_edge_data(self):
        """加载用户-设备预约关系（边）"""
        logger.info("加载预约关系边数据...")
        
        sql = """
            SELECT DISTINCT user_id, device_id 
            FROM lab_reservation
            WHERE user_id IS NOT NULL AND device_id IS NOT NULL
        """
        
        edge_data = db.execute_query(sql, dict_cursor=True)
        
        # 构建边索引
        edges = []
        for row in edge_data:
            user_id = row['user_id']
            device_id = row['device_id']
            
            if user_id in self.user_id_map and device_id in self.device_id_map:
                user_idx = self.user_id_map[user_id]
                # 设备节点在用户节点之后
                device_idx = self.user_count + self.device_id_map[device_id]
                edges.append([user_idx, device_idx])
                edges.append([device_idx, user_idx])  # 双向边
        
        if edges:
            self.edge_index = torch.tensor(edges, dtype=torch.long).t().contiguous()
        else:
            self.edge_index = torch.empty((2, 0), dtype=torch.long)
        
        logger.debug(f"边索引维度: {self.edge_index.shape}")
    
    def _build_graph_data(self) -> Data:
        """构建PyTorch Geometric图数据对象"""
        # 合并用户和设备特征
        x = np.vstack((self.user_features, self.device_features))
        x_tensor = torch.tensor(x, dtype=torch.float32)
        
        graph_data = Data(x=x_tensor, edge_index=self.edge_index)
        return graph_data
    
    def _save_to_file(self):
        """保存数据到文件"""
        data_dir = config.data_dir
        np.save(data_dir / "user_features.npy", self.user_features)
        np.save(data_dir / "device_features.npy", self.device_features)
        np.save(data_dir / "edge_index.npy", self.edge_index.numpy())
        
        # 保存ID映射
        import pickle
        with open(data_dir / "user_id_map.pkl", 'wb') as f:
            pickle.dump(self.user_id_map, f)
        with open(data_dir / "device_id_map.pkl", 'wb') as f:
            pickle.dump(self.device_id_map, f)
        
        logger.debug(f"数据已保存到: {data_dir}")
    
    def load_from_file(self) -> Data:
        """
        从文件加载数据
        
        Returns:
            Data: PyTorch Geometric图数据对象
        """
        logger.info("从文件加载图数据...")
        
        data_dir = config.data_dir
        
        self.user_features = np.load(data_dir / "user_features.npy")
        self.device_features = np.load(data_dir / "device_features.npy")
        edge_idx_np = np.load(data_dir / "edge_index.npy")
        self.edge_index = torch.tensor(edge_idx_np, dtype=torch.long)
        
        # 加载ID映射
        import pickle
        with open(data_dir / "user_id_map.pkl", 'rb') as f:
            self.user_id_map = pickle.load(f)
        with open(data_dir / "device_id_map.pkl", 'rb') as f:
            self.device_id_map = pickle.load(f)
        
        self.user_count = len(self.user_features)
        self.device_count = len(self.device_features)
        
        graph_data = self._build_graph_data()
        
        logger.info(f"从文件加载完成: 用户数={self.user_count}, 设备数={self.device_count}")
        return graph_data
    
    def data_exists(self) -> bool:
        """检查数据文件是否存在"""
        data_dir = config.data_dir
        return (
            (data_dir / "user_features.npy").exists() and
            (data_dir / "device_features.npy").exists() and
            (data_dir / "edge_index.npy").exists()
        )
