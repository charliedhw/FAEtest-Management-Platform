package com.sugon.testplatform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sugon.testplatform.common.PageResult;
import com.sugon.testplatform.entity.NotifyMsg;
import com.sugon.testplatform.entity.SysUser;
import com.sugon.testplatform.mapper.NotifyMsgMapper;
import com.sugon.testplatform.service.NotifyService;
import com.sugon.testplatform.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotifyServiceImpl implements NotifyService {
    private final NotifyMsgMapper notifyMsgMapper;
    private final UserService userService;

    @Override
    public void send(Long userId, String title, String content, String msgType, Long bizId) {
        send(userId, title, content, msgType, bizId, null);
    }

    @Override
    public void send(Long userId, String title, String content, String msgType, Long bizId, String jumpUrl) {
        NotifyMsg msg = new NotifyMsg();
        msg.setUserId(userId);
        msg.setTitle(title);
        msg.setContent(content);
        msg.setMsgType(msgType);
        msg.setBizId(bizId);
        msg.setJumpUrl(jumpUrl);
        msg.setIsRead(0);
        notifyMsgMapper.insert(msg);
    }

    @Override
    public void sendToRole(String roleCode, String title, String content, String msgType, Long bizId) {
        sendToRole(roleCode, title, content, msgType, bizId, null);
    }

    @Override
    public void sendToRole(String roleCode, String title, String content, String msgType, Long bizId, String jumpUrl) {
        List<SysUser> users = userService.listByRole(roleCode);
        for (SysUser u : users) {
            send(u.getId(), title, content, msgType, bizId, jumpUrl);
        }
    }

    @Override
    public PageResult<NotifyMsg> myMessages(Long userId, int pageNum, int pageSize) {
        return myMessages(userId, pageNum, pageSize, null);
    }

    @Override
    public PageResult<NotifyMsg> myMessages(Long userId, int pageNum, int pageSize, Integer isRead) {
        Page<NotifyMsg> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<NotifyMsg> qw = new LambdaQueryWrapper<NotifyMsg>()
                .eq(NotifyMsg::getUserId, userId)
                .orderByDesc(NotifyMsg::getCreateTime);
        if (isRead != null) {
            qw.eq(NotifyMsg::getIsRead, isRead);
        }
        Page<NotifyMsg> result = notifyMsgMapper.selectPage(page, qw);
        return new PageResult<>(result.getTotal(), result.getRecords());
    }

    @Override
    public long unreadCount(Long userId) {
        return notifyMsgMapper.selectCount(new LambdaQueryWrapper<NotifyMsg>()
                .eq(NotifyMsg::getUserId, userId).eq(NotifyMsg::getIsRead, 0));
    }

    @Override
    public void markRead(Long id) {
        NotifyMsg msg = new NotifyMsg();
        msg.setId(id);
        msg.setIsRead(1);
        notifyMsgMapper.updateById(msg);
    }

    @Override
    public void markAllRead(Long userId) {
        NotifyMsg update = new NotifyMsg();
        update.setIsRead(1);
        notifyMsgMapper.update(update, new LambdaQueryWrapper<NotifyMsg>()
                .eq(NotifyMsg::getUserId, userId).eq(NotifyMsg::getIsRead, 0));
    }
}
