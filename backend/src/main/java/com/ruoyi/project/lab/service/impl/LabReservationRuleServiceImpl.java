package com.ruoyi.project.lab.service.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.project.lab.mapper.LabReservationRuleMapper;
import com.ruoyi.project.lab.domain.LabReservationRule;
import com.ruoyi.project.lab.service.ILabReservationRuleService;

/**
 * 预约规则配置Service业务层处理
 *
 * @author asahi
 * @date 2026-04-16
 */
@Service
public class LabReservationRuleServiceImpl implements ILabReservationRuleService
{
    @Autowired
    private LabReservationRuleMapper labReservationRuleMapper;

    /**
     * 查询预约规则配置
     *
     * @param ruleId 预约规则配置主键
     * @return 预约规则配置
     */
    @Override
    public LabReservationRule selectLabReservationRuleByRuleId(Long ruleId)
    {
        return labReservationRuleMapper.selectLabReservationRuleByRuleId(ruleId);
    }

    /**
     * 查询预约规则配置列表
     *
     * @param labReservationRule 预约规则配置
     * @return 预约规则配置
     */
    @Override
    public List<LabReservationRule> selectLabReservationRuleList(LabReservationRule labReservationRule)
    {
        return labReservationRuleMapper.selectLabReservationRuleList(labReservationRule);
    }

    /**
     * 根据规则键名查询（只查询正常状态的规则）
     *
     * @param ruleKey 规则键名
     * @return 预约规则配置
     */
    @Override
    public LabReservationRule selectLabReservationRuleByRuleKey(String ruleKey)
    {
        LabReservationRule rule = labReservationRuleMapper.selectLabReservationRuleByRuleKey(ruleKey);
        // 只返回状态为正常的规则
        if (rule != null && "0".equals(rule.getStatus())) {
            return rule;
        }
        return null;
    }

    /**
     * 根据规则类型查询（只查询正常状态的规则）
     *
     * @param ruleType 规则类型
     * @return 预约规则配置列表
     */
    @Override
    public List<LabReservationRule> selectLabReservationRuleByType(String ruleType)
    {
        List<LabReservationRule> rules = labReservationRuleMapper.selectLabReservationRuleByType(ruleType);
        // 只返回状态为正常的规则
        return rules.stream().filter(rule -> "0".equals(rule.getStatus())).collect(java.util.stream.Collectors.toList());
    }

    /**
     * 获取所有规则配置（键值对形式，只包含正常状态的规则）
     *
     * @return 规则配置Map
     */
    @Override
    public Map<String, String> selectAllRulesAsMap()
    {
        Map<String, String> rulesMap = new HashMap<>();
        List<LabReservationRule> rules = labReservationRuleMapper.selectLabReservationRuleList(null);
        for (LabReservationRule rule : rules)
        {
            // 只添加状态为正常的规则
            if ("0".equals(rule.getStatus())) {
                rulesMap.put(rule.getRuleKey(), rule.getRuleValue());
            }
        }
        return rulesMap;
    }

    /**
     * 新增预约规则配置
     *
     * @param labReservationRule 预约规则配置
     * @return 结果
     */
    @Override
    public int insertLabReservationRule(LabReservationRule labReservationRule)
    {
        return labReservationRuleMapper.insertLabReservationRule(labReservationRule);
    }

    /**
     * 修改预约规则配置
     *
     * @param labReservationRule 预约规则配置
     * @return 结果
     */
    @Override
    public int updateLabReservationRule(LabReservationRule labReservationRule)
    {
        return labReservationRuleMapper.updateLabReservationRule(labReservationRule);
    }

    /**
     * 删除预约规则配置
     *
     * @param ruleId 预约规则配置主键
     * @return 结果
     */
    @Override
    public int deleteLabReservationRuleByRuleId(Long ruleId)
    {
        return labReservationRuleMapper.deleteLabReservationRuleByRuleId(ruleId);
    }

    /**
     * 批量删除预约规则配置
     *
     * @param ruleIds 需要删除的数据主键集合
     * @return 结果
     */
    @Override
    public int deleteLabReservationRuleByRuleIds(Long[] ruleIds)
    {
        return labReservationRuleMapper.deleteLabReservationRuleByRuleIds(ruleIds);
    }
}
