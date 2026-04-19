package com.ruoyi.project.lab.service.impl;

import java.util.List;
import com.ruoyi.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.project.lab.mapper.LabDeviceMapper;
import com.ruoyi.project.lab.domain.LabDevice;
import com.ruoyi.project.lab.service.ILabDeviceService;

/**
 * 实验室设备Service业务层处理
 * 
 * @author asahi
 * @date 2026-04-10
 */
@Service
public class LabDeviceServiceImpl implements ILabDeviceService 
{
    @Autowired
    private LabDeviceMapper labDeviceMapper;

    /**
     * 查询实验室设备
     * 
     * @param deviceId 实验室设备主键
     * @return 实验室设备
     */
    @Override
    public LabDevice selectLabDeviceByDeviceId(Long deviceId)
    {
        return labDeviceMapper.selectLabDeviceByDeviceId(deviceId);
    }

    /**
     * 查询实验室设备列表
     * 
     * @param labDevice 实验室设备
     * @return 实验室设备
     */
    @Override
    public List<LabDevice> selectLabDeviceList(LabDevice labDevice)
    {
        return labDeviceMapper.selectLabDeviceList(labDevice);
    }

    /**
     * 新增实验室设备
     * 
     * @param labDevice 实验室设备
     * @return 结果
     */
    @Override
    public int insertLabDevice(LabDevice labDevice)
    {
        labDevice.setCreateTime(DateUtils.getNowDate());
        return labDeviceMapper.insertLabDevice(labDevice);
    }

    /**
     * 修改实验室设备
     * 
     * @param labDevice 实验室设备
     * @return 结果
     */
    @Override
    public int updateLabDevice(LabDevice labDevice)
    {
        labDevice.setUpdateTime(DateUtils.getNowDate());
        return labDeviceMapper.updateLabDevice(labDevice);
    }

    /**
     * 批量删除实验室设备
     * 
     * @param deviceIds 需要删除的实验室设备主键
     * @return 结果
     */
    @Override
    public int deleteLabDeviceByDeviceIds(Long[] deviceIds)
    {
        return labDeviceMapper.deleteLabDeviceByDeviceIds(deviceIds);
    }

    /**
     * 删除实验室设备信息
     *
     * @param deviceId 实验室设备主键
     * @return 结果
     */
    @Override
    public int deleteLabDeviceByDeviceId(Long deviceId)
    {
        return labDeviceMapper.deleteLabDeviceByDeviceId(deviceId);
    }

    /**
     * 更新设备状态
     *
     * @param deviceId 设备ID
     * @param status 设备状态
     * @return 结果
     */
    @Override
    public int updateDeviceStatus(Long deviceId, String status)
    {
        LabDevice labDevice = new LabDevice();
        labDevice.setDeviceId(deviceId);
        labDevice.setStatus(status);
        labDevice.setUpdateTime(DateUtils.getNowDate());
        return labDeviceMapper.updateLabDevice(labDevice);
    }
}
