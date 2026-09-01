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
