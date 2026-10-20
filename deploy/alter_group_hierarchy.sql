-- 用户组层级：售前大组(根) -> 业务部 -> 子组
-- 组长可见范围 = 本组成员 + 所有下级组成员（递归）
ALTER TABLE sys_user_group ADD COLUMN parent_id BIGINT DEFAULT NULL COMMENT '父组ID(售前大组->业务部->子组)' AFTER group_type;
CREATE INDEX idx_parent_id ON sys_user_group (parent_id);
