package com.ruoyi.project.lab.controller;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.framework.aspectj.lang.annotation.Log;
import com.ruoyi.framework.aspectj.lang.enums.BusinessType;
import com.ruoyi.framework.web.controller.BaseController;
import com.ruoyi.framework.web.domain.AjaxResult;
import com.ruoyi.project.lab.config.AlgorithmConfig;
import com.ruoyi.project.lab.service.ILabAlgorithmLogService;

/**
 * 算法模块接口转发Controller
 *
 * @author ruoyi
 * @date 2026-04-15
 */
@RestController
@RequestMapping("/lab/algorithm")
public class AlgorithmProxyController extends BaseController
{
    private static final Logger log = LoggerFactory.getLogger(AlgorithmProxyController.class);

    @Autowired
    private AlgorithmConfig algorithmConfig;

    @Autowired
    private ILabAlgorithmLogService labAlgorithmLogService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * GAT推荐接口转发
     */
    @PreAuthorize("@ss.hasPermi('lab:algorithm:proxy')")
    @Log(title = "GAT推荐", businessType = BusinessType.OTHER)
    @PostMapping("/gat/recommend")
    public AjaxResult gatRecommend(@RequestBody String body, HttpServletRequest request)
    {
        return proxyToAlgorithm("/gat/recommend", "GAT", "RECOMMEND", body, request, "POST");
    }

    /**
     * GAT批量推荐接口转发
     */
    @PreAuthorize("@ss.hasPermi('lab:algorithm:proxy')")
    @Log(title = "GAT批量推荐", businessType = BusinessType.OTHER)
    @PostMapping("/gat/recommend/batch")
    public AjaxResult gatRecommendBatch(@RequestBody String body, HttpServletRequest request)
    {
        return proxyToAlgorithm("/gat/recommend/batch", "GAT", "RECOMMEND", body, request, "POST");
    }

    /**
     * GAT训练接口转发
     */
    @PreAuthorize("@ss.hasPermi('lab:algorithm:proxy')")
    @Log(title = "GAT训练", businessType = BusinessType.OTHER)
    @PostMapping("/gat/train")
    public AjaxResult gatTrain(@RequestBody String body, HttpServletRequest request)
    {
        return proxyToAlgorithm("/gat/train", "GAT", "TRAIN", body, request, "POST");
    }

    /**
     * GAT状态查询接口转发
     */
    @PreAuthorize("@ss.hasPermi('lab:algorithm:proxy')")
    @GetMapping("/gat/status")
    public AjaxResult gatStatus()
    {
        return proxyToAlgorithm("/gat/status", "GAT", "STATUS", null, null, "GET");
    }

    /**
     * PPO训练接口转发
     */
    @PreAuthorize("@ss.hasPermi('lab:algorithm:proxy')")
    @Log(title = "PPO训练", businessType = BusinessType.OTHER)
    @PostMapping("/ppo/train")
    public AjaxResult ppoTrain(@RequestBody String body, HttpServletRequest request)
    {
        return proxyToAlgorithm("/ppo/train", "PPO", "TRAIN", body, request, "POST");
    }

    /**
     * PPO调度接口转发
     */
    @PreAuthorize("@ss.hasPermi('lab:algorithm:proxy')")
    @Log(title = "PPO调度", businessType = BusinessType.OTHER)
    @PostMapping("/ppo/dispatch")
    public AjaxResult ppoDispatch(@RequestBody String body, HttpServletRequest request)
    {
        return proxyToAlgorithm("/ppo/dispatch", "PPO", "DISPATCH", body, request, "POST");
    }

    /**
     * PPO单个调度接口转发
     */
    @PreAuthorize("@ss.hasPermi('lab:algorithm:proxy')")
    @Log(title = "PPO单个调度", businessType = BusinessType.OTHER)
    @PostMapping("/ppo/dispatch/single")
    public AjaxResult ppoDispatchSingle(@RequestBody String body, HttpServletRequest request)
    {
        return proxyToAlgorithm("/ppo/dispatch/single", "PPO", "DISPATCH", body, request, "POST");
    }

    /**
     * PPO批量调度接口转发
     */
    @PreAuthorize("@ss.hasPermi('lab:algorithm:proxy')")
    @Log(title = "PPO批量调度", businessType = BusinessType.OTHER)
    @PostMapping("/ppo/dispatch/batch")
    public AjaxResult ppoDispatchBatch(@RequestBody String body, HttpServletRequest request)
    {
        return proxyToAlgorithm("/ppo/dispatch/batch", "PPO", "DISPATCH", body, request, "POST");
    }

    /**
     * PPO状态查询接口转发
     */
    @PreAuthorize("@ss.hasPermi('lab:algorithm:proxy')")
    @GetMapping("/ppo/status")
    public AjaxResult ppoStatus()
    {
        return proxyToAlgorithm("/ppo/status", "PPO", "STATUS", null, null, "GET");
    }

