package com.ruoyi.project.lab.service;

import java.util.List;
import com.ruoyi.project.lab.domain.LabTimeSlot;

/**
 * 设备可预约时段Service接口
 * 
 * @author asahi
 * @date 2026-04-10
 */
public interface ILabTimeSlotService 
{
    /**
     * 查询设备可预约时段
     * 
     * @param slotId 设备可预约时段主键
     * @return 设备可预约时段
     */
    public LabTimeSlot selectLabTimeSlotBySlotId(Long slotId);

    /**
     * 查询设备可预约时段列表
     * 
     * @param labTimeSlot 设备可预约时段
     * @return 设备可预约时段集合
     */
    public List<LabTimeSlot> selectLabTimeSlotList(LabTimeSlot labTimeSlot);

    /**
     * 新增设备可预约时段
     * 
     * @param labTimeSlot 设备可预约时段
     * @return 结果
     */
    public int insertLabTimeSlot(LabTimeSlot labTimeSlot);

    /**
     * 修改设备可预约时段
     * 
     * @param labTimeSlot 设备可预约时段
     * @return 结果
     */
    public int updateLabTimeSlot(LabTimeSlot labTimeSlot);

    /**
     * 批量删除设备可预约时段
     * 
     * @param slotIds 需要删除的设备可预约时段主键集合
     * @return 结果
     */
    public int deleteLabTimeSlotBySlotIds(Long[] slotIds);

    /**
     * 删除设备可预约时段信息
     * 
     * @param slotId 设备可预约时段主键
     * @return 结果
     */
    public int deleteLabTimeSlotBySlotId(Long slotId);
    
    /**
     * 批量新增设备可预约时段
     * 
     * @param labTimeSlotList 设备可预约时段列表
     * @return 结果
     */
    public int batchInsertLabTimeSlot(List<LabTimeSlot> labTimeSlotList);
}
