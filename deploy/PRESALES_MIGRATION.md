# 售前组织架构与权限迁移方案（正式平台）

## 一、迁移内容

1. 新增角色 **PRESALES_LEADER（售前负责人）**：张磊、李晓鹏、高琦、陈华新，可见全部售前测试项目。
2. **三级组织结构**（组长可见范围 = 本组成员 + 所有下级组成员，递归）：
   - **售前大组**（PRESALES_ROOT，复用现有 PRESALES_GROUP“售前组”作为根，原大组成员关系保留）
     - **区域业务部**（总负责人 李国君）：浙江&安徽(王绍祥)、上海(邓密海)、江苏&山东(孙思宇)、广东&深圳(宁迁树)、湖南&湖北(覃玲俐)、西南(王德志)、西北(乌劭宇)、东北(彭欢)
     - **综合业务部**（总负责人 赵铭）：综合一(赵铭)、综合二(吕磊)
     - **创新方案部**（总负责人 邹强利）：地学(邹强利)、AI组(李冉)、仿真组(姚浪)、生信医疗组(肖贵宝)、存储组(向小康)、数理化材组(冯施施)
   - 组员只挂**叶子子组**；无子组归属的业务部人员（李国君）直接挂业务部
3. 账号处理（正式平台售前人员不全）：
   - 优先按 **itcode（用户名）** 匹配已有账号，**已有账号的密码、信息一律不动**；
   - itcode 未命中时按**姓名**匹配（仅当姓名唯一）；重名的跳过并输出清单人工确认；
   - 都不存在的**新建账号**，用户名=itcode，初始密码=平台默认密码 `Sugon@123`，首次登录标记=1；
   - 全员补 PRESALES 角色，4 位负责人补 PRESALES_LEADER 角色。
4. 权限效果：
   - 子组组长：项目清单/项目统计/日报中可见**本子组**售前的项目；
   - 业务部总负责人：可见**本部全部**售前的项目（沿层级向下汇总）；
   - 售前大组负责人（如需指定）：可见全部售前项目；
   - 售前负责人（PRESALES_LEADER 角色）：可见**全部**项目；
   - 普通售前：仍只能看自己相关项目，不受影响。

脚本 `deploy/presales_migrate.py` **幂等**，可重复执行，已建账号/组/关系自动跳过。

## 二、前置条件

- 正式平台后端已升级到包含本次改动的版本（B1 审批/分配/编辑角色校验 + B3 数据范围统一 + 组层级）。
- 执行一次结构 SQL：`deploy/alter_group_hierarchy.sql`（sys_user_group 增加 parent_id 列，可重复执行会报列已存在，忽略即可）。

## 三、执行步骤（正式平台）

```bash
# 1. 更新代码并仅重建后端（前端无改动则不用重建）
cd /sugongdxt/testplatform
git status --short          # 有本地修改先处理，禁止 git reset --hard
git pull --ff-only origin main
docker compose build backend && docker compose up -d --no-deps backend

# 2. 组层级 SQL + 执行迁移脚本（自动从 .env 读取数据库密码）
docker exec -i tp-mysql mysql -utestplatform -p$(grep '^DB_PASSWORD=' .env | cut -d= -f2) test_platform < deploy/alter_group_hierarchy.sql
python3 deploy/presales_migrate.py /sugongdxt/testplatform/.env
```

脚本输出关注：
- `新建账号 N`：新创建的售前账号清单（初始密码 Sugon@123，通知本人修改）；
- `按姓名匹配已有账号 N`：itcode 不同但姓名匹配上的账号，核对是否正确；
- `!! 姓名重名未处理`：需人工指定账号后再重跑脚本（脚本幂等）；
- `!! @sugom.com 邮箱`：源表中李国君邮箱为 `ligja@sugom.com`，疑为 `@sugon.com` 笔误，请人工核对修正。

## 四、验证

1. 用任意子组组长账号登录（如 wangsx / Sugon@123），项目清单只显示本子组售前项目；
2. 用总负责人账号（ligja / zhaoming1 / zouql）登录，可见本部全部售前项目（含所有下级子组）；
3. 用售前负责人账号（zhanglei2 等）登录，项目清单为全部项目；
4. 项目清单总数与"项目统计"卡片总数一致（B3 口径统一）；
5. 普通售前账号可见范围无变化。

## 五、回滚

脚本只新增/更新以下数据，回滚按需执行：

```sql
-- 删除新建的组及成员关系（不影响已有 PRESALES_GROUP 等）
DELETE r FROM sys_user_group_rel r JOIN sys_user_group g ON r.group_id=g.id
 WHERE g.group_code LIKE 'PD_%' OR g.group_code LIKE 'PR_%' OR g.group_code LIKE 'PS_%' OR g.group_code LIKE 'PI_%';
DELETE FROM sys_user_group WHERE group_code LIKE 'PD_%' OR group_code LIKE 'PR_%'
    OR group_code LIKE 'PS_%' OR group_code LIKE 'PI_%';
-- 移除角色分配（保留角色定义无影响）
DELETE ur FROM sys_user_role ur JOIN sys_role r ON ur.role_id=r.id WHERE r.role_code='PRESALES_LEADER';
-- 新建账号如需删除：按脚本输出清单逐个核对后删除（勿删匹配到的已有账号）
```
