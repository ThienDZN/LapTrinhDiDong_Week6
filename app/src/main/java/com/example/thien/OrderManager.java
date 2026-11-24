package com.example.thien;

import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Đơn hàng trên Firestore (collection orders). Mỗi đơn có trường "account" nên đăng xuất rồi
// đăng nhập lại (kể cả trên máy khác) vẫn xem được đúng lịch sử của tài khoản đó.
public final class OrderManager {

    public static final double SHIPPING_FEE = 12000;
    public static final double FREE_SHIP_FROM = 200000;

    // Class tiện ích, không tạo object
    private OrderManager() { }

    // Phí giao theo khoảng cách: 12.000 ₫ cho 2 km đầu, mỗi km thêm (làm tròn lên) 4.000 ₫; miễn phí cho đơn từ 200.000 ₫
    public static double shippingFee(double subtotal, double km) {
        if (subtotal >= FREE_SHIP_FROM) return 0;
        return SHIPPING_FEE + 4000 * Math.max(0, Math.ceil(km - 2));
    }

    // Lưu đơn mới, cộng lượt bán cho từng món rồi xóa giỏ. Trả về mã document, null nếu lỗi.
    public static void placeOrder(String account, Order o, Map<Integer, Integer> cart, Cloud.Callback<String> cb) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (Map.Entry<Integer, Integer> e : cart.entrySet()) {
            Food f = Menu.find(e.getKey());
            Map<String, Object> m = new HashMap<>();
            m.put("foodId", f.id);
            m.put("name", f.name);
            m.put("price", f.price);
            m.put("qty", e.getValue());
            list.add(m);
        }
        Map<String, Object> data = new HashMap<>();
        data.put("account", account);
        data.put("shopId", o.shopId);
        data.put("shopName", o.shopName);
        data.put("area", o.area);
        data.put("distanceKm", o.distanceKm);
        data.put("receiver", o.receiver);
        data.put("phone", o.phone);
        data.put("address", o.address);
        data.put("payment", o.payment);
        data.put("voucher", o.voucher);
        data.put("note", o.note);
        data.put("subtotal", o.subtotal);
        data.put("shippingFee", o.shippingFee);
        data.put("discount", o.discount);
        data.put("total", o.total);
        data.put("createdAt", System.currentTimeMillis());
        data.put("items", list);
        Cloud.db().collection("orders").add(data).addOnCompleteListener(t -> {
            if (!t.isSuccessful()) { cb.done(null); return; }
            for (Map.Entry<Integer, Integer> e : cart.entrySet()) Menu.addSold(e.getKey(), e.getValue());
            CartManager.clear(account);
            UserStore.saveDelivery(account, o.phone, o.address);   // nhớ SĐT + địa chỉ cho lần sau
            cb.done(t.getResult().getId());
        });
    }

    // Lịch sử đơn của tài khoản, mới nhất trước
    public static void getOrders(String account, Cloud.Callback<List<Order>> cb) {
        Cloud.db().collection("orders").whereEqualTo("account", account).get().addOnCompleteListener(t -> {
            List<Order> list = new ArrayList<>();
            if (t.isSuccessful()) for (DocumentSnapshot d : t.getResult()) list.add(read(d));
            list.sort((x, y) -> Long.compare(y.createdAt, x.createdAt));
            cb.done(list);
        });
    }

    // Khách hủy đơn (chỉ gọi khi đơn còn đang chuẩn bị); trả lại lượt bán cho các món
    public static void cancel(Order o, Runnable done) {
        Cloud.db().collection("orders").document(o.id).update("cancelled", true).addOnCompleteListener(t -> {
            for (Order.Item i : o.items) Menu.addSold(i.foodId, -i.qty);
            done.run();
        });
    }

    // Khách gửi yêu cầu trả hàng / hoàn tiền cho đơn đã giao (quán sẽ liên hệ xử lý)
    public static void requestReturn(Order o, String reason, String note, Runnable done) {
        Map<String, Object> data = new HashMap<>();
        data.put("returnRequested", true);
        data.put("returnReason", reason);
        data.put("returnNote", note);
        data.put("returnAt", System.currentTimeMillis());
        Cloud.db().collection("orders").document(o.id).update(data).addOnCompleteListener(t -> done.run());
    }

    // 1 đơn theo mã document
    public static void getOrder(String id, Cloud.Callback<Order> cb) {
        Cloud.db().collection("orders").document(id).get()
                .addOnCompleteListener(t -> cb.done(t.isSuccessful() && t.getResult().exists() ? read(t.getResult()) : null));
    }

    // Đọc 1 document đơn hàng thành Order (trường thiếu thì để trống / 0)
    private static Order read(DocumentSnapshot d) {
        Order o = new Order();
        o.id = d.getId();
        o.shopId = (int) num(d, "shopId");
        o.shopName = str(d, "shopName");
        o.area = str(d, "area");
        o.distanceKm = num(d, "distanceKm");
        o.receiver = str(d, "receiver");
        o.phone = str(d, "phone");
        o.address = str(d, "address");
        o.payment = str(d, "payment");
        o.voucher = str(d, "voucher");
        o.note = str(d, "note");
        o.subtotal = num(d, "subtotal");
        o.shippingFee = num(d, "shippingFee");
        o.discount = num(d, "discount");
        o.total = num(d, "total");
        o.createdAt = (long) num(d, "createdAt");
        o.cancelled = Boolean.TRUE.equals(d.getBoolean("cancelled"));
        o.returnRequested = Boolean.TRUE.equals(d.getBoolean("returnRequested"));
        o.returnReason = str(d, "returnReason");
        o.returnNote = str(d, "returnNote");
        o.returnAt = (long) num(d, "returnAt");
        if (d.get("items") instanceof List) {
            for (Object x : (List<?>) d.get("items")) {
                Map<?, ?> m = (Map<?, ?>) x;
                if (m.get("foodId") == null) continue;   // bỏ qua đơn cũ không phải đồ ăn
                o.items.add(new Order.Item(((Number) m.get("foodId")).intValue(), String.valueOf(m.get("name")),
                        ((Number) m.get("price")).doubleValue(), ((Number) m.get("qty")).intValue()));
            }
        }
        return o;
    }

    private static String str(DocumentSnapshot d, String f) { return d.getString(f) == null ? "" : d.getString(f); }

    private static double num(DocumentSnapshot d, String f) { return d.getDouble(f) == null ? 0 : d.getDouble(f); }
}
