package com.ruoyi.project.lab.mapper;

import java.util.List;
import com.ruoyi.project.lab.domain.LabUserCredit;

/**
 * 用户信用评分Mapper接口
 * 
 * @author asahi
 * @date 2026-04-10
 */
public interface LabUserCreditMapper 
{
    /**
     * 查询用户信用评分
     * 
     * @param id 用户信用评分主键
     * @return 用户信用评分
     */
    public LabUserCredit selectLabUserCreditById(Long id);

    /**
     * 根据用户ID查询信用记录
     * 
     * @param userId 用户ID
     * @return 信用记录
     */
    public LabUserCredit selectLabUserCreditByUserId(Long userId);

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
     * 删除用户信用评分
     * 
     * @param id 用户信用评分主键
     * @return 结果
     */
    public int deleteLabUserCreditById(Long id);

    /**
     * 批量删除用户信用评分
     * 
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteLabUserCreditByIds(Long[] ids);
}
