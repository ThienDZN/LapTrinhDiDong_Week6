package com.example.thien;

import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.SetOptions;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

// Giỏ hàng trên Firestore: document carts/<tài khoản> có map "items": { "id món": số lượng }
public final class CartManager {

    public static final int MAX_QTY = 20;

    // Class tiện ích, không tạo object
    private CartManager() { }

    // Thêm qty phần của 1 món (cộng dồn nếu đã có). Mỗi đơn chỉ đặt từ 1 quán: giỏ đang có món của quán khác
    // thì không thêm và báo false (màn hình sẽ hỏi khách có muốn xóa giỏ cũ không).
    public static void add(String account, Food food, int qty, Cloud.Callback<Boolean> cb) {
        getItems(account, items -> {
            for (int id : items.keySet()) {
                if (Menu.find(id).shopId != food.shopId) { cb.done(false); return; }
            }
            Map<String, Object> m = Collections.singletonMap(String.valueOf(food.id), FieldValue.increment(qty));
            Cloud.db().collection("carts").document(account)
                    .set(Collections.singletonMap("items", m), SetOptions.merge())
                    .addOnCompleteListener(t -> cb.done(true));
        });
    }

    // Thay toàn bộ giỏ bằng danh sách mới: id món -> số lượng (dùng khi "Đặt lại" đơn cũ hoặc đổi sang quán khác)
    public static void replaceAll(String account, Map<Integer, Integer> items, Runnable done) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (Map.Entry<Integer, Integer> e : items.entrySet()) m.put(String.valueOf(e.getKey()), Math.min(e.getValue(), MAX_QTY));
        Cloud.db().collection("carts").document(account).set(Collections.singletonMap("items", m))
                .addOnCompleteListener(t -> done.run());
    }

    // Quán của các món trong giỏ (null nếu giỏ trống)
    public static Shop shopOf(Map<Integer, Integer> items) {
        for (int id : items.keySet()) return Menu.findShop(Menu.find(id).shopId);
        return null;
    }

    // Đặt lại số lượng 1 món (<= 0 thì xóa khỏi giỏ)
    public static void setQty(String account, int foodId, int qty, Runnable done) {
        Object value = qty <= 0 ? FieldValue.delete() : Math.min(qty, MAX_QTY);
        Cloud.db().collection("carts").document(account)
                .update("items." + foodId, value).addOnCompleteListener(t -> done.run());
    }

    // Xóa cả giỏ (sau khi đặt hàng)
    public static void clear(String account) {
        Cloud.db().collection("carts").document(account).delete();
    }

    // Đọc giỏ: id món -> số lượng
    public static void getItems(String account, Cloud.Callback<Map<Integer, Integer>> cb) {
        Cloud.db().collection("carts").document(account).get().addOnCompleteListener(t -> {
            Map<Integer, Integer> items = new LinkedHashMap<>();
            Object raw = t.isSuccessful() ? t.getResult().get("items") : null;
            if (raw instanceof Map) {
                for (Map.Entry<?, ?> e : ((Map<?, ?>) raw).entrySet()) {
                    int id = Integer.parseInt(String.valueOf(e.getKey()));
                    int qty = ((Number) e.getValue()).intValue();
                    if (Menu.find(id) != null && qty > 0) items.put(id, qty);
                }
            }
            cb.done(items);
        });
    }

    // Tổng tiền = giá x số lượng
    public static double total(Map<Integer, Integer> items) {
        double t = 0;
        for (Map.Entry<Integer, Integer> e : items.entrySet()) {
            Food f = Menu.find(e.getKey());
            if (f != null) t += f.price * e.getValue();
        }
        return t;
    }

    // Tổng số phần
    public static int count(Map<Integer, Integer> items) {
        int n = 0;
        for (int q : items.values()) n += q;
        return n;
    }
}
