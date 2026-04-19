package com.ruoyi.project.lab.controller;

import java.util.List;
import java.util.Map;
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
import com.ruoyi.project.lab.domain.LabReservation;
import com.ruoyi.project.lab.service.ILabReservationService;
import com.ruoyi.framework.web.controller.BaseController;
import com.ruoyi.framework.web.domain.AjaxResult;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.framework.web.page.TableDataInfo;

/**
 * 实验室设备预约Controller
 *
 * @author asahi
 * @date 2026-04-10
 */
@RestController
@RequestMapping("/lab/reservation")
public class LabReservationController extends BaseController
{
    @Autowired
    private ILabReservationService labReservationService;

    /**
     * 查询实验室设备预约列表
     */
    @PreAuthorize("@ss.hasPermi('lab:reservation:list')")
    @GetMapping("/list")
    public TableDataInfo list(LabReservation labReservation)
    {
        startPage();
        List<LabReservation> list = labReservationService.selectLabReservationList(labReservation);
        return getDataTable(list);
    }

    /**
     * 导出实验室设备预约列表
     */
    @PreAuthorize("@ss.hasPermi('lab:reservation:export')")
    @Log(title = "实验室设备预约", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, LabReservation labReservation)
    {
        List<LabReservation> list = labReservationService.selectLabReservationList(labReservation);
        ExcelUtil<LabReservation> util = new ExcelUtil<LabReservation>(LabReservation.class);
        util.exportExcel(response, list, "实验室设备预约数据");
    }

    /**
     * 获取实验室设备预约详细信息
     */
    @PreAuthorize("@ss.hasPermi('lab:reservation:query')")
    @GetMapping(value = "/{reserveId}")
    public AjaxResult getInfo(@PathVariable("reserveId") Long reserveId)
    {
        return success(labReservationService.selectLabReservationByReserveId(reserveId));
    }

    /**
     * 新增实验室设备预约
     */
    @PreAuthorize("@ss.hasPermi('lab:reservation:add')")
    @Log(title = "实验室设备预约", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody LabReservation labReservation)
    {
        labReservation.setCreateBy(getUsername());
        return toAjax(labReservationService.insertLabReservation(labReservation));
    }

    /**
     * 提交预约申请
     */
    @PreAuthorize("@ss.hasPermi('lab:reservation:submit')")
    @Log(title = "提交预约申请", businessType = BusinessType.INSERT)
    @PostMapping("/submit")
    public AjaxResult submit(@RequestBody LabReservation labReservation)
    {
        logger.info("收到预约提交请求, approvalMode={}, userId={}, deviceId={}", 
                labReservation.getApprovalMode(), labReservation.getUserId(), labReservation.getDeviceId());
        labReservation.setCreateBy(getUsername());
        return toAjax(labReservationService.submitReservation(labReservation));
    }

    /**
     * 审核预约
     */
    @PreAuthorize("@ss.hasPermi('lab:reservation:audit')")
    @Log(title = "审核预约", businessType = BusinessType.UPDATE)
    @PutMapping("/audit")
    public AjaxResult audit(@RequestBody LabReservation labReservation)
    {
        labReservation.setAuditBy(getUsername());
        return toAjax(labReservationService.auditReservation(labReservation));
    }

    /**
     * PPO批量自动审批预约
     */
    @PreAuthorize("@ss.hasPermi('lab:reservation:audit')")
    @Log(title = "PPO批量审批", businessType = BusinessType.UPDATE)
    @PostMapping("/ppoBatchAudit")
    public AjaxResult ppoBatchAudit(@RequestBody Map<String, Object> params)
    {
        @SuppressWarnings("unchecked")
        List<Number> reserveIdList = (List<Number>) params.get("reserveIds");
        if (reserveIdList == null || reserveIdList.isEmpty())
        {
            return error("请选择需要审批的预约");
        }
        Long[] reserveIds = new Long[reserveIdList.size()];
        for (int i = 0; i < reserveIdList.size(); i++)
        {
            reserveIds[i] = reserveIdList.get(i).longValue();
        }
        List<Map<String, Object>> results = labReservationService.ppoBatchAudit(reserveIds);
        return success(results);
    }

    /**
     * 取消预约
     */
    @PreAuthorize("@ss.hasPermi('lab:reservation:cancel')")
    @Log(title = "取消预约", businessType = BusinessType.UPDATE)
    @PutMapping("/cancel/{reserveId}")
    public AjaxResult cancel(@PathVariable Long reserveId)
    {
        return toAjax(labReservationService.cancelReservation(reserveId));
    }

    /**
     * 完成使用
     */
    @PreAuthorize("@ss.hasPermi('lab:reservation:complete')")
    @Log(title = "完成使用", businessType = BusinessType.UPDATE)
    @PutMapping("/complete/{reserveId}")
    public AjaxResult complete(@PathVariable Long reserveId)
    {
        return toAjax(labReservationService.completeReservation(reserveId));
    }

    /**
     * 开始使用
     */
    @PreAuthorize("@ss.hasPermi('lab:reservation:start')")
    @Log(title = "开始使用", businessType = BusinessType.UPDATE)
    @PutMapping("/start/{reserveId}")
    public AjaxResult startUsing(@PathVariable Long reserveId)
    {
        return toAjax(labReservationService.startUsing(reserveId));
    }

    /**
     * 获取当前用户的预约列表
     */
    @PreAuthorize("@ss.hasPermi('lab:myReservation:list')")
    @GetMapping("/my/list")
    public TableDataInfo myList(LabReservation labReservation)
    {
        startPage();
        List<LabReservation> list = labReservationService.selectMyReservationList(labReservation);
        return getDataTable(list);
    }

    /**
     * 检测预约冲突
     */
    @PreAuthorize("@ss.hasPermi('lab:reservation:check')")
    @PostMapping("/checkConflict")
    public AjaxResult checkConflict(@RequestBody LabReservation labReservation)
    {
        return success(labReservationService.checkConflict(labReservation));
    }

    /**
     * 修改实验室设备预约
     */
    @PreAuthorize("@ss.hasPermi('lab:reservation:edit')")
    @Log(title = "实验室设备预约", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody LabReservation labReservation)
    {
        labReservation.setUpdateBy(getUsername());
        return toAjax(labReservationService.updateLabReservation(labReservation));
    }

    /**
     * 删除实验室设备预约
     */
    @PreAuthorize("@ss.hasPermi('lab:reservation:remove')")
    @Log(title = "实验室设备预约", businessType = BusinessType.DELETE)
	@DeleteMapping("/{reserveIds}")
    public AjaxResult remove(@PathVariable Long[] reserveIds)
    {
        return toAjax(labReservationService.deleteLabReservationByReserveIds(reserveIds));
    }
}
