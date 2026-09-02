-- 更新测试类型字典为固定选项（幂等）
-- 删除旧的 test_type 字典项
DELETE FROM sys_dict WHERE dict_type='test_type';

-- 插入新的固定测试类型选项
INSERT INTO sys_dict (dict_type, dict_label, dict_value, sort) VALUES
('test_type','BW1100','BW1100',1),
('test_type','BW1000','BW1000',2),
('test_type','BW100','BW100',3),
('test_type','BW150','BW150',4),
('test_type','K100AI','K100AI',5),
('test_type','HG 7495/7490','HG 7495/7490',6),
('test_type','HG 9459','HG 9459',7),
('test_type','国产IB','国产IB',8),
('test_type','分布式存储','分布式存储',9),
('test_type','集中式存储','集中式存储',10),
('test_type','GV+SothisAI','GV+SothisAI',11),
('test_type','超节点','超节点',12),
('test_type','其他','其他',13);
