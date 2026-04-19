"""
算法 API 主服务模块
启动 FastAPI 服务并注册所有路由
"""

# 在导入其他模块之前先修补 torch_geometric 的兼容性问题
import sys
from pathlib import Path
from contextlib import asynccontextmanager

# 添加项目根目录到路径
project_root = Path(__file__).parent.parent.parent
sys.path.insert(0, str(project_root))

# 修补 torch_geometric 兼容性问题
try:
    from src.utils.torch_geometric_fix import patch_torch_geometric
    patch_torch_geometric()
except Exception as e:
    print(f"⚠ 应用 torch_geometric 修补失败: {e}")

from fastapi import FastAPI, Request
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse
import uvicorn

from src.utils import config, logger
from src.api.routes.gat import router as gat_router
from src.api.routes.ppo import router as ppo_router


@asynccontextmanager
async def lifespan(app: FastAPI):
    """
    FastAPI应用生命周期管理
    在应用启动和关闭时执行相应逻辑
    """
    # 启动逻辑 - 在应用开始接收请求之前执行
    logger.info("=" * 60)
    logger.info("实验室智能算法服务启动中...")
    logger.info("=" * 60)
    
    # 尝试初始化GATv2推荐器（不强制）
    try:
        from src.algorithms.gatv2 import recommender
        logger.info("正在初始化GATv2推荐器...")
        # 只检查数据是否存在，不强制训练
        if recommender.data_loader.data_exists():
            model_path = config.model_dir / "gatv2_model.pth"
            if model_path.exists():
                success = recommender.initialize(force_reload=False)
                if success:
                    logger.info("GATv2推荐器初始化成功！")
                else:
                    logger.warning("GATv2推荐器初始化失败，将在首次请求时初始化")
            else:
                logger.info("GATv2模型文件不存在，将在首次请求时训练")
        else:
            logger.info("GATv2数据文件不存在，将在首次请求时加载")
    except Exception as e:
        logger.warning(f"启动时初始化GATv2推荐器失败: {str(e)}")
        logger.info("将在首次请求时初始化")
    
    # 尝试初始化PPO调度器（不强制）
    try:
        from src.algorithms.ppo import scheduler
        logger.info("正在初始化PPO调度器...")
        # 检查scheduler是否有data_exists方法，没有则跳过
        has_data_exists = hasattr(scheduler, 'data_exists')
        if has_data_exists:
            # 只检查数据是否存在，不强制训练
            if scheduler.data_exists():
                model_path = config.model_dir / "ppo_model.pth"
                if model_path.exists():
                    success = scheduler.initialize(force_reload=False)
                    if success:
                        logger.info("PPO调度器初始化成功！")
                    else:
                        logger.warning("PPO调度器初始化失败，将在首次请求时初始化")
                else:
                    logger.info("PPO模型文件不存在，将在首次请求时训练")
            else:
                logger.info("PPO数据文件不存在，将在首次请求时加载")
        else:
            logger.info("PPO调度器没有data_exists方法，将在首次请求时初始化")
    except Exception as e:
        logger.warning(f"启动时初始化PPO调度器失败: {str(e)}")
        logger.info("将在首次请求时初始化")
    
    logger.info("=" * 60)
    logger.info("实验室智能算法服务启动完成！")
    logger.info(f"API文档地址: http://{config.api_host}:{config.api_port}/docs")
    logger.info("=" * 60)
    
    # yield之前是启动逻辑，之后是关闭逻辑
    yield
    
    # 关闭逻辑 - 在应用完成处理所有请求后执行
    logger.info("实验室智能算法服务正在关闭...")
    logger.info("服务已停止")


def create_app() -> FastAPI:
    """
    创建并配置 FastAPI 应用
    
    Returns:
        FastAPI: 配置好的 FastAPI 应用
    """
    app = FastAPI(
        title="实验室智能算法服务 API",
        description="基于GATv2和PPO算法的实验室设备智能推荐与调度系统 API 文档",
        version="1.0.0",
        docs_url="/docs",
        redoc_url="/redoc",
        openapi_url="/openapi.json",
        lifespan=lifespan
    )
    
    # 配置 CORS
    app.add_middleware(
        CORSMiddleware,
        allow_origins=["*"],
        allow_credentials=True,
        allow_methods=["*"],
        allow_headers=["*"],
    )
    
    # 异常处理器
    @app.exception_handler(Exception)
    async def global_exception_handler(request: Request, exc: Exception):
        logger.exception(f"未处理的异常: {str(exc)}")
        return JSONResponse(
            status_code=500,
            content={
                "code": 500,
                "message": "内部服务器错误",
                "detail": str(exc) if config.api_debug else None
            }
        )
    
    # 注册路由
    app.include_router(gat_router, prefix="/api/gat")
    app.include_router(ppo_router, prefix="/api/ppo")
    
    # 根路径
    @app.get("/")
    async def root():
        return {
            "service": "Laboratory Intelligent Algorithm Service",
            "version": "1.0.0",
            "status": "running",
            "docs": "/docs",
            "endpoints": {
                "gat": "/api/gat",
                "ppo": "/api/ppo"
            }
        }
    
    return app


app = create_app()


def main():
    """
    启动服务的主函数
    """
    uvicorn.run(
        "src.api.main:app",
        host=config.api_host,
        port=config.api_port,
        # reload=config.api_debug,
        reload=False,
        log_level=config.log_level.lower()
    )


if __name__ == "__main__":
    main()
