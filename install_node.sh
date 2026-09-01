#!/bin/bash
# =========================================================
# 测试项目管理平台 - 新节点一键安装部署脚本
# 在目标 Linux 服务器上,解压迁移包后运行本脚本
# 用法: bash install.sh
# =========================================================
set -e

# 迁移包解压后的目录(本脚本所在目录)
MIG_DIR=$(cd "$(dirname "$0")" && pwd)
APP_DIR=/opt/testplatform
GREEN='\033[0;32m'; RED='\033[0;31m'; NC='\033[0m'

log() { echo -e "${GREEN}[安装]${NC} $1"; }
err() { echo -e "${RED}[错误]${NC} $1"; }

echo "==================================================="
echo "  测试项目管理平台 - 新节点安装部署"
echo "==================================================="

# ---------- 0. 环境检查 ----------
log "[0/7] 检查运行环境"
if [ "$EUID" -ne 0 ]; then
  err "请使用 root 用户运行"
  exit 1
fi

# ---------- 1. 安装 Docker(如未安装) ----------
if ! command -v docker &> /dev/null; then
  log "[1/7] Docker 未安装,开始安装"
  # 检测包管理器
  if command -v dnf &> /dev/null; then
    dnf install -y yum-utils device-mapper-persistent-data lvm2
    yum-config-manager --add-repo https://mirrors.aliyun.com/docker-ce/linux/centos/docker-ce.repo 2>/dev/null || true
    sed -i 's|download.docker.com|mirrors.aliyun.com/docker-ce|g' /etc/yum.repos.d/docker-ce.repo 2>/dev/null || true
    dnf install -y --allowerasing docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
  elif command -v apt-get &> /dev/null; then
    apt-get update
    apt-get install -y docker.io docker-compose-plugin || apt-get install -y docker.io docker-compose
  else
    err "无法识别的包管理器,请手动安装 Docker 后重试"
    exit 1
  fi
else
  log "[1/7] Docker 已安装: $(docker --version)"
fi

# ---------- 2. 配置 Docker ----------
log "[2/7] 配置 Docker (镜像加速/DNS/日志)"
mkdir -p /etc/docker
cat > /etc/docker/daemon.json <<'EOF'
{
  "registry-mirrors": ["https://docker.m.daocloud.io", "https://dockerproxy.net"],
  "dns": ["114.114.114.114", "8.8.8.8"],
  "log-driver": "json-file",
  "log-opts": {"max-size": "50m", "max-file": "3"}
}
EOF
systemctl enable --now docker
systemctl restart docker
sleep 3

# ---------- 3. 导入镜像 ----------
log "[3/7] 导入 Docker 镜像(离线)"
if [ -f "$MIG_DIR/images/images.tar.gz" ]; then
  docker load < $MIG_DIR/images/images.tar.gz
  log "    镜像导入完成"
else
  err "未找到镜像包 images.tar.gz"
  exit 1
fi

# ---------- 4. 部署目录与配置 ----------
log "[4/7] 部署应用目录与配置"
mkdir -p $APP_DIR/data/{mysql,redis,minio,backend/logs,backup}
# 拷贝配置
cp $MIG_DIR/config/.env $APP_DIR/
cp $MIG_DIR/config/docker-compose.yml $APP_DIR/
cp -r $MIG_DIR/config/deploy $APP_DIR/ 2>/dev/null || true
cp $MIG_DIR/config/backup.sh $APP_DIR/ 2>/dev/null || true
chmod +x $APP_DIR/backup.sh 2>/dev/null || true
# 拷贝源码(用于后续升级)
cp -r $MIG_DIR/source/backend $APP_DIR/ 2>/dev/null || true
cp -r $MIG_DIR/source/frontend $APP_DIR/ 2>/dev/null || true
# 统一将文本配置文件转为 Linux LF 格式(去除 Windows CRLF 回车符,避免密码等值带\r导致认证失败)
for f in $APP_DIR/.env $APP_DIR/docker-compose.yml $APP_DIR/backup.sh; do
  [ -f "$f" ] && sed -i 's/\r$//' "$f"
done
log "    配置部署完成"

# ---------- 5. 启动基础服务 ----------
log "[5/7] 启动基础服务 (mysql/redis/minio)"
cd $APP_DIR
docker compose up -d mysql redis minio
log "    等待 MySQL 就绪..."
for i in $(seq 1 30); do
  if docker exec tp-mysql mysqladmin ping -h localhost >/dev/null 2>&1; then
    log "    MySQL 已就绪"
    break
  fi
  sleep 2
done

# ---------- 6. 恢复数据 ----------
log "[6/7] 恢复数据库与文件"
# 读取密码(统一去除 .env CRLF 带入的回车符)
set -a; source $APP_DIR/.env; set +a
DB_PASSWORD=$(echo -n "$DB_PASSWORD" | tr -d '\r')
MYSQL_ROOT_PASSWORD=$(echo -n "$MYSQL_ROOT_PASSWORD" | tr -d '\r')

# 恢复数据库(优先用 root,失败则用 testplatform)
if [ -f "$MIG_DIR/data/test_platform.sql.gz" ]; then
  gunzip -c $MIG_DIR/data/test_platform.sql.gz > /tmp/restore.sql
  log "    正在导入数据库..."
  if docker exec -i tp-mysql mysql -uroot -p"$MYSQL_ROOT_PASSWORD" \
      --default-character-set=utf8mb4 test_platform < /tmp/restore.sql 2>/dev/null; then
    log "    使用 root 导入成功"
  else
    log "    root 导入失败,尝试 testplatform 账号"
    docker exec -i tp-mysql mysql -utestplatform -p"$DB_PASSWORD" \
      --default-character-set=utf8mb4 test_platform < /tmp/restore.sql
  fi
  PROJ_COUNT=$(docker exec -i tp-mysql mysql -utestplatform -p"$DB_PASSWORD" test_platform -N -e "SELECT COUNT(*) FROM test_project" 2>/dev/null)
  log "    数据库恢复完成, 项目数: $PROJ_COUNT"
  rm -f /tmp/restore.sql
else
  log "    无数据库备份,将使用初始化脚本建表"
fi

# 恢复 MinIO 文件
if [ -f "$MIG_DIR/data/minio.tar.gz" ]; then
  tar -xzf $MIG_DIR/data/minio.tar.gz -C $APP_DIR/data/
  log "    MinIO 文件恢复完成"
fi

# ---------- 7. 启动应用 ----------
log "[7/7] 启动应用服务"
docker compose up -d
sleep 5

echo ""
echo "==================================================="
echo "  安装部署完成!"
echo "==================================================="
docker compose ps
echo ""
IP=$(hostname -I | awk '{print $1}')
echo "访问地址: http://$IP"
echo "管理员账号: admin / Admin@123"
echo ""
echo "验证: curl http://localhost/ 应返回前端页面"
