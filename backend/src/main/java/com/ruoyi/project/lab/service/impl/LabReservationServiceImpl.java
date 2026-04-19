package com.ruoyi.project.lab.service.impl;

import java.util.*;
import java.text.SimpleDateFormat;
import com.ruoyi.project.lab.domain.LabTimeSlot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.project.lab.mapper.LabReservationMapper;
import com.ruoyi.project.lab.domain.LabReservation;
import com.ruoyi.project.lab.service.ILabReservationService;
import com.ruoyi.project.lab.service.ILabReservationRuleService;
import com.ruoyi.project.lab.service.PpoAutoAuditService;

/**
 * 实验室设备预约Service业务层处理
 *
 * @author asahi
 * @date 2026-04-10
 */
@Service
public class LabReservationServiceImpl implements ILabReservationService
{
    private static final Logger log = LoggerFactory.getLogger(LabReservationServiceImpl.class);

    @Autowired
    private LabReservationMapper labReservationMapper;

    @Autowired
    private PpoAutoAuditService ppoAutoAuditService;

    @Autowired
    private ILabReservationRuleService labReservationRuleService;

    @Autowired
    private com.ruoyi.project.lab.service.ILabDeviceService labDeviceService;
    
    @Autowired
    private com.ruoyi.project.lab.service.ILabTimeSlotService labTimeSlotService;

    @Autowired
    private com.ruoyi.project.lab.service.ILabUserCreditService labUserCreditService;

    /**
     * 查询实验室设备预约
     *
     * @param reserveId 实验室设备预约主键
     * @return 实验室设备预约
     */
    @Override
    public LabReservation selectLabReservationByReserveId(Long reserveId)
    {
        return labReservationMapper.selectLabReservationByReserveId(reserveId);
    }

    /**
     * 查询实验室设备预约列表
     *
     * @param labReservation 实验室设备预约
     * @return 实验室设备预约
     */
    @Override
    public List<LabReservation> selectLabReservationList(LabReservation labReservation)
    {
        return labReservationMapper.selectLabReservationList(labReservation);
    }

    /**
     * 获取当前用户的预约列表
     *
     * @param labReservation 预约信息
     * @return 当前用户的预约列表
     */
    @Override
    public List<LabReservation> selectMyReservationList(LabReservation labReservation)
    {
        return labReservationMapper.selectMyReservationList(labReservation);
    }

    /**
     * 新增实验室设备预约
     *
     * @param labReservation 实验室设备预约
     * @return 结果
     */
    @Override
    public int insertLabReservation(LabReservation labReservation)
    {
        labReservation.setCreateTime(DateUtils.getNowDate());
        return labReservationMapper.insertLabReservation(labReservation);
    }

    /**
     * 修改实验室设备预约
     *
     * @param labReservation 实验室设备预约
     * @return 结果
     */
    @Override
    @Transactional
    public int updateLabReservation(LabReservation labReservation)
    {
        labReservation.setUpdateTime(DateUtils.getNowDate());
        return labReservationMapper.updateLabReservation(labReservation);
    }

    /**
     * 批量删除实验室设备预约
     *
     * @param reserveIds 需要删除的实验室设备预约主键集合
     * @return 结果
     */
    @Override
    @Transactional
    public int deleteLabReservationByReserveIds(Long[] reserveIds)
    {
        return labReservationMapper.deleteLabReservationByReserveIds(reserveIds);
    }

    /**
     * 删除实验室设备预约信息
     *
     * @param reserveId 实验室设备预约主键
     * @return 结果
     */
    @Override
    @Transactional
    public int deleteLabReservationByReserveId(Long reserveId)
    {
        return labReservationMapper.deleteLabReservationByReserveId(reserveId);
    }

