package com.ruoyi.project.lab.domain;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.framework.web.domain.BaseEntity;

/**
 * 用户信用分变化日志对象 lab_user_credit_log
 *
 * @author asahi
 * @date 2026-04-18
 */
public class LabUserCreditLog extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 日志ID */
    private Long logId;

    /** 用户ID */
    private Long userId;

    /** 变化类型: ON_TIME=守约, VIOLATION=违约, CANCEL_APPROVED=取消已批准, CANCEL_PENDING=取消待审核, MANUAL=手动调整, OTHER=其他 */
    private String changeType;

    /** 变化量（正数加分，负数扣分） */
    private Integer delta;

    /** 变化前信用分 */
    private Integer scoreBefore;

    /** 变化后信用分 */
    private Integer scoreAfter;

    /** 关联的预约ID（如果有） */
    private Long reserveId;

    /** 备注/原因说明 */
    private String remark;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    public Long getLogId()
    {
        return logId;
    }

    public void setLogId(Long logId)
    {
        this.logId = logId;
    }

    public Long getUserId()
    {
        return userId;
    }

    public void setUserId(Long userId)
    {
        this.userId = userId;
    }

    public String getChangeType()
    {
        return changeType;
    }

    public void setChangeType(String changeType)
    {
        this.changeType = changeType;
    }

    public Integer getDelta()
    {
        return delta;
    }

    public void setDelta(Integer delta)
    {
        this.delta = delta;
    }

    public Integer getScoreBefore()
    {
        return scoreBefore;
    }

    public void setScoreBefore(Integer scoreBefore)
    {
        this.scoreBefore = scoreBefore;
    }

    public Integer getScoreAfter()
    {
        return scoreAfter;
    }

    public void setScoreAfter(Integer scoreAfter)
    {
        this.scoreAfter = scoreAfter;
    }

    public Long getReserveId()
    {
        return reserveId;
    }

    public void setReserveId(Long reserveId)
    {
        this.reserveId = reserveId;
    }

    public String getRemark()
    {
        return remark;
    }

    public void setRemark(String remark)
    {
        this.remark = remark;
    }

    public Date getCreateTime()
    {
        return createTime;
    }

    public void setCreateTime(Date createTime)
    {
        this.createTime = createTime;
    }

    @Override
    public String toString()
    {
        return "LabUserCreditLog{" +
                "logId=" + logId +
                ", userId=" + userId +
                ", changeType='" + changeType + '\'' +
                ", delta=" + delta +
                ", scoreBefore=" + scoreBefore +
                ", scoreAfter=" + scoreAfter +
                ", reserveId=" + reserveId +
                ", createTime=" + createTime +
                '}';
    }
}
