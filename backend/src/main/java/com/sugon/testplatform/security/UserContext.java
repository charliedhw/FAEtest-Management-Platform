package com.sugon.testplatform.security;

public class UserContext {
    private static final ThreadLocal<LoginUser> HOLDER = new ThreadLocal<>();

    public static void set(LoginUser user) { HOLDER.set(user); }
    public static LoginUser get() { return HOLDER.get(); }

    /**
     * 获取当前用户，未登录时抛业务异常而不是NPE
     */
    public static LoginUser require() {
        LoginUser u = HOLDER.get();
        if (u == null || u.getUserId() == null) {
            throw new com.sugon.testplatform.common.BizException("未登录或登录已过期，请重新登录");
        }
        return u;
    }

    public static Long getUserId() {
        LoginUser u = HOLDER.get();
        return u == null ? null : u.getUserId();
    }

    public static Long requireUserId() {
        return require().getUserId();
    }

    public static String getRealName() {
        LoginUser u = HOLDER.get();
        return u == null ? null : u.getRealName();
    }

    public static java.util.List<String> getRoles() {
        LoginUser u = HOLDER.get();
        return (u == null || u.getRoles() == null) ? java.util.List.of() : u.getRoles();
    }

    /**
     * 要求当前用户具备指定角色，否则抛业务异常
     */
    public static void requireRole(String roleCode) {
        LoginUser u = require();
        if (u.getRoles() == null || !u.getRoles().contains(roleCode)) {
            throw new com.sugon.testplatform.common.BizException("无权限执行该操作");
        }
    }

    /**
     * 是否具备任一指定角色
     */
    public static boolean hasAnyRole(String... roleCodes) {
        java.util.List<String> roles = getRoles();
        for (String r : roleCodes) {
            if (roles.contains(r)) return true;
        }
        return false;
    }

    public static void clear() { HOLDER.remove(); }
}
