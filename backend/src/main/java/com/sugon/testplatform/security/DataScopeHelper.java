package com.sugon.testplatform.security;

import java.util.List;

/**
 * 数据权限辅助类
 * 规则:
 *  - 全部可见: ADMIN(管理员) / APPROVER(测试审批组) / LEADER(分管领导) / FAE_LEADER(FAE负责人) / RESOURCE_ADMIN(资源管理员)
 *  - 受限: PRESALES(售前) / SALES(销售) / TESTER(FAE测试) / 其他
 */
public class DataScopeHelper {

    /**
     * 是否可见全部数据
     */
    public static boolean seeAll() {
        List<String> roles = UserContext.getRoles();
        return roles.contains("ADMIN") || roles.contains("APPROVER")
                || roles.contains("LEADER") || roles.contains("FAE_LEADER")
                || roles.contains("RESOURCE_ADMIN");
    }

    /**
     * 生成 test_project 表的数据权限 SQL 片段(用于自定义SQL/统计)
     * 返回空串表示不限制; 否则返回 AND (...) 条件
     * 别名为 t
     */
    public static String projectScopeSql(String alias) {
        if (seeAll()) return "";
        Long uid = UserContext.getUserId();
        if (uid == null) return " AND 1=0 ";
        String a = (alias == null || alias.isEmpty()) ? "" : alias + ".";
        List<String> roles = UserContext.getRoles();
        StringBuilder sb = new StringBuilder(" AND (");
        boolean has = false;
        if (roles.contains("PRESALES")) {
            sb.append(a).append("presales_id = ").append(uid);
            has = true;
        }
        if (roles.contains("SALES")) {
            if (has) sb.append(" OR ");
            sb.append(a).append("sales_id = ").append(uid);
            has = true;
        }
        if (roles.contains("TESTER")) {
            if (has) sb.append(" OR ");
            // tester_ids 逗号分隔精确匹配
            sb.append("( ").append(a).append("tester_ids = '").append(uid).append("'")
              .append(" OR ").append(a).append("tester_ids LIKE CONCAT('").append(uid).append("',',%')")
              .append(" OR ").append(a).append("tester_ids LIKE CONCAT('%,','").append(uid).append("')")
              .append(" OR ").append(a).append("tester_ids LIKE CONCAT('%,','").append(uid).append("',',%') )");
            has = true;
        }
        if (has) sb.append(" OR ");
        sb.append(a).append("create_by = ").append(uid);
        sb.append(")");
        return sb.toString();
    }
}
