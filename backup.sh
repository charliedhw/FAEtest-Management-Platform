#!/bin/bash
# =========================================================
# 测试项目管理平台 数据备份脚本
# 备份内容: MySQL数据库 + MinIO报告文件 + 配置文件
# 用法: bash backup.sh [保留天数]   (默认保留30天)
# =========================================================
set -e

# ---------- 配置 ----------
APP_DIR=/opt/testplatform
BACKUP_DIR=$APP_DIR/data/backup
KEEP_DAYS=${1:-30}          # 保留天数,默认30天
TS=$(date +%Y%m%d_%H%M%S)
LOG_FILE=$BACKUP_DIR/backup_$TS.log

mkdir -p $BACKUP_DIR

# 读取 .env 配置
if [ -f "$APP_DIR/.env" ]; then
  set -a; source $APP_DIR/.env; set +a
fi
# 去除 Windows CRLF 可能带入的回车符
MYSQL_ROOT_PASSWORD=$(echo -n "${MYSQL_ROOT_PASSWORD:-Sugon@root2026}" | tr -d '\r')
DB_PASSWORD=$(echo -n "${DB_PASSWORD:-Sugon@test2026}" | tr -d '\r')

# 日志函数(同时输出到控制台和日志文件)
log() {
  echo "[$(date '+%Y-%m-%d %H:%M:%S')] $1" | tee -a $LOG_FILE
}

log "========== 开始备份 =========="
log "保留天数: $KEEP_DAYS 天"

# ---------- 1. 备份 MySQL 数据库 ----------
log "==> 备份 MySQL 数据库 test_platform"
DB_FILE=$BACKUP_DIR/db_test_platform_$TS.sql.gz
docker exec tp-mysql mysqldump -utestplatform -p"$DB_PASSWORD" \
  --default-character-set=utf8mb4 \
  --single-transaction --routines --triggers \
  test_platform 2>/dev/null | gzip > "$DB_FILE"

# 校验数据库备份非空(解压后含SQL关键字)
if [ -s "$DB_FILE" ] && zcat "$DB_FILE" 2>/dev/null | grep -q "CREATE TABLE"; then
  DB_SIZE=$(du -h "$DB_FILE" | cut -f1)
  log "    数据库备份成功: $DB_FILE ($DB_SIZE)"
else
  log "    [错误] 数据库备份失败或文件为空: $DB_FILE"
  exit 1
fi

# ---------- 2. 备份 MinIO 报告文件 ----------
log "==> 备份 MinIO 报告文件"
MINIO_FILE=$BACKUP_DIR/minio_$TS.tar.gz
if [ -d "$APP_DIR/data/minio" ]; then
  tar -czf "$MINIO_FILE" -C $APP_DIR/data minio 2>/dev/null || true
  MINIO_SIZE=$(du -h "$MINIO_FILE" 2>/dev/null | cut -f1)
  log "    MinIO备份成功: $MINIO_FILE (${MINIO_SIZE:-0})"
else
  log "    [警告] MinIO数据目录不存在,跳过"
fi

# ---------- 3. 备份配置文件 ----------
log "==> 备份配置文件"
CONFIG_FILE=$BACKUP_DIR/config_$TS.tar.gz
tar -czf "$CONFIG_FILE" -C $APP_DIR .env docker-compose.yml deploy 2>/dev/null || true
log "    配置备份成功: $CONFIG_FILE"

# ---------- 4. 清理过期备份 ----------
log "==> 清理 $KEEP_DAYS 天前的旧备份"
DELETED=$(find $BACKUP_DIR -name "db_test_platform_*.sql.gz" -mtime +$KEEP_DAYS -print -delete | wc -l)
DELETED=$((DELETED + $(find $BACKUP_DIR -name "minio_*.tar.gz" -mtime +$KEEP_DAYS -print -delete | wc -l)))
DELETED=$((DELETED + $(find $BACKUP_DIR -name "config_*.tar.gz" -mtime +$KEEP_DAYS -print -delete | wc -l)))
DELETED=$((DELETED + $(find $BACKUP_DIR -name "backup_*.log" -mtime +$KEEP_DAYS -print -delete | wc -l)))
log "    清理旧备份文件: $DELETED 个"

# ---------- 5. 备份汇总 ----------
log "==> 备份汇总"
BACKUP_SIZE=$(du -sh $BACKUP_DIR 2>/dev/null | cut -f1)
log "    备份目录总大小: $BACKUP_SIZE"
log "========== 备份完成 =========="

# 输出本次备份文件清单
echo ""
echo "本次备份文件:"
ls -lh $BACKUP_DIR/*$TS* 2>/dev/null
