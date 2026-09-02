-- 撤销误改的 test_type 字典，恢复为原测试类型选项（幂等）
DELETE FROM sys_dict WHERE dict_type='test_type';
INSERT INTO sys_dict (dict_type, dict_label, dict_value, sort) VALUES
('test_type','AI','AI',1),
('test_type','CPU','CPU',2),
('test_type','分布式存储','分布式存储',3),
('test_type','AI4S','AI4S',4),
('test_type','GV+AI软件','GV+AI软件',5),
('test_type','国产IB','国产IB',6);

-- 设备类型设置为固定13个选项（幂等）
DELETE FROM sys_dict WHERE dict_type='device_type';
INSERT INTO sys_dict (dict_type, dict_label, dict_value, sort) VALUES
('device_type','BW1100','BW1100',1),
('device_type','BW1000','BW1000',2),
('device_type','BW100','BW100',3),
('device_type','BW150','BW150',4),
('device_type','K100AI','K100AI',5),
('device_type','HG 7495/7490','HG 7495/7490',6),
('device_type','HG 9459','HG 9459',7),
('device_type','国产IB','国产IB',8),
('device_type','分布式存储','分布式存储',9),
('device_type','集中式存储','集中式存储',10),
('device_type','GV+SothisAI','GV+SothisAI',11),
('device_type','超节点','超节点',12),
('device_type','其他','其他',13);
