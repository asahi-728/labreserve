# GATv2 个性化推荐服务

基于图注意力网络（GATv2）的高校实验室设备个性化推荐系统。

## 项目简介

本系统使用 GATv2 图注意力网络实现高校实验室设备的个性化推荐。通过构建用户-设备二部图，学习用户偏好和设备关联特征，为用户提供精准的设备推荐。

## 技术栈

- **深度学习框架**: PyTorch 1.12.1
- **图神经网络**: PyTorch Geometric 2.3.1
- **API框架**: FastAPI 0.103.1
- **数据库连接**: PyMySQL 1.0.2
- **数据处理**: NumPy 1.24.3, scikit-learn 1.3.0
- **测试框架**: pytest 7.4.0

## 目录结构

```
GAT/
├── src/
│   ├── __init__.py
│   ├── algorithms/         # 算法核心模块
│   │   ├── __init__.py
│   │   ├── data_loader.py # 数据加载器
│   │   ├── model.py        # GATv2模型定义
│   │   └── recommender.py  # 推荐器
│   ├── api/                # API接口模块
│   │   ├── __init__.py
│   │   ├── main.py         # FastAPI主应用
│   │   ├── routes.py       # API路由
│   │   └── schemas.py      # 数据模型
│   └── utils/              # 工具模块
│       ├── __init__.py
│       ├── config.py       # 配置管理
│       ├── logger.py       # 日志模块
│       └── database.py     # 数据库连接
├── tests/                   # 测试模块
│   ├── __init__.py
│   ├── conftest.py
│   ├── test_model.py       # 模型测试
│   └── test_api.py         # API测试
├── data/                    # 数据目录
├── models/                  # 模型目录
├── logs/                    # 日志目录
├── requirements.txt         # 依赖包
├── .env.example            # 环境变量示例
└── README.md               # 项目文档
```

## 快速开始

### 1. 环境配置

#### 1.1 复制环境变量文件

```bash
cp .env.example .env
```

#### 1.2 编辑环境变量

编辑 `.env` 文件，配置数据库连接等参数：

```env
# 数据库配置
DB_HOST=localhost
DB_PORT=3306
DB_USER=root
DB_PASSWORD=123456
DB_NAME=ruoyi

# 模型配置
MODEL_IN_CHANNELS=8
MODEL_HIDDEN_CHANNELS=16
MODEL_OUT_CHANNELS=4
MODEL_LR=0.01
MODEL_EPOCHS=100

# API配置
API_HOST=0.0.0.0
API_PORT=8000
API_DEBUG=True
```

### 2. 安装依赖

```bash
pip install -r requirements.txt
```

注意：PyTorch Geometric 的安装可能需要根据CUDA版本选择对应的安装包。详情请参考 [PyTorch Geometric 官网](https://pytorch-geometric.readthedocs.io/)。

### 3. 启动服务

```bash
# 方式1：直接运行
python -m src.api.main

# 方式2：使用uvicorn
uvicorn src.api.main:app --host 0.0.0.0 --port 8000 --reload
```

服务启动后，访问以下地址查看API文档：
- Swagger UI: http://localhost:8000/docs
- ReDoc: http://localhost:8000/redoc

## API 接口文档

### 健康检查

```http
GET /api/v1/health
```

**响应示例**:
```json
{
  "status": "ok",
  "service": "GATv2 Recommendation Service",
  "timestamp": "2026-04-15T12:00:00",
  "version": "1.0.0"
}
```

### 获取推荐

```http
POST /api/v1/recommend
Content-Type: application/json

{
  "user_id": 1,
  "top_k": 5,
  "save_to_db": true
}
```

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "user_id": 1,
      "device_id": 10,
      "similarity": 0.85,
      "rank": 1,
      "recommend_time": "2026-04-15 12:00:00"
    }
  ]
}
```

### 获取嵌入向量

```http
POST /api/v1/embedding
Content-Type: application/json

{
  "user_id": 1,
  "device_id": 10
}
```

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "timestamp": "2026-04-15T12:00:00",
    "user_id": 1,
    "user_embedding": [0.1, 0.2, 0.3, 0.4],
    "device_id": 10,
    "device_embedding": [0.5, 0.6, 0.7, 0.8]
  }
}
```

