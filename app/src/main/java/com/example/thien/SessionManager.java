package com.example.thien;

import android.content.Context;
import android.content.SharedPreferences;

// Ghi nhớ ai đang đăng nhập (lưu trên máy bằng SharedPreferences) để mọi màn hình đọc được
public final class SessionManager {

    private static final String PREFS = "session";

    // Class tiện ích, không tạo object
    private SessionManager() { }

    // Lưu tài khoản, tên hiển thị và vai trò (admin / user) khi đăng nhập
    public static void login(Context c, String account, String displayName, String role) {
        prefs(c).edit().putString("account", account).putString("name", displayName)
                .putString("role", role).apply();
    }

    // Cập nhật tên hiển thị (sau khi sửa hồ sơ)
    public static void setDisplayName(Context c, String name) { prefs(c).edit().putString("name", name).apply(); }

    // Đăng xuất: xóa phiên
    public static void logout(Context c) { prefs(c).edit().clear().apply(); }

    // Tên đăng nhập hiện tại
    public static String getAccount(Context c) { return prefs(c).getString("account", ""); }

    // Tên hiển thị hiện tại
    public static String getDisplayName(Context c) { return prefs(c).getString("name", ""); }

    // Tài khoản hiện tại có phải admin không
    public static boolean isAdmin(Context c) { return "admin".equals(prefs(c).getString("role", "")); }

    private static SharedPreferences prefs(Context c) { return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE); }
}
