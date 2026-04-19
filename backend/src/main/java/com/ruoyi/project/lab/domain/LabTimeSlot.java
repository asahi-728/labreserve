package com.ruoyi.project.lab.domain;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.framework.aspectj.lang.annotation.Excel;
import com.ruoyi.framework.web.domain.BaseEntity;

/**
 * 设备可预约时段对象 lab_time_slot
 * 
 * @author asahi
 * @date 2026-04-10
 */
public class LabTimeSlot extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 时段ID */
    private Long slotId;

    /** 设备ID */
    @Excel(name = "设备ID")
    private Long deviceId;

    /** 日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "日期", width = 30, dateFormat = "yyyy-MM-dd")
    private Date slotDate;

    /** 开始时段 */
    @Excel(name = "开始时段", width = 30)
    private String startTime;

    /** 结束时段 */
    @Excel(name = "结束时段", width = 30)
    private String endTime;

    /** 是否可用 */
    @Excel(name = "是否可用")
    private String isAvailable;

    public void setSlotId(Long slotId) 
    {
        this.slotId = slotId;
    }

    public Long getSlotId() 
    {
        return slotId;
    }

    public void setDeviceId(Long deviceId) 
    {
        this.deviceId = deviceId;
    }

    public Long getDeviceId() 
    {
        return deviceId;
    }

    public void setSlotDate(Date slotDate) 
    {
        this.slotDate = slotDate;
    }

    public Date getSlotDate() 
    {
        return slotDate;
    }

    public void setStartTime(String startTime) 
    {
        this.startTime = startTime;
    }

    public String getStartTime() 
    {
        return startTime;
    }

    public void setEndTime(String endTime) 
    {
        this.endTime = endTime;
    }

    public String getEndTime() 
    {
        return endTime;
    }

    public void setIsAvailable(String isAvailable) 
    {
        this.isAvailable = isAvailable;
    }

    public String getIsAvailable() 
    {
        return isAvailable;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("slotId", getSlotId())
            .append("deviceId", getDeviceId())
            .append("slotDate", getSlotDate())
            .append("startTime", getStartTime())
            .append("endTime", getEndTime())
            .append("isAvailable", getIsAvailable())
            .append("createTime", getCreateTime())
            .toString();
    }
}
