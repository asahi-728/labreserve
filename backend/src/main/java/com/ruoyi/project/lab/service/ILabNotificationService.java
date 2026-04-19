package com.ruoyi.project.lab.service;

/**
 * 预约消息通知Service接口
 * 
 * @author asahi
 * @date 2026-04-10
 */
public interface ILabNotificationService 
{
    /**
     * 发送预约成功通知
     * 
     * @param userId 接收用户ID
     * @param deviceName 设备名称
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 结果
     */
    public int sendReservationSuccessNotice(Long userId, String deviceName, String startTime, String endTime);

    /**
     * 发送预约失败通知
     * 
     * @param userId 接收用户ID
     * @param reason 失败原因
     * @return 结果
     */
    public int sendReservationFailNotice(Long userId, String reason);

    /**
     * 发送预约审核通知
     * 
     * @param userId 接收用户ID
     * @param deviceName 设备名称
     * @param auditResult 审核结果
     * @return 结果
     */
    public int sendAuditNotice(Long userId, String deviceName, String auditResult);

    /**
     * 发送预约取消通知
     * 
     * @param userId 接收用户ID
     * @param deviceName 设备名称
     * @return 结果
     */
    public int sendCancelNotice(Long userId, String deviceName);

    /**
     * 发送设备提醒通知
     * 
     * @param userId 接收用户ID
     * @param deviceName 设备名称
     * @param remindTime 提醒时间
     * @return 结果
     */
    public int sendDeviceRemindNotice(Long userId, String deviceName, String remindTime);
}
