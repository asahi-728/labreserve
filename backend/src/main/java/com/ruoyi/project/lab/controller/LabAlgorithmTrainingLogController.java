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
import com.ruoyi.project.lab.domain.LabAlgorithmTrainingLog;
import com.ruoyi.project.lab.service.ILabAlgorithmTrainingLogService;
import com.ruoyi.framework.web.controller.BaseController;
import com.ruoyi.framework.web.domain.AjaxResult;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.framework.web.page.TableDataInfo;

/**
 * 算法训练日志Controller
 * 
 * @author ruoyi
 * @date 2026-04-15
 */
@RestController
@RequestMapping("/lab/training")
public class LabAlgorithmTrainingLogController extends BaseController
{
    @Autowired
    private ILabAlgorithmTrainingLogService labAlgorithmTrainingLogService;

    /**
     * 查询算法训练日志列表
     */
    @PreAuthorize("@ss.hasPermi('lab:training:list')")
    @GetMapping("/list")
    public TableDataInfo list(LabAlgorithmTrainingLog labAlgorithmTrainingLog)
    {
        startPage();
        List<LabAlgorithmTrainingLog> list = labAlgorithmTrainingLogService.selectLabAlgorithmTrainingLogList(labAlgorithmTrainingLog);
        return getDataTable(list);
    }

    /**
     * 导出算法训练日志列表
     */
    @PreAuthorize("@ss.hasPermi('lab:training:export')")
    @Log(title = "算法训练日志", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, LabAlgorithmTrainingLog labAlgorithmTrainingLog)
    {
        List<LabAlgorithmTrainingLog> list = labAlgorithmTrainingLogService.selectLabAlgorithmTrainingLogList(labAlgorithmTrainingLog);
        ExcelUtil<LabAlgorithmTrainingLog> util = new ExcelUtil<LabAlgorithmTrainingLog>(LabAlgorithmTrainingLog.class);
        util.exportExcel(response, list, "算法训练日志数据");
    }

    /**
     * 获取算法训练日志详细信息
     */
    @PreAuthorize("@ss.hasPermi('lab:training:query')")
    @GetMapping(value = "/{trainingId}")
    public AjaxResult getInfo(@PathVariable("trainingId") Long trainingId)
    {
        return success(labAlgorithmTrainingLogService.selectLabAlgorithmTrainingLogByTrainingId(trainingId));
    }

    /**
     * 新增算法训练日志
     */
    @PreAuthorize("@ss.hasPermi('lab:training:add')")
    @Log(title = "算法训练日志", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody LabAlgorithmTrainingLog labAlgorithmTrainingLog)
    {
        return toAjax(labAlgorithmTrainingLogService.insertLabAlgorithmTrainingLog(labAlgorithmTrainingLog));
    }

    /**
     * 修改算法训练日志
     */
    @PreAuthorize("@ss.hasPermi('lab:training:edit')")
    @Log(title = "算法训练日志", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody LabAlgorithmTrainingLog labAlgorithmTrainingLog)
    {
        return toAjax(labAlgorithmTrainingLogService.updateLabAlgorithmTrainingLog(labAlgorithmTrainingLog));
    }

    /**
     * 删除算法训练日志
     */
    @PreAuthorize("@ss.hasPermi('lab:training:remove')")
    @Log(title = "算法训练日志", businessType = BusinessType.DELETE)
	@DeleteMapping("/{trainingIds}")
    public AjaxResult remove(@PathVariable Long[] trainingIds)
    {
        return toAjax(labAlgorithmTrainingLogService.deleteLabAlgorithmTrainingLogByTrainingIds(trainingIds));
    }

    /**
     * 开始训练任务
     */
    @PreAuthorize("@ss.hasPermi('lab:training:start')")
    @Log(title = "开始算法训练", businessType = BusinessType.OTHER)
    @PostMapping("/start")
    public AjaxResult startTraining(@RequestBody LabAlgorithmTrainingLog labAlgorithmTrainingLog)
    {
        Long trainingId = labAlgorithmTrainingLogService.startTraining(labAlgorithmTrainingLog);
        return success(trainingId);
    }

    /**
     * 更新训练进度
     */
    @PreAuthorize("@ss.hasPermi('lab:training:progress')")
    @Log(title = "更新训练进度", businessType = BusinessType.UPDATE)
    @PutMapping("/progress/{trainingId}")
    public AjaxResult updateTrainingProgress(
            @PathVariable Long trainingId,
            @RequestBody java.util.Map<String, Object> params)
    {
        Integer completedEpochs = params.get("completedEpochs") != null ? 
            Integer.valueOf(params.get("completedEpochs").toString()) : null;
        java.math.BigDecimal lossValue = params.get("lossValue") != null ? 
            new java.math.BigDecimal(params.get("lossValue").toString()) : null;
        return toAjax(labAlgorithmTrainingLogService.updateTrainingProgress(trainingId, completedEpochs, lossValue));
    }

    /**
     * 完成训练任务
     */
    @PreAuthorize("@ss.hasPermi('lab:training:complete')")
    @Log(title = "完成算法训练", businessType = BusinessType.UPDATE)
    @PutMapping("/complete/{trainingId}")
    public AjaxResult completeTraining(
            @PathVariable Long trainingId,
            @RequestBody java.util.Map<String, Object> params)
    {
        java.math.BigDecimal lossValue = params.get("lossValue") != null ? 
            new java.math.BigDecimal(params.get("lossValue").toString()) : null;
        java.math.BigDecimal accuracy = params.get("accuracy") != null ? 
            new java.math.BigDecimal(params.get("accuracy").toString()) : null;
        String modelVersion = params.get("modelVersion") != null ? 
            params.get("modelVersion").toString() : null;
        String metrics = params.get("metrics") != null ? 
            params.get("metrics").toString() : null;
        return toAjax(labAlgorithmTrainingLogService.completeTraining(trainingId, lossValue, accuracy, modelVersion, metrics));
    }

    /**
     * 失败训练任务
     */
    @PreAuthorize("@ss.hasPermi('lab:training:fail')")
    @Log(title = "算法训练失败", businessType = BusinessType.UPDATE)
    @PutMapping("/fail/{trainingId}")
    public AjaxResult failTraining(
            @PathVariable Long trainingId,
            @RequestBody java.util.Map<String, Object> params)
    {
        String errorMessage = params.get("errorMessage") != null ? 
            params.get("errorMessage").toString() : null;
        return toAjax(labAlgorithmTrainingLogService.failTraining(trainingId, errorMessage));
    }
}
