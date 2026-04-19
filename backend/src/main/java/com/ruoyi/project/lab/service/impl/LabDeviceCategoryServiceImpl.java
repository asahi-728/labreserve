package com.ruoyi.project.lab.service.impl;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.framework.web.domain.TreeSelect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.project.lab.mapper.LabDeviceCategoryMapper;
import com.ruoyi.project.lab.domain.LabDeviceCategory;
import com.ruoyi.project.lab.service.ILabDeviceCategoryService;

/**
 * 设备分类Service业务层处理
 * 
 * @author asahi
 * @date 2026-04-10
 */
@Service
public class LabDeviceCategoryServiceImpl implements ILabDeviceCategoryService 
{
    @Autowired
    private LabDeviceCategoryMapper labDeviceCategoryMapper;

    /**
     * 查询设备分类
     * 
     * @param categoryId 设备分类主键
     * @return 设备分类
     */
    @Override
    public LabDeviceCategory selectLabDeviceCategoryByCategoryId(Long categoryId)
    {
        return labDeviceCategoryMapper.selectLabDeviceCategoryByCategoryId(categoryId);
    }

    /**
     * 查询设备分类列表
     * 
     * @param labDeviceCategory 设备分类
     * @return 设备分类
     */
    @Override
    public List<LabDeviceCategory> selectLabDeviceCategoryList(LabDeviceCategory labDeviceCategory)
    {
        return labDeviceCategoryMapper.selectLabDeviceCategoryList(labDeviceCategory);
    }

    /**
     * 构建前端所需要树结构
     * 
     * @param categories 分类列表
     * @return 树结构列表
     */
    @Override
    public List<LabDeviceCategory> buildCategoryTree(List<LabDeviceCategory> categories)
    {
        List<LabDeviceCategory> returnList = new ArrayList<LabDeviceCategory>();
        List<Long> tempList = categories.stream().map(LabDeviceCategory::getCategoryId).collect(Collectors.toList());
        for (LabDeviceCategory category : categories)
        {
            if (!tempList.contains(category.getParentId()))
            {
                recursionFn(categories, category);
                returnList.add(category);
            }
        }
        if (returnList.isEmpty())
        {
            returnList = categories;
        }
        return returnList;
    }

    /**
     * 构建前端所需要下拉树结构
     * 
     * @param categories 分类列表
     * @return 下拉树结构列表
     */
    @Override
    public List<TreeSelect> buildCategoryTreeSelect(List<LabDeviceCategory> categories)
    {
        List<LabDeviceCategory> categoryTrees = buildCategoryTree(categories);
        return categoryTrees.stream().map(TreeSelect::new).collect(Collectors.toList());
    }

    /**
     * 递归列表
     */
    private void recursionFn(List<LabDeviceCategory> list, LabDeviceCategory t)
    {
        List<LabDeviceCategory> childList = getChildList(list, t);
        t.setChildren(childList);
        for (LabDeviceCategory tChild : childList)
        {
            if (hasChild(list, tChild))
            {
                recursionFn(list, tChild);
            }
        }
    }

    /**
     * 得到子节点列表
     */
    private List<LabDeviceCategory> getChildList(List<LabDeviceCategory> list, LabDeviceCategory t)
    {
        List<LabDeviceCategory> tlist = new ArrayList<LabDeviceCategory>();
        Iterator<LabDeviceCategory> it = list.iterator();
        while (it.hasNext())
        {
            LabDeviceCategory n = it.next();
            if (StringUtils.isNotNull(n.getParentId()) && n.getParentId().longValue() == t.getCategoryId().longValue())
            {
                tlist.add(n);
            }
        }
        return tlist;
    }

    /**
     * 判断是否有子节点
     */
    private boolean hasChild(List<LabDeviceCategory> list, LabDeviceCategory t)
    {
        return getChildList(list, t).size() > 0;
    }

    /**
     * 新增设备分类
     * 
     * @param labDeviceCategory 设备分类
     * @return 结果
     */
    @Override
    public int insertLabDeviceCategory(LabDeviceCategory labDeviceCategory)
    {
        labDeviceCategory.setCreateTime(DateUtils.getNowDate());
        return labDeviceCategoryMapper.insertLabDeviceCategory(labDeviceCategory);
    }

    /**
     * 修改设备分类
     * 
     * @param labDeviceCategory 设备分类
     * @return 结果
     */
    @Override
    public int updateLabDeviceCategory(LabDeviceCategory labDeviceCategory)
    {
        labDeviceCategory.setUpdateTime(DateUtils.getNowDate());
        return labDeviceCategoryMapper.updateLabDeviceCategory(labDeviceCategory);
    }

    /**
     * 批量删除设备分类
     * 
     * @param categoryIds 需要删除的设备分类主键
     * @return 结果
     */
    @Override
    public int deleteLabDeviceCategoryByCategoryIds(Long[] categoryIds)
    {
        return labDeviceCategoryMapper.deleteLabDeviceCategoryByCategoryIds(categoryIds);
    }

    /**
     * 删除设备分类信息
     * 
     * @param categoryId 设备分类主键
     * @return 结果
     */
    @Override
    public int deleteLabDeviceCategoryByCategoryId(Long categoryId)
    {
        return labDeviceCategoryMapper.deleteLabDeviceCategoryByCategoryId(categoryId);
    }
}
