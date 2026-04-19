package com.ruoyi.project.lab.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.project.lab.mapper.LabAlgorithmTrainingLogMapper;
import com.ruoyi.project.lab.domain.LabAlgorithmTrainingLog;
import com.ruoyi.project.lab.service.ILabAlgorithmTrainingLogService;

/**
 * 算法训练日志Service业务层处理
 * 
 * @author ruoyi
 * @date 2026-04-15
 */
@Service
public class LabAlgorithmTrainingLogServiceImpl implements ILabAlgorithmTrainingLogService 
{
    @Autowired
    private LabAlgorithmTrainingLogMapper labAlgorithmTrainingLogMapper;

    /**
     * 查询算法训练日志
     * 
     * @param trainingId 算法训练日志主键
     * @return 算法训练日志
     */
    @Override
    public LabAlgorithmTrainingLog selectLabAlgorithmTrainingLogByTrainingId(Long trainingId)
    {
        return labAlgorithmTrainingLogMapper.selectLabAlgorithmTrainingLogByTrainingId(trainingId);
    }

    /**
     * 查询算法训练日志列表
     * 
     * @param labAlgorithmTrainingLog 算法训练日志
     * @return 算法训练日志
     */
    @Override
    public List<LabAlgorithmTrainingLog> selectLabAlgorithmTrainingLogList(LabAlgorithmTrainingLog labAlgorithmTrainingLog)
    {
        return labAlgorithmTrainingLogMapper.selectLabAlgorithmTrainingLogList(labAlgorithmTrainingLog);
    }

    /**
     * 新增算法训练日志
     * 
     * @param labAlgorithmTrainingLog 算法训练日志
     * @return 结果
     */
    @Override
    @Transactional
    public int insertLabAlgorithmTrainingLog(LabAlgorithmTrainingLog labAlgorithmTrainingLog)
    {
        labAlgorithmTrainingLog.setCreateTime(DateUtils.getNowDate());
        labAlgorithmTrainingLog.setCreateBy(SecurityUtils.getUsername());
        return labAlgorithmTrainingLogMapper.insertLabAlgorithmTrainingLog(labAlgorithmTrainingLog);
    }

    /**
     * 修改算法训练日志
     * 
     * @param labAlgorithmTrainingLog 算法训练日志
     * @return 结果
     */
    @Override
    @Transactional
    public int updateLabAlgorithmTrainingLog(LabAlgorithmTrainingLog labAlgorithmTrainingLog)
    {
        labAlgorithmTrainingLog.setUpdateTime(DateUtils.getNowDate());
        labAlgorithmTrainingLog.setUpdateBy(SecurityUtils.getUsername());
        return labAlgorithmTrainingLogMapper.updateLabAlgorithmTrainingLog(labAlgorithmTrainingLog);
    }

    /**
     * 批量删除算法训练日志
     * 
     * @param trainingIds 需要删除的算法训练日志主键
     * @return 结果
     */
    @Override
    @Transactional
    public int deleteLabAlgorithmTrainingLogByTrainingIds(Long[] trainingIds)
    {
        return labAlgorithmTrainingLogMapper.deleteLabAlgorithmTrainingLogByTrainingIds(trainingIds);
    }

    /**
     * 删除算法训练日志信息
     * 
     * @param trainingId 算法训练日志主键
     * @return 结果
     */
    @Override
    @Transactional
    public int deleteLabAlgorithmTrainingLogByTrainingId(Long trainingId)
    {
        return labAlgorithmTrainingLogMapper.deleteLabAlgorithmTrainingLogByTrainingId(trainingId);
    }

    /**
     * 开始训练任务
     * 
     * @param labAlgorithmTrainingLog 算法训练日志
     * @return 训练任务ID
     */
    @Override
    @Transactional
    public Long startTraining(LabAlgorithmTrainingLog labAlgorithmTrainingLog)
    {
        labAlgorithmTrainingLog.setStartTime(DateUtils.getNowDate());
        labAlgorithmTrainingLog.setStatus("RUNNING");
        labAlgorithmTrainingLog.setCompletedEpochs(0);
        labAlgorithmTrainingLog.setCreateTime(DateUtils.getNowDate());
        labAlgorithmTrainingLog.setCreateBy(SecurityUtils.getUsername());
        labAlgorithmTrainingLogMapper.insertLabAlgorithmTrainingLog(labAlgorithmTrainingLog);
        return labAlgorithmTrainingLog.getTrainingId();
    }

    /**
     * 更新训练进度
     * 
     * @param trainingId 训练任务ID
     * @param completedEpochs 已完成迭代次数
     * @param lossValue 当前损失值
     * @return 结果
     */
    @Override
    @Transactional
    public int updateTrainingProgress(Long trainingId, Integer completedEpochs, java.math.BigDecimal lossValue)
    {
        LabAlgorithmTrainingLog log = new LabAlgorithmTrainingLog();
        log.setTrainingId(trainingId);
        log.setCompletedEpochs(completedEpochs);
        log.setLossValue(lossValue);
        log.setUpdateTime(DateUtils.getNowDate());
        log.setUpdateBy(SecurityUtils.getUsername());
        return labAlgorithmTrainingLogMapper.updateLabAlgorithmTrainingLog(log);
    }

    /**
     * 完成训练任务
     * 
     * @param trainingId 训练任务ID
     * @param lossValue 最终损失值
     * @param accuracy 准确率
     * @param modelVersion 模型版本
     * @return 结果
     */
    @Override
    @Transactional
    public int completeTraining(Long trainingId, java.math.BigDecimal lossValue, java.math.BigDecimal accuracy, String modelVersion, String metrics)
    {
        LabAlgorithmTrainingLog log = new LabAlgorithmTrainingLog();
        log.setTrainingId(trainingId);
        log.setEndTime(DateUtils.getNowDate());
        log.setStatus("COMPLETED");
        log.setLossValue(lossValue);
        log.setAccuracy(accuracy);
        log.setModelVersion(modelVersion);
        log.setMetrics(metrics);
        log.setUpdateTime(DateUtils.getNowDate());
        log.setUpdateBy(SecurityUtils.getUsername());
        return labAlgorithmTrainingLogMapper.updateLabAlgorithmTrainingLog(log);
    }

    /**
     * 失败训练任务
     * 
     * @param trainingId 训练任务ID
     * @param errorMessage 错误信息
     * @return 结果
     */
    @Override
    @Transactional
    public int failTraining(Long trainingId, String errorMessage)
    {
        LabAlgorithmTrainingLog log = new LabAlgorithmTrainingLog();
        log.setTrainingId(trainingId);
        log.setEndTime(DateUtils.getNowDate());
        log.setStatus("FAILED");
        log.setErrorMessage(errorMessage);
        log.setUpdateTime(DateUtils.getNowDate());
        log.setUpdateBy(SecurityUtils.getUsername());
        return labAlgorithmTrainingLogMapper.updateLabAlgorithmTrainingLog(log);
    }
}
