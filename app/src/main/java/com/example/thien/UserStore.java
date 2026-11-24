package com.example.thien;

import com.google.firebase.firestore.DocumentSnapshot;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

// Tài khoản lưu trên Firestore (collection "users", id document = tên đăng nhập viết thường).
// Mật khẩu không lưu thô: lưu muối ngẫu nhiên + SHA-256(muối + mật khẩu).
public final class UserStore {

    // Class tiện ích, không tạo object
    private UserStore() { }

    // Tạo sẵn 2 tài khoản test nếu chưa có: admin/123456 (quản trị) và user/123456 (người dùng thường)
    public static void createTestAccounts() {
        createIfMissing("admin", "Quản trị viên", "123456", "admin");
        createIfMissing("user", "Người dùng test", "123456", "user");
    }

    // Đăng ký tài khoản mới (vai trò user). Trả về false nếu tên đã tồn tại.
    public static void register(String username, String fullName, String password, Cloud.Callback<Boolean> cb) {
        Cloud.db().collection("users").document(username.toLowerCase()).get().addOnCompleteListener(t -> {
            if (!t.isSuccessful()) { cb.done(false); return; }
            if (t.getResult().exists()) { cb.done(false); return; }
            Cloud.db().collection("users").document(username.toLowerCase())
                    .set(newUser(fullName, password, "user"))
                    .addOnCompleteListener(w -> cb.done(w.isSuccessful()));
        });
    }

    // Đăng nhập: đúng mật khẩu thì trả về document của user, sai / lỗi mạng thì null
    public static void login(String username, String password, Cloud.Callback<DocumentSnapshot> cb) {
        Cloud.db().collection("users").document(username.toLowerCase()).get().addOnCompleteListener(t -> {
            DocumentSnapshot d = t.isSuccessful() ? t.getResult() : null;
            boolean ok = d != null && d.exists()
                    && hash(d.getString("salt"), password).equals(d.getString("hash"));
            cb.done(ok ? d : null);
        });
    }

    // Đọc hồ sơ (họ tên, email, số điện thoại, vai trò) của 1 tài khoản
    public static void get(String username, Cloud.Callback<DocumentSnapshot> cb) {
        Cloud.db().collection("users").document(username.toLowerCase()).get()
                .addOnCompleteListener(t -> cb.done(t.isSuccessful() ? t.getResult() : null));
    }

    // Cập nhật họ tên, email, số điện thoại
    public static void updateProfile(String username, String fullName, String email, String phone) {
        Map<String, Object> data = new HashMap<>();
        data.put("name", fullName);
        data.put("email", email);
        data.put("phone", phone);
        Cloud.db().collection("users").document(username.toLowerCase()).update(data);
    }

    // Ghi nhớ số điện thoại + địa chỉ giao gần nhất để điền sẵn ở lần đặt sau
    public static void saveDelivery(String username, String phone, String address) {
        Map<String, Object> data = new HashMap<>();
        data.put("phone", phone);
        data.put("address", address);
        Cloud.db().collection("users").document(username.toLowerCase()).update(data);
    }

    // Tạo tài khoản nếu document chưa tồn tại
    private static void createIfMissing(String username, String name, String password, String role) {
        Cloud.db().collection("users").document(username).get().addOnSuccessListener(d -> {
            if (!d.exists()) Cloud.db().collection("users").document(username).set(newUser(name, password, role));
        });
    }

    // Dữ liệu 1 tài khoản mới: muối ngẫu nhiên + mật khẩu đã băm
    private static Map<String, Object> newUser(String name, String password, String role) {
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        String saltB64 = Base64.getEncoder().encodeToString(salt);
        Map<String, Object> data = new HashMap<>();
        data.put("name", name);
        data.put("role", role);
        data.put("salt", saltB64);
        data.put("hash", hash(saltB64, password));
        data.put("email", "");
        data.put("phone", "");
        return data;
    }

    // Băm SHA-256 của muối + mật khẩu
    private static String hash(String salt, String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return Base64.getEncoder().encodeToString(md.digest((salt + password).getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
