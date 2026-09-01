package com.sugon.testplatform.service;

import com.sugon.testplatform.entity.NotifyMsg;
import com.sugon.testplatform.common.PageResult;

public interface NotifyService {
    void send(Long userId, String title, String content, String msgType, Long bizId);
    void send(Long userId, String title, String content, String msgType, Long bizId, String jumpUrl);
    void sendToRole(String roleCode, String title, String content, String msgType, Long bizId);
    void sendToRole(String roleCode, String title, String content, String msgType, Long bizId, String jumpUrl);
    PageResult<NotifyMsg> myMessages(Long userId, int pageNum, int pageSize);
    PageResult<NotifyMsg> myMessages(Long userId, int pageNum, int pageSize, Integer isRead);
    long unreadCount(Long userId);
    void markRead(Long id);
    void markAllRead(Long userId);
}
