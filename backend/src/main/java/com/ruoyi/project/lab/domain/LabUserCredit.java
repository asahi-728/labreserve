package com.ruoyi.project.lab.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.framework.aspectj.lang.annotation.Excel;
import com.ruoyi.framework.web.domain.BaseEntity;

/**
 * 用户信用评分对象 lab_user_credit
 * 
 * @author asahi
 * @date 2026-04-10
 */
public class LabUserCredit extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;

    /** 用户ID */
    @Excel(name = "用户ID")
    private Long userId;

    /** 信用分数 */
    @Excel(name = "信用分数")
    private Long creditScore;

    /** 违约次数 */
    @Excel(name = "违约次数")
    private Long violation;

    /** 守约次数 */
    @Excel(name = "守约次数")
    private Long onTime;

    /** 取消次数 */
    @Excel(name = "取消次数")
    private Long cancel;

    public void setId(Long id) 
    {
        this.id = id;
    }

    public Long getId() 
    {
        return id;
    }

    public void setUserId(Long userId) 
    {
        this.userId = userId;
    }

    public Long getUserId() 
    {
        return userId;
    }

    public void setCreditScore(Long creditScore) 
    {
        this.creditScore = creditScore;
    }

    public Long getCreditScore() 
    {
        return creditScore;
    }

    public void setViolation(Long violation) 
    {
        this.violation = violation;
    }

    public Long getViolation() 
    {
        return violation;
    }

    public void setOnTime(Long onTime) 
    {
        this.onTime = onTime;
    }

    public Long getOnTime() 
    {
        return onTime;
    }

    public void setCancel(Long cancel) 
    {
        this.cancel = cancel;
    }

    public Long getCancel() 
    {
        return cancel;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("userId", getUserId())
            .append("creditScore", getCreditScore())
            .append("violation", getViolation())
            .append("onTime", getOnTime())
            .append("cancel", getCancel())
            .append("updateTime", getUpdateTime())
            .toString();
    }
}
