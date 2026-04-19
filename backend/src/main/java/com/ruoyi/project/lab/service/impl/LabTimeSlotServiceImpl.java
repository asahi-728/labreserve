package com.ruoyi.project.lab.service.impl;

import java.util.List;
import com.ruoyi.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.project.lab.mapper.LabTimeSlotMapper;
import com.ruoyi.project.lab.domain.LabTimeSlot;
import com.ruoyi.project.lab.service.ILabTimeSlotService;

/**
 * 设备可预约时段Service业务层处理
 * 
 * @author asahi
 * @date 2026-04-10
 */
@Service
public class LabTimeSlotServiceImpl implements ILabTimeSlotService 
{
    @Autowired
    private LabTimeSlotMapper labTimeSlotMapper;

    /**
     * 查询设备可预约时段
     * 
     * @param slotId 设备可预约时段主键
     * @return 设备可预约时段
     */
    @Override
    public LabTimeSlot selectLabTimeSlotBySlotId(Long slotId)
    {
        return labTimeSlotMapper.selectLabTimeSlotBySlotId(slotId);
    }

    /**
     * 查询设备可预约时段列表
     * 
     * @param labTimeSlot 设备可预约时段
     * @return 设备可预约时段
     */
    @Override
    public List<LabTimeSlot> selectLabTimeSlotList(LabTimeSlot labTimeSlot)
    {
        return labTimeSlotMapper.selectLabTimeSlotList(labTimeSlot);
    }

    /**
     * 新增设备可预约时段
     * 
     * @param labTimeSlot 设备可预约时段
     * @return 结果
     */
    @Override
    public int insertLabTimeSlot(LabTimeSlot labTimeSlot)
    {
        labTimeSlot.setCreateTime(DateUtils.getNowDate());
        return labTimeSlotMapper.insertLabTimeSlot(labTimeSlot);
    }

    /**
     * 修改设备可预约时段
     * 
     * @param labTimeSlot 设备可预约时段
     * @return 结果
     */
    @Override
    public int updateLabTimeSlot(LabTimeSlot labTimeSlot)
    {
        return labTimeSlotMapper.updateLabTimeSlot(labTimeSlot);
    }

    /**
     * 批量删除设备可预约时段
     * 
     * @param slotIds 需要删除的设备可预约时段主键
     * @return 结果
     */
    @Override
    public int deleteLabTimeSlotBySlotIds(Long[] slotIds)
    {
        return labTimeSlotMapper.deleteLabTimeSlotBySlotIds(slotIds);
    }

    /**
     * 删除设备可预约时段信息
     * 
     * @param slotId 设备可预约时段主键
     * @return 结果
     */
    @Override
    public int deleteLabTimeSlotBySlotId(Long slotId)
    {
        return labTimeSlotMapper.deleteLabTimeSlotBySlotId(slotId);
    }
    
    /**
     * 批量新增设备可预约时段
     * 
     * @param labTimeSlotList 设备可预约时段列表
     * @return 结果
     */
    @Override
    public int batchInsertLabTimeSlot(List<LabTimeSlot> labTimeSlotList)
    {
        if (labTimeSlotList == null || labTimeSlotList.isEmpty()) {
            return 0;
        }
        for (LabTimeSlot slot : labTimeSlotList) {
            slot.setCreateTime(DateUtils.getNowDate());
        }
        return labTimeSlotMapper.batchInsertLabTimeSlot(labTimeSlotList);
    }
}
