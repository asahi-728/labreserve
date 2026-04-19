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
import com.ruoyi.project.lab.domain.LabDevice;
import com.ruoyi.project.lab.service.ILabDeviceService;
import com.ruoyi.framework.web.controller.BaseController;
import com.ruoyi.framework.web.domain.AjaxResult;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.framework.web.page.TableDataInfo;

/**
 * 实验室设备Controller
 * 
 * @author asahi
 * @date 2026-04-10
 */
@RestController
@RequestMapping("/lab/device")
public class LabDeviceController extends BaseController
{
    @Autowired
    private ILabDeviceService labDeviceService;

    /**
     * 查询实验室设备列表
     */
    @PreAuthorize("@ss.hasPermi('lab:device:list')")
    @GetMapping("/list")
    public TableDataInfo list(LabDevice labDevice)
    {
        startPage();
        List<LabDevice> list = labDeviceService.selectLabDeviceList(labDevice);
        return getDataTable(list);
    }

    /**
     * 导出实验室设备列表
     */
    @PreAuthorize("@ss.hasPermi('lab:device:export')")
    @Log(title = "实验室设备", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, LabDevice labDevice)
    {
        List<LabDevice> list = labDeviceService.selectLabDeviceList(labDevice);
        ExcelUtil<LabDevice> util = new ExcelUtil<LabDevice>(LabDevice.class);
        util.exportExcel(response, list, "实验室设备数据");
    }

    /**
     * 获取实验室设备详细信息
     */
    @PreAuthorize("@ss.hasPermi('lab:device:query')")
    @GetMapping(value = "/{deviceId}")
    public AjaxResult getInfo(@PathVariable("deviceId") Long deviceId)
    {
        return success(labDeviceService.selectLabDeviceByDeviceId(deviceId));
    }

    /**
     * 新增实验室设备
     */
    @PreAuthorize("@ss.hasPermi('lab:device:add')")
    @Log(title = "实验室设备", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody LabDevice labDevice)
    {
        labDevice.setCreateBy(getUsername());
        return toAjax(labDeviceService.insertLabDevice(labDevice));
    }

    /**
     * 修改实验室设备
     */
    @PreAuthorize("@ss.hasPermi('lab:device:edit')")
    @Log(title = "实验室设备", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody LabDevice labDevice)
    {
        labDevice.setUpdateBy(getUsername());
        return toAjax(labDeviceService.updateLabDevice(labDevice));
    }

    /**
     * 删除实验室设备
     */
    @PreAuthorize("@ss.hasPermi('lab:device:remove')")
    @Log(title = "实验室设备", businessType = BusinessType.DELETE)
	@DeleteMapping("/{deviceIds}")
    public AjaxResult remove(@PathVariable Long[] deviceIds)
    {
        return toAjax(labDeviceService.deleteLabDeviceByDeviceIds(deviceIds));
    }
}
