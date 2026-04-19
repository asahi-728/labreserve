package com.ruoyi.project.lab.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.framework.aspectj.lang.annotation.Excel;
import com.ruoyi.framework.web.domain.BaseEntity;

/**
 * 实验室信息对象 lab_laboratory
 * 
 * @author asahi
 * @date 2026-04-10
 */
public class LabLaboratory extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 实验室ID */
    private Long labId;

    /** 实验室名称 */
    @Excel(name = "实验室名称")
    private String labName;

    /** 实验室位置 */
    @Excel(name = "实验室位置")
    private String location;

    /** 负责人 */
    @Excel(name = "负责人")
    private String leader;

    /** 负责人电话 */
    @Excel(name = "负责人电话")
    private String leaderPhone;

    /** 开放时间 */
    @Excel(name = "开放时间", width = 30)
    private String openTime;

    /** 状态 */
    @Excel(name = "状态")
    private String status;

    /** 关闭时间 */
    @Excel(name = "关闭时间", width = 30)
    private String closeTime;

    public void setLabId(Long labId) 
    {
        this.labId = labId;
    }

    public Long getLabId() 
    {
        return labId;
    }

    public void setLabName(String labName) 
    {
        this.labName = labName;
    }

    public String getLabName() 
    {
        return labName;
    }

    public void setLocation(String location) 
    {
        this.location = location;
    }

    public String getLocation() 
    {
        return location;
    }

    public void setLeader(String leader) 
    {
        this.leader = leader;
    }

    public String getLeader() 
    {
        return leader;
    }

    public void setLeaderPhone(String leaderPhone) 
    {
        this.leaderPhone = leaderPhone;
    }

    public String getLeaderPhone() 
    {
        return leaderPhone;
    }

    public void setOpenTime(String openTime) 
    {
        this.openTime = openTime;
    }

    public String getOpenTime() 
    {
        return openTime;
    }

    public void setStatus(String status) 
    {
        this.status = status;
    }

    public String getStatus() 
    {
        return status;
    }

    public void setCloseTime(String closeTime) 
    {
        this.closeTime = closeTime;
    }

    public String getCloseTime() 
    {
        return closeTime;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("labId", getLabId())
            .append("labName", getLabName())
            .append("location", getLocation())
            .append("leader", getLeader())
            .append("leaderPhone", getLeaderPhone())
            .append("openTime", getOpenTime())
            .append("status", getStatus())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .append("closeTime", getCloseTime())
            .toString();
    }
}
