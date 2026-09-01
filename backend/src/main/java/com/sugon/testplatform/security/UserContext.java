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

    public static void clear() { HOLDER.remove(); }
}
