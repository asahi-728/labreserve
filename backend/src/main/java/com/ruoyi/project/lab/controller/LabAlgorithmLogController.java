package com.ruoyi.project.lab.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.ruoyi.framework.aspectj.lang.annotation.Log;
import com.ruoyi.framework.aspectj.lang.enums.BusinessType;
import com.ruoyi.framework.web.controller.BaseController;
import com.ruoyi.framework.web.domain.AjaxResult;
import com.ruoyi.framework.web.page.TableDataInfo;
import com.ruoyi.project.lab.config.AlgorithmConfig;
import com.ruoyi.project.lab.domain.LabAlgorithmLog;
import com.ruoyi.project.lab.service.ILabAlgorithmLogService;

/**
 * 算法日志Controller
 *
 * @author ruoyi
 * @date 2026-04-17
 */
@RestController
@RequestMapping("/lab/algorithm/log")
public class LabAlgorithmLogController extends BaseController
{
    @Autowired
    private ILabAlgorithmLogService labAlgorithmLogService;

    @Autowired
    private AlgorithmConfig algorithmConfig;

    /**
     * 查询算法日志列表
     */
    @PreAuthorize("@ss.hasPermi('lab:algorithm:list')")
    @Log(title = "算法日志", businessType = BusinessType.OTHER)
    @GetMapping("/list")
    public TableDataInfo list(LabAlgorithmLog labAlgorithmLog)
    {
        startPage();
        List<LabAlgorithmLog> list = labAlgorithmLogService.selectLabAlgorithmLogList(labAlgorithmLog);
        return getDataTable(list);
    }

    /**
     * 获取算法日志详细信息
     */
    @PreAuthorize("@ss.hasPermi('lab:algorithm:query')")
    @GetMapping("/{logId}")
    public AjaxResult getInfo(@PathVariable("logId") Long logId)
    {
        return AjaxResult.success(labAlgorithmLogService.selectLabAlgorithmLogByLogId(logId));
    }

    /**
     * 删除算法日志
     */
    @PreAuthorize("@ss.hasPermi('lab:algorithm:remove')")
    @Log(title = "算法日志", businessType = BusinessType.DELETE)
    @DeleteMapping("/{logId}")
    public AjaxResult remove(@PathVariable Long logId)
    {
        return toAjax(labAlgorithmLogService.deleteLabAlgorithmLogByLogId(logId));
    }

    /**
     * 批量删除算法日志
     */
    @PreAuthorize("@ss.hasPermi('lab:algorithm:remove')")
    @Log(title = "算法日志", businessType = BusinessType.DELETE)
    @DeleteMapping("/batch/{logIds}")
    public AjaxResult removeBatch(@PathVariable Long[] logIds)
    {
        return toAjax(labAlgorithmLogService.deleteLabAlgorithmLogByLogIds(logIds));
    }

    /**
     * 清空所有算法日志
     */
    @PreAuthorize("@ss.hasPermi('lab:algorithm:remove')")
    @Log(title = "算法日志", businessType = BusinessType.CLEAN)
    @DeleteMapping("/clean")
    public AjaxResult clean()
    {
        LabAlgorithmLog log = new LabAlgorithmLog();
        List<LabAlgorithmLog> list = labAlgorithmLogService.selectLabAlgorithmLogList(log);
        if (list.size() > 0)
        {
            Long[] logIds = list.stream().map(LabAlgorithmLog::getLogId).toArray(Long[]::new);
            return toAjax(labAlgorithmLogService.deleteLabAlgorithmLogByLogIds(logIds));
        }
        return AjaxResult.success("没有日志可清空");
    }
}