    /**
     * 提交预约申请（含冲突检测和PPO自动审批）
     *
     * @param labReservation 预约信息
     * @return 结果
     */
    @Override
    public int submitReservation(LabReservation labReservation)
    {
        if (checkConflict(labReservation)) {
            throw new RuntimeException("预约时段存在冲突，请选择其他时段");
        }

        labReservation.setStatus("0");
        labReservation.setCreateTime(DateUtils.getNowDate());
        labReservation.setCreateBy(SecurityUtils.getUsername());

        LabReservation savedReservation = saveReservation(labReservation);

        if (savedReservation.getReserveId() == null)
        {
            log.error("预约保存后 reserveId 为 null，跳过自动审批");
            return 1;
        }

        // 根据前端提交的审批模式决定是否自动调用 PPO
        String approvalMode = labReservation.getApprovalMode();
        log.info("当前审批模式: {}", approvalMode);

        try
        {
            // 只有在 auto 模式下才自动调用 PPO 审批，manual 模式保持待审核
            if (!"auto".equals(approvalMode))
            {
                log.info("当前为人工审批模式，跳过PPO自动审批，预约保持待审核状态: reserveId={}", savedReservation.getReserveId());
                return 1;
            }

            Boolean auditResult = ppoAutoAuditService.autoAuditReservation(savedReservation);

            if (auditResult != null)
            {
                String newStatus = auditResult ? "1" : "2";
                log.info("准备更新预约状态: reserveId={}, oldStatus={}, newStatus={}",
                        savedReservation.getReserveId(),
                        savedReservation.getStatus(),
                        newStatus);

                LabReservation checkReservation = labReservationMapper.selectLabReservationByReserveId(savedReservation.getReserveId());
                if (checkReservation == null)
                {
                    log.error("预约不存在，无法更新: reserveId={}", savedReservation.getReserveId());
                }
                else
                {
                    log.info("找到预约: reserveId={}, currentStatus={}",
                            checkReservation.getReserveId(),
                            checkReservation.getStatus());

                    LabReservation updateReservation = new LabReservation();
                    updateReservation.setReserveId(savedReservation.getReserveId());
                    updateReservation.setStatus(newStatus);
                    updateReservation.setAuditBy("PPO_ALGORITHM");
                    updateReservation.setAuditTime(DateUtils.getNowDate());
                    updateReservation.setSchedulerType("1");

                    log.info("执行更新: reserveId={}, status={}, auditBy={}, schedulerType={}",
                            updateReservation.getReserveId(),
                            updateReservation.getStatus(),
                            updateReservation.getAuditBy(),
                            updateReservation.getSchedulerType());

                    int updateCount = labReservationMapper.updateLabReservation(updateReservation);
                    log.info("更新结果: {} 行受影响", updateCount);

                    LabReservation afterUpdate = labReservationMapper.selectLabReservationByReserveId(savedReservation.getReserveId());
                    if (afterUpdate != null)
                    {
                        log.info("更新后状态: reserveId={}, status={}, auditBy={}, schedulerType={}",
                                afterUpdate.getReserveId(),
                                afterUpdate.getStatus(),
                                afterUpdate.getAuditBy(),
                                afterUpdate.getSchedulerType());
                        
                        // 如果PPO自动批准，更新设备时段状态为已预约
                        if (auditResult && "1".equals(afterUpdate.getStatus()))
                        {
                            updateTimeSlotStatus(afterUpdate.getDeviceId(), 
                                                afterUpdate.getStartTime(), 
                                                afterUpdate.getEndTime(), 
                                                "1"); // 1 表示已预约
                        }
                    }

                    log.info("PPO算法自动{}预约: reserveId={}",
                            auditResult ? "批准" : "拒绝",
                            savedReservation.getReserveId());
                }
            }
            else
            {
                log.info("PPO算法未给出明确审批结果，保持待审核状态: reserveId={}", savedReservation.getReserveId());
            }
        }
        catch (Exception e)
        {
            log.error("PPO自动审批过程出错，保持待审核状态: reserveId={}", savedReservation.getReserveId(), e);
        }

        return 1;
    }

