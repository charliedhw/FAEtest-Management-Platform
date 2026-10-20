-- 安全与可靠性修复（2026-10-20）
-- 1) 申请单号唯一约束（配合服务端并发重试，防止重复单号）
--    若执行报重复键错误，说明历史数据存在重复 app_no，需先人工处理重复数据
ALTER TABLE test_application ADD UNIQUE KEY uk_app_no (app_no);

-- 2) 邮件发送日志增加重试次数字段（失败邮件自动重试）
ALTER TABLE mail_log ADD COLUMN retry_count INT NOT NULL DEFAULT 0 COMMENT '失败重试次数';
