# FAE测试项目管理平台

战略客户服务部测试项目管理平台，用于替代原有的邮件审批 + 飞书表格统计方式，实现测试项目全生命周期线上管理。

## 功能概览

| 模块 | 功能 |
|------|------|
| 测试申请 | 售前提交测试申请（支持Word申请表导入），审批组审批，FAE负责人分配任务 |
| 审批中心 | 测试审批组审批、领导审批（>90天）、daihw分配FAE测试人员 |
| 项目清单 | 项目列表（自定义列、列宽拖拽、时间筛选），项目详情（编辑、状态变更、删除） |
| 阶段任务 | 项目测试阶段拆分，甘特图进度展示，按时间维度计算进度百分比 |
| 测试进展 | FAE填写测试日志/日报，项目维度时间线展示 |
| 日报 | 按日期汇总所有项目当日进展，审批组/管理层查看 |
| 周报 | FAE填写周报（本周进展/存在问题/下周计划），审批组查看全部 |
| 资源管理 | 资源池（设备上下线、序列号管理），借用/归还，项目完成自动回收 |
| 资产中心 | 销售/售前查看在线资产，关联项目借用 |
| 项目统计 | 可自定义仪表盘（11种统计维度 + 时间维度），PNG/PDF导出 |
| 用户管理 | 用户CRUD，Excel批量导入，角色/用户组分配 |
| 角色权限 | 9种角色RBAC权限控制，数据范围隔离（销售/售前/测试只能看自己的项目） |
| 消息通知 | 流程节点通知，点击跳转对应页面，未读/历史消息 |
| 字典管理 | 项目阶段、招标状态、测试方式、资源类型等字典维护 |

## 技术栈

| 层级 | 技术 |
|------|------|
| 前端 | Vue 3 + Element Plus + ECharts + Vite + Pinia |
| 后端 | Spring Boot 3 + MyBatis-Plus + Spring Security + JWT |
| 数据库 | MySQL 8.0 |
| 缓存 | Redis 7 |
| 文件存储 | MinIO |
| 部署 | Docker Compose |

## 项目结构

```
├── frontend/               # 前端（Vue3 + Element Plus）
│   ├── src/
│   │   ├── api/            # API 接口
│   │   ├── views/          # 页面组件
│   │   │   ├── application/   # 测试申请（申请表单、列表、审批中心）
│   │   │   ├── project/       # 项目清单、项目详情
│   │   │   ├── report/        # 日报汇总、周报
│   │   │   ├── resource/      # 资源池、借用管理、资产中心
│   │   │   ├── dashboard/     # 统计仪表盘
│   │   │   ├── system/        # 用户管理、用户组、字典
│   │   │   └── layout/        # 布局（菜单、通知）
│   │   ├── router/         # 路由（含权限守卫）
│   │   ├── store/          # Pinia 状态
│   │   └── utils/          # 工具函数
│   ├── Dockerfile
│   └── nginx.conf
├── backend/                # 后端（Spring Boot 3）
│   ├── src/main/java/com/sugon/testplatform/
│   │   ├── controller/     # REST API 控制器
│   │   ├── service/        # 业务逻辑接口
│   │   ├── service/impl/   # 业务逻辑实现
│   │   ├── entity/         # 数据库实体
│   │   ├── mapper/         # MyBatis Mapper
│   │   ├── dto/            # 请求/响应 DTO
│   │   ├── security/       # JWT 认证、数据权限
│   │   ├── config/         # 配置类
│   │   └── common/         # 通用类（Result、PageResult、异常）
│   ├── Dockerfile
│   └── pom.xml
├── deploy/
│   ├── schema.sql          # 数据库初始化脚本
│   └── alter_weekly_report.sql  # 增量更新脚本
├── docker-compose.yml      # Docker Compose 编排
├── .env.example            # 环境变量模板
├── backup.sh               # 备份脚本（数据库+文件+配置）
├── package_migration.sh    # 迁移打包脚本
├── install_node.sh         # 新节点安装脚本
└── gen_alter.py            # 增量SQL生成工具
```

## 快速部署

### 前置要求

- Linux 服务器（推荐 4C/8GB/100GB+）
- Docker & Docker Compose

### 部署步骤

```bash
# 1. 克隆代码
git clone https://github.com/charliedhw/FAEtest-Management-Platform.git
cd FAEtest-Management-Platform

# 2. 配置环境变量
cp .env.example .env
# 编辑 .env，修改数据库密码、Redis密码、JWT密钥等
vi .env

# 3. 启动所有服务
docker compose up -d

# 4. 查看状态
docker compose ps

# 5. 查看日志
docker compose logs -f backend
```

### 访问

- 平台地址：`http://<服务器IP>`
- 默认账号：`admin` / `Admin@123`

### 服务端口

| 服务 | 端口 | 说明 |
|------|------|------|
| frontend | 80 | Nginx 前端 + API 反向代理 |
| backend | 8080 | Spring Boot API（容器内部） |
| mysql | 3306 | MySQL 数据库（容器内部） |
| redis | 6379 | Redis 缓存（容器内部） |
| minio | 9000/9001 | MinIO 文件存储（容器内部） |

## 审批流程

```
售前提交申请 → 测试审批组审批 → (>90天? 领导审批) → daihw分配FAE → 测试执行
                    ↓                                      ↓
                  驳回                                   项目完成
```

## 角色说明

| 角色码 | 角色名 | 权限说明 |
|--------|--------|----------|
| ADMIN | 管理员 | 全部权限 |
| SALES | 销售 | 提交申请，查看自己的项目 |
| PRESALES | 售前 | 提交申请，查看自己的项目 |
| APPROVER | 测试审批组 | 审批申请，查看全部项目/日报/周报 |
| LEADER | 领导 | 长周期审批 |
| TESTER | FAE测试工程师 | 填写进展/周报，查看分配给自己的项目 |
| FAE_LEADER | FAE负责人 | 分配任务，查看全部项目/日报/周报 |
| RESOURCE_ADMIN | 资源管理员 | 资源池管理 |
| BUSINESS | 商务 | 查看项目 |

## 运维

### 备份

```bash
# 手动备份
./backup.sh

# 定时备份（每天凌晨2点）
crontab -e
0 2 * * * /opt/testplatform/backup.sh
```

### 迁移到新服务器

```bash
# 在原服务器打包
./package_migration.sh

# 在新服务器安装
./install_node.sh tp_migration_YYYYMMDD.tar.gz
```

## License

内部项目，不对外开放。
