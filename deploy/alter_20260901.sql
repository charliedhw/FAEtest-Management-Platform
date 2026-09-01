-- =========================================================
-- 测试项目管理平台 数据库增量变更脚本
-- 版本: v2026-09-01
-- 变更内容:
--   1. 新增表: sys_user_group(用户组)、sys_user_group_rel(用户组关联)、test_stage(阶段任务)、weekly_report(周报)
--   2. test_application 新增: project_stage, bid_status, test_method, sales_id, sales_name, presales_id, presales_name
--   3. test_project 新增: project_stage, test_method
--   4. test_progress 新增: stage_id
--   5. resource 新增: serial_no, online_status
--   6. notify_msg 新增: jump_url
--   7. 新增字典: test_method(测试方式)
-- 说明: 本脚本幂等,可重复执行不报错
-- =========================================================

USE test_platform;

-- ========== 1. 新增表 ==========

-- 用户组表
CREATE TABLE IF NOT EXISTS `sys_user_group` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `group_code` varchar(50) NOT NULL COMMENT '组编码 SALES_GROUP/PRESALES_GROUP/APPROVER_GROUP/FAE_GROUP',
  `group_name` varchar(100) NOT NULL COMMENT '组名称',
  `leader_id` bigint DEFAULT NULL COMMENT '组长用户ID',
  `leader_name` varchar(50) DEFAULT NULL COMMENT '组长姓名',
  `remark` varchar(255) DEFAULT NULL COMMENT '备注',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_group_code` (`group_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户组表';

