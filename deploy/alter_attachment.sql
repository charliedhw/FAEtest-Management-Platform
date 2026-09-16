-- =============================================================
-- 增量脚本：测试申请附件（测试指标要求 / 测试方案）
-- =============================================================
USE test_platform;

CREATE TABLE IF NOT EXISTS test_attachment (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  biz_type VARCHAR(30) DEFAULT 'APPLICATION' COMMENT '业务类型',
  biz_id BIGINT NOT NULL COMMENT '业务id(申请单id)',
  file_type VARCHAR(20) COMMENT 'METRIC测试指标要求/PLAN测试方案',
  file_name VARCHAR(300),
  file_key VARCHAR(500) COMMENT 'MinIO对象key',
  file_size BIGINT,
  upload_by BIGINT,
  upload_by_name VARCHAR(50),
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  KEY idx_biz (biz_type, biz_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='测试申请附件';