    /**
     * 审核预约
     *
     * @param labReservation 预约信息
     * @return 结果
     */
    @Override
    @Transactional
    public int auditReservation(LabReservation labReservation)
    {
        labReservation.setAuditBy(SecurityUtils.getUsername());
        labReservation.setAuditTime(DateUtils.getNowDate());
        
        int result = labReservationMapper.updateLabReservation(labReservation);
        
        // 如果审批通过，更新设备时段状态为已预约
        if (result > 0 && "1".equals(labReservation.getStatus()))
        {
            LabReservation updatedReservation = selectLabReservationByReserveId(labReservation.getReserveId());
            if (updatedReservation != null)
            {
                updateTimeSlotStatus(updatedReservation.getDeviceId(), 
                                    updatedReservation.getStartTime(), 
                                    updatedReservation.getEndTime(), 
                                    "1"); // 1 表示已预约
            }
        }
        
        return result;
    }

    /**
     * 取消预约
     *
     * @param reserveId 预约ID
     * @return 结果
     */
    @Override
    @Transactional
    public int cancelReservation(Long reserveId)
    {
        LabReservation reservation = selectLabReservationByReserveId(reserveId);
        if (reservation == null)
        {
            throw new RuntimeException("预约记录不存在");
        }

        if (!"0".equals(reservation.getStatus()) && !"1".equals(reservation.getStatus()))
        {
            throw new RuntimeException("只能取消待审核或已通过的预约");
        }

        Date now = new Date();
        Date startTime = reservation.getStartTime();

        // 对于待审核的预约，直接取消，不做时间限制
        if ("1".equals(reservation.getStatus()))
        {
            Map<String, String> rules = labReservationRuleService.selectAllRulesAsMap();
            Integer cancelBeforeHours = null;
            try
            {
                String cancelBeforeHoursStr = rules.get("cancel_before_hours");
                if (cancelBeforeHoursStr != null && !cancelBeforeHoursStr.isEmpty())
                {
                    cancelBeforeHours = Integer.parseInt(cancelBeforeHoursStr);
                }
            }
            catch (NumberFormatException e)
            {
                log.warn("取消预约提前时间配置无效，忽略该规则");
            }

            // 只有当规则存在且有效时，才进行时间限制检查
            if (cancelBeforeHours != null)
            {
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(startTime);
                calendar.add(Calendar.HOUR_OF_DAY, -cancelBeforeHours);

                if (now.after(calendar.getTime()))
                {
                    throw new RuntimeException("取消预约只能在开始时间前" + cancelBeforeHours + "小时内操作");
                }
            }
        }

        LabReservation updateReservation = new LabReservation();
        updateReservation.setReserveId(reserveId);
        updateReservation.setStatus("5");
        updateReservation.setCancelTime(DateUtils.getNowDate());
        updateReservation.setUpdateBy(SecurityUtils.getUsername());
        updateReservation.setUpdateTime(DateUtils.getNowDate());
        
        int result = labReservationMapper.updateLabReservation(updateReservation);

        // 取消预约后，将时段状态改回可预约
        if (result > 0)
        {
            updateTimeSlotStatus(reservation.getDeviceId(),
                                reservation.getStartTime(),
                                reservation.getEndTime(),
                                "0"); // 0 表示可预约
        }

        // 更新用户信用分：取消已批准-5，取消待审核-1
        if (result > 0)
        {
            try
            {
                if ("1".equals(reservation.getStatus()))
                {
                    labUserCreditService.updateCreditCancel(reservation.getUserId(), reserveId);
                    log.info("预约 {} 已取消（原状态=已批准），用户 {} 信用分 -5", reserveId, reservation.getUserId());
                }
                else if ("0".equals(reservation.getStatus()))
                {
                    labUserCreditService.updateCreditByDelta(reservation.getUserId(), -1L, reserveId);
                    log.info("预约 {} 已取消（原状态=待审核），用户 {} 信用分 -1", reserveId, reservation.getUserId());
                }
            }
            catch (Exception e)
            {
                log.warn("预约 {} 取消时更新信用分失败（不影响主流程）: {}", reserveId, e.getMessage());
            }
        }

        return result;
    }

