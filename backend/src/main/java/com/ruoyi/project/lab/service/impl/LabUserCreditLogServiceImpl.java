package com.ruoyi.project.lab.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.project.lab.domain.LabUserCreditLog;
import com.ruoyi.project.lab.mapper.LabUserCreditLogMapper;
import com.ruoyi.project.lab.service.ILabUserCreditLogService;

/**
 * 用户信用分变化日志Service业务层处理
 *
 * @author asahi
 * @date 2026-04-18
 */
@Service
public class LabUserCreditLogServiceImpl implements ILabUserCreditLogService
{
    @Autowired
    private LabUserCreditLogMapper labUserCreditLogMapper;

    @Override
    public LabUserCreditLog selectLabUserCreditLogByLogId(Long logId)
    {
        return labUserCreditLogMapper.selectLabUserCreditLogByLogId(logId);
    }

    @Override
    public List<LabUserCreditLog> selectLabUserCreditLogList(LabUserCreditLog labUserCreditLog)
    {
        return labUserCreditLogMapper.selectLabUserCreditLogList(labUserCreditLog);
    }

    @Override
    public List<LabUserCreditLog> selectLogsByUserId(Long userId)
    {
        return labUserCreditLogMapper.selectLogsByUserId(userId);
    }

    @Override
    public int insertLabUserCreditLog(LabUserCreditLog labUserCreditLog)
    {
        return labUserCreditLogMapper.insertLabUserCreditLog(labUserCreditLog);
    }

    @Override
    public int deleteLabUserCreditLogByLogId(Long logId)
    {
        return labUserCreditLogMapper.deleteLabUserCreditLogByLogId(logId);
    }

    @Override
    public int deleteLabUserCreditLogByLogIds(Long[] logIds)
    {
        return labUserCreditLogMapper.deleteLabUserCreditLogByLogIds(logIds);
    }
}
