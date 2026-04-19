package com.ruoyi.project.lab.service;

import java.util.List;
import java.util.Map;
import com.ruoyi.project.lab.domain.LabReservation;

/**
 * 实验室设备预约Service接口
 *
 * @author asahi
 * @date 2026-04-10
 */
public interface ILabReservationService
{
    /**
     * 查询实验室设备预约
     *
     * @param reserveId 实验室设备预约主键
     * @return 实验室设备预约
     */
    public LabReservation selectLabReservationByReserveId(Long reserveId);

    /**
     * 查询实验室设备预约列表
     *
     * @param labReservation 实验室设备预约
     * @return 实验室设备预约集合
     */
    public List<LabReservation> selectLabReservationList(LabReservation labReservation);

    /**
     * 新增实验室设备预约
     *
     * @param labReservation 实验室设备预约
     * @return 结果
     */
    public int insertLabReservation(LabReservation labReservation);

    /**
     * 修改实验室设备预约
     *
     * @param labReservation 实验室设备预约
     * @return 结果
     */
    public int updateLabReservation(LabReservation labReservation);

    /**
     * 批量删除实验室设备预约
     *
     * @param reserveIds 需要删除的实验室设备预约主键集合
     * @return 结果
     */
    public int deleteLabReservationByReserveIds(Long[] reserveIds);

    /**
     * 删除实验室设备预约信息
     *
     * @param reserveId 实验室设备预约主键
     * @return 结果
     */
    public int deleteLabReservationByReserveId(Long reserveId);

    /**
     * 提交预约申请（含冲突检测）
     *
     * @param labReservation 预约信息
     * @return 结果
     */
    public int submitReservation(LabReservation labReservation);

    /**
     * 审核预约
     *
     * @param labReservation 预约信息
     * @return 结果
     */
    public int auditReservation(LabReservation labReservation);

    /**
     * 取消预约
     *
     * @param reserveId 预约ID
     * @return 结果
     */
    public int cancelReservation(Long reserveId);

    /**
     * 完成使用
     *
     * @param reserveId 预约ID
     * @return 结果
     */
    public int completeReservation(Long reserveId);

    /**
     * 开始使用设备
     *
     * @param reserveId 预约ID
     * @return 结果
     */
    public int startUsing(Long reserveId);

    /**
     * 检测预约冲突
     *
     * @param labReservation 预约信息
     * @return 是否存在冲突
     */
    public boolean checkConflict(LabReservation labReservation);

    /**
     * 保存预约（独立事务，确保立即提交）
     *
     * @param labReservation 预约信息
     * @return 保存后的预约（包含reserveId）
     */
    public LabReservation saveReservation(LabReservation labReservation);

    /**
     * 获取当前用户的预约列表
     *
     * @param labReservation 预约信息
     * @return 当前用户的预约列表
     */
    public List<LabReservation> selectMyReservationList(LabReservation labReservation);

    /**
     * PPO批量自动审批预约
     *
     * @param reserveIds 预约ID列表
     * @return 批量审批结果
     */
    public List<Map<String, Object>> ppoBatchAudit(Long[] reserveIds);
}
