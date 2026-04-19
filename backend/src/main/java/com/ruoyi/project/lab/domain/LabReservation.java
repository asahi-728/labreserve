package com.ruoyi.project.lab.domain;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.framework.aspectj.lang.annotation.Excel;
import com.ruoyi.framework.web.domain.BaseEntity;

/**
 * 实验室设备预约对象 lab_reservation
 *
 * @author asahi
 * @date 2026-04-10
 */
public class LabReservation extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 预约ID */
    private Long reserveId;

    /** 用户ID */
    @Excel(name = "用户ID")
    private Long userId;

    /** 设备ID */
    @Excel(name = "设备ID")
    private Long deviceId;

    /** 开始时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "开始时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date startTime;

    /** 结束时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "结束时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date endTime;

    /** 状态 */
    @Excel(name = "状态")
    private String status;

    /** 审核人 */
    @Excel(name = "审核人")
    private String auditBy;

    /** 审核时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "审核时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date auditTime;

    /** 审核意见 */
    @Excel(name = "审核意见")
    private String auditRemark;

    /** 调度类型 */
    @Excel(name = "调度类型")
    private String schedulerType;

    /** 取消时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "取消时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date cancelTime;

    /** 完成时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "完成时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date completeTime;

    /** 设备分类ID（查询参数） */
    private Long categoryId;

    /** 实验室ID（查询参数） */
    private Long labId;

    /** 申请人姓名（查询参数） */
    private String userName;

    /** 开始时间字符串（查询参数） */
    private String startTimeStr;

    /** 结束时间字符串（查询参数） */
    private String endTimeStr;

    /** 设备名称（查询结果） */
    private String deviceName;

    /** 实验室名称（查询结果） */
    private String labName;

    /** 设备位置（查询结果） */
    private String location;

    /** 审批模式：manual=人工审批，auto=PPO自动审批 */
    private String approvalMode;

    public void setReserveId(Long reserveId)
    {
        this.reserveId = reserveId;
    }

    public Long getReserveId()
    {
        return reserveId;
    }

    public Long getUserId()
    {
        return userId;
    }

    public void setUserId(Long userId)
    {
        this.userId = userId;
    }

    public Long getDeviceId()
    {
        return deviceId;
    }

    public void setDeviceId(Long deviceId)
    {
        this.deviceId = deviceId;
    }

    public Date getStartTime()
    {
        return startTime;
    }

    public void setStartTime(Date startTime)
    {
        this.startTime = startTime;
    }

    public Date getEndTime()
    {
        return endTime;
    }

    public void setEndTime(Date endTime)
    {
        this.endTime = endTime;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    public String getAuditBy()
    {
        return auditBy;
    }

    public void setAuditBy(String auditBy)
    {
        this.auditBy = auditBy;
    }

    public Date getAuditTime()
    {
        return auditTime;
    }

    public void setAuditTime(Date auditTime)
    {
        this.auditTime = auditTime;
    }

    public String getAuditRemark()
    {
        return auditRemark;
    }

    public void setAuditRemark(String auditRemark)
    {
        this.auditRemark = auditRemark;
    }

    public String getSchedulerType()
    {
        return schedulerType;
    }

    public void setSchedulerType(String schedulerType)
    {
        this.schedulerType = schedulerType;
    }

    public Date getCancelTime()
    {
        return cancelTime;
    }

    public void setCancelTime(Date cancelTime)
    {
        this.cancelTime = cancelTime;
    }

    public Date getCompleteTime()
    {
        return completeTime;
    }

    public void setCompleteTime(Date completeTime)
    {
        this.completeTime = completeTime;
    }

    public Long getCategoryId()
    {
        return categoryId;
    }

    public void setCategoryId(Long categoryId)
    {
        this.categoryId = categoryId;
    }

    public Long getLabId()
    {
        return labId;
    }

    public void setLabId(Long labId)
    {
        this.labId = labId;
    }

    public String getUserName()
    {
        return userName;
    }

    public void setUserName(String userName)
    {
        this.userName = userName;
    }

    public String getStartTimeStr()
    {
        return startTimeStr;
    }

    public void setStartTimeStr(String startTimeStr)
    {
        this.startTimeStr = startTimeStr;
    }

    public String getEndTimeStr()
    {
        return endTimeStr;
    }

    public void setEndTimeStr(String endTimeStr)
    {
        this.endTimeStr = endTimeStr;
    }

    public String getDeviceName()
    {
        return deviceName;
    }

    public void setDeviceName(String deviceName)
    {
        this.deviceName = deviceName;
    }

    public String getLabName()
    {
        return labName;
    }

    public void setLabName(String labName)
    {
        this.labName = labName;
    }

    public String getLocation()
    {
        return location;
    }

    public void setLocation(String location)
    {
        this.location = location;
    }

    public String getApprovalMode()
    {
        return approvalMode;
    }

    public void setApprovalMode(String approvalMode)
    {
        this.approvalMode = approvalMode;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("reserveId", getReserveId())
            .append("userId", getUserId())
            .append("deviceId", getDeviceId())
            .append("startTime", getStartTime())
            .append("endTime", getEndTime())
            .append("status", getStatus())
            .append("auditBy", getAuditBy())
            .append("auditTime", getAuditTime())
            .append("auditRemark", getAuditRemark())
            .append("schedulerType", getSchedulerType())
            .append("cancelTime", getCancelTime())
            .append("completeTime", getCompleteTime())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .append("deviceName", getDeviceName())
            .append("labName", getLabName())
            .append("location", getLocation())
            .toString();
    }
}
