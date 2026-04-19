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
import com.ruoyi.project.lab.domain.LabTimeSlot;
import com.ruoyi.project.lab.service.ILabTimeSlotService;
import com.ruoyi.framework.web.controller.BaseController;
import com.ruoyi.framework.web.domain.AjaxResult;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.framework.web.page.TableDataInfo;

/**
 * 设备可预约时段Controller
 * 
 * @author asahi
 * @date 2026-04-10
 */
@RestController
@RequestMapping("/lab/slot")
public class LabTimeSlotController extends BaseController
{
    @Autowired
    private ILabTimeSlotService labTimeSlotService;

    /**
     * 查询设备可预约时段列表
     */
    @PreAuthorize("@ss.hasPermi('lab:slot:list')")
    @GetMapping("/list")
    public TableDataInfo list(LabTimeSlot labTimeSlot)
    {
        startPage();
        List<LabTimeSlot> list = labTimeSlotService.selectLabTimeSlotList(labTimeSlot);
        return getDataTable(list);
    }

    /**
     * 查询设备可预约时段列表（不分页）
     */
    @PreAuthorize("@ss.hasPermi('lab:slot:list')")
    @GetMapping("/listAll")
    public AjaxResult listAll(LabTimeSlot labTimeSlot)
    {
        List<LabTimeSlot> list = labTimeSlotService.selectLabTimeSlotList(labTimeSlot);
        return success(list);
    }

    /**
     * 导出设备可预约时段列表
     */
    @PreAuthorize("@ss.hasPermi('lab:slot:export')")
    @Log(title = "设备可预约时段", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, LabTimeSlot labTimeSlot)
    {
        List<LabTimeSlot> list = labTimeSlotService.selectLabTimeSlotList(labTimeSlot);
        ExcelUtil<LabTimeSlot> util = new ExcelUtil<LabTimeSlot>(LabTimeSlot.class);
        util.exportExcel(response, list, "设备可预约时段数据");
    }

    /**
     * 获取设备可预约时段详细信息
     */
    @PreAuthorize("@ss.hasPermi('lab:slot:query')")
    @GetMapping(value = "/{slotId}")
    public AjaxResult getInfo(@PathVariable("slotId") Long slotId)
    {
        return success(labTimeSlotService.selectLabTimeSlotBySlotId(slotId));
    }

    /**
     * 新增设备可预约时段
     */
    @PreAuthorize("@ss.hasPermi('lab:slot:add')")
    @Log(title = "设备可预约时段", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody LabTimeSlot labTimeSlot)
    {
        labTimeSlot.setCreateBy(getUsername());
        return toAjax(labTimeSlotService.insertLabTimeSlot(labTimeSlot));
    }
    
    /**
     * 批量新增设备可预约时段
     */
    @PreAuthorize("@ss.hasPermi('lab:slot:add')")
    @Log(title = "设备可预约时段", businessType = BusinessType.INSERT)
    @PostMapping("/batch")
    public AjaxResult batchAdd(@RequestBody List<LabTimeSlot> labTimeSlotList)
    {
        String username = getUsername();
        for (LabTimeSlot slot : labTimeSlotList) {
            slot.setCreateBy(username);
        }
        return toAjax(labTimeSlotService.batchInsertLabTimeSlot(labTimeSlotList));
    }

    /**
     * 修改设备可预约时段
     */
    @PreAuthorize("@ss.hasPermi('lab:slot:edit')")
    @Log(title = "设备可预约时段", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody LabTimeSlot labTimeSlot)
    {
        labTimeSlot.setUpdateBy(getUsername());
        return toAjax(labTimeSlotService.updateLabTimeSlot(labTimeSlot));
    }

    /**
     * 删除设备可预约时段
     */
    @PreAuthorize("@ss.hasPermi('lab:slot:remove')")
    @Log(title = "设备可预约时段", businessType = BusinessType.DELETE)
	@DeleteMapping("/{slotIds}")
    public AjaxResult remove(@PathVariable Long[] slotIds)
    {
        return toAjax(labTimeSlotService.deleteLabTimeSlotBySlotIds(slotIds));
    }
}