### 触发训练

```http
POST /api/v1/train
Content-Type: application/json

{
  "force_reload": false,
  "epochs": 100
}
```

**响应示例**:
```json
{
  "code": 200,
  "message": "训练任务已启动",
  "data": {
    "status": "training_started",
    "timestamp": "2026-04-15T12:00:00"
  }
}
```

### 获取系统状态

```http
GET /api/v1/status
```

### 获取训练状态

```http
GET /api/v1/train/status
```

### 批量推荐

```http
POST /api/v1/recommend/batch
Content-Type: application/json

{
  "user_ids": [1, 2, 3],
  "top_k": 5,
  "save_to_db": true
}
```

## 算法原理

### GATv2 图注意力网络

GATv2（Graph Attention Network v2）是一种动态图注意力网络，能够自适应地调整节点间的注意力权重。

**核心特点**:
- 动态注意力机制，自适应调整节点关联权重
- 两层图注意力卷积层，学习高维节点嵌入
- 自监督学习，通过特征重构损失训练

### 推荐流程

1. **数据加载**: 从数据库读取用户、设备、预约记录
2. **图构建**: 构建用户-设备二部图
3. **模型训练**: 使用GATv2学习节点嵌入
4. **相似度计算**: 计算用户与设备的余弦相似度
5. **推荐生成**: 筛选Top-K设备，排除不可用设备

## 测试

### 运行单元测试

```bash
pytest tests/test_model.py -v
```

### 运行API测试

```bash
pytest tests/test_api.py -v
```

### 运行所有测试

```bash
pytest tests/ -v --cov=src
```

## 配置说明

### 模型参数

| 参数 | 默认值 | 说明 |
|------|--------|------|
| MODEL_IN_CHANNELS | 8 | 输入特征维度 |
| MODEL_HIDDEN_CHANNELS | 16 | 隐藏层维度 |
| MODEL_OUT_CHANNELS | 4 | 输出嵌入维度 |
| MODEL_LR | 0.01 | 学习率 |
| MODEL_EPOCHS | 100 | 训练轮数 |

### 推荐参数

| 参数 | 默认值 | 说明 |
|------|--------|------|
| RECOMMEND_TOP_K | 5 | 默认推荐数量 |
| RECOMMEND_EXCLUDE_RESERVED | True | 是否排除已预约设备 |
| RECOMMEND_EXCLUDE_MAINTENANCE | True | 是否排除维修中设备 |

## 与系统集成

### 后端对接

Java后端可以通过HTTP客户端调用GATv2 API接口：

```java
// 获取推荐
RestTemplate restTemplate = new RestTemplate();
String url = "http://localhost:8000/api/v1/recommend";
RecommendRequest request = new RecommendRequest(userId, 5, true);
RecommendResponse response = restTemplate.postForObject(url, request, RecommendResponse.class);
```

### 前端展示

前端通过API获取推荐列表，在设备推荐页面展示：

```javascript
// 调用API获取推荐
const response = await axios.post('/api/v1/recommend', {
  user_id: userId,
  top_k: 5
});

// 展示推荐结果
recommendations.value = response.data.data;
```

## 常见问题

### Q: 模型训练需要多长时间？

A: 训练时间取决于数据规模。一般情况下，100轮训练在1000个节点和10000条边的图上需要5-10分钟。

### Q: 如何处理冷启动问题？

A: 对于新用户，系统会推荐热门设备；对于新设备，系统会推荐给相似用户。

### Q: 如何更新模型？

A: 可以通过调用 `/api/v1/train` 接口触发重新训练，也可以设置定时任务定期更新模型。

## 许可证

本项目基于若依框架开发，遵循相应的开源协议。

## 联系方式

如有问题或建议，请通过以下方式联系：

- 提交 Issue
- 发送邮件

## 更新日志

### v1.0.0 (2026-04-15)

- 初始版本发布
- 实现GATv2图注意力网络推荐
- 实现FastAPI接口
- 支持个性化设备推荐
- 支持用户和设备嵌入向量获取
