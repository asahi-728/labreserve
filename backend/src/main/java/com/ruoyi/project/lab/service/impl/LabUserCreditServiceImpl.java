package com.ruoyi.project.lab.service.impl;

import java.util.List;
import com.ruoyi.common.utils.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.project.lab.mapper.LabUserCreditMapper;
import com.ruoyi.project.lab.domain.LabUserCredit;
import com.ruoyi.project.lab.domain.LabUserCreditLog;
import com.ruoyi.project.lab.service.ILabUserCreditLogService;
import com.ruoyi.project.lab.service.ILabUserCreditService;

/**
 * 用户信用评分Service业务层处理
 * 
 * @author asahi
 * @date 2026-04-10
 */
@Service
public class LabUserCreditServiceImpl implements ILabUserCreditService 
{
    private static final Logger log = LoggerFactory.getLogger(LabUserCreditServiceImpl.class);

    @Autowired
    private LabUserCreditMapper labUserCreditMapper;

    @Autowired
    private ILabUserCreditLogService creditLogService;

    private static final long INITIAL_CREDIT_SCORE = 100L;
    private static final long ON_TIME_BONUS = 2L;
    private static final long VIOLATION_PENALTY = 2L;
    private static final long CANCEL_PENALTY = 5L;
    private static final long MIN_CREDIT_SCORE = 0L;
    private static final long MAX_CREDIT_SCORE = 150L;

    /**
     * 查询用户信用评分
     * 
     * @param id 用户信用评分主键
     * @return 用户信用评分
     */
    @Override
    public LabUserCredit selectLabUserCreditById(Long id)
    {
        return labUserCreditMapper.selectLabUserCreditById(id);
    }

    /**
     * 查询用户信用评分列表
     * 
     * @param labUserCredit 用户信用评分
     * @return 用户信用评分
     */
    @Override
    public List<LabUserCredit> selectLabUserCreditList(LabUserCredit labUserCredit)
    {
        return labUserCreditMapper.selectLabUserCreditList(labUserCredit);
    }

    /**
     * 新增用户信用评分
     * 
     * @param labUserCredit 用户信用评分
     * @return 结果
     */
    @Override
    public int insertLabUserCredit(LabUserCredit labUserCredit)
    {
        return labUserCreditMapper.insertLabUserCredit(labUserCredit);
    }

    /**
     * 修改用户信用评分
     * 
     * @param labUserCredit 用户信用评分
     * @return 结果
     */
    @Override
    public int updateLabUserCredit(LabUserCredit labUserCredit)
    {
        labUserCredit.setUpdateTime(DateUtils.getNowDate());
        return labUserCreditMapper.updateLabUserCredit(labUserCredit);
    }

    /**
     * 批量删除用户信用评分
     * 
     * @param ids 需要删除的用户信用评分主键集合
     * @return 结果
     */
    @Override
    public int deleteLabUserCreditByIds(Long[] ids)
    {
        return labUserCreditMapper.deleteLabUserCreditByIds(ids);
    }

    /**
     * 删除用户信用评分信息
     * 
     * @param id 用户信用评分主键
     * @return 结果
     */
    @Override
    public int deleteLabUserCreditById(Long id)
    {
        return labUserCreditMapper.deleteLabUserCreditById(id);
    }

    /**
     * 根据用户ID查询信用记录
     * 
     * @param userId 用户ID
     * @return 信用记录
     */
    @Override
    public LabUserCredit selectLabUserCreditByUserId(Long userId)
    {
        return labUserCreditMapper.selectLabUserCreditByUserId(userId);
    }

    /**
     * 获取或创建用户信用记录
     */
    private LabUserCredit getOrCreateCreditRecord(Long userId)
    {
        LabUserCredit credit = selectLabUserCreditByUserId(userId);
        if (credit == null) {
            credit = new LabUserCredit();
            credit.setUserId(userId);
            credit.setCreditScore(INITIAL_CREDIT_SCORE);
            credit.setViolation(0L);
            credit.setOnTime(0L);
            credit.setCancel(0L);
            insertLabUserCredit(credit);
        }
        return credit;
    }

