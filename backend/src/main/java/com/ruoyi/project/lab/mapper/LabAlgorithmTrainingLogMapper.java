package com.ruoyi.project.lab.mapper;

import java.util.List;
import com.ruoyi.project.lab.domain.LabAlgorithmTrainingLog;

/**
 * 算法训练日志Mapper接口
 * 
 * @author ruoyi
 * @date 2026-04-15
 */
public interface LabAlgorithmTrainingLogMapper 
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
     * 删除算法训练日志
     * 
     * @param trainingId 算法训练日志主键
     * @return 结果
     */
    public int deleteLabAlgorithmTrainingLogByTrainingId(Long trainingId);

    /**
     * 批量删除算法训练日志
     * 
     * @param trainingIds 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteLabAlgorithmTrainingLogByTrainingIds(Long[] trainingIds);
}
