package com.ruoyi.project.lab.service;

import java.util.List;
import com.ruoyi.project.lab.domain.LabDeviceCategory;
import com.ruoyi.framework.web.domain.TreeSelect;

/**
 * 设备分类Service接口
 * 
 * @author asahi
 * @date 2026-04-10
 */
public interface ILabDeviceCategoryService 
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
     * 构建前端所需要树结构
     * 
     * @param categories 分类列表
     * @return 树结构列表
     */
    public List<LabDeviceCategory> buildCategoryTree(List<LabDeviceCategory> categories);

    /**
     * 构建前端所需要下拉树结构
     * 
     * @param categories 分类列表
     * @return 下拉树结构列表
     */
    public List<TreeSelect> buildCategoryTreeSelect(List<LabDeviceCategory> categories);

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
     * 批量删除设备分类
     * 
     * @param categoryIds 需要删除的设备分类主键集合
     * @return 结果
     */
    public int deleteLabDeviceCategoryByCategoryIds(Long[] categoryIds);

    /**
     * 删除设备分类信息
     * 
     * @param categoryId 设备分类主键
     * @return 结果
     */
    public int deleteLabDeviceCategoryByCategoryId(Long categoryId);
}
