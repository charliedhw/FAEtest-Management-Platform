package com.sugon.testplatform.security;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sugon.testplatform.entity.SysUserGroup;
import com.sugon.testplatform.entity.SysUserGroupRel;
import com.sugon.testplatform.mapper.SysUserGroupMapper;
import com.sugon.testplatform.mapper.SysUserGroupRelMapper;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 售前区域组数据范围：当前用户若是某个"售前区域组"的负责人(组长)，
 * 则可见该组全体组员作为申请人/方案售前关联的申请与项目。
 */
@Component
public class PresalesScope {
    private final SysUserGroupMapper groupMapper;
    private final SysUserGroupRelMapper relMapper;

    public PresalesScope(SysUserGroupMapper groupMapper, SysUserGroupRelMapper relMapper) {
        this.groupMapper = groupMapper;
        this.relMapper = relMapper;
    }

    /**
     * 返回当前用户作为售前区域组组长可管辖的组员id集合；非组长返回空集合
     */
    public List<Long> memberIdsOfLedGroups(Long uid) {
        if (uid == null) return Collections.emptyList();
        List<SysUserGroup> groups = groupMapper.selectList(new LambdaQueryWrapper<SysUserGroup>()
                .eq(SysUserGroup::getGroupType, "PRESALES_REGION")
                .eq(SysUserGroup::getLeaderId, uid));
        if (groups.isEmpty()) return Collections.emptyList();
        List<Long> gids = groups.stream().map(SysUserGroup::getId).collect(Collectors.toList());
        List<SysUserGroupRel> rels = relMapper.selectList(new LambdaQueryWrapper<SysUserGroupRel>()
                .in(SysUserGroupRel::getGroupId, gids));
        return rels.stream().map(SysUserGroupRel::getUserId).distinct().collect(Collectors.toList());
    }

    /** 当前用户是否是售前区域组组长 */
    public boolean isRegionLeader(Long uid) {
        return !memberIdsOfLedGroups(uid).isEmpty();
    }
}
