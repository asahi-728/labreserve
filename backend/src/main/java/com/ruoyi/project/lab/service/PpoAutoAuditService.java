package com.ruoyi.project.lab.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.project.lab.config.AlgorithmConfig;
import com.ruoyi.project.lab.domain.LabReservation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * PPO算法自动审批服务
 * 
 * @author ruoyi
 */
@Service
public class PpoAutoAuditService
{
    private static final Logger log = LoggerFactory.getLogger(PpoAutoAuditService.class);
    
    @Autowired
    private AlgorithmConfig algorithmConfig;
    
    @Autowired
    private ILabAlgorithmLogService labAlgorithmLogService;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * 使用PPO算法自动审批预约
     * 
     * @param reservation 预约信息
     * @return 审批结果，true表示批准，false表示拒绝，null表示审批失败
     */
    public Boolean autoAuditReservation(LabReservation reservation)
    {
        log.info("========== PPO自动审批开始 ==========");
        log.info("预约ID: {}, 设备ID: {}, 用户ID: {}", 
                reservation.getReserveId(), 
                reservation.getDeviceId(),
                reservation.getUserId());
        
        long startTime = System.currentTimeMillis();
        String params = null;
        String resultJson = null;
        String status = "0";
        String errorMessage = null;
        Boolean result = null;
        
        // 检查PPO自动审批是否启用
        if (algorithmConfig.getPpoAutoAudit() == null)
        {
            log.warn("PPO自动审批配置为空，跳过自动审批");
            return null;
        }
        
        if (!Boolean.TRUE.equals(algorithmConfig.getPpoAutoAudit().getEnabled()))
        {
            log.info("PPO自动审批未启用，跳过自动审批");
            return null;
        }
        
        // 检查算法服务是否启用
        if (!Boolean.TRUE.equals(algorithmConfig.getEnabled()))
        {
            log.warn("算法服务未启用，跳过PPO自动审批");
            return null;
        }
        
        try
        {
            log.info("算法服务地址: {}", algorithmConfig.getServerUrl());
            log.info("开始调用PPO调度接口...");
            
            // 构建请求参数
            Map<String, Object> requestMap = new HashMap<>();
            requestMap.put("reserve_id", reservation.getReserveId());
            params = objectMapper.writeValueAsString(requestMap);
            
            // 调用PPO调度接口
            Map<String, Object> dispatchResult = callPpoDispatch(reservation.getReserveId());
            
            if (dispatchResult == null)
            {
                log.warn("PPO调度调用失败，跳过自动审批");
                status = "1";
                errorMessage = "PPO调度调用失败";
                return null;
            }
            
            resultJson = objectMapper.writeValueAsString(dispatchResult);
            log.info("PPO调度响应: {}", dispatchResult);
            
            // 解析调度结果
            result = parseDispatchResult(dispatchResult);
            log.info("PPO自动审批结果: {}", result != null ? (result ? "批准" : "拒绝") : "待定");
            
            return result;
        }
        catch (Exception e)
        {
            log.error("PPO自动审批失败", e);
            status = "1";
            errorMessage = e.getMessage();
            log.info("========== PPO自动审批异常结束 ==========");
            return null;
        }
        finally
        {
            // 记录算法日志
            long duration = System.currentTimeMillis() - startTime;
            try
            {
                labAlgorithmLogService.logAlgorithm(
                        "PPO",
                        "DISPATCH",
                        reservation.getUserId(),
                        truncateString(params, 2000),
                        truncateString(resultJson, 2000),
                        status,
                        truncateString(errorMessage, 500),
                        duration
                );
            }
            catch (Exception logError)
            {
                log.error("记录PPO算法日志失败", logError);
            }
            log.info("========== PPO自动审批结束 ==========");
        }
    }
    
    /**
     * 使用PPO算法批量审批预约
     * 
     * @param reserveIds 预约ID列表
     * @return 批量审批结果列表
     */
    public List<Map<String, Object>> batchAuditReservations(List<Long> reserveIds)
    {
        log.info("========== PPO批量自动审批开始 ==========");
        log.info("批量审批预约数量: {}", reserveIds.size());
        
        List<Map<String, Object>> results = new ArrayList<>();
        
        try
        {
            // 直接调用批量dispatch接口（一次性发送所有reserveIds）
            Map<String, Object> dispatchResult = callPpoBatchDispatch(reserveIds);
            
            if (dispatchResult == null)
            {
                log.warn("PPO调度返回null，无法获取结果");
                // 为每个预约ID生成一个error结果
                for (Long reserveId : reserveIds)
                {
                    Map<String, Object> itemResult = new HashMap<>();
                    itemResult.put("reserve_id", reserveId);
                    itemResult.put("status", "error");
                    itemResult.put("reason", "PPO调度返回空响应");
                    results.add(itemResult);
                }
                return results;
            }
            
            log.info("PPO原始响应: {}", dispatchResult);
            
            // 解析data字段（可能是list或dict）
            Object dataObj = dispatchResult.get("data");
            if (dataObj instanceof List)
            {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> dataList = (List<Map<String, Object>>) dataObj;
                results.addAll(dataList);
                log.info("从data字段解析到 {} 条结果", dataList.size());
            }
            else if (dataObj instanceof Map)
            {
                @SuppressWarnings("unchecked")
                Map<String, Object> dataMap = (Map<String, Object>) dataObj;
                results.add(dataMap);
                log.info("data字段为单条结果");
            }
            else if (dataObj == null)
            {
                log.warn("响应中无data字段，使用完整响应作为结果");
                // 尝试直接使用整个响应
                results.add(dispatchResult);
            }
            else
            {
                log.warn("data字段类型未知: {}", dataObj.getClass().getName());
                results.add(dispatchResult);
            }
        }
        catch (Exception e)
        {
            log.error("PPO批量审批异常", e);
            // 为每个预约ID生成一个error结果
            for (Long reserveId : reserveIds)
            {
                Map<String, Object> itemResult = new HashMap<>();
                itemResult.put("reserve_id", reserveId);
                itemResult.put("status", "error");
                itemResult.put("reason", e.getMessage());
                results.add(itemResult);
            }
        }
        
        log.info("========== PPO批量自动审批结束，共处理 {} 条 ==========", results.size());
        return results;
    }
    
