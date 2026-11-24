package com.example.thien;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Món yêu thích (nút Thích) trên Firestore: collection favorites, id = "<tài khoản>_<id món>".
// Mỗi lần thích / bỏ thích thì cộng / trừ "likes" của món để hiện lượt thích.
public final class FavoriteManager {

    // Class tiện ích, không tạo object
    private FavoriteManager() { }

    // Món này tài khoản đã thích chưa
    public static void isFavorite(String account, int foodId, Cloud.Callback<Boolean> cb) {
        Cloud.db().collection("favorites").document(account + "_" + foodId).get()
                .addOnCompleteListener(t -> cb.done(t.isSuccessful() && t.getResult().exists()));
    }

    // Thích / bỏ thích; nowLiked = trạng thái mới
    public static void set(String account, int foodId, boolean nowLiked) {
        if (nowLiked) {
            Map<String, Object> data = new HashMap<>();
            data.put("account", account);
            data.put("foodId", foodId);
            data.put("createdAt", System.currentTimeMillis());
            Cloud.db().collection("favorites").document(account + "_" + foodId).set(data);
        } else {
            Cloud.db().collection("favorites").document(account + "_" + foodId).delete();
        }
        Cloud.db().collection(Menu.COLLECTION).document(String.valueOf(foodId))
                .set(Collections.singletonMap("likes", FieldValue.increment(nowLiked ? 1 : -1)), SetOptions.merge());
    }

    // Các món tài khoản đã thích
    public static void getAll(String account, Cloud.Callback<List<Food>> cb) {
        Cloud.db().collection("favorites").whereEqualTo("account", account).get().addOnCompleteListener(t -> {
            List<Food> list = new ArrayList<>();
            if (t.isSuccessful()) {
                for (DocumentSnapshot d : t.getResult()) {
                    Food a = Menu.find(d.getLong("foodId").intValue());
                    if (a != null) list.add(a);
                }
            }
            cb.done(list);
        });
    }
}