    /**
     * 完成使用
     *
     * @param reserveId 预约ID
     * @return 结果
     */
    @Override
    @Transactional
    public int completeReservation(Long reserveId)
    {
        LabReservation reservation = selectLabReservationByReserveId(reserveId);
        if (reservation == null)
        {
            throw new RuntimeException("预约记录不存在");
        }

        if (!"6".equals(reservation.getStatus()))
        {
            throw new RuntimeException("只能完成使用中的预约");
        }

        Date now = new Date();
        Date startTime = reservation.getStartTime();
        Date endTime = reservation.getEndTime();

        if (now.before(startTime))
        {
            throw new RuntimeException("设备使用尚未开始，无法完成使用");
        }

        Map<String, String> rules = labReservationRuleService.selectAllRulesAsMap();
        Integer graceHours = null;
        try
        {
            String graceHoursStr = rules.get("complete_grace_hours");
            if (graceHoursStr != null && !graceHoursStr.isEmpty())
            {
                graceHours = Integer.parseInt(graceHoursStr);
            }
        }
        catch (NumberFormatException e)
        {
            log.warn("完成使用宽限期配置无效，忽略该规则");
        }

        Calendar graceCalendar = Calendar.getInstance();
        graceCalendar.setTime(endTime);
        if (graceHours != null)
        {
            graceCalendar.add(Calendar.HOUR_OF_DAY, graceHours);
        }

        String newStatus;
        if (graceHours != null && now.after(graceCalendar.getTime()))
        {
            newStatus = "4";
            log.info("预约 {} 已超时，标记为违约", reserveId);
        }
        else
        {
            newStatus = "3";
            log.info("预约 {} 按时完成使用", reserveId);
        }

        LabReservation updateReservation = new LabReservation();
        updateReservation.setReserveId(reserveId);
        updateReservation.setStatus(newStatus);
        updateReservation.setCompleteTime(DateUtils.getNowDate());
        updateReservation.setUpdateBy(SecurityUtils.getUsername());
        updateReservation.setUpdateTime(DateUtils.getNowDate());
        int result = labReservationMapper.updateLabReservation(updateReservation);

        if ("3".equals(newStatus))
        {
            labDeviceService.updateDeviceStatus(reservation.getDeviceId(), "0");
            log.info("预约 {} 完成使用，设备 {} 状态已设置为空闲", reserveId, reservation.getDeviceId());
        }

        // 更新用户信用分：按时完成+2，违约-2
        try
        {
            Long userId = reservation.getUserId();
            if ("4".equals(newStatus))
            {
                labUserCreditService.updateCreditViolation(userId, reserveId);
                log.info("预约 {} 违约，用户 {} 信用分 -2", reserveId, userId);
            }
            else
            {
                labUserCreditService.updateCreditOnTime(userId, reserveId);
                log.info("预约 {} 按时完成，用户 {} 信用分 +2", reserveId, userId);
            }
        }
        catch (Exception e)
        {
            log.warn("预约 {} 更新信用分失败（不影响主流程）: {}", reserveId, e.getMessage());
        }

        return result;
    }

