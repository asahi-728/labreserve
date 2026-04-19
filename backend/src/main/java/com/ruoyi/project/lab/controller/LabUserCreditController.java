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
import com.ruoyi.project.lab.domain.LabUserCredit;
import com.ruoyi.project.lab.service.ILabUserCreditService;
import com.ruoyi.framework.web.controller.BaseController;
import com.ruoyi.framework.web.domain.AjaxResult;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.framework.web.page.TableDataInfo;

/**
 * 用户信用评分Controller
 * 
 * @author asahi
 * @date 2026-04-10
 */
@RestController
@RequestMapping("/lab/credit")
public class LabUserCreditController extends BaseController
{
    @Autowired
    private ILabUserCreditService labUserCreditService;

    /**
     * 查询用户信用评分列表
     */
    @PreAuthorize("@ss.hasPermi('lab:credit:list')")
    @GetMapping("/list")
    public TableDataInfo list(LabUserCredit labUserCredit)
    {
        startPage();
        List<LabUserCredit> list = labUserCreditService.selectLabUserCreditList(labUserCredit);
        return getDataTable(list);
    }

    /**
     * 导出用户信用评分列表
     */
    @PreAuthorize("@ss.hasPermi('lab:credit:export')")
    @Log(title = "用户信用评分", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, LabUserCredit labUserCredit)
    {
        List<LabUserCredit> list = labUserCreditService.selectLabUserCreditList(labUserCredit);
        ExcelUtil<LabUserCredit> util = new ExcelUtil<LabUserCredit>(LabUserCredit.class);
        util.exportExcel(response, list, "用户信用评分数据");
    }

    /**
     * 获取用户信用评分详细信息
     */
    @PreAuthorize("@ss.hasPermi('lab:credit:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return success(labUserCreditService.selectLabUserCreditById(id));
    }

    /**
     * 根据用户ID获取信用记录
     */
    @PreAuthorize("@ss.hasPermi('lab:credit:query')")
    @GetMapping("/user/{userId}")
    public AjaxResult getByUserId(@PathVariable Long userId)
    {
        return success(labUserCreditService.selectLabUserCreditByUserId(userId));
    }

    /**
     * 新增用户信用评分
     */
    @PreAuthorize("@ss.hasPermi('lab:credit:add')")
    @Log(title = "用户信用评分", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody LabUserCredit labUserCredit)
    {
        labUserCredit.setCreateBy(getUsername());
        return toAjax(labUserCreditService.insertLabUserCredit(labUserCredit));
    }

    /**
     * 修改用户信用评分
     */
    @PreAuthorize("@ss.hasPermi('lab:credit:edit')")
    @Log(title = "用户信用评分", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody LabUserCredit labUserCredit)
    {
        labUserCredit.setUpdateBy(getUsername());
        return toAjax(labUserCreditService.updateLabUserCredit(labUserCredit));
    }

    /**
     * 更新用户信用评分（守约）
     */
    @PreAuthorize("@ss.hasPermi('lab:credit:update')")
    @Log(title = "更新信用评分-守约", businessType = BusinessType.UPDATE)
    @PutMapping("/onTime/{userId}")
    public AjaxResult updateCreditOnTime(@PathVariable Long userId)
    {
        return toAjax(labUserCreditService.updateCreditOnTime(userId));
    }

    /**
     * 更新用户信用评分（违约）
     */
    @PreAuthorize("@ss.hasPermi('lab:credit:update')")
    @Log(title = "更新信用评分-违约", businessType = BusinessType.UPDATE)
    @PutMapping("/violation/{userId}")
    public AjaxResult updateCreditViolation(@PathVariable Long userId)
    {
        return toAjax(labUserCreditService.updateCreditViolation(userId));
    }

    /**
     * 更新用户信用评分（取消预约）
     */
    @PreAuthorize("@ss.hasPermi('lab:credit:update')")
    @Log(title = "更新信用评分-取消", businessType = BusinessType.UPDATE)
    @PutMapping("/cancel/{userId}")
    public AjaxResult updateCreditCancel(@PathVariable Long userId)
    {
        return toAjax(labUserCreditService.updateCreditCancel(userId));
    }

    /**
     * 删除用户信用评分
     */
    @PreAuthorize("@ss.hasPermi('lab:credit:remove')")
    @Log(title = "用户信用评分", businessType = BusinessType.DELETE)
	@DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(labUserCreditService.deleteLabUserCreditByIds(ids));
    }
}
