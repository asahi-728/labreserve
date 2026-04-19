package com.ruoyi.project.lab.service;

import java.util.List;
import com.ruoyi.project.lab.domain.LabAlgorithmTrainingLog;

/**
 * 算法训练日志Service接口
 * 
 * @author ruoyi
 * @date 2026-04-15
 */
public interface ILabAlgorithmTrainingLogService 
{
    /**
     * 查询算法训练日志
     * 
     * @param trainingId 算法训练日志主键
     * @return 算法训练日志
     */
    public LabAlgorithmTrainingLog selectLabAlgorithmTrainingLogByTrainingId(Long trainingId);

    /**
     * 查询算法训练日志列表
     * 
     * @param labAlgorithmTrainingLog 算法训练日志
     * @return 算法训练日志集合
     */
    public List<LabAlgorithmTrainingLog> selectLabAlgorithmTrainingLogList(LabAlgorithmTrainingLog labAlgorithmTrainingLog);

    /**
     * 新增算法训练日志
     * 
     * @param labAlgorithmTrainingLog 算法训练日志
     * @return 结果
     */
    public int insertLabAlgorithmTrainingLog(LabAlgorithmTrainingLog labAlgorithmTrainingLog);

    /**
     * 修改算法训练日志
     * 
     * @param labAlgorithmTrainingLog 算法训练日志
     * @return 结果
     */
    public int updateLabAlgorithmTrainingLog(LabAlgorithmTrainingLog labAlgorithmTrainingLog);

    /**
     * 批量删除算法训练日志
     * 
     * @param trainingIds 需要删除的算法训练日志主键集合
     * @return 结果
     */
    public int deleteLabAlgorithmTrainingLogByTrainingIds(Long[] trainingIds);

    /**
     * 删除算法训练日志信息
     * 
     * @param trainingId 算法训练日志主键
     * @return 结果
     */
    public int deleteLabAlgorithmTrainingLogByTrainingId(Long trainingId);

    /**
     * 开始训练任务
     * 
     * @param labAlgorithmTrainingLog 算法训练日志
     * @return 训练任务ID
     */
    public Long startTraining(LabAlgorithmTrainingLog labAlgorithmTrainingLog);

    /**
     * 更新训练进度
     * 
     * @param trainingId 训练任务ID
     * @param completedEpochs 已完成迭代次数
     * @param lossValue 当前损失值
     * @return 结果
     */
    public int updateTrainingProgress(Long trainingId, Integer completedEpochs, java.math.BigDecimal lossValue);

    /**
     * 完成训练任务
     * 
     * @param trainingId 训练任务ID
     * @param lossValue 最终损失值
     * @param accuracy 准确率
     * @param modelVersion 模型版本
     * @param metrics 训练指标详情（JSON格式，每轮的loss/accuracy/reward等）
     * @return 结果
     */
    public int completeTraining(Long trainingId, java.math.BigDecimal lossValue, java.math.BigDecimal accuracy, String modelVersion, String metrics);

    /**
     * 失败训练任务
     * 
     * @param trainingId 训练任务ID
     * @param errorMessage 错误信息
     * @return 结果
     */
    public int failTraining(Long trainingId, String errorMessage);
}
