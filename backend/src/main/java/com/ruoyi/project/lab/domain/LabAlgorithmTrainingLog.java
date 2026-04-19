package com.ruoyi.project.lab.domain;

import java.math.BigDecimal;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.framework.aspectj.lang.annotation.Excel;
import com.ruoyi.framework.web.domain.BaseEntity;

/**
 * 算法训练日志对象 lab_algorithm_training_log
 * 
 * @author ruoyi
 * @date 2026-04-15
 */
public class LabAlgorithmTrainingLog extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 训练任务ID */
    private Long trainingId;

    /** 算法类型：GATv2/PPO */
    @Excel(name = "算法类型")
    private String algorithmType;

    /** 任务名称 */
    @Excel(name = "任务名称")
    private String taskName;

    /** 开始时间 */
    @Excel(name = "开始时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private java.util.Date startTime;

    /** 结束时间 */
    @Excel(name = "结束时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private java.util.Date endTime;

    /** 执行状态：PENDING/RUNNING/COMPLETED/FAILED */
    @Excel(name = "执行状态")
    private String status;

    /** 训练参数（JSON格式） */
    @Excel(name = "训练参数")
    private String trainingParams;

    /** 总迭代次数 */
    @Excel(name = "总迭代次数")
    private Integer totalEpochs;

    /** 已完成迭代次数 */
    @Excel(name = "已完成迭代次数")
    private Integer completedEpochs;

    /** 最终损失值 */
    @Excel(name = "最终损失值")
    private BigDecimal lossValue;

    /** 准确率 */
    @Excel(name = "准确率")
    private BigDecimal accuracy;

    /** 模型版本 */
    @Excel(name = "模型版本")
    private String modelVersion;

    /** 错误信息 */
    @Excel(name = "错误信息")
    private String errorMessage;

    /** 训练指标详情（JSON格式，存储每轮的loss/accuracy/reward等） */
    @Excel(name = "训练指标")
    private String metrics;

    public void setTrainingId(Long trainingId) 
    {
        this.trainingId = trainingId;
    }

    public Long getTrainingId() 
    {
        return trainingId;
    }

    public void setAlgorithmType(String algorithmType) 
    {
        this.algorithmType = algorithmType;
    }

    public String getAlgorithmType() 
    {
        return algorithmType;
    }

    public void setTaskName(String taskName) 
    {
        this.taskName = taskName;
    }

    public String getTaskName() 
    {
        return taskName;
    }

    public void setStartTime(java.util.Date startTime) 
    {
        this.startTime = startTime;
    }

    public java.util.Date getStartTime() 
    {
        return startTime;
    }

    public void setEndTime(java.util.Date endTime) 
    {
        this.endTime = endTime;
    }

    public java.util.Date getEndTime() 
    {
        return endTime;
    }

    public void setStatus(String status) 
    {
        this.status = status;
    }

    public String getStatus() 
    {
        return status;
    }

    public void setTrainingParams(String trainingParams) 
    {
        this.trainingParams = trainingParams;
    }

    public String getTrainingParams() 
    {
        return trainingParams;
    }

    public void setTotalEpochs(Integer totalEpochs) 
    {
        this.totalEpochs = totalEpochs;
    }

    public Integer getTotalEpochs() 
    {
        return totalEpochs;
    }

    public void setCompletedEpochs(Integer completedEpochs) 
    {
        this.completedEpochs = completedEpochs;
    }

    public Integer getCompletedEpochs() 
    {
        return completedEpochs;
    }

    public void setLossValue(BigDecimal lossValue) 
    {
        this.lossValue = lossValue;
    }

    public BigDecimal getLossValue() 
    {
        return lossValue;
    }

    public void setAccuracy(BigDecimal accuracy) 
    {
        this.accuracy = accuracy;
    }

    public BigDecimal getAccuracy() 
    {
        return accuracy;
    }

    public void setModelVersion(String modelVersion) 
    {
        this.modelVersion = modelVersion;
    }

    public String getModelVersion() 
    {
        return modelVersion;
    }

    public void setErrorMessage(String errorMessage) 
    {
        this.errorMessage = errorMessage;
    }

    public String getErrorMessage() 
    {
        return errorMessage;
    }

    public void setMetrics(String metrics) 
    {
        this.metrics = metrics;
    }

    public String getMetrics() 
    {
        return metrics;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("trainingId", getTrainingId())
            .append("algorithmType", getAlgorithmType())
            .append("taskName", getTaskName())
            .append("startTime", getStartTime())
            .append("endTime", getEndTime())
            .append("status", getStatus())
            .append("trainingParams", getTrainingParams())
            .append("totalEpochs", getTotalEpochs())
            .append("completedEpochs", getCompletedEpochs())
            .append("lossValue", getLossValue())
            .append("accuracy", getAccuracy())
            .append("modelVersion", getModelVersion())
            .append("errorMessage", getErrorMessage())
            .append("metrics", getMetrics())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
