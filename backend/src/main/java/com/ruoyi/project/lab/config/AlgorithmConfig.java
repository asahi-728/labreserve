package com.ruoyi.project.lab.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 算法服务配置
 * 
 * @author ruoyi
 */
@Component
@ConfigurationProperties(prefix = "algorithm")
public class AlgorithmConfig
{
    /** 算法服务地址 */
    private String serverUrl;
    
    /** 连接超时时间（毫秒） */
    private Integer connectTimeout;
    
    /** 读取超时时间（毫秒） */
    private Integer readTimeout;
    
    /** 是否启用算法服务 */
    private Boolean enabled;
    
    /** 日志记录配置 */
    private LoggingConfig logging;
    
    /** PPO自动审批配置 */
    private PpoAutoAuditConfig ppoAutoAudit;
    
    public String getServerUrl()
    {
        return serverUrl;
    }
    
    public void setServerUrl(String serverUrl)
    {
        this.serverUrl = serverUrl;
    }
    
    public Integer getConnectTimeout()
    {
        return connectTimeout;
    }
    
    public void setConnectTimeout(Integer connectTimeout)
    {
        this.connectTimeout = connectTimeout;
    }
    
    public Integer getReadTimeout()
    {
        return readTimeout;
    }
    
    public void setReadTimeout(Integer readTimeout)
    {
        this.readTimeout = readTimeout;
    }
    
    public Boolean getEnabled()
    {
        return enabled;
    }
    
    public void setEnabled(Boolean enabled)
    {
        this.enabled = enabled;
    }
    
    public LoggingConfig getLogging()
    {
        return logging;
    }
    
    public void setLogging(LoggingConfig logging)
    {
        this.logging = logging;
    }
    
    public PpoAutoAuditConfig getPpoAutoAudit()
    {
        return ppoAutoAudit;
    }
    
    public void setPpoAutoAudit(PpoAutoAuditConfig ppoAutoAudit)
    {
        this.ppoAutoAudit = ppoAutoAudit;
    }
    
    /**
     * 日志记录配置
     */
    public static class LoggingConfig
    {
        /** 是否记录推荐日志 */
        private Boolean recommendEnabled;
        
        /** 是否记录PPO调度日志 */
        private Boolean ppoEnabled;
        
        /** 异步记录日志 */
        private Boolean asyncEnabled;
        
        public Boolean getRecommendEnabled()
        {
            return recommendEnabled;
        }
        
        public void setRecommendEnabled(Boolean recommendEnabled)
        {
            this.recommendEnabled = recommendEnabled;
        }
        
        public Boolean getPpoEnabled()
        {
            return ppoEnabled;
        }
        
        public void setPpoEnabled(Boolean ppoEnabled)
        {
            this.ppoEnabled = ppoEnabled;
        }
        
        public Boolean getAsyncEnabled()
        {
            return asyncEnabled;
        }
        
        public void setAsyncEnabled(Boolean asyncEnabled)
        {
            this.asyncEnabled = asyncEnabled;
        }
    }
    
    /**
     * PPO自动审批配置
     */
    public static class PpoAutoAuditConfig
    {
        /** 是否启用PPO自动审批 */
        private Boolean enabled;
        
        /** 是否在提交预约时自动触发 */
        private Boolean autoTriggerOnSubmit;
        
        /** PPO调度超时时间（毫秒） */
        private Integer timeout;
        
        public Boolean getEnabled()
        {
            return enabled;
        }
        
        public void setEnabled(Boolean enabled)
        {
            this.enabled = enabled;
        }
        
        public Boolean getAutoTriggerOnSubmit()
        {
            return autoTriggerOnSubmit;
        }
        
        public void setAutoTriggerOnSubmit(Boolean autoTriggerOnSubmit)
        {
            this.autoTriggerOnSubmit = autoTriggerOnSubmit;
        }
        
        public Integer getTimeout()
        {
            return timeout;
        }
        
        public void setTimeout(Integer timeout)
        {
            this.timeout = timeout;
        }
    }
}
