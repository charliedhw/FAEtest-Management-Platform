#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
测试项目管理平台 - alter.sql 自动生成脚本
原理: 对比开发库与正式库的表结构差异, 自动生成幂等的增量变更脚本(alter.sql)

用法:
  python gen_alter.py --dev-host 127.0.0.1 --prod-host <正式库IP> \
      --prod-user testplatform --prod-pass <正式库密码>

参数说明:
  --dev-host   开发库地址(默认 127.0.0.1, 本机docker内mysql)
  --dev-port   开发库端口(默认 3306)
  --dev-user   开发库账号(默认 testplatform)
  --dev-pass   开发库密码(默认从 /opt/testplatform/.env 读取)
  --prod-host  正式库地址(必填)
  --prod-port  正式库端口(默认 3306)
  --prod-user  正式库账号(默认 testplatform)
  --prod-pass  正式库密码(必填, 或从环境变量 PROD_DB_PASSWORD 读取)
  --db         数据库名(默认 test_platform)
  --out        输出文件(默认 deploy/alter_当天日期.sql)

注意: 需要在能同时连通开发库和正式库的机器上运行。
      在服务器上可用 python 容器运行:
      docker run --rm --network testplatform_tp-net -v /opt/testplatform:/work python:3.11-slim \
        sh -c 'pip install -q pymysql -i https://mirrors.aliyun.com/pypi/simple/ && python /work/deploy/gen_alter.py --dev-host mysql --prod-host <正式IP> --prod-pass <密码>'
"""
import sys, os, argparse, datetime, re

sys.stdout.reconfigure(encoding="utf-8", errors="replace")

try:
    import pymysql
except ImportError:
    print("缺少 pymysql, 请先安装: pip install pymysql")
    sys.exit(1)


def read_env_password():
    """从 .env 读取 DB_PASSWORD 作为开发库密码默认值"""
    env_file = "/opt/testplatform/.env"
    if os.path.exists(env_file):
        with open(env_file, encoding="utf-8") as f:
            for line in f:
                line = line.strip().rstrip("\r")
                if line.startswith("DB_PASSWORD="):
                    return line.split("=", 1)[1].strip()
    return None


def get_conn(host, port, user, password, db):
    return pymysql.connect(host=host, port=port, user=user, password=password,
                           database=db, charset="utf8mb4")


def get_tables(conn):
    """获取所有表名"""
    cur = conn.cursor()
    cur.execute("SHOW TABLES")
    return set(r[0] for r in cur.fetchall())


def get_columns(conn, table):
    """获取表的列定义: {列名: 完整列定义SQL片段}"""
    cur = conn.cursor()
    cur.execute(f"SHOW FULL COLUMNS FROM `{table}`")
    cols = {}
    for row in cur.fetchall():
        # Field, Type, Collation, Null, Key, Default, Extra, ..., Comment
        field, col_type, collation, null, key, default, extra = row[0], row[1], row[2], row[3], row[4], row[5], row[6]
        comment = row[8] if len(row) > 8 else ""
        cols[field] = {
            "type": col_type, "null": null, "default": default,
            "extra": extra, "comment": comment
        }
    return cols


def get_create_table(conn, table):
    """获取建表语句"""
    cur = conn.cursor()
    cur.execute(f"SHOW CREATE TABLE `{table}`")
    return cur.fetchone()[1]


def col_def_sql(col, info, after_col=None):
    """生成 ADD COLUMN 语句"""
    s = f"ADD COLUMN `{col}` {info['type']}"
    s += " NULL" if info["null"] == "YES" else " NOT NULL"
    if info["default"] is not None:
        s += f" DEFAULT '{info['default']}'"
    if info["extra"]:
        s += f" {info['extra']}"
    if info.get("comment"):
        s += f" COMMENT '{info['comment']}'"
    if after_col:
        s += f" AFTER `{after_col}`"
    return s


def wrap_idempotent_col(table, col, alter_body):
    """把 ADD COLUMN 包装成幂等(判断字段是否存在)"""
    return f"""SET @has_{table}_{col} := (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='{table}' AND COLUMN_NAME='{col}');
