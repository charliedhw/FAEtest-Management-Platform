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
     * 返回当前用户作为售前组组长可管辖的组员id集合（含所有下级组成员，递归）：
     * 售前大组(PRESALES_ROOT)负责人 -> 全部售前；业务部(PRESALES_DEPT)负责人 -> 本部全员；
     * 子组(PRESALES_REGION)组长 -> 本组成员。非组长返回空集合。
     */
    public List<Long> memberIdsOfLedGroups(Long uid) {
        if (uid == null) return Collections.emptyList();
        List<SysUserGroup> led = groupMapper.selectList(new LambdaQueryWrapper<SysUserGroup>()
                .in(SysUserGroup::getGroupType, "PRESALES_ROOT", "PRESALES_DEPT", "PRESALES_REGION")
                .eq(SysUserGroup::getLeaderId, uid));
        if (led.isEmpty()) return Collections.emptyList();

        // 以所领导的组为根，沿 parent_id 向下收集全部后代组
        List<SysUserGroup> all = groupMapper.selectList(new LambdaQueryWrapper<SysUserGroup>()
                .in(SysUserGroup::getGroupType, "PRESALES_ROOT", "PRESALES_DEPT", "PRESALES_REGION"));
        java.util.Map<Long, List<Long>> childrenOf = new java.util.HashMap<>();
        for (SysUserGroup g : all) {
            if (g.getParentId() != null) {
                childrenOf.computeIfAbsent(g.getParentId(), k -> new java.util.ArrayList<>()).add(g.getId());
            }
        }
        java.util.Set<Long> groupIds = new java.util.HashSet<>();
        java.util.Deque<Long> queue = new java.util.ArrayDeque<>();
        for (SysUserGroup g : led) queue.add(g.getId());
        while (!queue.isEmpty()) {
            Long gid = queue.poll();
            if (!groupIds.add(gid)) continue;
            queue.addAll(childrenOf.getOrDefault(gid, Collections.emptyList()));
        }
        List<SysUserGroupRel> rels = relMapper.selectList(new LambdaQueryWrapper<SysUserGroupRel>()
                .in(SysUserGroupRel::getGroupId, groupIds));
        return rels.stream().map(SysUserGroupRel::getUserId).distinct().collect(Collectors.toList());
    }

    /** 当前用户是否是售前区域组组长 */
    public boolean isRegionLeader(Long uid) {
        return !memberIdsOfLedGroups(uid).isEmpty();
    }
}
