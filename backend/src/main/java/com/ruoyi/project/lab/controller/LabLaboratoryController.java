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
import com.ruoyi.project.lab.domain.LabLaboratory;
import com.ruoyi.project.lab.service.ILabLaboratoryService;
import com.ruoyi.framework.web.controller.BaseController;
import com.ruoyi.framework.web.domain.AjaxResult;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.framework.web.page.TableDataInfo;

/**
 * 实验室信息Controller
 * 
 * @author asahi
 * @date 2026-04-10
 */
@RestController
@RequestMapping("/lab/laboratory")
public class LabLaboratoryController extends BaseController
{
    @Autowired
    private ILabLaboratoryService labLaboratoryService;

    /**
     * 查询实验室信息列表
     */
    @PreAuthorize("@ss.hasPermi('lab:laboratory:list')")
    @GetMapping("/list")
    public TableDataInfo list(LabLaboratory labLaboratory)
    {
        startPage();
        List<LabLaboratory> list = labLaboratoryService.selectLabLaboratoryList(labLaboratory);
        return getDataTable(list);
    }

    /**
     * 导出实验室信息列表
     */
    @PreAuthorize("@ss.hasPermi('lab:laboratory:export')")
    @Log(title = "实验室信息", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, LabLaboratory labLaboratory)
    {
        List<LabLaboratory> list = labLaboratoryService.selectLabLaboratoryList(labLaboratory);
        ExcelUtil<LabLaboratory> util = new ExcelUtil<LabLaboratory>(LabLaboratory.class);
        util.exportExcel(response, list, "实验室信息数据");
    }

    /**
     * 获取实验室信息详细信息
     */
    @PreAuthorize("@ss.hasPermi('lab:laboratory:query')")
    @GetMapping(value = "/{labId}")
    public AjaxResult getInfo(@PathVariable("labId") Long labId)
    {
        return success(labLaboratoryService.selectLabLaboratoryByLabId(labId));
    }

    /**
     * 新增实验室信息
     */
    @PreAuthorize("@ss.hasPermi('lab:laboratory:add')")
    @Log(title = "实验室信息", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody LabLaboratory labLaboratory)
    {
        labLaboratory.setCreateBy(getUsername());
        return toAjax(labLaboratoryService.insertLabLaboratory(labLaboratory));
    }

    /**
     * 修改实验室信息
     */
    @PreAuthorize("@ss.hasPermi('lab:laboratory:edit')")
    @Log(title = "实验室信息", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody LabLaboratory labLaboratory)
    {
        labLaboratory.setUpdateBy(getUsername());
        return toAjax(labLaboratoryService.updateLabLaboratory(labLaboratory));
    }

    /**
     * 删除实验室信息
     */
    @PreAuthorize("@ss.hasPermi('lab:laboratory:remove')")
    @Log(title = "实验室信息", businessType = BusinessType.DELETE)
	@DeleteMapping("/{labIds}")
    public AjaxResult remove(@PathVariable Long[] labIds)
    {
        return toAjax(labLaboratoryService.deleteLabLaboratoryByLabIds(labIds));
    }
}
