-- =============================================================
-- 增量脚本：项目排期功能
-- 1) test_application 增加排期字段
-- 2) 新增排期员角色 SCHEDULER，并赋予 张俊聪(zhangjc) 和 戴海伟(daihw)
-- =============================================================
USE test_platform;

ALTER TABLE test_application
  ADD COLUMN schedule_start_time DATE NULL COMMENT '排期-计划开始测试时间' AFTER current_node,
  ADD COLUMN schedule_end_time DATE NULL COMMENT '排期-计划结束测试时间' AFTER schedule_start_time,
  ADD COLUMN schedule_remark VARCHAR(500) NULL COMMENT '排期说明(等资源/等人员等)' AFTER schedule_end_time,
  ADD COLUMN schedule_by BIGINT NULL COMMENT '排期人id' AFTER schedule_remark,
  ADD COLUMN schedule_by_name VARCHAR(50) NULL COMMENT '排期人姓名' AFTER schedule_by,
  ADD COLUMN schedule_time DATETIME NULL COMMENT '排期操作时间' AFTER schedule_by_name;

-- 排期员角色
INSERT INTO sys_role (role_code, role_name, remark) VALUES
('SCHEDULER','排期员','对审批通过的测试项目进行排期')
ON DUPLICATE KEY UPDATE role_name=VALUES(role_name);

-- 赋予张俊聪、戴海伟排期员角色（按用户名匹配，存在才插入）
INSERT INTO sys_user_role (user_id, role_id)
SELECT u.id, r.id FROM sys_user u, sys_role r
WHERE u.username IN ('zhangjc','daihw') AND r.role_code='SCHEDULER'
AND NOT EXISTS (SELECT 1 FROM sys_user_role ur WHERE ur.user_id=u.id AND ur.role_id=r.id);
