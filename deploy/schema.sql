-- =============================================================
-- 测试项目管理平台 数据库初始化脚本
-- DB: MySQL 8.0  charset: utf8mb4
-- =============================================================
CREATE DATABASE IF NOT EXISTS test_platform DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE test_platform;

-- ---------- 系统管理 ----------
CREATE TABLE IF NOT EXISTS sys_dept (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  dept_name VARCHAR(100) NOT NULL,
  parent_id BIGINT DEFAULT 0,
  sort INT DEFAULT 0,
  status TINYINT DEFAULT 1,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_dept_name (dept_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='部门表';

CREATE TABLE IF NOT EXISTS sys_role (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  role_code VARCHAR(50) NOT NULL,
  role_name VARCHAR(100) NOT NULL,
  remark VARCHAR(255),
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_role_code (role_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色表';

CREATE TABLE IF NOT EXISTS sys_user (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  username VARCHAR(50) NOT NULL,
  password VARCHAR(100) NOT NULL,
  real_name VARCHAR(50) NOT NULL,
  email VARCHAR(100),
  phone VARCHAR(20),
  dept_id BIGINT,
  oa_account VARCHAR(50) DEFAULT NULL COMMENT 'OA账号(预留)',
  status TINYINT DEFAULT 1 COMMENT '1启用 0停用',
  is_first_login TINYINT DEFAULT 1,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

CREATE TABLE IF NOT EXISTS sys_user_role (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  role_id BIGINT NOT NULL,
  UNIQUE KEY uk_user_role (user_id, role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色关联';

CREATE TABLE IF NOT EXISTS sys_dict (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  dict_type VARCHAR(50) NOT NULL COMMENT '字典类型',
  dict_label VARCHAR(100) NOT NULL,
  dict_value VARCHAR(100) NOT NULL,
  sort INT DEFAULT 0,
  status TINYINT DEFAULT 1,
  remark VARCHAR(255),
  UNIQUE KEY uk_type_value (dict_type, dict_value)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据字典';

CREATE TABLE IF NOT EXISTS sys_config (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  config_key VARCHAR(100) NOT NULL,
  config_value VARCHAR(500),
  remark VARCHAR(255),
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_config_key (config_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统参数';

-- ---------- 项目与申请 ----------
CREATE TABLE IF NOT EXISTS test_project (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  project_no VARCHAR(50) COMMENT '平台项目编号',
  customer_name VARCHAR(200) NOT NULL COMMENT '客户名称',
  project_name VARCHAR(300) NOT NULL COMMENT '项目名称',
  region VARCHAR(50) COMMENT '所属区域',
  spm_no VARCHAR(50) COMMENT '项目SPM号',
  sales_id BIGINT COMMENT '销售',
  sales_name VARCHAR(50),
  presales_id BIGINT COMMENT '方案售前',
  presales_name VARCHAR(50),
  tester_ids VARCHAR(200) COMMENT '测试人员id逗号分隔',
  tester_names VARCHAR(200),
  test_plan TEXT COMMENT '测试计划及内容',
  test_type VARCHAR(200) COMMENT '测试类型(多选json)',
  device_type VARCHAR(200) COMMENT '设备类型',
  hardware_config TEXT COMMENT '硬件配置',
  software_app TEXT COMMENT '软件及应用',
  is_internal_resource VARCHAR(50) COMMENT '是否内部测试资源',
  apply_time DATETIME COMMENT '申请时间',
  apply_period VARCHAR(50) COMMENT '申请测试周期',
  test_start_time DATE COMMENT '测试开始时间',
  test_end_time DATE COMMENT '测试结束时间',
  test_conclusion TEXT COMMENT '测试结论',
  status VARCHAR(20) DEFAULT 'DRAFT' COMMENT '项目状态',
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  report_link VARCHAR(500) COMMENT '测试报告链接',
  bid_status VARCHAR(20) COMMENT '中标状态',
  bid_amount DECIMAL(15,2) COMMENT '中标金额(万元)',
  is_key_project TINYINT DEFAULT 0 COMMENT '是否重点项目',
  biz_type VARCHAR(20) DEFAULT 'PRESALES' COMMENT 'PRESALES售前/DELIVERY交付售后',
  create_by BIGINT,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  KEY idx_status (status),
  KEY idx_region (region),
  KEY idx_sales (sales_id),
  KEY idx_presales (presales_id),
  KEY idx_apply_time (apply_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='测试项目主表';

CREATE TABLE IF NOT EXISTS test_application (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  app_no VARCHAR(50) COMMENT '申请单号',
  project_id BIGINT COMMENT '关联项目',
  applicant_id BIGINT NOT NULL COMMENT '申请人(销售)',
  applicant_name VARCHAR(50),
  customer_name VARCHAR(200),
  project_name VARCHAR(300),
  region VARCHAR(50),
  spm_no VARCHAR(50),
  requirement TEXT COMMENT '需求描述',
  test_plan TEXT,
  test_type VARCHAR(200),
  device_type VARCHAR(200),
  hardware_config TEXT,
  software_app TEXT,
  apply_period VARCHAR(50),
  expect_resource_type VARCHAR(50) COMMENT '期望资源类型',
  apply_days INT COMMENT '申请天数',
  current_node VARCHAR(50) COMMENT '当前流程节点',
  status VARCHAR(20) DEFAULT 'SUBMITTED',
  reject_reason VARCHAR(500),
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_applicant (applicant_id),
  KEY idx_status (status),
  KEY idx_project (project_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='测试申请单';

CREATE TABLE IF NOT EXISTS approval_record (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  app_id BIGINT NOT NULL COMMENT '申请单id',
  node VARCHAR(50) COMMENT '审批节点',
  approver_id BIGINT,
  approver_name VARCHAR(50),
  action VARCHAR(20) COMMENT 'APPROVE/REJECT/SUBMIT',
  opinion VARCHAR(1000) COMMENT '审批意见',
  cost_seconds INT COMMENT '耗时',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  KEY idx_app (app_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审批记录';

CREATE TABLE IF NOT EXISTS test_progress (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  project_id BIGINT NOT NULL,
  progress_date DATE NOT NULL,
  content TEXT,
  create_by BIGINT,
  create_by_name VARCHAR(50),
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  KEY idx_project_date (project_id, progress_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='测试进展日报';

-- ---------- 资源与借用 ----------
CREATE TABLE IF NOT EXISTS resource (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  resource_code VARCHAR(50) COMMENT '资源编号',
  resource_name VARCHAR(200) COMMENT '资源名称',
  resource_type VARCHAR(50) COMMENT '循环机/线上508/超算互联网/产品部机器/客户机器',
  hardware_config TEXT,
  factory_price DECIMAL(15,2) COMMENT '出厂价',
  dept_id BIGINT COMMENT '所属部门',
  dept_name VARCHAR(100),
  status VARCHAR(20) DEFAULT 'IDLE' COMMENT 'IDLE空闲/IN_USE占用/MAINTENANCE维护',
  location VARCHAR(200),
  remark VARCHAR(500),
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  KEY idx_type (resource_type),
  KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='资源池';

CREATE TABLE IF NOT EXISTS resource_loan (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  resource_id BIGINT NOT NULL,
  project_id BIGINT,
  app_id BIGINT,
  borrower_id BIGINT COMMENT '借用人(销售)',
  borrower_name VARCHAR(50),
  dept_id BIGINT COMMENT '费用归属部门',
  dept_name VARCHAR(100),
  loan_time DATETIME COMMENT '借出时间',
  expect_return_time DATETIME COMMENT '应还时间',
  actual_return_time DATETIME COMMENT '实际归还时间',
  loan_days INT,
  status VARCHAR(20) DEFAULT 'BORROWED' COMMENT 'BORROWED借出/RETURNED已还/OVERDUE超期',
  cost_amount DECIMAL(15,2) COMMENT '费用金额',
  remark VARCHAR(500),
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  KEY idx_resource (resource_id),
  KEY idx_borrower (borrower_id),
  KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='资源借用记录';

-- ---------- 报告 ----------
CREATE TABLE IF NOT EXISTS test_report (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  project_id BIGINT NOT NULL,
  file_name VARCHAR(300),
  file_key VARCHAR(500) COMMENT 'MinIO对象key',
  file_size BIGINT,
  version INT DEFAULT 1,
  upload_by BIGINT,
  upload_by_name VARCHAR(50),
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  KEY idx_project (project_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='测试报告';

-- ---------- 通知与审计 ----------
CREATE TABLE IF NOT EXISTS notify_msg (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  title VARCHAR(200),
  content TEXT,
  msg_type VARCHAR(20) COMMENT 'APPROVAL/PROGRESS/OVERDUE/SYSTEM',
  biz_id BIGINT,
  is_read TINYINT DEFAULT 0,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  KEY idx_user_read (user_id, is_read)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='站内通知';

CREATE TABLE IF NOT EXISTS audit_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT,
  user_name VARCHAR(50),
  module VARCHAR(50),
  action VARCHAR(100),
  detail TEXT,
  ip VARCHAR(50),
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  KEY idx_user (user_id),
  KEY idx_module (module)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审计日志';

-- ---------- 初始数据 ----------
INSERT INTO sys_role (role_code, role_name, remark) VALUES
('ADMIN','系统管理员','全部权限'),
('SALES','销售','发起测试申请'),
('PRESALES','售前工程师','评估并填写测试申请表'),
('APPROVER','售前审批组','审批测试申请'),
('LEADER','分管领导','超90天二级审批'),
('TESTER','FAE测试工程师','执行测试填写进展'),
('RESOURCE_ADMIN','资源管理员','资源分配与回收'),
('BUSINESS','商务','借用跟催')
ON DUPLICATE KEY UPDATE role_name=VALUES(role_name);

-- 默认管理员 admin / Admin@123 (BCrypt)
INSERT INTO sys_user (username, password, real_name, email, status, is_first_login)
SELECT 'admin', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '系统管理员', 'admin@sugon.com', 1, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE username='admin');

INSERT INTO sys_user_role (user_id, role_id)
SELECT u.id, r.id FROM sys_user u, sys_role r
WHERE u.username='admin' AND r.role_code='ADMIN'
AND NOT EXISTS (SELECT 1 FROM sys_user_role ur WHERE ur.user_id=u.id AND ur.role_id=r.id);

-- 字典
INSERT INTO sys_dict (dict_type, dict_label, dict_value, sort) VALUES
('test_type','AI','AI',1),('test_type','CPU','CPU',2),('test_type','分布式存储','分布式存储',3),
('test_type','AI4S','AI4S',4),('test_type','GV+AI软件','GV+AI软件',5),('test_type','国产IB','国产IB',6),
('resource_type','循环机','循环机',1),('resource_type','线上508','线上508',2),
('resource_type','超算互联网','超算互联网',3),('resource_type','产品部机器','产品部机器',4),
('resource_type','客户机器','客户机器',5),('resource_type','其他资源','其他资源',6),
('reject_reason','测试费用超标','测试费用超标',1),('reject_reason','测试需求不合理','测试需求不合理',2),('reject_reason','其他','其他',3),
('bid_status','未招标','未招标',1),('bid_status','招标中','招标中',2),('bid_status','已中标','已中标',3),
('bid_status','未中标','未中标',4),('bid_status','流标待招','流标待招',5),('bid_status','其他','其他',6)
ON DUPLICATE KEY UPDATE dict_label=VALUES(dict_label);

-- 系统参数(借用规则阈值)
INSERT INTO sys_config (config_key, config_value, remark) VALUES
('loan.warn.days','30','借出超N天开始跟催'),
('loan.approve.days','90','借测超N天需魏总审批'),
('loan.force.days','365','超N天未还触发考核预警'),
('loan.dept.ratio','0.02','部门借用费用超部门任务比例'),
('loan.sales.ratio','0.02','个人借用金额超销售任务比例'),
('loan.follow.interval.days','14','跟催间隔(每两周)')
ON DUPLICATE KEY UPDATE config_value=VALUES(config_value);
