#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
售前组织与权限迁移脚本（开发/正式平台通用，幂等，可重复执行）
用法:  python3 presales_migrate.py [env文件路径，默认 /opt/testplatform/.env]
内容:
  1. 新增角色 PRESALES_LEADER(售前负责人，可见全部售前项目)
  2. 按 itcode(优先)/姓名(兜底唯一匹配) 匹配已有账号；缺失的按平台默认密码 Sugon@123 创建
  3. 全部售前人员补 PRESALES 角色；张磊/李晓鹏/高琦/陈华新 补 PRESALES_LEADER 角色
  4. 三级组织结构：售前大组(PRESALES_ROOT，复用PRESALES_GROUP) -> 业务部(PRESALES_DEPT) -> 子组(PRESALES_REGION)，
     组长可见范围 = 本组成员 + 所有下级组成员（递归）
  5. 组员关系落库：成员只放叶子子组；无子组归属的业务部人员直接挂业务部
"""
import json
import os
import subprocess
import sys

ORG = json.loads(r'''{"people": [{"dept": "区域业务部李国君", "name": "李国君", "inner": "李国君", "itcode": "ligja", "email": "ligja@sugom.com", "group": "区域组负责人"}, {"dept": "区域业务部李国君", "name": "王绍祥", "inner": "王绍祥", "itcode": "wangsx", "email": "wangsx@sugon.com", "group": "浙江&安徽"}, {"dept": "区域业务部李国君", "name": "肖雯", "inner": "肖雯", "itcode": "xiaowen", "email": "xiaowen@sugon.com", "group": "浙江&安徽"}, {"dept": "区域业务部李国君", "name": "钟文洁", "inner": "钟文洁", "itcode": "xiaoyj", "email": "zhongwj@sugon.com", "group": "浙江&安徽"}, {"dept": "区域业务部李国君", "name": "张健", "inner": "张健_分公司", "itcode": "zhangjian7", "email": "zhangjian7@sugon.com", "group": "浙江&安徽"}, {"dept": "区域业务部李国君", "name": "马越", "inner": "马越_解决", "itcode": "mayue1", "email": "mayue1@sugon.com", "group": "浙江&安徽"}, {"dept": "区域业务部李国君", "name": "邓密海", "inner": "邓密海", "itcode": "dengmh", "email": "dengmh@sugon.com", "group": "上海"}, {"dept": "区域业务部李国君", "name": "桂子坤", "inner": "桂子坤", "itcode": "guizk", "email": "guizk@sugon.com", "group": "上海"}, {"dept": "区域业务部李国君", "name": "郭佳明", "inner": "郭佳明", "itcode": "guojm1", "email": "guojm1@sugon.com", "group": "上海"}, {"dept": "区域业务部李国君", "name": "任玉军", "inner": "任玉军", "itcode": "renyj3", "email": "renyj3@sugon.com", "group": "上海"}, {"dept": "区域业务部李国君", "name": "赵琳", "inner": "赵琳", "itcode": "zhaolin1", "email": "zhaolin1@sugon.com", "group": "上海"}, {"dept": "区域业务部李国君", "name": "覃小刚", "inner": "覃小刚", "itcode": "qinxg", "email": "qinxg@sugon.com", "group": "上海"}, {"dept": "区域业务部李国君", "name": "孙思宇", "inner": "孙思宇", "itcode": "sunsy", "email": "sunsy@sugon.com", "group": "江苏&山东"}, {"dept": "区域业务部李国君", "name": "李栋", "inner": "李栋", "itcode": "lidong2", "email": "lidong2@sugon.com", "group": "江苏&山东"}, {"dept": "区域业务部李国君", "name": "李骏奡", "inner": "李骏奡", "itcode": "lijao", "email": "lijao@sugon.com", "group": "江苏&山东"}, {"dept": "区域业务部李国君", "name": "侯乐怡", "inner": "侯乐怡", "itcode": "houly", "email": "houly@sugon.com", "group": "江苏&山东"}, {"dept": "区域业务部李国君", "name": "李志强", "inner": "李志强", "itcode": "lizhq1", "email": "lizhq1@sugon.com", "group": "江苏&山东"}, {"dept": "区域业务部李国君", "name": "马祥瑞", "inner": "马祥瑞", "itcode": "maxr", "email": "maxr@sugon.com", "group": "江苏&山东"}, {"dept": "区域业务部李国君", "name": "王晓珍", "inner": "王晓珍", "itcode": "wangxzh3", "email": "wangxzh3@sugon.com", "group": "江苏&山东"}, {"dept": "区域业务部李国君", "name": "卞悦津", "inner": "卞悦津", "itcode": "bianyj", "email": "bianyj@sugon.com", "group": "江苏&山东"}, {"dept": "区域业务部李国君", "name": "宁迁树", "inner": "宁迁树", "itcode": "ningqsh", "email": "ningqsh@sugon.com", "group": "广东&深圳"}, {"dept": "区域业务部李国君", "name": "林贵福", "inner": "林贵福", "itcode": "lingf", "email": "lingf@sugon.com", "group": "广东&深圳"}, {"dept": "区域业务部李国君", "name": "刘瑜兴", "inner": "刘瑜兴", "itcode": "liuyx6", "email": "liuyx6@sugon.com", "group": "广东&深圳"}, {"dept": "区域业务部李国君", "name": "叶国烽", "inner": "叶国烽", "itcode": "yegf", "email": "yegf@sugon.com", "group": "广东&深圳"}, {"dept": "区域业务部李国君", "name": "江良良", "inner": "江良良", "itcode": "jiangll1", "email": "jiangll1@sugon.com", "group": "广东&深圳"}, {"dept": "区域业务部李国君", "name": "肖婷", "inner": "肖婷", "itcode": "xiaoting1", "email": "xiaoting1@sugon.com", "group": "广东&深圳"}, {"dept": "区域业务部李国君", "name": "覃玲俐", "inner": "覃玲俐", "itcode": "qinll", "email": "qinll@sugon.com", "group": "湖南&湖北"}, {"dept": "区域业务部李国君", "name": "马天", "inner": "马天", "itcode": "matian", "email": "matian@sugon.com", "group": "湖南&湖北"}, {"dept": "区域业务部李国君", "name": "黄保尚", "inner": "黄保尚", "itcode": "huangbsh", "email": "huangbsh@sugon.com", "group": "湖南&湖北"}, {"dept": "区域业务部李国君", "name": "徐许涛", "inner": "徐许涛", "itcode": "xuxt1", "email": "xuxt1@sugon.com", "group": "湖南&湖北"}, {"dept": "区域业务部李国君", "name": "刘哲", "inner": "刘哲_高性能", "itcode": "liuzhe1", "email": "liuzhe1@sugon.com", "group": "湖南&湖北"}, {"dept": "区域业务部李国君", "name": "张金怀", "inner": "张金怀", "itcode": "zhangjh2", "email": "zhangjh2@sugon.com", "group": "湖南&湖北"}, {"dept": "区域业务部李国君", "name": "张华", "inner": "张华", "itcode": "zhanghua", "email": "zhanghua@sugon.com", "group": "湖南&湖北"}, {"dept": "区域业务部李国君", "name": "王德志", "inner": "王德志", "itcode": "wangdzh1", "email": "wangdzh1@sugon.com", "group": "西南"}, {"dept": "区域业务部李国君", "name": "潘家松", "inner": "潘家松", "itcode": "panjs", "email": "panjs@sugon.com", "group": "西南"}, {"dept": "区域业务部李国君", "name": "张黎", "inner": "张黎", "itcode": "zhanglib", "email": "zhanglib@sugon.com", "group": "西南"}, {"dept": "区域业务部李国君", "name": "严贵宾", "inner": "严贵宾", "itcode": "yanggb", "email": "yanggb@sugon.com", "group": "西南"}, {"dept": "区域业务部李国君", "name": "宋达", "inner": "宋达", "itcode": "songda", "email": "songda@sugon.com", "group": "西南"}, {"dept": "区域业务部李国君", "name": "乌劭宇", "inner": "乌劭宇", "itcode": "wushy", "email": "wushy@sugon.com", "group": "西北"}, {"dept": "区域业务部李国君", "name": "刘伟", "inner": "刘伟_陕西", "itcode": "liuwei", "email": "liuwei@sugon.com", "group": "西北"}, {"dept": "区域业务部李国君", "name": "蒋宜海", "inner": "蒋宜海", "itcode": "jiangyh", "email": "jiangyh@sugon.com", "group": "西北"}, {"dept": "区域业务部李国君", "name": "彭欢", "inner": "彭欢", "itcode": "penghuan", "email": "penghuan@sugon.com", "group": "东北"}, {"dept": "区域业务部李国君", "name": "张美巍", "inner": "张美巍", "itcode": "zhangmw", "email": "zhangmw@sugon.com", "group": "东北"}, {"dept": "综合业务部赵铭", "name": "赵铭", "inner": "赵铭_解决", "itcode": "zhaoming1", "email": "zhaoming1@sugon.com", "group": "综合一"}, {"dept": "综合业务部赵铭", "name": "罗宾", "inner": "罗宾", "itcode": "luobin", "email": "luobin@sugon.com", "group": "综合一"}, {"dept": "综合业务部赵铭", "name": "靳晓辉", "inner": "靳晓辉", "itcode": "jinxiaohui", "email": "jinxiaohui@sugon.com", "group": "综合一"}, {"dept": "综合业务部赵铭", "name": "王玉枝", "inner": "王玉枝", "itcode": "wangyzh3", "email": "wangyzh3@sugon.com", "group": "综合一"}, {"dept": "综合业务部赵铭", "name": "单大伟", "inner": "单大伟", "itcode": "shandw", "email": "shandw@sugon.com", "group": "综合一"}, {"dept": "综合业务部赵铭", "name": "张强", "inner": "张强_解决", "itcode": "zhangqiang3", "email": "zhangqiang3@sugon.com", "group": "综合一"}, {"dept": "综合业务部赵铭", "name": "隋新", "inner": "隋新", "itcode": "suixin", "email": "suixin@sugon.com", "group": "综合一"}, {"dept": "综合业务部赵铭", "name": "王挺进", "inner": "王挺进", "itcode": "wangtj1", "email": "wangtj1@sugon.com", "group": "综合一"}, {"dept": "综合业务部赵铭", "name": "赵雪豹", "inner": "赵雪豹", "itcode": "zhaoxb", "email": "zhaoxb@sugon.com", "group": "综合一"}, {"dept": "综合业务部赵铭", "name": "侯伟杰", "inner": "侯伟杰", "itcode": "houwj", "email": "houwj@sugon.com", "group": "综合一"}, {"dept": "综合业务部赵铭", "name": "吕磊", "inner": "吕磊", "itcode": "lvlei", "email": "lvlei@sugon.com", "group": "综合二"}, {"dept": "综合业务部赵铭", "name": "牛镜萱", "inner": "牛镜萱", "itcode": "niujx", "email": "niujx@sugon.com", "group": "综合二"}, {"dept": "综合业务部赵铭", "name": "张天航", "inner": "张天航_解决", "itcode": "zhangth2", "email": "zhangth2@sugon.com", "group": "综合二"}, {"dept": "综合业务部赵铭", "name": "马腾跃", "inner": "马腾跃", "itcode": "maty", "email": "maty@sugon.com", "group": "综合二"}, {"dept": "综合业务部赵铭", "name": "计岩", "inner": "计岩", "itcode": "jiyan", "email": "jiyan@sugon.com", "group": "综合二"}, {"dept": "综合业务部赵铭", "name": "阎鸣", "inner": "阎鸣", "itcode": "yanming3", "email": "yanming3@sugon.com", "group": "综合二"}, {"dept": "综合业务部赵铭", "name": "卜江涛", "inner": "卜江涛", "itcode": "bojt", "email": "bojt@sugon.com", "group": "综合二"}, {"dept": "综合业务部赵铭", "name": "邓玉铎", "inner": "邓玉铎", "itcode": "dengyd", "email": "dengyd@sugon.com", "group": "综合二"}, {"dept": "创新方案部邹强利", "name": "邹强利", "inner": "邹强利", "itcode": "zouql", "email": "zouql@sugon.com", "group": "地学"}, {"dept": "创新方案部邹强利", "name": "卓海峰", "inner": "卓海峰", "itcode": "zhuohf", "email": "zhuohf@sugon.com", "group": "地学"}, {"dept": "创新方案部邹强利", "name": "李冉", "inner": "李冉", "itcode": "liran1", "email": "liran1@sugon.com", "group": "AI组"}, {"dept": "创新方案部邹强利", "name": "李浩铭", "inner": "李浩铭", "itcode": "lihm5", "email": "lihm5@sugon.com", "group": "AI组"}, {"dept": "创新方案部邹强利", "name": "朱芳娟", "inner": "朱芳娟", "itcode": "zhufj", "email": "zhufj@sugon.com", "group": "AI组"}, {"dept": "创新方案部邹强利", "name": "王晓珍", "inner": "王晓珍", "itcode": "wangxzh3", "email": "wangxzh3@sugon.com", "group": "AI组"}, {"dept": "创新方案部邹强利", "name": "姚浪", "inner": "姚浪", "itcode": "yaolang", "email": "yaolang@sugon.com", "group": "仿真组"}, {"dept": "创新方案部邹强利", "name": "张郭亮", "inner": "张郭亮", "itcode": "zhanggl", "email": "zhanggl@sugon.com", "group": "仿真组"}, {"dept": "创新方案部邹强利", "name": "林建建", "inner": "林建建", "itcode": "linjj", "email": "linjj@sugon.com", "group": "仿真组"}, {"dept": "创新方案部邹强利", "name": "肖贵宝", "inner": "肖贵宝", "itcode": "xiaogb", "email": "xiaogb@sugon.com", "group": "生信医疗组"}, {"dept": "创新方案部邹强利", "name": "黄秀芝", "inner": "黄秀芝", "itcode": "huangxzh", "email": "huangxzh@sugon.com", "group": "生信医疗组"}, {"dept": "创新方案部邹强利", "name": "向小康", "inner": "向小康", "itcode": "xiangxk", "email": "xiangxk@sugon.com", "group": "存储组"}, {"dept": "创新方案部邹强利", "name": "张发", "inner": "张发", "itcode": "zhangfa", "email": "zhangfa@sugon.com", "group": "存储组"}, {"dept": "创新方案部邹强利", "name": "冯施施", "inner": "冯施施", "itcode": "fengss", "email": "fengss@sugon.com", "group": "数理化材组"}, {"dept": "创新方案部邹强利", "name": "左春洁", "inner": "左春洁", "itcode": "zuochj", "email": "", "group": "数理化材组"}], "presales_leaders": [{"name": "张磊", "itcode": "zhanglei2", "email": "zhanglei2@sugon.com"}, {"name": "李晓鹏", "itcode": "lixp", "email": "lixp@sugon.com"}, {"name": "高琦", "itcode": "glh", "email": "glh@sugon.com"}, {"name": "陈华新", "itcode": "chenhx", "email": "chenhx@sugon.com"}]}''')

DEFAULT_PWD_HASH = '$2b$10$EjWgSxbyde63NmNTIDLfhe5iVgyPqAT0ish/GeaYiRKaxWcLXzB6.'  # Sugon@123（已用 bcrypt.checkpw 验证）
DEPT_CODES = {'区域业务部': 'PD_QYYWB', '综合业务部': 'PD_ZHYWB', '创新方案部': 'PD_CXYAB'}
DEPT_LEADER = {'区域业务部': 'ligja', '综合业务部': 'zhaoming1', '创新方案部': 'zouql'}
GROUP_CODES = {
    '浙江&安徽': 'PR_ZJAH', '上海': 'PR_SH', '江苏&山东': 'PR_JSSD', '广东&深圳': 'PR_GDSZ',
    '湖南&湖北': 'PR_HNHB', '西南': 'PR_XN', '西北': 'PR_XB', '东北': 'PR_DB',
    '综合一': 'PS_ZH1', '综合二': 'PS_ZH2',
    '地学': 'PI_DX', 'AI组': 'PI_AI', '仿真组': 'PI_FZ', '生信医疗组': 'PI_SXYL',
    '存储组': 'PI_CC', '数理化材组': 'PI_SLHC',
}

ENV_PATH = sys.argv[1] if len(sys.argv) > 1 else '/opt/testplatform/.env'


def db_password():
    with open(ENV_PATH, encoding='utf-8') as f:
        for line in f:
            if line.startswith('DB_PASSWORD='):
                return line.strip().split('=', 1)[1]
    raise SystemExit('未在 %s 找到 DB_PASSWORD' % ENV_PATH)


PWD = db_password()


def run_sql(stmt):
    r = subprocess.run(['docker', 'exec', '-i', 'tp-mysql', 'mysql', '-utestplatform',
                        '-p' + PWD, '--default-character-set=utf8mb4', '-N', '-B',
                        'test_platform', '-e', stmt],
                       stdout=subprocess.PIPE, stderr=subprocess.PIPE)
    if r.returncode != 0:
        raise SystemExit('SQL执行失败: %s\n%s' % (stmt[:200], r.stderr.decode('utf-8', 'ignore')))
    return r.stdout.decode('utf-8', 'ignore')


def query(stmt):
    out = run_sql(stmt)
    rows = []
    for line in out.splitlines():
        if line.strip():
            rows.append(line.split('\t'))
    return rows


def esc(s):
    return s.replace('\\', '\\\\').replace("'", "\\'")


def dept_of(p):
    for d in DEPT_CODES:
        if p['dept'].startswith(d):
            return d
    return None


def main():
    people = ORG['people']
    leaders = ORG['presales_leaders']

    # 1. 角色
    run_sql("INSERT INTO sys_role (role_code, role_name, remark) "
            "SELECT 'PRESALES_LEADER','售前负责人','可见全部售前测试项目' FROM DUAL "
            "WHERE NOT EXISTS (SELECT 1 FROM sys_role WHERE role_code='PRESALES_LEADER')")
    role_ids = {r[1]: int(r[0]) for r in query('SELECT id, role_code FROM sys_role')}

    # 2. 用户匹配/创建
    users = query('SELECT id, username, real_name FROM sys_user')
    by_name = {}
    by_username = {}
    for uid, un, rn in users:
        by_username[un] = int(uid)
        by_name.setdefault(rn, []).append(int(uid))
    uid_of = {}
    created, matched_name, ambiguous = [], [], []
    for p in people + leaders:
        key = p['itcode']
        if key in uid_of:
            continue
        if key in by_username:
            uid_of[key] = by_username[key]
        elif p['name'] in by_name and len(by_name[p['name']]) == 1:
            uid_of[key] = by_name[p['name']][0]
            matched_name.append('%s(%s->id=%d)' % (p['name'], key, uid_of[key]))
        elif p['name'] in by_name and len(by_name[p['name']]) > 1:
            ambiguous.append(p['name'])
        else:
            run_sql("INSERT INTO sys_user (username, password, real_name, email, status, is_first_login) "
                    "VALUES ('%s','%s','%s','%s',1,1)" % (esc(key), DEFAULT_PWD_HASH, esc(p['name']), esc(p['email'])))
            uid_of[key] = int(query("SELECT id FROM sys_user WHERE username='%s'" % esc(key))[0][0])
            created.append('%s(%s)' % (p['name'], key))

    # 3. 角色分配（全员PRESALES；负责人PRESALES_LEADER）
    leader_keys = [l['itcode'] for l in leaders]
    existing_ur = set((int(a), int(b)) for a, b in query('SELECT user_id, role_id FROM sys_user_role'))
    for key, uid in uid_of.items():
        for rc in (['PRESALES', 'PRESALES_LEADER'] if key in leader_keys else ['PRESALES']):
            rid = role_ids[rc]
            if (uid, rid) not in existing_ur:
                run_sql('INSERT INTO sys_user_role (user_id, role_id) VALUES (%d,%d)' % (uid, rid))
                existing_ur.add((uid, rid))

    # 4. 三级结构：售前大组(根，复用PRESALES_GROUP) -> 业务部 -> 子组；组长=业务部总负责人/子组第一个人
    root_rows = query("SELECT id FROM sys_user_group WHERE group_code='PRESALES_GROUP'")
    if root_rows:
        root_id = int(root_rows[0][0])
        run_sql("UPDATE sys_user_group SET group_type='PRESALES_ROOT', parent_id=NULL WHERE id=%d" % root_id)
    else:
        run_sql("INSERT INTO sys_user_group (group_code, group_name, group_type) "
                "VALUES ('PRESALES_GROUP','售前大组','PRESALES_ROOT')")
        root_id = int(query("SELECT id FROM sys_user_group WHERE group_code='PRESALES_GROUP'")[0][0])

    groups = {}  # code -> (name, type, parent_code, leader_itcode)
    for d, code in DEPT_CODES.items():
        groups[code] = (d, 'PRESALES_DEPT', 'PRESALES_GROUP', DEPT_LEADER[d])
    for p in people:
        g = p['group']
        if g in GROUP_CODES and GROUP_CODES[g] not in groups:
            first = next(x for x in people if x['group'] == g)
            groups[GROUP_CODES[g]] = (g, 'PRESALES_REGION', DEPT_CODES[dept_of(p)], first['itcode'])

    gid_of = {'PRESALES_GROUP': root_id}
    for code, (name, gtype, parent_code, leader_key) in groups.items():
        leader_id = uid_of.get(leader_key)
        leader_name = next((p['name'] for p in people + leaders if p['itcode'] == leader_key), None)
        parent_id = gid_of[parent_code]
        rows = query("SELECT id FROM sys_user_group WHERE group_code='%s'" % code)
        if rows:
            gid = int(rows[0][0])
            run_sql("UPDATE sys_user_group SET group_type='%s', parent_id=%d, leader_id=%s, leader_name=%s WHERE id=%d"
                    % (gtype, parent_id, leader_id if leader_id else 'NULL',
                       ("'%s'" % esc(leader_name)) if leader_name else 'NULL', gid))
        else:
            run_sql("INSERT INTO sys_user_group (group_code, group_name, group_type, parent_id, leader_id, leader_name) "
                    "VALUES ('%s','%s','%s',%d,%s,%s)"
                    % (code, esc(name), gtype, parent_id, leader_id if leader_id else 'NULL',
                       ("'%s'" % esc(leader_name)) if leader_name else 'NULL'))
            gid = int(query("SELECT id FROM sys_user_group WHERE group_code='%s'" % code)[0][0])
        gid_of[code] = gid

    # 5. 组员关系：成员只放叶子子组；无子组的业务部人员(如李国君)直接挂业务部；可见范围按层级向下汇总
    existing_rel = set((int(a), int(b)) for a, b in query('SELECT user_id, group_id FROM sys_user_group_rel'))
    added = 0

    def add_member(code, key):
        nonlocal added
        uid = uid_of.get(key)
        if uid is None:
            return
        pair = (uid, gid_of[code])
        if pair not in existing_rel:
            run_sql('INSERT INTO sys_user_group_rel (user_id, group_id) VALUES (%d,%d)' % pair)
            existing_rel.add(pair)
            added += 1

    for p in people:
        if p['group'] in GROUP_CODES:
            add_member(GROUP_CODES[p['group']], p['itcode'])
        else:
            d = dept_of(p)
            if d:
                add_member(DEPT_CODES[d], p['itcode'])

    # 清理旧版迁移挂在业务部大组下的冗余直接成员（已有子组归属的人不需要同时挂业务部）
    for d, code in DEPT_CODES.items():
        sub_gids = [str(gid_of[GROUP_CODES[p['group']]]) for p in people
                    if dept_of(p) == d and p['group'] in GROUP_CODES]
        if sub_gids:
            run_sql("DELETE r FROM sys_user_group_rel r WHERE r.group_id=%d AND r.user_id IN "
                    "(SELECT user_id FROM (SELECT DISTINCT user_id FROM sys_user_group_rel WHERE group_id IN (%s)) t)"
                    % (gid_of[code], ','.join(sorted(set(sub_gids)))))

    print('=== 迁移完成 ===')
    print('新建账号 %d: %s' % (len(created), ', '.join(created) if created else '无'))
    print('按姓名匹配已有账号 %d: %s' % (len(matched_name), ', '.join(matched_name) if matched_name else '无'))
    if ambiguous:
        print('!! 姓名重名未处理，请人工确认: %s' % ', '.join(sorted(set(ambiguous))))
    print('用户组 %d 个，新增组员关系 %d 条' % (len(gid_of), added))
    if any(p['email'].endswith('@sugom.com') for p in people + leaders):
        print('!! 注意: 存在 @sugom.com 邮箱(疑为 @sugon.com 笔误)，请人工核对')


if __name__ == '__main__':
    main()
