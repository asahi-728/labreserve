package com.ruoyi.project.lab.domain;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * 算法日志表 lab_algorithm_log
 *
 * @author ruoyi
 * @date 2026-04-17
 */
public class LabAlgorithmLog
{
    private static final long serialVersionUID = 1L;

    /** 日志ID */
    private Long logId;

    /** 算法类型：GAT=gat, PPO=ppo */
    private String algorithmType;

    /** 操作类型：TRAIN=train, RECOMMEND=recommend, DISPATCH=dispatch */
    private String operationType;

    /** 用户ID */
    private Long userId;

    /** 请求参数 */
    private String params;

    /** 结果 */
    private String result;

    /** 状态：0=成功, 1=失败 */
    private String status;

    /** 错误信息 */
    private String errorMessage;

    /** 执行时长 */
    private Long durationMs;

    /** 创建者 */
    private String createBy;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    /** 更新者 */
    private String updateBy;

    /** 更新时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;

    /** 备注 */
    private String remark;

    public Long getLogId()
    {
        return logId;
    }

    public void setLogId(Long logId)
    {
        this.logId = logId;
    }

    public String getAlgorithmType()
    {
        return algorithmType;
    }

    public void setAlgorithmType(String algorithmType)
    {
        this.algorithmType = algorithmType;
    }

    public String getOperationType()
    {
        return operationType;
    }

    public void setOperationType(String operationType)
    {
        this.operationType = operationType;
    }

    public Long getUserId()
    {
        return userId;
    }

    public void setUserId(Long userId)
    {
        this.userId = userId;
    }

    public String getParams()
    {
        return params;
    }

    public void setParams(String params)
    {
        this.params = params;
    }

    public String getResult()
    {
        return result;
    }

    public void setResult(String result)
    {
        this.result = result;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    public String getErrorMessage()
    {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage)
    {
        this.errorMessage = errorMessage;
    }

    public Long getDurationMs()
    {
        return durationMs;
    }

    public void setDurationMs(Long durationMs)
    {
        this.durationMs = durationMs;
    }

    public String getCreateBy()
    {
        return createBy;
    }

    public void setCreateBy(String createBy)
    {
        this.createBy = createBy;
    }

    public Date getCreateTime()
    {
        return createTime;
    }

    public void setCreateTime(Date createTime)
    {
        this.createTime = createTime;
    }

    public String getUpdateBy()
    {
        return updateBy;
    }

    public void setUpdateBy(String updateBy)
    {
        this.updateBy = updateBy;
    }

    public Date getUpdateTime()
    {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime)
    {
        this.updateTime = updateTime;
    }

    public String getRemark()
    {
        return remark;
    }

    public void setRemark(String remark)
    {
        this.remark = remark;
    }

    @Override
    public String toString()
    {
        return "LabAlgorithmLog{" +
                "logId=" + logId +
                ", algorithmType='" + algorithmType + '\'' +
                ", operationType='" + operationType + '\'' +
                ", userId=" + userId +
                ", status='" + status + '\'' +
                ", durationMs=" + durationMs +
                ", createTime=" + createTime +
                '}';
    }
}
