package com.ruoyi.project.lab.mapper;

import java.util.List;
import com.ruoyi.project.lab.domain.LabDeviceCategory;

/**
 * 设备分类Mapper接口
 * 
 * @author asahi
 * @date 2026-04-10
 */
public interface LabDeviceCategoryMapper 
{
    /**
     * 查询设备分类
     * 
     * @param categoryId 设备分类主键
     * @return 设备分类
     */
    public LabDeviceCategory selectLabDeviceCategoryByCategoryId(Long categoryId);

    /**
     * 查询设备分类列表
     * 
     * @param labDeviceCategory 设备分类
     * @return 设备分类集合
     */
    public List<LabDeviceCategory> selectLabDeviceCategoryList(LabDeviceCategory labDeviceCategory);

    /**
     * 新增设备分类
     * 
     * @param labDeviceCategory 设备分类
     * @return 结果
     */
    public int insertLabDeviceCategory(LabDeviceCategory labDeviceCategory);

    /**
     * 修改设备分类
     * 
     * @param labDeviceCategory 设备分类
     * @return 结果
     */
    public int updateLabDeviceCategory(LabDeviceCategory labDeviceCategory);

    /**
     * 删除设备分类
     * 
     * @param categoryId 设备分类主键
     * @return 结果
     */
    public int deleteLabDeviceCategoryByCategoryId(Long categoryId);

    /**
     * 批量删除设备分类
     * 
     * @param categoryIds 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteLabDeviceCategoryByCategoryIds(Long[] categoryIds);
}
