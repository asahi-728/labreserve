package com.ruoyi.project.lab.mapper;

import java.util.List;
import com.ruoyi.project.lab.domain.LabDevice;

/**
 * 实验室设备Mapper接口
 * 
 * @author asahi
 * @date 2026-04-10
 */
public interface LabDeviceMapper 
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
     * 删除实验室设备
     * 
     * @param deviceId 实验室设备主键
     * @return 结果
     */
    public int deleteLabDeviceByDeviceId(Long deviceId);

    /**
     * 批量删除实验室设备
     * 
     * @param deviceIds 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteLabDeviceByDeviceIds(Long[] deviceIds);
}