    /**
     * 开始使用设备
     *
     * @param reserveId 预约ID
     * @return 结果
     */
    @Override
    @Transactional
    public int startUsing(Long reserveId)
    {
        LabReservation reservation = selectLabReservationByReserveId(reserveId);
        if (reservation == null)
        {
            throw new RuntimeException("预约记录不存在");
        }

        if (!"1".equals(reservation.getStatus()))
        {
            throw new RuntimeException("只能开始使用已通过的预约");
        }

        Date now = new Date();
        Date startTime = reservation.getStartTime();

        Map<String, String> rules = labReservationRuleService.selectAllRulesAsMap();
        Integer beforeMinutes = null;
        try
        {
            String beforeMinutesStr = rules.get("start_use_before_minutes");
            if (beforeMinutesStr != null && !beforeMinutesStr.isEmpty())
            {
                beforeMinutes = Integer.parseInt(beforeMinutesStr);
            }
        }
        catch (NumberFormatException e)
        {
            log.warn("开始使用提前时间配置无效，忽略该规则");
        }

        // 只有当规则存在且有效时，才进行时间限制检查
        if (beforeMinutes != null)
        {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(startTime);
            calendar.add(Calendar.MINUTE, -beforeMinutes);

            if (now.before(calendar.getTime()))
            {
                throw new RuntimeException("开始使用只能在预约时间前" + beforeMinutes + "分钟内操作");
            }
        }

        LabReservation updateReservation = new LabReservation();
        updateReservation.setReserveId(reserveId);
        updateReservation.setStatus("6");
        updateReservation.setUpdateBy(SecurityUtils.getUsername());
        updateReservation.setUpdateTime(DateUtils.getNowDate());
        int result = labReservationMapper.updateLabReservation(updateReservation);

        labDeviceService.updateDeviceStatus(reservation.getDeviceId(), "1");
        log.info("预约 {} 开始使用，设备 {} 状态已设置为占用", reserveId, reservation.getDeviceId());

        return result;
    }

    /**
     * 更新预约时段状态
     *
     * @param deviceId 设备ID
     * @param startTime 开始时间
     * @param endTime 结束时间
     */
    private void updateTimeSlotStatus(Long deviceId, Date startTime, Date endTime, String status)
    {
        try
        {
            // 解析开始时间和结束时间，获取日期和时间段
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
            SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm");
            
            // 提取日期部分作为 Date 类型
            String dateStr = dateFormat.format(startTime);
            Date slotDate = dateFormat.parse(dateStr);
            String startTimeStr = timeFormat.format(startTime);
            String endTimeStr = timeFormat.format(endTime);
            
            // 查询对应的时段
            LabTimeSlot slot = new LabTimeSlot();
            slot.setDeviceId(deviceId);
            slot.setSlotDate(slotDate);
            slot.setStartTime(startTimeStr);
            slot.setEndTime(endTimeStr);
            
            List<LabTimeSlot> slotList = labTimeSlotService.selectLabTimeSlotList(slot);
            
            if (!slotList.isEmpty())
            {
                for (LabTimeSlot timeSlot : slotList)
                {
                    timeSlot.setIsAvailable(status);
                    timeSlot.setUpdateBy(SecurityUtils.getUsername());
                    timeSlot.setUpdateTime(DateUtils.getNowDate());
                    labTimeSlotService.updateLabTimeSlot(timeSlot);
                    log.info("更新设备时段状态: 设备ID={}, 日期={}, 时段={}-{}, 状态={}", 
                            deviceId, dateStr, startTimeStr, endTimeStr, status);
                }
            }
        }
        catch (Exception e)
        {
            log.error("更新预约时段状态失败", e);
        }
    }

    /**
     * 检测预约冲突
     *
     * @param labReservation 预约信息
     * @return 是否存在冲突
     */
    @Override
    public boolean checkConflict(LabReservation labReservation)
    {
        List<LabReservation> existingReservations = labReservationMapper.selectConflictingReservations(
            labReservation.getDeviceId(),
            labReservation.getStartTime(),
            labReservation.getEndTime(),
            labReservation.getReserveId()
        );
        return !existingReservations.isEmpty();
    }

