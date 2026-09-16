-- =============================================================
-- 增量脚本：售前区域组（区域售前组长可见本组售前项目）
-- sys_user_group 增加 group_type，售前区域组负责人在数据范围中可见组员的申请与项目
-- =============================================================
USE test_platform;

ALTER TABLE sys_user_group
  ADD COLUMN group_type VARCHAR(30) DEFAULT 'GENERAL' COMMENT 'GENERAL普通组/PRESALES_REGION售前区域组' AFTER group_name;

CREATE INDEX idx_group_type ON sys_user_group (group_type);
