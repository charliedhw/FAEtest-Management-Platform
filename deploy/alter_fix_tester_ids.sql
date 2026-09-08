-- 修复历史项目 tester_ids 与 tester_names 错位（Excel导入脏数据）
-- 按 tester_names 中姓名匹配 FAE 测试组成员重建 tester_ids，仅处理能匹配到的人员
-- 田鹏飞(id=191) 等此前因 ids 缺失/错位导致登录后看不到名下项目
SET NAMES utf8mb4;
UPDATE test_project SET tester_ids='191,199,201' WHERE id=2;
UPDATE test_project SET tester_ids='192,191' WHERE id=10;
UPDATE test_project SET tester_ids='192,191' WHERE id=15;
UPDATE test_project SET tester_ids='191' WHERE id=16;
UPDATE test_project SET tester_ids='191,192' WHERE id=17;
UPDATE test_project SET tester_ids='194,191' WHERE id=19;
UPDATE test_project SET tester_ids='193,191' WHERE id=24;
UPDATE test_project SET tester_ids='191' WHERE id=73;
UPDATE test_project SET tester_ids='191,192' WHERE id=77;