    /**
     * 通用接口转发方法（带日志记录）
     */
    private AjaxResult proxyToAlgorithm(String path, String algorithmType, String operationType, String body, HttpServletRequest request, String method)
    {
        if (!algorithmConfig.getEnabled())
        {
            return error("算法服务未启用");
        }

        long startTime = System.currentTimeMillis();
        String userId = null;
        String params = null;
        String result = null;
        String status = "0";
        String errorMessage = null;

        try
        {
            // 构建目标URL
            String targetUrl = algorithmConfig.getServerUrl() + "/api" + path;
            URL url = new URL(targetUrl);

            // 打开连接
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod(method);
            connection.setConnectTimeout(algorithmConfig.getConnectTimeout());
            connection.setReadTimeout(algorithmConfig.getReadTimeout());
            connection.setDoInput(true);

            // 设置请求头
            if (body != null && !body.isEmpty())
            {
                connection.setRequestProperty("Content-Type", "application/json");
            }

            // 处理请求体
            if (body != null && !body.isEmpty())
            {
                params = body;
                connection.setDoOutput(true);
                try (OutputStream os = connection.getOutputStream())
                {
                    os.write(body.getBytes(StandardCharsets.UTF_8));
                }
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

            // 解析响应
            String responseStr = response.toString();
            result = responseStr;

            // 返回响应
            if (responseCode >= 200 && responseCode < 300)
            {
                try
                {
                    Map<String, Object> responseMap = objectMapper.readValue(responseStr, Map.class);
                    return AjaxResult.success(responseMap);
                }
                catch (Exception e)
                {
                    return AjaxResult.success(responseStr);
                }
            }
            else
            {
                status = "1";
                errorMessage = "HTTP " + responseCode + ": " + responseStr;
                return AjaxResult.error(responseCode, responseStr);
            }
        }
        catch (Exception e)
        {
            status = "1";
            errorMessage = e.getMessage();
            log.error("算法接口转发失败", e);
            return AjaxResult.error("算法接口转发失败: " + e.getMessage());
        }
        finally
        {
            // 记录日志
            long duration = System.currentTimeMillis() - startTime;
            try
            {
                // 尝试从params中提取user_id
                Long userIdLong = null;
                if (params != null && params.contains("user_id"))
                {
                    try
                    {
                        Map<String, Object> paramsMap = objectMapper.readValue(params, Map.class);
                        Object userIdObj = paramsMap.get("user_id");
                        if (userIdObj != null)
                        {
                            userIdLong = Long.parseLong(userIdObj.toString());
                        }
                    }
                    catch (Exception ignored)
                    {
                    }
                }

                labAlgorithmLogService.logAlgorithm(
                        algorithmType,
                        operationType,
                        userIdLong,
                        truncateString(params, 2000),
                        truncateString(result, 2000),
                        status,
                        truncateString(errorMessage, 500),
                        duration
                );
            }
            catch (Exception logError)
            {
                log.error("记录算法日志失败", logError);
            }
        }
    }

    /**
     * 通用接口转发方法（保留旧接口兼容）
     */
    @PreAuthorize("@ss.hasPermi('lab:algorithm:proxy')")
    @Log(title = "算法接口转发", businessType = BusinessType.OTHER)
    @RequestMapping(value = "/**", method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE})
    public AjaxResult proxyRequest(HttpServletRequest request, @RequestBody(required = false) String body)
    {
        if (!algorithmConfig.getEnabled())
        {
            return error("算法服务未启用");
        }

        try
        {
            // 获取请求路径
            String requestUri = request.getRequestURI();
            String contextPath = request.getContextPath();
            String path = requestUri.substring(contextPath.length() + "/lab/algorithm".length());

            // 确定算法类型和操作类型
            String algorithmType = path.contains("/gat/") ? "GAT" : (path.contains("/ppo/") ? "PPO" : "UNKNOWN");
            String operationType = getOperationType(path);

            // 获取原始请求方法
            String method = request.getMethod();

            return proxyToAlgorithm(path, algorithmType, operationType, body, request, method);
        }
        catch (Exception e)
        {
            log.error("算法接口转发失败", e);
            return AjaxResult.error("算法接口转发失败: " + e.getMessage());
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
     * 根据路径获取操作类型
     */
    private String getOperationType(String path)
    {
        if (path.contains("/recommend"))
        {
            return "RECOMMEND";
        }
        else if (path.contains("/dispatch"))
        {
            return "SCHEDULE";
        }
        else if (path.contains("/train"))
        {
            return "TRAIN";
        }
        else if (path.contains("/embedding"))
        {
            return "EMBEDDING";
        }
        else if (path.contains("/status"))
        {
            return "STATUS";
        }
        else if (path.contains("/health"))
        {
            return "HEALTH";
        }
        return "OTHER";
    }
}
