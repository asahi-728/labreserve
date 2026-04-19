package com.ruoyi.project.lab.service.impl;

import java.util.Date;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.project.lab.mapper.LabAlgorithmLogMapper;
import com.ruoyi.project.lab.domain.LabAlgorithmLog;
import com.ruoyi.project.lab.service.ILabAlgorithmLogService;

/**
 * 算法日志Service业务层处理
 *
 * @author ruoyi
 * @date 2026-04-17
 */
@Service
public class LabAlgorithmLogServiceImpl implements ILabAlgorithmLogService
{
    @Autowired
    private LabAlgorithmLogMapper labAlgorithmLogMapper;

    /**
     * 查询算法日志
     *
     * @param logId 日志ID
     * @return 算法日志
     */
    @Override
    public LabAlgorithmLog selectLabAlgorithmLogByLogId(Long logId)
    {
        return labAlgorithmLogMapper.selectLabAlgorithmLogByLogId(logId);
    }

    /**
     * 查询算法日志列表
     *
     * @param labAlgorithmLog 算法日志
     * @return 算法日志
     */
    @Override
    public java.util.List<LabAlgorithmLog> selectLabAlgorithmLogList(LabAlgorithmLog labAlgorithmLog)
    {
        return labAlgorithmLogMapper.selectLabAlgorithmLogList(labAlgorithmLog);
    }

    /**
     * 新增算法日志
     *
     * @param labAlgorithmLog 算法日志
     * @return 结果
     */
    @Override
    public int insertLabAlgorithmLog(LabAlgorithmLog labAlgorithmLog)
    {
        return labAlgorithmLogMapper.insertLabAlgorithmLog(labAlgorithmLog);
    }

    /**
     * 修改算法日志
     *
     * @param labAlgorithmLog 算法日志
     * @return 结果
     */
    @Override
    public int updateLabAlgorithmLog(LabAlgorithmLog labAlgorithmLog)
    {
        return labAlgorithmLogMapper.updateLabAlgorithmLog(labAlgorithmLog);
    }

    /**
     * 删除算法日志
     *
     * @param logId 日志ID
     * @return 结果
     */
    @Override
    public int deleteLabAlgorithmLogByLogId(Long logId)
    {
        return labAlgorithmLogMapper.deleteLabAlgorithmLogByLogId(logId);
    }

    /**
     * 批量删除算法日志
     *
     * @param logIds 需要删除的数据主键集合
     * @return 结果
     */
    @Override
    public int deleteLabAlgorithmLogByLogIds(Long[] logIds)
    {
        return labAlgorithmLogMapper.deleteLabAlgorithmLogByLogIds(logIds);
    }

    /**
     * 记录算法日志（快速方法）
     *
     * @param algorithmType 算法类型：GAT, PPO
     * @param operationType 操作类型：TRAIN, RECOMMEND, DISPATCH
     * @param userId 用户ID
     * @param params 请求参数
     * @param result 结果
     * @param status 状态：0=成功, 1=失败
     * @param errorMessage 错误信息
     * @param durationMs 执行时长
     * @return 结果
     */
    @Override
    @Transactional
    public int logAlgorithm(String algorithmType, String operationType, Long userId, String params, String result, String status, String errorMessage, Long durationMs)
    {
        LabAlgorithmLog log = new LabAlgorithmLog();
        log.setAlgorithmType(algorithmType);
        log.setOperationType(operationType);
        log.setUserId(userId);
        log.setParams(params);
        log.setResult(result);
        log.setStatus(status);
        log.setErrorMessage(errorMessage);
        log.setDurationMs(durationMs);
        log.setCreateTime(new Date());
        return labAlgorithmLogMapper.insertLabAlgorithmLog(log);
    }
}
