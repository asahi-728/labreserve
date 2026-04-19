"""
GATv2 算法数据库模块
提供数据库连接和数据操作功能
"""

import pymysql
from pymysql.cursors import DictCursor
from typing import List, Dict, Any, Optional, Tuple
from contextlib import contextmanager
from .config import config
from .logger import logger


class DatabaseConnection:
    """数据库连接管理器"""
    
    def __init__(self):
        self.db_config = config.get_db_config()
    
    @contextmanager
    def get_connection(self):
        """
        获取数据库连接（上下文管理器）
        
        Yields:
            pymysql.Connection: 数据库连接
        """
        conn = None
        try:
            conn = pymysql.connect(**self.db_config)
            logger.debug(f"数据库连接成功: {self.db_config['host']}:{self.db_config['port']}")
            yield conn
        except pymysql.Error as e:
            logger.error(f"数据库连接失败: {str(e)}")
            raise
        finally:
            if conn:
                conn.close()
                logger.debug("数据库连接已关闭")
    
    @contextmanager
    def get_cursor(self, dict_cursor: bool = False):
        """
        获取数据库游标（上下文管理器）
        
        Args:
            dict_cursor: 是否使用字典游标
        
        Yields:
            pymysql.Cursor: 数据库游标
        """
        with self.get_connection() as conn:
            cursor_class = DictCursor if dict_cursor else pymysql.cursors.Cursor
            cursor = conn.cursor(cursor=cursor_class)
            try:
                yield cursor
                conn.commit()
            except Exception as e:
                conn.rollback()
                logger.error(f"数据库操作失败，已回滚: {str(e)}")
                raise
            finally:
                cursor.close()
    
    def execute_query(self, sql: str, params: Optional[Tuple] = None, 
                     dict_cursor: bool = False) -> List[Dict[str, Any]]:
        """
        执行查询语句
        
        Args:
            sql: SQL语句
            params: 参数元组
            dict_cursor: 是否返回字典格式
        
        Returns:
            List[Dict]: 查询结果列表
        """
        with self.get_cursor(dict_cursor=dict_cursor) as cursor:
            cursor.execute(sql, params or ())
            result = cursor.fetchall()
            logger.debug(f"执行查询: {sql[:50]}... 返回 {len(result)} 条记录")
            return result
    
    def execute_update(self, sql: str, params: Optional[Tuple] = None) -> int:
        """
        执行更新/插入/删除语句
        
        Args:
            sql: SQL语句
            params: 参数元组
        
        Returns:
            int: 影响的行数
        """
        with self.get_cursor() as cursor:
            affected_rows = cursor.execute(sql, params or ())
            logger.debug(f"执行更新: {sql[:50]}... 影响 {affected_rows} 行")
            return affected_rows
    
    def execute_batch(self, sql: str, params_list: List[Tuple]) -> int:
        """
        批量执行SQL语句
        
        Args:
            sql: SQL语句
            params_list: 参数列表
        
        Returns:
            int: 影响的总行数
        """
        with self.get_cursor() as cursor:
            affected_rows = cursor.executemany(sql, params_list)
            logger.debug(f"批量执行: {sql[:50]}... 影响 {affected_rows} 行")
            return affected_rows


# 全局数据库实例
db = DatabaseConnection()
