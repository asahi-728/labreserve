package com.ruoyi.project.lab.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.framework.aspectj.lang.annotation.Excel;
import com.ruoyi.framework.web.domain.BaseEntity;

/**
 * 预约规则配置对象 lab_reservation_rule
 *
 * @author asahi
 * @date 2026-04-16
 */
public class LabReservationRule extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 规则ID */
    private Long ruleId;

    /** 规则名称 */
    @Excel(name = "规则名称")
    private String ruleName;

    /** 规则键名 */
    @Excel(name = "规则键名")
    private String ruleKey;

    /** 规则值 */
    @Excel(name = "规则值")
    private String ruleValue;

    /** 规则类型 */
    @Excel(name = "规则类型")
    private String ruleType;

    /** 规则描述 */
    @Excel(name = "规则描述")
    private String description;

    /** 状态 */
    @Excel(name = "状态")
    private String status;

    public void setRuleId(Long ruleId)
    {
        this.ruleId = ruleId;
    }

    public Long getRuleId()
    {
        return ruleId;
    }

    public void setRuleName(String ruleName)
    {
        this.ruleName = ruleName;
    }

    public String getRuleName()
    {
        return ruleName;
    }

    public void setRuleKey(String ruleKey)
    {
        this.ruleKey = ruleKey;
    }

    public String getRuleKey()
    {
        return ruleKey;
    }

    public void setRuleValue(String ruleValue)
    {
        this.ruleValue = ruleValue;
    }

    public String getRuleValue()
    {
        return ruleValue;
    }

    public String getRuleType()
    {
        return ruleType;
    }

    public void setRuleType(String ruleType)
    {
        this.ruleType = ruleType;
    }

    public String getDescription()
    {
        return description;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    public String getStatus()
    {
        return status;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("ruleId", getRuleId())
            .append("ruleName", getRuleName())
            .append("ruleKey", getRuleKey())
            .append("ruleValue", getRuleValue())
            .append("ruleType", getRuleType())
            .append("description", getDescription())
            .append("status", getStatus())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
