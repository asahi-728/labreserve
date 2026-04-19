package com.ruoyi.project.lab.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.framework.aspectj.lang.annotation.Excel;
import com.ruoyi.framework.web.domain.BaseEntity;

/**
 * 实验室设备对象 lab_device
 * 
 * @author asahi
 * @date 2026-04-10
 */
public class LabDevice extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 设备ID */
    private Long deviceId;

    /** 设备名称 */
    @Excel(name = "设备名称")
    private String deviceName;

    /** 设备分类ID */
    @Excel(name = "设备分类ID")
    private Long categoryId;

    /** 所属实验室ID */
    @Excel(name = "所属实验室ID")
    private Long labId;

    /** 设备状态 */
    @Excel(name = "设备状态")
    private String status;

    /** 设备规格/说明 */
    @Excel(name = "设备规格/说明")
    private String specs;

    public void setDeviceId(Long deviceId) 
    {
        this.deviceId = deviceId;
    }

    public Long getDeviceId() 
    {
        return deviceId;
    }

    public void setDeviceName(String deviceName) 
    {
        this.deviceName = deviceName;
    }

    public String getDeviceName() 
    {
        return deviceName;
    }

    public void setCategoryId(Long categoryId) 
    {
        this.categoryId = categoryId;
    }

    public Long getCategoryId() 
    {
        return categoryId;
    }

    public void setLabId(Long labId) 
    {
        this.labId = labId;
    }

    public Long getLabId() 
    {
        return labId;
    }

    public void setStatus(String status) 
    {
        this.status = status;
    }

    public String getStatus() 
    {
        return status;
    }

    public void setSpecs(String specs) 
    {
        this.specs = specs;
    }

    public String getSpecs() 
    {
        return specs;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("deviceId", getDeviceId())
            .append("deviceName", getDeviceName())
            .append("categoryId", getCategoryId())
            .append("labId", getLabId())
            .append("status", getStatus())
            .append("specs", getSpecs())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