    /**
     * 写入信用分变化日志（内部方法）
     *
     * @param userId 用户ID
     * @param changeType 变化类型
     * @param delta 变化量
     * @param scoreBefore 变化前分数
     * @param scoreAfter 变化后分数
     * @param reserveId 关联预约ID（可为null）
     * @param remark 备注
     */
    private void writeCreditLog(Long userId, String changeType, int delta,
                                 int scoreBefore, int scoreAfter,
                                 Long reserveId, String remark)
    {
        try
        {
            LabUserCreditLog logEntry = new LabUserCreditLog();
            logEntry.setUserId(userId);
            logEntry.setChangeType(changeType);
            logEntry.setDelta(delta);
            logEntry.setScoreBefore(scoreBefore);
            logEntry.setScoreAfter(scoreAfter);
            logEntry.setReserveId(reserveId);
            logEntry.setRemark(remark);
            logEntry.setCreateTime(DateUtils.getNowDate());
            creditLogService.insertLabUserCreditLog(logEntry);
        }
        catch (Exception e)
        {
            // 日志写入失败不影响主流程
            log.warn("写入用户 {} 信用分变化日志失败: {}", userId, e.getMessage());
        }
    }

    /**
     * 更新用户信用评分（守约）
     * 
     * @param userId 用户ID
     * @return 结果
     */
    @Override
    @Transactional
    public int updateCreditOnTime(Long userId)
    {
        LabUserCredit credit = getOrCreateCreditRecord(userId);
        int scoreBefore = credit.getCreditScore().intValue();
        credit.setOnTime(credit.getOnTime() + 1);
        long newScore = Math.min(credit.getCreditScore() + ON_TIME_BONUS, MAX_CREDIT_SCORE);
        credit.setCreditScore(newScore);
        int result = updateLabUserCredit(credit);

        writeCreditLog(userId, "ON_TIME", (int) ON_TIME_BONUS, scoreBefore, (int) newScore,
                       null, "按时完成使用，守约奖励");

        return result;
    }

    /**
     * 更新用户信用评分（违约）
     * 
     * @param userId 用户ID
     * @return 结果
     */
    @Override
    @Transactional
    public int updateCreditViolation(Long userId)
    {
        LabUserCredit credit = getOrCreateCreditRecord(userId);
        int scoreBefore = credit.getCreditScore().intValue();
        credit.setViolation(credit.getViolation() + 1);
        long newScore = Math.max(credit.getCreditScore() - VIOLATION_PENALTY, MIN_CREDIT_SCORE);
        credit.setCreditScore(newScore);
        int result = updateLabUserCredit(credit);

        writeCreditLog(userId, "VIOLATION", -(int) VIOLATION_PENALTY, scoreBefore, (int) newScore,
                       null, "超时完成使用，违约扣分");

        return result;
    }

    /**
     * 更新用户信用评分（取消已批准的预约）
     * 
     * @param userId 用户ID
     * @return 结果
     */
    @Override
    @Transactional
    public int updateCreditCancel(Long userId)
    {
        LabUserCredit credit = getOrCreateCreditRecord(userId);
        int scoreBefore = credit.getCreditScore().intValue();
        credit.setCancel(credit.getCancel() + 1);
        long newScore = Math.max(credit.getCreditScore() - CANCEL_PENALTY, MIN_CREDIT_SCORE);
        credit.setCreditScore(newScore);
        int result = updateLabUserCredit(credit);

        writeCreditLog(userId, "CANCEL_APPROVED", -(int) CANCEL_PENALTY, scoreBefore, (int) newScore,
                       null, "取消已批准的预约，扣分");

        return result;
    }