-- 用户组关联表
CREATE TABLE IF NOT EXISTS `sys_user_group_rel` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `group_id` bigint NOT NULL COMMENT '组ID',
  PRIMARY KEY (`id`),
  KEY `idx_user` (`user_id`),
  KEY `idx_group` (`group_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户组关联表';

-- 阶段任务表
CREATE TABLE IF NOT EXISTS `test_stage` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `project_id` bigint NOT NULL COMMENT '项目ID',
  `stage_name` varchar(100) NOT NULL COMMENT '阶段名称',
  `test_desc` text COMMENT '测试方案描述',
  `plan_start` date DEFAULT NULL COMMENT '计划开始时间',
  `plan_end` date DEFAULT NULL COMMENT '计划结束时间',
  `status` varchar(20) DEFAULT 'NOT_START' COMMENT '状态 NOT_START/IN_PROGRESS/DONE',
  `sort` int DEFAULT 0 COMMENT '排序',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `create_by_name` varchar(50) DEFAULT NULL COMMENT '创建人姓名',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_project` (`project_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='阶段任务表';

-- 周报表
CREATE TABLE IF NOT EXISTS `weekly_report` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `title` varchar(200) NOT NULL COMMENT '周报名称 如: w35-应用测试部-胡深',
  `week_num` int NOT NULL COMMENT '周数',
  `year` int NOT NULL COMMENT '年份',
  `this_week_progress` text COMMENT '本周测试进展',
  `problems` text COMMENT '存在问题',
  `next_week_plan` text COMMENT '下周工作计划',
  `author_id` bigint NOT NULL COMMENT '作者ID',
  `author_name` varchar(50) NOT NULL COMMENT '作者姓名',
  `dept_name` varchar(100) DEFAULT '应用测试部' COMMENT '部门名称',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_week` (`year`, `week_num`),
  KEY `idx_author` (`author_id`),
  UNIQUE KEY `uk_author_week` (`author_id`, `year`, `week_num`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='周报表';

-- ========== 2. test_application 新增字段 ==========

-- project_stage (可能已由alter_20260831.sql添加)
SET @has := (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA='test_platform' AND TABLE_NAME='test_application' AND COLUMN_NAME='project_stage');
SET @sql := IF(@has=0,
  'ALTER TABLE test_application ADD COLUMN project_stage VARCHAR(20) NULL COMMENT ''项目阶段L1-L9'' AFTER spm_no',
  'SELECT ''test_application.project_stage 已存在'' AS tip');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- bid_status (可能已由alter_20260831.sql添加)
SET @has := (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA='test_platform' AND TABLE_NAME='test_application' AND COLUMN_NAME='bid_status');
SET @sql := IF(@has=0,
  'ALTER TABLE test_application ADD COLUMN bid_status VARCHAR(20) NULL COMMENT ''招标状态'' AFTER project_stage',
  'SELECT ''test_application.bid_status 已存在'' AS tip');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- test_method
SET @has := (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA='test_platform' AND TABLE_NAME='test_application' AND COLUMN_NAME='test_method');
SET @sql := IF(@has=0,
  'ALTER TABLE test_application ADD COLUMN test_method VARCHAR(50) NULL COMMENT ''测试方式'' AFTER expect_resource_type',
  'SELECT ''test_application.test_method 已存在'' AS tip');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- sales_id
SET @has := (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA='test_platform' AND TABLE_NAME='test_application' AND COLUMN_NAME='sales_id');
SET @sql := IF(@has=0,
  'ALTER TABLE test_application ADD COLUMN sales_id BIGINT NULL COMMENT ''销售ID'' AFTER apply_days',
  'SELECT ''test_application.sales_id 已存在'' AS tip');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- sales_name
SET @has := (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA='test_platform' AND TABLE_NAME='test_application' AND COLUMN_NAME='sales_name');
SET @sql := IF(@has=0,
  'ALTER TABLE test_application ADD COLUMN sales_name VARCHAR(50) NULL COMMENT ''销售姓名'' AFTER sales_id',
  'SELECT ''test_application.sales_name 已存在'' AS tip');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- presales_id
SET @has := (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA='test_platform' AND TABLE_NAME='test_application' AND COLUMN_NAME='presales_id');
SET @sql := IF(@has=0,
  'ALTER TABLE test_application ADD COLUMN presales_id BIGINT NULL COMMENT ''售前ID'' AFTER sales_name',
  'SELECT ''test_application.presales_id 已存在'' AS tip');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- presales_name
SET @has := (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA='test_platform' AND TABLE_NAME='test_application' AND COLUMN_NAME='presales_name');
SET @sql := IF(@has=0,
  'ALTER TABLE test_application ADD COLUMN presales_name VARCHAR(50) NULL COMMENT ''售前姓名'' AFTER presales_id',
  'SELECT ''test_application.presales_name 已存在'' AS tip');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ========== 3. test_project 新增字段 ==========

-- project_stage (可能已由alter_20260831.sql添加)
SET @has := (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA='test_platform' AND TABLE_NAME='test_project' AND COLUMN_NAME='project_stage');
SET @sql := IF(@has=0,
  'ALTER TABLE test_project ADD COLUMN project_stage VARCHAR(20) NULL COMMENT ''项目阶段L1-L9'' AFTER spm_no',
  'SELECT ''test_project.project_stage 已存在'' AS tip');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- test_method
SET @has := (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA='test_platform' AND TABLE_NAME='test_project' AND COLUMN_NAME='test_method');
SET @sql := IF(@has=0,
  'ALTER TABLE test_project ADD COLUMN test_method VARCHAR(50) NULL COMMENT ''测试方式'' AFTER is_internal_resource',
  'SELECT ''test_project.test_method 已存在'' AS tip');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ========== 4. test_progress 新增字段 ==========

SET @has := (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA='test_platform' AND TABLE_NAME='test_progress' AND COLUMN_NAME='stage_id');
SET @sql := IF(@has=0,
  'ALTER TABLE test_progress ADD COLUMN stage_id BIGINT NULL COMMENT ''关联阶段ID'' AFTER project_id',
  'SELECT ''test_progress.stage_id 已存在'' AS tip');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ========== 5. resource 新增字段 ==========

SET @has := (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA='test_platform' AND TABLE_NAME='resource' AND COLUMN_NAME='serial_no');
SET @sql := IF(@has=0,
  'ALTER TABLE resource ADD COLUMN serial_no VARCHAR(100) NULL COMMENT ''设备序列号'' AFTER resource_code',
  'SELECT ''resource.serial_no 已存在'' AS tip');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has := (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA='test_platform' AND TABLE_NAME='resource' AND COLUMN_NAME='online_status');
SET @sql := IF(@has=0,
  'ALTER TABLE resource ADD COLUMN online_status VARCHAR(20) DEFAULT ''OFFLINE'' COMMENT ''在线状态 ONLINE/OFFLINE'' AFTER status',
  'SELECT ''resource.online_status 已存在'' AS tip');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ========== 6. notify_msg 新增字段 ==========

SET @has := (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA='test_platform' AND TABLE_NAME='notify_msg' AND COLUMN_NAME='jump_url');
SET @sql := IF(@has=0,
  'ALTER TABLE notify_msg ADD COLUMN jump_url VARCHAR(300) NULL COMMENT ''跳转链接'' AFTER biz_id',
  'SELECT ''notify_msg.jump_url 已存在'' AS tip');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ========== 7. 新增字典数据 ==========

-- 测试方式字典
INSERT INTO sys_dict (dict_type, dict_label, dict_value, sort) VALUES
('test_method','线上测试用户自测','线上测试用户自测',1),
('test_method','线上FAE支持测试','线上FAE支持测试',2),
('test_method','线下用户自测','线下用户自测',3),
('test_method','线下FAE支持测试','线下FAE支持测试',4)
ON DUPLICATE KEY UPDATE dict_label=VALUES(dict_label), sort=VALUES(sort);

-- 项目阶段字典(如alter_20260831.sql未执行过)
INSERT INTO sys_dict (dict_type, dict_label, dict_value, sort) VALUES
('project_stage','L1','L1',1),
('project_stage','L2','L2',2),
('project_stage','L3','L3',3),
('project_stage','L4','L4',4),
('project_stage','L5','L5',5),
('project_stage','L6','L6',6),
('project_stage','L7','L7',7),
('project_stage','L8','L8',8),
('project_stage','L9','L9',9)
ON DUPLICATE KEY UPDATE dict_label=VALUES(dict_label), sort=VALUES(sort);

-- 用户组初始数据
INSERT INTO sys_user_group (group_code, group_name) VALUES
('SALES_GROUP','销售组'),
('PRESALES_GROUP','售前组'),
('APPROVER_GROUP','测试审批组'),
('FAE_GROUP','FAE测试组')
ON DUPLICATE KEY UPDATE group_name=VALUES(group_name);

-- ========== 完成 ==========
SELECT 'alter_20260901.sql 执行完成' AS result;
