package com.ruoyi.project.lab.service;

import java.util.List;
import com.ruoyi.project.lab.domain.LabDevice;

/**
 * 实验室设备Service接口
 * 
 * @author asahi
 * @date 2026-04-10
 */
public interface ILabDeviceService 
{
    /**
     * 查询实验室设备
     * 
     * @param deviceId 实验室设备主键
     * @return 实验室设备
     */
    public LabDevice selectLabDeviceByDeviceId(Long deviceId);

    /**
     * 查询实验室设备列表
     * 
     * @param labDevice 实验室设备
     * @return 实验室设备集合
     */
    public List<LabDevice> selectLabDeviceList(LabDevice labDevice);

    /**
     * 新增实验室设备
     * 
     * @param labDevice 实验室设备
     * @return 结果
     */
    public int insertLabDevice(LabDevice labDevice);

    /**
     * 修改实验室设备
     * 
     * @param labDevice 实验室设备
     * @return 结果
     */
    public int updateLabDevice(LabDevice labDevice);

    /**
     * 批量删除实验室设备
     * 
     * @param deviceIds 需要删除的实验室设备主键集合
     * @return 结果
     */
    public int deleteLabDeviceByDeviceIds(Long[] deviceIds);

    /**
     * 删除实验室设备信息
     *
     * @param deviceId 实验室设备主键
     * @return 结果
     */
    public int deleteLabDeviceByDeviceId(Long deviceId);

    /**
     * 更新设备状态
     *
     * @param deviceId 设备ID
     * @param status 设备状态
     * @return 结果
     */
    public int updateDeviceStatus(Long deviceId, String status);
}