    /**
     * 按指定分值更新用户信用分
     * 
     * @param userId 用户ID
     * @param delta 变化量（正数加分，负数扣分）
     * @return 结果
     */
    @Override
    @Transactional
    public int updateCreditByDelta(Long userId, long delta)
    {
        LabUserCredit credit = getOrCreateCreditRecord(userId);
        int scoreBefore = credit.getCreditScore().intValue();
        long newScore = Math.max(Math.min(credit.getCreditScore() + delta, MAX_CREDIT_SCORE), MIN_CREDIT_SCORE);
        credit.setCreditScore(newScore);
        int result = updateLabUserCredit(credit);

        String changeType = delta >= 0 ? "OTHER" : "CANCEL_PENDING";
        String remark = delta >= 0 ? "手动加分" : "取消待审核的预约，扣分";

        writeCreditLog(userId, changeType, (int) delta, scoreBefore, (int) newScore,
                       null, remark);

        return result;
    }

    // ==================== 带 reserveId 的重载方法（从业务层调用，日志关联预约记录）====================

    @Override
    @Transactional
    public int updateCreditOnTime(Long userId, Long reserveId)
    {
        LabUserCredit credit = getOrCreateCreditRecord(userId);
        int scoreBefore = credit.getCreditScore().intValue();
        credit.setOnTime(credit.getOnTime() + 1);
        long newScore = Math.min(credit.getCreditScore() + ON_TIME_BONUS, MAX_CREDIT_SCORE);
        credit.setCreditScore(newScore);
        int result = updateLabUserCredit(credit);

        writeCreditLog(userId, "ON_TIME", (int) ON_TIME_BONUS, scoreBefore, (int) newScore,
                       reserveId, "按时完成使用，守约奖励");

        return result;
    }

    @Override
    @Transactional
    public int updateCreditViolation(Long userId, Long reserveId)
    {
        LabUserCredit credit = getOrCreateCreditRecord(userId);
        int scoreBefore = credit.getCreditScore().intValue();
        credit.setViolation(credit.getViolation() + 1);
        long newScore = Math.max(credit.getCreditScore() - VIOLATION_PENALTY, MIN_CREDIT_SCORE);
        credit.setCreditScore(newScore);
        int result = updateLabUserCredit(credit);

        writeCreditLog(userId, "VIOLATION", -(int) VIOLATION_PENALTY, scoreBefore, (int) newScore,
                       reserveId, "超时完成使用，违约扣分");

        return result;
    }

    @Override
    @Transactional
    public int updateCreditCancel(Long userId, Long reserveId)
    {
        LabUserCredit credit = getOrCreateCreditRecord(userId);
        int scoreBefore = credit.getCreditScore().intValue();
        credit.setCancel(credit.getCancel() + 1);
        long newScore = Math.max(credit.getCreditScore() - CANCEL_PENALTY, MIN_CREDIT_SCORE);
        credit.setCreditScore(newScore);
        int result = updateLabUserCredit(credit);

        writeCreditLog(userId, "CANCEL_APPROVED", -(int) CANCEL_PENALTY, scoreBefore, (int) newScore,
                       reserveId, "取消已批准的预约，扣分");

        return result;
    }

    @Override
    @Transactional
    public int updateCreditByDelta(Long userId, long delta, Long reserveId)
    {
        LabUserCredit credit = getOrCreateCreditRecord(userId);
        int scoreBefore = credit.getCreditScore().intValue();
        long newScore = Math.max(Math.min(credit.getCreditScore() + delta, MAX_CREDIT_SCORE), MIN_CREDIT_SCORE);
        credit.setCreditScore(newScore);
        int result = updateLabUserCredit(credit);

        String changeType = delta >= 0 ? "OTHER" : "CANCEL_PENDING";
        String remark = delta >= 0 ? "手动加分" : "取消待审核的预约，扣分";

        writeCreditLog(userId, changeType, (int) delta, scoreBefore, (int) newScore,
                       reserveId, remark);

        return result;
    }
}
