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
import com.ruoyi.project.lab.domain.LabReservationRule;
import com.ruoyi.project.lab.service.ILabReservationRuleService;
import com.ruoyi.framework.web.controller.BaseController;
import com.ruoyi.framework.web.domain.AjaxResult;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.framework.web.page.TableDataInfo;

/**
 * 预约规则配置Controller
 *
 * @author asahi
 * @date 2026-04-16
 */
@RestController
@RequestMapping("/lab/reservation/rule")
public class LabReservationRuleController extends BaseController
{
    @Autowired
    private ILabReservationRuleService labReservationRuleService;

    /**
     * 查询预约规则配置列表
     */
    @PreAuthorize("@ss.hasPermi('lab:rule:list')")
    @GetMapping("/list")
    public TableDataInfo list(LabReservationRule labReservationRule)
    {
        startPage();
        List<LabReservationRule> list = labReservationRuleService.selectLabReservationRuleList(labReservationRule);
        return getDataTable(list);
    }

    /**
     * 导出预约规则配置列表
     */
    @PreAuthorize("@ss.hasPermi('lab:rule:export')")
    @Log(title = "预约规则配置", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, LabReservationRule labReservationRule)
    {
        List<LabReservationRule> list = labReservationRuleService.selectLabReservationRuleList(labReservationRule);
        ExcelUtil<LabReservationRule> util = new ExcelUtil<LabReservationRule>(LabReservationRule.class);
        util.exportExcel(response, list, "预约规则配置数据");
    }

    /**
     * 获取预约规则配置详细信息
     */
    @PreAuthorize("@ss.hasPermi('lab:rule:query')")
    @GetMapping(value = "/{ruleId}")
    public AjaxResult getInfo(@PathVariable("ruleId") Long ruleId)
    {
        return success(labReservationRuleService.selectLabReservationRuleByRuleId(ruleId));
    }

    /**
     * 新增预约规则配置
     */
    @PreAuthorize("@ss.hasPermi('lab:rule:add')")
    @Log(title = "预约规则配置", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody LabReservationRule labReservationRule)
    {
        return toAjax(labReservationRuleService.insertLabReservationRule(labReservationRule));
    }

    /**
     * 修改预约规则配置
     */
    @PreAuthorize("@ss.hasPermi('lab:rule:edit')")
    @Log(title = "预约规则配置", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody LabReservationRule labReservationRule)
    {
        return toAjax(labReservationRuleService.updateLabReservationRule(labReservationRule));
    }

    /**
     * 删除预约规则配置
     */
    @PreAuthorize("@ss.hasPermi('lab:rule:remove')")
    @Log(title = "预约规则配置", businessType = BusinessType.DELETE)
	@DeleteMapping("/{ruleIds}")
    public AjaxResult remove(@PathVariable Long[] ruleIds)
    {
        return toAjax(labReservationRuleService.deleteLabReservationRuleByRuleIds(ruleIds));
    }
}
