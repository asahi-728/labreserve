package com.ruoyi.project.lab.service.impl;

import java.util.List;
import com.ruoyi.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.project.lab.mapper.LabLaboratoryMapper;
import com.ruoyi.project.lab.domain.LabLaboratory;
import com.ruoyi.project.lab.service.ILabLaboratoryService;

/**
 * 实验室信息Service业务层处理
 * 
 * @author asahi
 * @date 2026-04-10
 */
@Service
public class LabLaboratoryServiceImpl implements ILabLaboratoryService 
{
    @Autowired
    private LabLaboratoryMapper labLaboratoryMapper;

    /**
     * 查询实验室信息
     * 
     * @param labId 实验室信息主键
     * @return 实验室信息
     */
    @Override
    public LabLaboratory selectLabLaboratoryByLabId(Long labId)
    {
        return labLaboratoryMapper.selectLabLaboratoryByLabId(labId);
    }

    /**
     * 查询实验室信息列表
     * 
     * @param labLaboratory 实验室信息
     * @return 实验室信息
     */
    @Override
    public List<LabLaboratory> selectLabLaboratoryList(LabLaboratory labLaboratory)
    {
        return labLaboratoryMapper.selectLabLaboratoryList(labLaboratory);
    }

    /**
     * 新增实验室信息
     * 
     * @param labLaboratory 实验室信息
     * @return 结果
     */
    @Override
    public int insertLabLaboratory(LabLaboratory labLaboratory)
    {
        labLaboratory.setCreateTime(DateUtils.getNowDate());
        return labLaboratoryMapper.insertLabLaboratory(labLaboratory);
    }

    /**
     * 修改实验室信息
     * 
     * @param labLaboratory 实验室信息
     * @return 结果
     */
    @Override
    public int updateLabLaboratory(LabLaboratory labLaboratory)
    {
        labLaboratory.setUpdateTime(DateUtils.getNowDate());
        return labLaboratoryMapper.updateLabLaboratory(labLaboratory);
    }

    /**
     * 批量删除实验室信息
     * 
     * @param labIds 需要删除的实验室信息主键
     * @return 结果
     */
    @Override
    public int deleteLabLaboratoryByLabIds(Long[] labIds)
    {
        return labLaboratoryMapper.deleteLabLaboratoryByLabIds(labIds);
    }

    /**
     * 删除实验室信息信息
     * 
     * @param labId 实验室信息主键
     * @return 结果
     */
    @Override
    public int deleteLabLaboratoryByLabId(Long labId)
    {
        return labLaboratoryMapper.deleteLabLaboratoryByLabId(labId);
    }
}
