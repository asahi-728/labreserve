package com.ruoyi.project.lab.service;

import java.util.List;
import com.ruoyi.project.lab.domain.LabUserCreditLog;

/**
 * 用户信用分变化日志Service接口
 *
 * @author asahi
 * @date 2026-04-18
 */
public interface ILabUserCreditLogService
{
    /**
     * 查询信用分变化日志
     *
     * @param logId 日志ID
     * @return 信用分变化日志
     */
    public LabUserCreditLog selectLabUserCreditLogByLogId(Long logId);

    /**
     * 查询信用分变化日志列表
     *
     * @param labUserCreditLog 查询条件
     * @return 信用分变化日志集合
     */
    public List<LabUserCreditLog> selectLabUserCreditLogList(LabUserCreditLog labUserCreditLog);

    /**
     * 根据用户ID查询信用分变化日志
     *
     * @param userId 用户ID
     * @return 信用分变化日志集合（按时间倒序）
     */
    public List<LabUserCreditLog> selectLogsByUserId(Long userId);

    /**
     * 新增信用分变化日志
     *
     * @param labUserCreditLog 信用分变化日志
     * @return 结果
     */
    public int insertLabUserCreditLog(LabUserCreditLog labUserCreditLog);

    /**
     * 删除信用分变化日志
     *
     * @param logId 日志ID
     * @return 结果
     */
    public int deleteLabUserCreditLogByLogId(Long logId);

    /**
     * 批量删除信用分变化日志
     *
     * @param logIds 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteLabUserCreditLogByLogIds(Long[] logIds);
}
