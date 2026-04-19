"""
GATv2 算法日志模块
提供统一的日志记录功能
"""

import logging
import colorlog
from pathlib import Path
from datetime import datetime
from typing import Optional, List, Dict
from collections import deque
from .config import config


class MemoryLogHandler(logging.Handler):
    """内存日志处理器，用于捕获日志并存储在内存中"""
    
    def __init__(self, max_logs: int = 100):
        super().__init__()
        self.logs = deque(maxlen=max_logs)
    
    def emit(self, record):
        """处理日志记录"""
        try:
            log_entry = {
                'time': datetime.fromtimestamp(record.created).strftime('%H:%M:%S'),
                'type': record.levelname.lower(),
                'message': self.format(record)
            }
            self.logs.append(log_entry)
        except Exception:
            pass
    
    def get_logs(self) -> List[Dict]:
        """获取所有日志"""
        return list(self.logs)
    
    def clear_logs(self):
        """清空日志"""
        self.logs.clear()


class Logger:
    """GATv2 算法日志类"""
    
    _instance: Optional['Logger'] = None
    
    def __new__(cls, *args, **kwargs):
        """单例模式，确保全局只有一个日志实例"""
        if cls._instance is None:
            cls._instance = super().__new__(cls)
        return cls._instance
    
    def __init__(self, name: str = "GATv2"):
        """
        初始化日志记录器
        
        Args:
            name: 日志记录器名称
        """
        if hasattr(self, '_initialized'):
            return
        
        self.logger = logging.getLogger(name)
        self.logger.setLevel(getattr(logging, config.log_level.upper(), logging.INFO))
        self.logger.handlers.clear()
        
        # 日志格式
        log_format = (
            "%(log_color)s%(asctime)s | %(levelname)-8s | "
            "%(name)s:%(lineno)d | %(message)s%(reset)s"
        )
        date_format = "%Y-%m-%d %H:%M:%S"
        
        # 简单格式（用于内存存储）
        simple_format = "%(message)s"
        simple_formatter = logging.Formatter(simple_format)
        
        # 内存日志处理器
        self.memory_handler = MemoryLogHandler(max_logs=200)
        self.memory_handler.setFormatter(simple_formatter)
        self.logger.addHandler(self.memory_handler)
        
        # 控制台输出（带颜色）
        console_handler = colorlog.StreamHandler()
        console_formatter = colorlog.ColoredFormatter(
            log_format,
            datefmt=date_format,
            log_colors={
                'DEBUG': 'cyan',
                'INFO': 'green',
                'WARNING': 'yellow',
                'ERROR': 'red',
                'CRITICAL': 'red,bg_white',
            }
        )
        console_handler.setFormatter(console_formatter)
        self.logger.addHandler(console_handler)
        
        # 文件输出
        log_file = config.log_dir / f"gatv2_{datetime.now().strftime('%Y%m%d')}.log"
        file_handler = logging.FileHandler(log_file, encoding='utf-8')
        file_formatter = logging.Formatter(
            "%(asctime)s | %(levelname)-8s | %(name)s:%(lineno)d | %(message)s",
            datefmt=date_format
        )
        file_handler.setFormatter(file_formatter)
        self.logger.addHandler(file_handler)
        
        self._initialized = True
    
    def get_memory_logs(self) -> List[Dict]:
        """获取内存中的日志"""
        return self.memory_handler.get_logs()
    
    def clear_memory_logs(self):
        """清空内存中的日志"""
        self.memory_handler.clear_logs()
    
    def debug(self, msg: str, *args, **kwargs):
        """记录调试日志"""
        self.logger.debug(msg, *args, **kwargs)
    
    def info(self, msg: str, *args, **kwargs):
        """记录信息日志"""
        self.logger.info(msg, *args, **kwargs)
    
    def warning(self, msg: str, *args, **kwargs):
        """记录警告日志"""
        self.logger.warning(msg, *args, **kwargs)
    
    def error(self, msg: str, *args, **kwargs):
        """记录错误日志"""
        self.logger.error(msg, *args, **kwargs)
    
    def critical(self, msg: str, *args, **kwargs):
        """记录严重错误日志"""
        self.logger.critical(msg, *args, **kwargs)
    
    def exception(self, msg: str, *args, **kwargs):
        """记录异常日志（带堆栈信息）"""
        self.logger.exception(msg, *args, **kwargs)


# 全局日志实例
logger = Logger()
