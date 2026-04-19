"""
测试配置和fixtures
"""

import sys
from pathlib import Path
import pytest

# 添加项目根目录到路径
project_root = Path(__file__).parent.parent
sys.path.insert(0, str(project_root))


@pytest.fixture(scope="session")
def test_data_dir(tmp_path_factory):
    """测试数据目录fixture"""
    data_dir = tmp_path_factory.mktemp("test_data")
    return data_dir


@pytest.fixture(scope="session")
def test_model_dir(tmp_path_factory):
    """测试模型目录fixture"""
    model_dir = tmp_path_factory.mktemp("test_models")
    return model_dir
