-- =============================================================
-- 增量脚本：对外邮件通知
-- 1) mail_config 平台发件账号配置（单行）
-- 2) mail_log 发送日志
-- 3) sys_config 增加平台访问地址
-- =============================================================
USE test_platform;

CREATE TABLE IF NOT EXISTS mail_config (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  host VARCHAR(100) COMMENT 'SMTP主机',
  port INT DEFAULT 465 COMMENT 'SMTP端口',
  username VARCHAR(100) COMMENT '发件邮箱账号',
  password VARCHAR(200) COMMENT '邮箱授权码/密码',
  from_addr VARCHAR(100) COMMENT '发件人地址',
  from_name VARCHAR(50) DEFAULT '测试项目管理平台' COMMENT '发件人名称',
  use_ssl TINYINT DEFAULT 1 COMMENT '是否SSL',
  enabled TINYINT DEFAULT 0 COMMENT '是否启用',
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='邮件配置';

CREATE TABLE IF NOT EXISTS mail_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  to_addr VARCHAR(200) COMMENT '收件人',
  subject VARCHAR(300) COMMENT '主题',
  content TEXT,
  biz_type VARCHAR(30),
  biz_id BIGINT,
  success TINYINT DEFAULT 0,
  error VARCHAR(500),
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  KEY idx_biz (biz_type, biz_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='邮件发送日志';

INSERT INTO sys_config (config_key, config_value, remark) VALUES
('platform.base-url','http://192.168.101.165:6080','平台对外访问地址(邮件链接用)')
ON DUPLICATE KEY UPDATE remark=VALUES(remark);
