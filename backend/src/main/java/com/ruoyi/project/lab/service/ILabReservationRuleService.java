package com.ruoyi.project.lab.service;

import java.util.List;
import java.util.Map;
import com.ruoyi.project.lab.domain.LabReservationRule;

/**
 * 预约规则配置Service接口
 *
 * @author asahi
 * @date 2026-04-16
 */
public interface ILabReservationRuleService
{
    /**
     * 查询预约规则配置
     *
     * @param ruleId 预约规则配置主键
     * @return 预约规则配置
     */
    public LabReservationRule selectLabReservationRuleByRuleId(Long ruleId);

    /**
     * 查询预约规则配置列表
     *
     * @param labReservationRule 预约规则配置
     * @return 预约规则配置集合
     */
    public List<LabReservationRule> selectLabReservationRuleList(LabReservationRule labReservationRule);

    /**
     * 根据规则键名查询
     *
     * @param ruleKey 规则键名
     * @return 预约规则配置
     */
    public LabReservationRule selectLabReservationRuleByRuleKey(String ruleKey);

    /**
     * 根据规则类型查询
     *
     * @param ruleType 规则类型
     * @return 预约规则配置列表
     */
    public List<LabReservationRule> selectLabReservationRuleByType(String ruleType);

    /**
     * 获取所有规则配置（键值对形式）
     *
     * @return 规则配置Map
     */
    public Map<String, String> selectAllRulesAsMap();

    /**
     * 新增预约规则配置
     *
     * @param labReservationRule 预约规则配置
     * @return 结果
     */
    public int insertLabReservationRule(LabReservationRule labReservationRule);

    /**
     * 修改预约规则配置
     *
     * @param labReservationRule 预约规则配置
     * @return 结果
     */
    public int updateLabReservationRule(LabReservationRule labReservationRule);

    /**
     * 删除预约规则配置
     *
     * @param ruleId 预约规则配置主键
     * @return 结果
     */
    public int deleteLabReservationRuleByRuleId(Long ruleId);

    /**
     * 批量删除预约规则配置
     *
     * @param ruleIds 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteLabReservationRuleByRuleIds(Long[] ruleIds);
}
