#!/bin/bash
# =========================================================
# 测试项目管理平台 - 迁移打包脚本
# 在当前服务器上完整打包迁移到新节点所需的所有内容
# 用法: bash package_migration.sh
# 产物: /root/tp_migration_时间戳.tar.gz
# =========================================================
set -e

APP_DIR=/opt/testplatform
TS=$(date +%Y%m%d_%H%M%S)
WORK_DIR=/root/tp_migration_$TS
PKG_FILE=/root/tp_migration_$TS.tar.gz

echo "==================================================="
echo "  测试项目管理平台 - 迁移打包"
echo "  时间: $TS"
echo "==================================================="

# ---------- 准备工作目录 ----------
rm -rf $WORK_DIR
mkdir -p $WORK_DIR/{images,source,config,data}

cd $APP_DIR
set -a; source .env; set +a
DB_PASSWORD=$(echo -n "$DB_PASSWORD" | tr -d '\r')

# ---------- 1. 数据库导出 ----------
echo ""
echo "==> [1/6] 导出数据库 test_platform"
docker exec tp-mysql mysqldump -utestplatform -p"$DB_PASSWORD" \
  --default-character-set=utf8mb4 \
  --single-transaction --routines --triggers \
  test_platform 2>/dev/null | gzip > $WORK_DIR/data/test_platform.sql.gz
# 校验
if zcat $WORK_DIR/data/test_platform.sql.gz 2>/dev/null | grep -q "CREATE TABLE"; then
  echo "    数据库导出成功: $(du -h $WORK_DIR/data/test_platform.sql.gz | cut -f1)"
else
  echo "    [错误] 数据库导出失败!"
  exit 1
fi

# ---------- 2. MinIO 文件 ----------
echo ""
echo "==> [2/6] 打包 MinIO 报告文件"
if [ -d "$APP_DIR/data/minio" ] && [ "$(ls -A $APP_DIR/data/minio 2>/dev/null)" ]; then
  tar -czf $WORK_DIR/data/minio.tar.gz -C $APP_DIR/data minio
  echo "    MinIO打包成功: $(du -h $WORK_DIR/data/minio.tar.gz | cut -f1)"
else
  echo "    MinIO无数据,跳过"
fi

# ---------- 3. 导出 Docker 镜像 ----------
echo ""
echo "==> [3/6] 导出 Docker 镜像(自研镜像+基础镜像,新节点可离线部署)"
docker save \
  testplatform-backend \
  testplatform-frontend \
  mysql:8.0 \
  redis:7-alpine \
  minio/minio:latest \
  | gzip > $WORK_DIR/images/images.tar.gz
echo "    镜像导出成功: $(du -h $WORK_DIR/images/images.tar.gz | cut -f1)"

# ---------- 4. 源码 ----------
echo ""
echo "==> [4/6] 拷贝源码(用于后续构建升级)"
cp -r $APP_DIR/backend $WORK_DIR/source/
cp -r $APP_DIR/frontend $WORK_DIR/source/
# 清理构建产物减小体积
rm -rf $WORK_DIR/source/frontend/dist $WORK_DIR/source/frontend/node_modules
rm -rf $WORK_DIR/source/backend/target
echo "    源码拷贝完成"

# ---------- 5. 配置文件 ----------
echo ""
echo "==> [5/6] 拷贝配置与脚本"
cp $APP_DIR/.env $WORK_DIR/config/
cp $APP_DIR/docker-compose.yml $WORK_DIR/config/
cp -r $APP_DIR/deploy $WORK_DIR/config/
cp $APP_DIR/backup.sh $WORK_DIR/config/ 2>/dev/null || true
echo "    配置拷贝完成"

# ---------- 6. 生成清单与部署脚本 ----------
echo ""
echo "==> [6/6] 生成清单与新节点部署脚本"

# 清单
cat > $WORK_DIR/MANIFEST.txt <<EOF
测试项目管理平台 迁移包
打包时间: $TS
源服务器: $(hostname)
内容清单:
  images/images.tar.gz       - 全部Docker镜像(自研+基础,可离线导入)
  source/backend             - 后端源码
  source/frontend            - 前端源码
  config/.env                - 环境配置(含密码,注意保密)
  config/docker-compose.yml  - 编排文件
  config/deploy/schema.sql   - 数据库初始化脚本
  config/backup.sh           - 备份脚本
  data/test_platform.sql.gz  - 数据库全量导出
  data/minio.tar.gz          - 测试报告文件
部署: 解压后运行 bash install.sh
EOF

# 拷贝安装脚本(部署脚本单独维护)
cp $APP_DIR/install_node.sh $WORK_DIR/install.sh 2>/dev/null || true

# ---------- 打包 ----------
echo ""
echo "==> 打包迁移包"
tar -czf $PKG_FILE -C /root $(basename $WORK_DIR)
PKG_SIZE=$(du -h $PKG_FILE | cut -f1)

echo ""
echo "==================================================="
echo "  打包完成!"
echo "  迁移包: $PKG_FILE ($PKG_SIZE)"
echo "==================================================="
echo ""
echo "迁移包内容:"
ls -lh $WORK_DIR/images $WORK_DIR/data 2>/dev/null
echo ""
echo "下一步: 将迁移包拷贝到新节点"
echo "  scp $PKG_FILE root@<新节点IP>:/root/"
echo "  在新节点解压后运行: bash install.sh"
