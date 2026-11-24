package com.example.thien;

import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Mã giảm giá trên Firestore: collection vouchers, id = mã viết hoa.
// Mỗi mã có: % giảm, giảm tối đa, đơn tối thiểu, hạn dùng, đang bật hay đã tạm ngưng.
// Admin tạo / tạm ngưng / kích hoạt lại / hủy (xóa); khách nhập mã ở màn thanh toán.
public final class VoucherManager {

    public static final long DAY = 24L * 60 * 60 * 1000;

    // 1 mã giảm giá
    public static class Voucher {
        public final String code;
        public final int percent;          // % giảm
        public final double maxDiscount;   // giảm tối đa (0 = không giới hạn)
        public final double minOrder;      // đơn tối thiểu để dùng mã
        public final long expiresAt;       // hết hạn lúc (millis)
        public final boolean active;       // false = admin đã tạm ngưng

        public Voucher(String code, int percent, double maxDiscount, double minOrder, long expiresAt, boolean active) {
            this.code = code;
            this.percent = percent;
            this.maxDiscount = maxDiscount;
            this.minOrder = minOrder;
            this.expiresAt = expiresAt;
            this.active = active;
        }

        // Còn hạn dùng không
        public boolean expired(long now) { return now > expiresAt; }
    }

    // Class tiện ích, không tạo object
    private VoucherManager() { }

    // Số tiền giảm của mã cho 1 đơn: subtotal * % (làm tròn), không quá mức giảm tối đa;
    // mã null / đã tắt / hết hạn / đơn chưa đủ tối thiểu thì 0
    public static double discount(Voucher v, double subtotal, long now) {
        if (v == null || !v.active || v.expired(now) || subtotal < v.minOrder) return 0;
        double d = Math.round(subtotal * v.percent / 100.0);
        return v.maxDiscount > 0 ? Math.min(d, v.maxDiscount) : d;
    }

    // Tạo sẵn mã DEMKHUYA (giảm 10%, tối đa 30.000 ₫, hạn 1 năm) nếu chưa có, để thử thanh toán
    public static void createDefault() {
        Cloud.db().collection("vouchers").document("DEMKHUYA").get().addOnSuccessListener(d -> {
            if (!d.exists()) save(new Voucher("DEMKHUYA", 10, 30000, 0,
                    System.currentTimeMillis() + 365 * DAY, true), "system", () -> { });
        });
    }

    // Tìm 1 mã theo tên (không phân biệt hoa thường); không có thì null
    public static void find(String code, Cloud.Callback<Voucher> cb) {
        if (code.isEmpty()) { cb.done(null); return; }
        Cloud.db().collection("vouchers").document(code.toUpperCase()).get()
                .addOnCompleteListener(t -> cb.done(t.isSuccessful() ? read(t.getResult()) : null));
    }

    // Tất cả mã (cho màn hình admin), mã mới tạo trước
    public static void getAll(Cloud.Callback<List<Voucher>> cb) {
        Cloud.db().collection("vouchers").get().addOnCompleteListener(t -> {
            List<Voucher> list = new ArrayList<>();
            if (t.isSuccessful()) {
                for (DocumentSnapshot d : t.getResult()) {
                    Voucher v = read(d);
                    if (v != null) list.add(v);
                }
            }
            list.sort((x, y) -> Long.compare(y.expiresAt, x.expiresAt));
            cb.done(list);
        });
    }

    // Tạo mới hoặc ghi đè 1 mã
    public static void save(Voucher v, String createdBy, Runnable done) {
        Map<String, Object> data = new HashMap<>();
        data.put("percent", v.percent);
        data.put("maxDiscount", v.maxDiscount);
        data.put("minOrder", v.minOrder);
        data.put("expiresAt", v.expiresAt);
        data.put("active", v.active);
        data.put("createdBy", createdBy);
        Cloud.db().collection("vouchers").document(v.code.toUpperCase()).set(data)
                .addOnCompleteListener(t -> done.run());
    }

    // Tạm ngưng / kích hoạt lại 1 mã
    public static void setActive(String code, boolean active, Runnable done) {
        Cloud.db().collection("vouchers").document(code).update("active", active)
                .addOnCompleteListener(t -> done.run());
    }

    // Hủy (xóa hẳn) 1 mã
    public static void delete(String code, Runnable done) {
        Cloud.db().collection("vouchers").document(code).delete().addOnCompleteListener(t -> done.run());
    }

    // Đọc 1 document thành Voucher (không tồn tại thì null; mã cũ thiếu trường thì dùng giá trị mặc định)
    private static Voucher read(DocumentSnapshot d) {
        if (d == null || !d.exists() || d.getLong("percent") == null) return null;
        return new Voucher(d.getId(), d.getLong("percent").intValue(),
                d.getDouble("maxDiscount") == null ? 0 : d.getDouble("maxDiscount"),
                d.getDouble("minOrder") == null ? 0 : d.getDouble("minOrder"),
                d.getLong("expiresAt") == null ? Long.MAX_VALUE : d.getLong("expiresAt"),
                Boolean.TRUE.equals(d.getBoolean("active")));
    }
}
