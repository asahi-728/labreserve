package com.ruoyi.project.lab.controller;

import java.util.List;
import javax.servlet.http.HttpServletResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.framework.aspectj.lang.annotation.Log;
import com.ruoyi.framework.aspectj.lang.enums.BusinessType;
import com.ruoyi.project.lab.domain.LabUserCreditLog;
import com.ruoyi.project.lab.service.ILabUserCreditLogService;
import com.ruoyi.framework.web.controller.BaseController;
import com.ruoyi.framework.web.domain.AjaxResult;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.framework.web.page.TableDataInfo;

/**
 * 用户信用分变化日志Controller
 * 
 * @author asahi
 * @date 2026-04-18
 */
@RestController
@RequestMapping("/lab/credit/log")
public class LabUserCreditLogController extends BaseController
{
    @Autowired
    private ILabUserCreditLogService labUserCreditLogService;

    /**
     * 查询用户信用分变化日志列表
     */
    @PreAuthorize("@ss.hasPermi('lab:creditLog:list')")
    @GetMapping("/list")
    public TableDataInfo list(LabUserCreditLog labUserCreditLog)
    {
        startPage();
        List<LabUserCreditLog> list = labUserCreditLogService.selectLabUserCreditLogList(labUserCreditLog);
        return getDataTable(list);
    }

    /**
     * 导出用户信用分变化日志列表
     */
    @PreAuthorize("@ss.hasPermi('lab:creditLog:export')")
    @Log(title = "用户信用分变化日志", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, LabUserCreditLog labUserCreditLog)
    {
        List<LabUserCreditLog> list = labUserCreditLogService.selectLabUserCreditLogList(labUserCreditLog);
        ExcelUtil<LabUserCreditLog> util = new ExcelUtil<LabUserCreditLog>(LabUserCreditLog.class);
        util.exportExcel(response, list, "用户信用分变化日志数据");
    }

    /**
     * 获取用户信用分变化日志详细信息
     */
    @PreAuthorize("@ss.hasPermi('lab:creditLog:query')")
    @GetMapping(value = "/{logId}")
    public AjaxResult getInfo(@PathVariable("logId") Long logId)
    {
        return success(labUserCreditLogService.selectLabUserCreditLogByLogId(logId));
    }

    /**
     * 根据用户ID获取信用分变化日志
     */
    @PreAuthorize("@ss.hasPermi('lab:creditLog:query')")
    @GetMapping("/user/{userId}")
    public AjaxResult getByUserId(@PathVariable Long userId)
    {
        return success(labUserCreditLogService.selectLogsByUserId(userId));
    }

    /**
     * 新增用户信用分变化日志
     */
    @PreAuthorize("@ss.hasPermi('lab:creditLog:add')")
    @Log(title = "用户信用分变化日志", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody LabUserCreditLog labUserCreditLog)
    {
        labUserCreditLog.setCreateBy(getUsername());
        return toAjax(labUserCreditLogService.insertLabUserCreditLog(labUserCreditLog));
    }

    /**
     * 删除用户信用分变化日志
     */
    @PreAuthorize("@ss.hasPermi('lab:creditLog:remove')")
    @Log(title = "用户信用分变化日志", businessType = BusinessType.DELETE)
	@DeleteMapping("/{logIds}")
    public AjaxResult remove(@PathVariable Long[] logIds)
    {
        return toAjax(labUserCreditLogService.deleteLabUserCreditLogByLogIds(logIds));
    }
}
