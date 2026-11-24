package com.example.thien;

import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Bình luận + số sao trên Firestore, mỗi tài khoản 1 bình luận cho mỗi món / mỗi quán (viết lại thì ghi đè).
//   - đánh giá MÓN : collection "reviews",     trường "foodId", document id = "<id món>_<tài khoản>"
//   - nhận xét QUÁN: collection "shopReviews", trường "shopId", document id = "<id quán>_<tài khoản>"
public final class ReviewManager {

    private static final String DISH = "reviews", SHOP = "shopReviews";

    // Class tiện ích, không tạo object
    private ReviewManager() { }

    // ===== Đánh giá MÓN =====

    public static void getForFood(int foodId, Cloud.Callback<List<Review>> cb) { getList(DISH, "foodId", foodId, cb); }

    public static void getMine(String account, int foodId, Cloud.Callback<Review> cb) { getOne(DISH, foodId, account, cb); }

    public static void save(String account, String author, int foodId, int rating, String content, Runnable done) {
        save(DISH, "foodId", foodId, account, author, rating, content, done);
    }

    public static void delete(String account, int foodId, Runnable done) { delete(DISH, foodId, account, done); }

    // Điểm sao trung bình của mọi món: id món -> {sao trung bình, số đánh giá}
    public static void getAverages(Cloud.Callback<Map<Integer, double[]>> cb) { averages(DISH, "foodId", cb); }

    // ===== Nhận xét QUÁN =====

    public static void getForShop(int shopId, Cloud.Callback<List<Review>> cb) { getList(SHOP, "shopId", shopId, cb); }

    public static void saveShop(String account, String author, int shopId, int rating, String content, Runnable done) {
        save(SHOP, "shopId", shopId, account, author, rating, content, done);
    }

    public static void deleteShop(String account, int shopId, Runnable done) { delete(SHOP, shopId, account, done); }

    // Điểm sao trung bình của mọi quán: id quán -> {sao trung bình, số nhận xét thật trong app}
    public static void getShopAverages(Cloud.Callback<Map<Integer, double[]>> cb) { averages(SHOP, "shopId", cb); }

    // ===== Phần dùng chung =====

    // Các đánh giá của 1 món / 1 quán (field = "foodId" hoặc "shopId"), mới nhất trước
    private static void getList(String collection, String field, int id, Cloud.Callback<List<Review>> cb) {
        Cloud.db().collection(collection).whereEqualTo(field, id).get().addOnCompleteListener(t -> {
            List<Review> list = new ArrayList<>();
            if (t.isSuccessful()) for (DocumentSnapshot d : t.getResult()) list.add(toReview(d));
            list.sort((x, y) -> Long.compare(y.createdAt, x.createdAt));
            cb.done(list);
        });
    }

    // Đánh giá của 1 tài khoản (null nếu chưa viết)
    private static void getOne(String collection, int id, String account, Cloud.Callback<Review> cb) {
        Cloud.db().collection(collection).document(id + "_" + account).get().addOnCompleteListener(t -> {
            DocumentSnapshot d = t.isSuccessful() ? t.getResult() : null;
            cb.done(d != null && d.exists() && d.getLong("rating") != null ? toReview(d) : null);
        });
    }

    // Thêm mới hoặc thay đánh giá cũ
    private static void save(String collection, String field, int id, String account, String author,
                             int rating, String content, Runnable done) {
        Map<String, Object> data = new HashMap<>();
        data.put(field, id);
        data.put("account", account);
        data.put("author", author);
        data.put("rating", rating);
        data.put("content", content);
        data.put("createdAt", System.currentTimeMillis());
        Cloud.db().collection(collection).document(id + "_" + account).set(data).addOnCompleteListener(t -> done.run());
    }

    private static void delete(String collection, int id, String account, Runnable done) {
        Cloud.db().collection(collection).document(id + "_" + account).delete().addOnCompleteListener(t -> done.run());
    }

    // Sao trung bình theo id: id -> {sao trung bình, số đánh giá}
    private static void averages(String collection, String field, Cloud.Callback<Map<Integer, double[]>> cb) {
        Cloud.db().collection(collection).get().addOnCompleteListener(t -> {
            Map<Integer, double[]> sum = new HashMap<>();   // id -> {tổng sao, số lượt}
            if (t.isSuccessful()) {
                for (DocumentSnapshot d : t.getResult()) {
                    if (d.getLong(field) == null || d.getLong("rating") == null) continue;
                    double[] s = sum.computeIfAbsent(d.getLong(field).intValue(), k -> new double[2]);
                    s[0] += d.getLong("rating");
                    s[1]++;
                }
            }
            for (double[] s : sum.values()) s[0] = s[0] / s[1];   // tổng -> trung bình
            cb.done(sum);
        });
    }

    // Đổi 1 document thành Review
    private static Review toReview(DocumentSnapshot d) {
        Long time = d.getLong("createdAt");
        return new Review(d.getString("account"), d.getString("author"), d.getLong("rating").intValue(),
                d.getString("content"), time == null ? 0 : time);
    }
}