    /**
     * 调用PPO批量调度接口
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> callPpoBatchDispatch(List<Long> reserveIds) throws Exception
    {
        String targetUrl = algorithmConfig.getServerUrl() + "/api/ppo/dispatch/batch";
        URL url = new URL(targetUrl);
        
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setConnectTimeout(algorithmConfig.getConnectTimeout());
        
        Integer timeout = algorithmConfig.getPpoAutoAudit() != null ? algorithmConfig.getPpoAutoAudit().getTimeout() : null;
        connection.setReadTimeout(timeout != null ? timeout : 30000);
        
        connection.setDoInput(true);
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "application/json");
        
        // 构建请求体
        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("reserve_ids", reserveIds);
        String requestBody = objectMapper.writeValueAsString(requestMap);
        
        try (OutputStream os = connection.getOutputStream())
        {
            os.write(requestBody.getBytes(StandardCharsets.UTF_8));
        }
        
        int responseCode = connection.getResponseCode();
        StringBuilder response = new StringBuilder();
        
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(
                        responseCode >= 200 && responseCode < 300 
                            ? connection.getInputStream() 
                            : connection.getErrorStream(),
                        StandardCharsets.UTF_8)))
        {
            String line;
            while ((line = br.readLine()) != null)
            {
                response.append(line);
            }
        }
        
        if (responseCode >= 200 && responseCode < 300)
        {
            log.info("PPO批量调度接口响应成功: code={}, body={}", responseCode, response);
            return objectMapper.readValue(response.toString(), Map.class);
        }
        else
        {
            log.error("PPO批量调度接口调用失败: code={}, response={}", responseCode, response);
            throw new Exception("PPO算法服务返回错误: HTTP " + responseCode + ", 响应: " + response);
        }
    }
    
    /**
     * 截断字符串
     */
    private String truncateString(String str, int maxLength)
    {
        if (str == null || str.length() <= maxLength)
        {
            return str;
        }
        return str.substring(0, maxLength - 3) + "...";
    }
    
    /**
     * 调用PPO调度接口
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> callPpoDispatch(Long reserveId) throws Exception
    {
        String targetUrl = algorithmConfig.getServerUrl() + "/api/ppo/dispatch/single";
        URL url = new URL(targetUrl);
        
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setConnectTimeout(algorithmConfig.getConnectTimeout());
        
        // 使用配置的超时时间或默认值
        Integer timeout = algorithmConfig.getPpoAutoAudit().getTimeout();
        connection.setReadTimeout(timeout != null ? timeout : 15000);
        
        connection.setDoInput(true);
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "application/json");
        
        // 构建请求体
        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("reserve_id", reserveId);
        String requestBody = objectMapper.writeValueAsString(requestMap);
        
        try (OutputStream os = connection.getOutputStream())
        {
            os.write(requestBody.getBytes(StandardCharsets.UTF_8));
        }
        
        // 获取响应
        int responseCode = connection.getResponseCode();
        StringBuilder response = new StringBuilder();
        
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(
                        responseCode >= 200 && responseCode < 300 
                            ? connection.getInputStream() 
                            : connection.getErrorStream(),
                        StandardCharsets.UTF_8)))
        {
            String line;
            while ((line = br.readLine()) != null)
            {
                response.append(line);
            }
        }
        
        if (responseCode >= 200 && responseCode < 300)
        {
            return objectMapper.readValue(response.toString(), Map.class);
        }
        else
        {
            log.warn("PPO调度接口调用失败: code={}, response={}", responseCode, response);
            return null;
        }
    }
    
    /**
     * 解析PPO调度结果
     */
    @SuppressWarnings("unchecked")
    private Boolean parseDispatchResult(Map<String, Object> result)
    {
        // 检查响应格式
        Object codeObj = result.get("code");
        if (codeObj == null || !Integer.valueOf(200).equals(codeObj))
        {
            log.warn("PPO调度响应异常: {}", result);
            return null;
        }
        
        Object dataObj = result.get("data");
        if (!(dataObj instanceof Map))
        {
            log.warn("PPO调度数据格式异常: {}", result);
            return null;
        }
        
        Map<String, Object> data = (Map<String, Object>) dataObj;
        Object statusObj = data.get("status");
        
        if (statusObj == null)
        {
            log.warn("PPO调度状态缺失: {}", result);
            return null;
        }
        
        String status = String.valueOf(statusObj);
        
        // 根据调度状态决定审批结果
        // approved: 批准
        // rejected: 拒绝
        // pending: 待人工审核
        switch (status)
        {
            case "approved":
                log.info("PPO算法批准预约");
                return true;
            case "rejected":
                log.info("PPO算法拒绝预约");
                return false;
            case "pending":
            default:
                log.info("PPO算法建议人工审核");
                return null;
        }
    }
}
