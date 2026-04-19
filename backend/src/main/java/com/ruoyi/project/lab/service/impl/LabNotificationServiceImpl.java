package com.ruoyi.project.lab.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.project.system.domain.SysNotice;
import com.ruoyi.project.system.service.ISysNoticeService;
import com.ruoyi.project.lab.service.ILabNotificationService;

/**
 * 预约消息通知Service业务层处理
 * 
 * @author asahi
 * @date 2026-04-10
 */
@Service
public class LabNotificationServiceImpl implements ILabNotificationService 
{
    @Autowired
    private ISysNoticeService sysNoticeService;

    /**
     * 发送预约成功通知
     */
    @Override
    @Transactional
    public int sendReservationSuccessNotice(Long userId, String deviceName, String startTime, String endTime)
    {
        SysNotice notice = new SysNotice();
        notice.setNoticeTitle("预约成功通知");
        notice.setNoticeType("1");
        String content = String.format("您预约的设备【%s】已成功！预约时间：%s 至 %s，请准时使用。", deviceName, startTime, endTime);
        notice.setNoticeContent(content);
        notice.setStatus("0");
        notice.setCreateBy(SecurityUtils.getUsername());
        notice.setCreateTime(DateUtils.getNowDate());
        return sysNoticeService.insertNotice(notice);
    }

    /**
     * 发送预约失败通知
     */
    @Override
    @Transactional
    public int sendReservationFailNotice(Long userId, String reason)
    {
        SysNotice notice = new SysNotice();
        notice.setNoticeTitle("预约失败通知");
        notice.setNoticeType("1");
        String content = String.format("您的设备预约失败，原因：%s。请重新选择设备或时段。", reason);
        notice.setNoticeContent(content);
        notice.setStatus("0");
        notice.setCreateBy(SecurityUtils.getUsername());
        notice.setCreateTime(DateUtils.getNowDate());
        return sysNoticeService.insertNotice(notice);
    }

    /**
     * 发送预约审核通知
     */
    @Override
    @Transactional
    public int sendAuditNotice(Long userId, String deviceName, String auditResult)
    {
        SysNotice notice = new SysNotice();
        notice.setNoticeTitle("预约审核通知");
        notice.setNoticeType("1");
        String content = String.format("您预约的设备【%s】审核结果：%s。", deviceName, auditResult);
        notice.setNoticeContent(content);
        notice.setStatus("0");
        notice.setCreateBy(SecurityUtils.getUsername());
        notice.setCreateTime(DateUtils.getNowDate());
        return sysNoticeService.insertNotice(notice);
    }

    /**
     * 发送预约取消通知
     */
    @Override
    @Transactional
    public int sendCancelNotice(Long userId, String deviceName)
    {
        SysNotice notice = new SysNotice();
        notice.setNoticeTitle("预约取消通知");
        notice.setNoticeType("1");
        String content = String.format("您已成功取消设备【%s】的预约。", deviceName);
        notice.setNoticeContent(content);
        notice.setStatus("0");
        notice.setCreateBy(SecurityUtils.getUsername());
        notice.setCreateTime(DateUtils.getNowDate());
        return sysNoticeService.insertNotice(notice);
    }

    /**
     * 发送设备提醒通知
     */
    @Override
    @Transactional
    public int sendDeviceRemindNotice(Long userId, String deviceName, String remindTime)
    {
        SysNotice notice = new SysNotice();
        notice.setNoticeTitle("设备使用提醒");
        notice.setNoticeType("1");
        String content = String.format("您预约的设备【%s】将于 %s 开始使用，请准时到达！", deviceName, remindTime);
        notice.setNoticeContent(content);
        notice.setStatus("0");
        notice.setCreateBy(SecurityUtils.getUsername());
        notice.setCreateTime(DateUtils.getNowDate());
        return sysNoticeService.insertNotice(notice);
    }
}
