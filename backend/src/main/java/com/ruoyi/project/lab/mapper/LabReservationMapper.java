package com.ruoyi.project.lab.mapper;

import java.util.Date;
import java.util.List;
import com.ruoyi.project.lab.domain.LabReservation;

/**
 * 实验室设备预约Mapper接口
 *
 * @author asahi
 * @date 2026-04-10
 */
public interface LabReservationMapper
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
     * 获取当前用户的预约列表
     *
     * @param labReservation 预约信息
     * @return 当前用户的预约列表
     */
    public List<LabReservation> selectMyReservationList(LabReservation labReservation);

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
     * 删除实验室设备预约
     *
     * @param reserveId 实验室设备预约主键
     * @return 结果
     */
    public int deleteLabReservationByReserveId(Long reserveId);

    /**
     * 批量删除实验室设备预约
     *
     * @param reserveIds 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteLabReservationByReserveIds(Long[] reserveIds);

    /**
     * 查询冲突的预约记录
     *
     * @param deviceId 设备ID
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param excludeReserveId 排除的预约ID（编辑时使用）
     * @return 冲突的预约列表
     */
    public List<LabReservation> selectConflictingReservations(Long deviceId, Date startTime, Date endTime, Long excludeReserveId);
}
