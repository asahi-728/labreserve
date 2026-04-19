package com.ruoyi.project.lab.service;

import java.util.List;
import com.ruoyi.project.lab.domain.LabLaboratory;

/**
 * 实验室信息Service接口
 * 
 * @author asahi
 * @date 2026-04-10
 */
public interface ILabLaboratoryService 
{
    /**
     * 查询实验室信息
     * 
     * @param labId 实验室信息主键
     * @return 实验室信息
     */
    public LabLaboratory selectLabLaboratoryByLabId(Long labId);

    /**
     * 查询实验室信息列表
     * 
     * @param labLaboratory 实验室信息
     * @return 实验室信息集合
     */
    public List<LabLaboratory> selectLabLaboratoryList(LabLaboratory labLaboratory);

    /**
     * 新增实验室信息
     * 
     * @param labLaboratory 实验室信息
     * @return 结果
     */
    public int insertLabLaboratory(LabLaboratory labLaboratory);

    /**
     * 修改实验室信息
     * 
     * @param labLaboratory 实验室信息
     * @return 结果
     */
    public int updateLabLaboratory(LabLaboratory labLaboratory);

    /**
     * 批量删除实验室信息
     * 
     * @param labIds 需要删除的实验室信息主键集合
     * @return 结果
     */
    public int deleteLabLaboratoryByLabIds(Long[] labIds);

    /**
     * 删除实验室信息信息
     * 
     * @param labId 实验室信息主键
     * @return 结果
     */
    public int deleteLabLaboratoryByLabId(Long labId);
}
