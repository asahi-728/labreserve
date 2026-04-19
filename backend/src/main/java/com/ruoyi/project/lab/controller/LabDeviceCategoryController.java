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
import com.ruoyi.project.lab.domain.LabDeviceCategory;
import com.ruoyi.project.lab.service.ILabDeviceCategoryService;
import com.ruoyi.framework.web.controller.BaseController;
import com.ruoyi.framework.web.domain.AjaxResult;
import com.ruoyi.common.utils.poi.ExcelUtil;

/**
 * 设备分类Controller
 * 
 * @author asahi
 * @date 2026-04-10
 */
@RestController
@RequestMapping("/lab/category")
public class LabDeviceCategoryController extends BaseController
{
    @Autowired
    private ILabDeviceCategoryService labDeviceCategoryService;

    /**
     * 查询设备分类列表（树形）
     */
    @PreAuthorize("@ss.hasPermi('lab:category:list')")
    @GetMapping("/list")
    public AjaxResult list(LabDeviceCategory labDeviceCategory)
    {
        List<LabDeviceCategory> list = labDeviceCategoryService.selectLabDeviceCategoryList(labDeviceCategory);
        return success(labDeviceCategoryService.buildCategoryTree(list));
    }

    /**
     * 查询设备分类下拉树结构
     */
    @PreAuthorize("@ss.hasPermi('lab:category:list')")
    @GetMapping("/treeSelect")
    public AjaxResult treeSelect(LabDeviceCategory labDeviceCategory)
    {
        List<LabDeviceCategory> list = labDeviceCategoryService.selectLabDeviceCategoryList(labDeviceCategory);
        return success(labDeviceCategoryService.buildCategoryTreeSelect(list));
    }

    /**
     * 导出设备分类列表
     */
    @PreAuthorize("@ss.hasPermi('lab:category:export')")
    @Log(title = "设备分类", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, LabDeviceCategory labDeviceCategory)
    {
        List<LabDeviceCategory> list = labDeviceCategoryService.selectLabDeviceCategoryList(labDeviceCategory);
        ExcelUtil<LabDeviceCategory> util = new ExcelUtil<LabDeviceCategory>(LabDeviceCategory.class);
        util.exportExcel(response, list, "设备分类数据");
    }

    /**
     * 获取设备分类详细信息
     */
    @PreAuthorize("@ss.hasPermi('lab:category:query')")
    @GetMapping(value = "/{categoryId}")
    public AjaxResult getInfo(@PathVariable("categoryId") Long categoryId)
    {
        return success(labDeviceCategoryService.selectLabDeviceCategoryByCategoryId(categoryId));
    }

    /**
     * 新增设备分类
     */
    @PreAuthorize("@ss.hasPermi('lab:category:add')")
    @Log(title = "设备分类", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody LabDeviceCategory labDeviceCategory)
    {
        labDeviceCategory.setCreateBy(getUsername());
        return toAjax(labDeviceCategoryService.insertLabDeviceCategory(labDeviceCategory));
    }

    /**
     * 修改设备分类
     */
    @PreAuthorize("@ss.hasPermi('lab:category:edit')")
    @Log(title = "设备分类", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody LabDeviceCategory labDeviceCategory)
    {
        labDeviceCategory.setUpdateBy(getUsername());
        return toAjax(labDeviceCategoryService.updateLabDeviceCategory(labDeviceCategory));
    }

    /**
     * 删除设备分类
     */
    @PreAuthorize("@ss.hasPermi('lab:category:remove')")
    @Log(title = "设备分类", businessType = BusinessType.DELETE)
	@DeleteMapping("/{categoryIds}")
    public AjaxResult remove(@PathVariable Long[] categoryIds)
    {
        return toAjax(labDeviceCategoryService.deleteLabDeviceCategoryByCategoryIds(categoryIds));
    }
}