    /**
     * 保存预约（独立事务，确保立即提交）
     *
     * @param labReservation 预约信息
     * @return 保存后的预约（包含reserveId）
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public LabReservation saveReservation(LabReservation labReservation)
    {
        log.info("保存预约（独立事务）: userId={}, deviceId={}",
                labReservation.getUserId(),
                labReservation.getDeviceId());

        labReservationMapper.insertLabReservation(labReservation);

        log.info("预约保存完成（独立事务）, reserveId={}", labReservation.getReserveId());

        return labReservation;
    }

    /**
     * PPO批量自动审批预约
     * 调用PPO算法服务对选中的待审批预约进行批量自动审批
     *
     * @param reserveIds 预约ID列表
     * @return 批量审批结果
     */
    @Override
    public List<Map<String, Object>> ppoBatchAudit(Long[] reserveIds)
    {
        log.info("========== PPO批量审批开始，共 {} 条预约 ==========", reserveIds.length);
        List<Map<String, Object>> results = new ArrayList<>();

        try
        {
            // 1. 调用 PPO 算法服务进行批量调度
            List<Long> reserveIdList = java.util.Arrays.asList(reserveIds);
            List<Map<String, Object>> dispatchResults = ppoAutoAuditService.batchAuditReservations(reserveIdList);

            // 2. 根据调度结果逐条更新数据库
            for (Map<String, Object> dispatchResult : dispatchResults)
            {
                Map<String, Object> itemResult = new HashMap<>();
                Object reserveIdObj = dispatchResult.get("reserve_id");
                Long reserveId = reserveIdObj instanceof Number ? ((Number) reserveIdObj).longValue() : null;

                if (reserveId == null)
                {
                    reserveId = reserveIdObj instanceof Number ? ((Number) dispatchResult.get("reserveId")).longValue() : null;
                }

                itemResult.put("reserveId", reserveId);
                String status = String.valueOf(dispatchResult.get("status"));
                String reason = String.valueOf(dispatchResult.getOrDefault("reason", ""));
                Object confidenceObj = dispatchResult.get("confidence");

                log.info("处理预约 {} 的PPO审批结果: status={}, reason={}", reserveId, status, reason);

                if ("approved".equals(status))
                {
                    // PPO 批准
                    LabReservation updateRes = new LabReservation();
                    updateRes.setReserveId(reserveId);
                    updateRes.setStatus("1"); // 已批准
                    updateRes.setAuditBy("PPO_ALGORITHM");
                    updateRes.setAuditTime(DateUtils.getNowDate());
                    updateRes.setAuditRemark("PPO算法自动批准: " + reason);
                    updateRes.setSchedulerType("1");

                    int updateResult = labReservationMapper.updateLabReservation(updateRes);
                    if (updateResult > 0)
                    {
                        // 更新时段状态
                        LabReservation updated = selectLabReservationByReserveId(reserveId);
                        if (updated != null)
                        {
                            updateTimeSlotStatus(updated.getDeviceId(), updated.getStartTime(), updated.getEndTime(), "1");
                        }
                    }

                    itemResult.put("status", "approved");
                    itemResult.put("message", "PPO算法已批准");
                    itemResult.put("confidence", confidenceObj);
                }
                else if ("rejected".equals(status))
                {
                    // PPO 拒绝
                    LabReservation updateRes = new LabReservation();
                    updateRes.setReserveId(reserveId);
                    updateRes.setStatus("2"); // 已拒绝
                    updateRes.setAuditBy("PPO_ALGORITHM");
                    updateRes.setAuditTime(DateUtils.getNowDate());
                    updateRes.setAuditRemark("PPO算法自动拒绝: " + reason);
                    updateRes.setSchedulerType("1");

                    labReservationMapper.updateLabReservation(updateRes);

                    itemResult.put("status", "rejected");
                    itemResult.put("message", "PPO算法已拒绝");
                    itemResult.put("confidence", confidenceObj);
                }
                else if ("error".equals(status))
                {
                    itemResult.put("status", "error");
                    itemResult.put("message", reason != null ? reason : "审批异常");
                }
                else
                {
                    itemResult.put("status", "pending");
                    itemResult.put("message", "PPO算法未给出明确结果，保持待审核");
                }

                results.add(itemResult);
            }
        }
        catch (Exception e)
        {
            log.error("PPO批量审批失败", e);
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("status", "error");
            errorResult.put("message", "批量审批异常: " + e.getMessage());
            results.add(errorResult);
        }

        log.info("========== PPO批量审批结束，共处理 {} 条 ==========", results.size());
        return results;
    }
}
