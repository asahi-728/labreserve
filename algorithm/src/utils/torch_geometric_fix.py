"""
torch_geometric 兼容性修复模块
解决 Python 3.10+ 与 torch_geometric 的类型提示兼容性问题
"""

import sys
import types
from typing import Union, Optional, Any


def patch_torch_geometric_inspector():
    """
    修补 torch_geometric 的 inspector 模块
    解决 'typing.Union' object has no attribute '_name' 错误
    """
    try:
        import torch_geometric.inspector as inspector_module
        
        # 保存原始的 type_repr 函数
        original_type_repr = getattr(inspector_module, 'type_repr', None)
        
        if original_type_repr is None:
            # 尝试从内部找到 type_repr
            for attr_name in dir(inspector_module):
                attr = getattr(inspector_module, attr_name)
                if callable(attr) and 'type_repr' in attr_name:
                    original_type_repr = attr
                    break
        
        def patched_type_repr(obj: Any, _globals: Optional[dict] = None) -> str:
            """
            修补后的 type_repr 函数
            处理 Union 和 UnionType 类型
            """
            try:
                # 检查是否是 Union 或 UnionType
                origin = getattr(obj, '__origin__', None)
                
                if origin is Union or (sys.version_info >= (3, 10) and 
                                      origin is types.UnionType):
                    # 处理 Union 类型
                    args = getattr(obj, '__args__', [])
                    if args:
                        arg_reprs = [patched_type_repr(arg, _globals) for arg in args]
                        return f"Union[{', '.join(arg_reprs)}]"
                    return "Union"
                
                # 检查是否有 _name 属性
                if hasattr(obj, '_name'):
                    return original_type_repr(obj, _globals) if original_type_repr else str(obj)
                
                # 检查是否有 __name__ 属性
                if hasattr(obj, '__name__'):
                    return obj.__name__
                
                # 检查是否是 typing 模块中的类型
                if hasattr(obj, '__module__') and obj.__module__ == 'typing':
                    return repr(obj)
                
                # 回退到原始函数或字符串表示
                if original_type_repr:
                    try:
                        return original_type_repr(obj, _globals)
                    except:
                        pass
                
                return str(obj)
                
            except Exception as e:
                # 如果出错，返回简单的字符串表示
                return str(obj)
        
        # 替换 inspector 模块中的 type_repr 函数
        # 首先查找 Inspector 类
        if hasattr(inspector_module, 'Inspector'):
            Inspector = inspector_module.Inspector
            
            # 替换 Inspector 类的 type_repr 方法
            if hasattr(Inspector, 'type_repr'):
                original_inspector_type_repr = Inspector.type_repr
                
                def new_inspector_type_repr(self, obj):
                    return patched_type_repr(obj)
                
                Inspector.type_repr = new_inspector_type_repr
        
        # 同时也替换模块级别的 type_repr 函数（如果存在）
        if hasattr(inspector_module, 'type_repr'):
            inspector_module.type_repr = patched_type_repr
        
        # 也尝试替换内部的 type_repr 函数
        for attr_name in dir(inspector_module):
            attr = getattr(inspector_module, attr_name)
            if callable(attr) and 'type_repr' in attr.__name__:
                setattr(inspector_module, attr_name, patched_type_repr)
        
        print("✓ torch_geometric inspector 模块修补完成")
        return True
        
    except ImportError:
        print("⚠ torch_geometric 未安装，跳过修补")
        return False
    except Exception as e:
        print(f"⚠ 修补 torch_geometric 时出错: {e}")
        import traceback
        traceback.print_exc()
        return False


def patch_torch_geometric():
    """
    应用所有 torch_geometric 相关的修补
    """
    print("开始修补 torch_geometric 兼容性问题...")
    
    # 1. 修补 inspector 模块
    success = patch_torch_geometric_inspector()
    
    if success:
        print("torch_geometric 兼容性修补完成！")
    else:
        print("torch_geometric 修补失败或无需修补")
    
    return success


# 在导入 torch_geometric 之前自动应用修补
if __name__ != '__main__':
    patch_torch_geometric()
