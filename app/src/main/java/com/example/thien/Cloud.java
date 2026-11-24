package com.example.thien;

import com.google.firebase.firestore.FirebaseFirestore;

// Kết nối Firestore (database trên đám mây của Firebase) dùng chung cho cả app.
// Các collection:
//   albums    : thông tin album + danh sách bài + lượt nghe, lượt thích
//   users     : tài khoản (mật khẩu đã băm), vai trò admin / user
//   carts     : giỏ hàng của từng tài khoản
//   orders    : đơn hàng đã thanh toán
//   reviews   : bình luận + số sao
//   favorites : album đã thích
//   vouchers  : mã giảm giá do admin tạo
public final class Cloud {

    // Kết quả trả về sau khi Firestore xử lý xong (Firestore chạy bất đồng bộ, không chặn giao diện)
    public interface Callback<T> { void done(T result); }

    // Class tiện ích, không tạo object
    private Cloud() { }

    // Lấy database Firestore
    public static FirebaseFirestore db() { return FirebaseFirestore.getInstance(); }
}