SET @sql := IF(@has_{table}_{col}=0, 'ALTER TABLE `{table}` {alter_body}', 'SELECT ''{table}.{col} 已存在,跳过'' AS tip');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;"""


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--dev-host", default="127.0.0.1")
    ap.add_argument("--dev-port", type=int, default=3306)
    ap.add_argument("--dev-user", default="testplatform")
    ap.add_argument("--dev-pass", default=None)
    ap.add_argument("--prod-host", required=True)
    ap.add_argument("--prod-port", type=int, default=3306)
    ap.add_argument("--prod-user", default="testplatform")
    ap.add_argument("--prod-pass", default=os.environ.get("PROD_DB_PASSWORD"))
    ap.add_argument("--db", default="test_platform")
    ap.add_argument("--out", default=None)
    args = ap.parse_args()

    dev_pass = args.dev_pass or read_env_password()
    if not dev_pass:
        print("错误: 未提供开发库密码, 且 .env 中未读取到 DB_PASSWORD")
        sys.exit(1)
    if not args.prod_pass:
        print("错误: 未提供正式库密码(--prod-pass 或环境变量 PROD_DB_PASSWORD)")
        sys.exit(1)

    today = datetime.datetime.now().strftime("%Y%m%d")
    out_file = args.out or f"/opt/testplatform/deploy/alter_{today}.sql"

    print(f"开发库: {args.dev_host}:{args.dev_port}")
    print(f"正式库: {args.prod_host}:{args.prod_port}")
    print(f"数据库: {args.db}")
    print(f"输出:   {out_file}")

    # 连接两个库
    dev = get_conn(args.dev_host, args.dev_port, args.dev_user, dev_pass, args.db)
    prod = get_conn(args.prod_host, args.prod_port, args.prod_user, args.prod_pass, args.db)

    dev_tables = get_tables(dev)
    prod_tables = get_tables(prod)

    stmts = []
    stmts.append("-- =========================================================")
    stmts.append(f"-- 数据库结构增量变更脚本(自动生成)  版本: v{today}")
    stmts.append(f"-- 对比: 开发库({args.dev_host}) -> 正式库({args.prod_host})")
    stmts.append("-- 说明: 本脚本幂等,可重复执行")
    stmts.append("-- =========================================================\n")

    # 1. 新增表(开发有,正式没有)
    new_tables = dev_tables - prod_tables
    for t in sorted(new_tables):
        stmts.append(f"-- 新增表: {t}")
        stmts.append(get_create_table(dev, t) + ";\n")

    # 2. 已有表的字段差异
    common_tables = dev_tables & prod_tables
    for t in sorted(common_tables):
        dev_cols = get_columns(dev, t)
        prod_cols = get_columns(prod, t)
        new_cols = [c for c in dev_cols if c not in prod_cols]
        if not new_cols:
            continue
        stmts.append(f"-- 表 {t} 新增字段")
        # 保持字段顺序, AFTER 用前一个已存在的字段
        dev_col_list = list(dev_cols.keys())
        for c in new_cols:
            idx = dev_col_list.index(c)
            after = dev_col_list[idx - 1] if idx > 0 else None
            body = col_def_sql(c, dev_cols[c], after)
            stmts.append(wrap_idempotent_col(t, c, body))
            stmts.append("")

    if len(stmts) <= 5:
        stmts.append("-- 本次无结构差异")

    stmts.append("SELECT 'alter.sql 执行完成' AS result;")

    content = "\n".join(stmts)
    os.makedirs(os.path.dirname(out_file), exist_ok=True)
    with open(out_file, "w", encoding="utf-8") as f:
        f.write(content)

    print(f"\n生成完成: {out_file}")
    print(f"新增表: {len(new_tables)} 个, 涉及字段变更的表已写入脚本")
    dev.close(); prod.close()


if __name__ == "__main__":
    main()
