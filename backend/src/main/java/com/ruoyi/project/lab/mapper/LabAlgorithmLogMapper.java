package com.ruoyi.project.lab.mapper;

import java.util.List;
import com.ruoyi.project.lab.domain.LabAlgorithmLog;

/**
 * 算法日志Mapper接口
 *
 * @author ruoyi
 * @date 2026-04-17
 */
public interface LabAlgorithmLogMapper
{
    /**
     * 查询算法日志
     *
     * @param logId 日志ID
     * @return 算法日志
     */
    public LabAlgorithmLog selectLabAlgorithmLogByLogId(Long logId);

    /**
     * 查询算法日志列表
     *
     * @param labAlgorithmLog 算法日志
     * @return 算法日志集合
     */
    public List<LabAlgorithmLog> selectLabAlgorithmLogList(LabAlgorithmLog labAlgorithmLog);

    /**
     * 新增算法日志
     *
     * @param labAlgorithmLog 算法日志
     * @return 结果
     */
    public int insertLabAlgorithmLog(LabAlgorithmLog labAlgorithmLog);

    /**
     * 修改算法日志
     *
     * @param labAlgorithmLog 算法日志
     * @return 结果
     */
    public int updateLabAlgorithmLog(LabAlgorithmLog labAlgorithmLog);

    /**
     * 删除算法日志
     *
     * @param logId 日志ID
     * @return 结果
     */
    public int deleteLabAlgorithmLogByLogId(Long logId);

    /**
     * 批量删除算法日志
     *
     * @param logIds 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteLabAlgorithmLogByLogIds(Long[] logIds);
}
