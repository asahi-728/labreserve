"""
算法配置模块
提供统一的配置管理
"""

import os
from pathlib import Path
from typing import Optional
from dotenv import load_dotenv


class Config:
    """算法配置类"""
    
    # 项目根目录
    BASE_DIR = Path(__file__).parent.parent.parent
    
    def __init__(self, env_file: Optional[str] = None):
        """
        初始化配置
        
        Args:
            env_file: 环境变量文件路径，默认为 .env
        """
        if env_file is None:
            env_file = str(self.BASE_DIR / ".env")
        
        load_dotenv(env_file)
        
        # 数据库配置
        self.db_host = os.getenv("DB_HOST", "localhost")
        self.db_port = int(os.getenv("DB_PORT", "3306"))
        self.db_user = os.getenv("DB_USER", "root")
        self.db_password = os.getenv("DB_PASSWORD", "123456")
        self.db_name = os.getenv("DB_NAME", "ruoyi")
        self.db_charset = os.getenv("DB_CHARSET", "utf8mb4")
        
        # GATv2 模型配置
        self.model_in_channels = int(os.getenv("MODEL_IN_CHANNELS", "8"))
        self.model_hidden_channels = int(os.getenv("MODEL_HIDDEN_CHANNELS", "16"))
        self.model_out_channels = int(os.getenv("MODEL_OUT_CHANNELS", "4"))
        self.model_lr = float(os.getenv("MODEL_LR", "0.01"))
        self.model_epochs = int(os.getenv("MODEL_EPOCHS", "100"))
        
        # PPO 模型配置
        self.ppo_hidden_size = int(os.getenv("PPO_HIDDEN_SIZE", "32"))
        self.ppo_lr = float(os.getenv("PPO_LR", "3e-4"))
        self.ppo_gamma = float(os.getenv("PPO_GAMMA", "0.99"))
        self.ppo_clip_epsilon = float(os.getenv("PPO_CLIP_EPSILON", "0.2"))
        self.ppo_epochs = int(os.getenv("PPO_EPOCHS", "10"))
        self.ppo_max_steps = int(os.getenv("PPO_MAX_STEPS", "100"))
        self.ppo_batch_size = int(os.getenv("PPO_BATCH_SIZE", "32"))
        
        # API服务配置
        self.api_host = os.getenv("API_HOST", "0.0.0.0")
        self.api_port = int(os.getenv("API_PORT", "8000"))
        self.api_debug = os.getenv("API_DEBUG", "True").lower() == "true"
        
        # 推荐配置
        self.recommend_top_k = int(os.getenv("RECOMMEND_TOP_K", "5"))
        self.recommend_exclude_reserved = os.getenv("RECOMMEND_EXCLUDE_RESERVED", "True").lower() == "true"
        self.recommend_exclude_maintenance = os.getenv("RECOMMEND_EXCLUDE_MAINTENANCE", "True").lower() == "true"
        
        # 日志配置
        self.log_level = os.getenv("LOG_LEVEL", "INFO")
        self.log_dir = Path(os.getenv("LOG_DIR", "./logs"))
        
        # 确保日志目录存在
        self.log_dir.mkdir(parents=True, exist_ok=True)
        
        # 数据目录
        self.data_dir = self.BASE_DIR / "data"
        self.data_dir.mkdir(parents=True, exist_ok=True)
        
        # 模型目录
        self.model_dir = self.BASE_DIR / "models"
        self.model_dir.mkdir(parents=True, exist_ok=True)
    
    def get_db_config(self) -> dict:
        """
        获取数据库配置字典
        
        Returns:
            dict: 数据库连接配置
        """
        return {
            "host": self.db_host,
            "port": self.db_port,
            "user": self.db_user,
            "password": self.db_password,
            "database": self.db_name,
            "charset": self.db_charset
        }


# 全局配置实例
config = Config()
