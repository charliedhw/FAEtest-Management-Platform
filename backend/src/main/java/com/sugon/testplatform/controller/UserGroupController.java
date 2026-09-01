package com.sugon.testplatform.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sugon.testplatform.common.Result;
import com.sugon.testplatform.entity.SysUserGroup;
import com.sugon.testplatform.entity.SysUserGroupRel;
import com.sugon.testplatform.mapper.SysUserGroupMapper;
import com.sugon.testplatform.mapper.SysUserGroupRelMapper;
import com.sugon.testplatform.mapper.SysUserMapper;
import com.sugon.testplatform.entity.SysUser;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/group")
@RequiredArgsConstructor
public class UserGroupController {
    private final SysUserGroupMapper groupMapper;
    private final SysUserGroupRelMapper relMapper;
    private final SysUserMapper userMapper;

    @GetMapping("/list")
    public Result<List<SysUserGroup>> list() {
        return Result.ok(groupMapper.selectList(null));
    }

    @PostMapping("/save")
    public Result<Void> save(@RequestBody SysUserGroup group) {
        if (group.getId() == null) {
            groupMapper.insert(group);
        } else {
            groupMapper.updateById(group);
        }
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @Transactional
    public Result<Void> delete(@PathVariable Long id) {
        relMapper.delete(new LambdaQueryWrapper<SysUserGroupRel>().eq(SysUserGroupRel::getGroupId, id));
        groupMapper.deleteById(id);
        return Result.ok();
    }

    /**
     * 设置组成员
     */
    @PostMapping("/members/{groupId}")
    @Transactional
    public Result<Void> setMembers(@PathVariable Long groupId, @RequestBody List<Long> userIds) {
        relMapper.delete(new LambdaQueryWrapper<SysUserGroupRel>().eq(SysUserGroupRel::getGroupId, groupId));
        if (userIds != null) {
            for (Long uid : userIds) {
                SysUserGroupRel rel = new SysUserGroupRel();
                rel.setGroupId(groupId);
                rel.setUserId(uid);
                relMapper.insert(rel);
            }
        }
        return Result.ok();
    }

    /**
     * 获取组成员
     */
    @GetMapping("/members/{groupId}")
    public Result<List<SysUser>> members(@PathVariable Long groupId) {
        List<SysUserGroupRel> rels = relMapper.selectList(
                new LambdaQueryWrapper<SysUserGroupRel>().eq(SysUserGroupRel::getGroupId, groupId));
        if (rels.isEmpty()) return Result.ok(List.of());
        List<Long> userIds = rels.stream().map(SysUserGroupRel::getUserId).collect(Collectors.toList());
        List<SysUser> users = userMapper.selectBatchIds(userIds);
        users.forEach(u -> u.setPassword(null));
        return Result.ok(users);
    }

    /**
     * 设置组负责人
     */
    @PostMapping("/leader")
    public Result<Void> setLeader(@RequestBody Map<String, Object> body) {
        Long groupId = Long.valueOf(body.get("groupId").toString());
        Long leaderId = Long.valueOf(body.get("leaderId").toString());
        SysUser leader = userMapper.selectById(leaderId);
        SysUserGroup group = new SysUserGroup();
        group.setId(groupId);
        group.setLeaderId(leaderId);
        group.setLeaderName(leader == null ? null : leader.getRealName());
        groupMapper.updateById(group);
        return Result.ok();
    }
}
