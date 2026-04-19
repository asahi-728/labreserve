package com.ruoyi.project.lab.service;

import java.util.List;
import com.ruoyi.project.lab.domain.LabUserCredit;

/**
 * 用户信用评分Service接口
 * 
 * @author asahi
 * @date 2026-04-10
 */
public interface ILabUserCreditService 
{
    /**
     * 查询用户信用评分
     * 
     * @param id 用户信用评分主键
     * @return 用户信用评分
     */
    public LabUserCredit selectLabUserCreditById(Long id);

    /**
     * 查询用户信用评分列表
     * 
     * @param labUserCredit 用户信用评分
     * @return 用户信用评分集合
     */
    public List<LabUserCredit> selectLabUserCreditList(LabUserCredit labUserCredit);

    /**
     * 新增用户信用评分
     * 
     * @param labUserCredit 用户信用评分
     * @return 结果
     */
    public int insertLabUserCredit(LabUserCredit labUserCredit);

    /**
     * 修改用户信用评分
     * 
     * @param labUserCredit 用户信用评分
     * @return 结果
     */
    public int updateLabUserCredit(LabUserCredit labUserCredit);

    /**
     * 批量删除用户信用评分
     * 
     * @param ids 需要删除的用户信用评分主键集合
     * @return 结果
     */
    public int deleteLabUserCreditByIds(Long[] ids);

    /**
     * 删除用户信用评分信息
     * 
     * @param id 用户信用评分主键
     * @return 结果
     */
    public int deleteLabUserCreditById(Long id);

    /**
     * 根据用户ID查询信用记录
     * 
     * @param userId 用户ID
     * @return 信用记录
     */
    public LabUserCredit selectLabUserCreditByUserId(Long userId);

    /**
     * 更新用户信用评分（守约）
     * 
     * @param userId 用户ID
     * @return 结果
     */
    public int updateCreditOnTime(Long userId);

    /**
     * 更新用户信用评分（违约）
     * 
     * @param userId 用户ID
     * @return 结果
     */
    public int updateCreditViolation(Long userId);

    /**
     * 更新用户信用评分（取消预约）
     * 
     * @param userId 用户ID
     * @return 结果
     */
    public int updateCreditCancel(Long userId);

    /**
     * 按指定分值更新用户信用分
     * 
     * @param userId 用户ID
     * @param delta 变化量（正数加分，负数扣分）
     * @return 结果
     */
    public int updateCreditByDelta(Long userId, long delta);

    /**
     * 更新用户信用评分（守约），关联预约记录
     *
     * @param userId 用户ID
     * @param reserveId 关联的预约ID
     * @return 结果
     */
    public int updateCreditOnTime(Long userId, Long reserveId);

    /**
     * 更新用户信用评分（违约），关联预约记录
     *
     * @param userId 用户ID
     * @param reserveId 关联的预约ID
     * @return 结果
     */
    public int updateCreditViolation(Long userId, Long reserveId);

    /**
     * 更新用户信用评分（取消已批准的预约），关联预约记录
     *
     * @param userId 用户ID
     * @param reserveId 关联的预约ID
     * @return 结果
     */
    public int updateCreditCancel(Long userId, Long reserveId);

    /**
     * 按指定分值更新用户信用分，关联预约记录
     *
     * @param userId 用户ID
     * @param delta 变化量（正数加分，负数扣分）
     * @param reserveId 关联的预约ID
     * @return 结果
     */
    public int updateCreditByDelta(Long userId, long delta, Long reserveId);
}
