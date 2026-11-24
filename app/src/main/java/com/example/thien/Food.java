package com.example.thien;

import java.io.Serializable;

// 1 món ăn / đồ uống trong thực đơn
public class Food implements Serializable {

    public final int id;
    public final int shopId;         // món thuộc quán nào (0 = món mẫu, chưa thuộc quán)
    public final int photoId;        // id món mẫu có ảnh (res/drawable-nodpi/food_<photoId>.jpg)
    public String name;
    public final String category;    // Cơm, Bún & Mì, Đồ nướng...
    public double price;
    public final double oldPrice;    // giá gốc, > price nghĩa là đang giảm giá
    public String description;
    public final int calories;       // kcal / phần
    public final int prepMinutes;    // thời gian chuẩn bị
    public final int spicy;          // độ cay 0..3
    public long sold = 0;            // đã bán (đếm trên Firestore)
    public long likes = 0;           // lượt thích (đếm trên Firestore)

    public Food(int id, String name, String category, double price, double oldPrice,
                int calories, int prepMinutes, int spicy, String description) {
        this.id = id;
        this.shopId = 0;
        this.photoId = id;
        this.name = name;
        this.category = category;
        this.price = price;
        this.oldPrice = oldPrice;
        this.calories = calories;
        this.prepMinutes = prepMinutes;
        this.spicy = spicy;
        this.description = description;
    }

    // Sao chép món mẫu cho 1 quán: id riêng, giá chênh nhẹ giữa các quán (làm tròn đến nghìn đồng)
    public Food(Food base, int id, int shopId) {
        double factor = 0.92 + (id * 7 % 9) * 0.03;   // 0,92 .. 1,16
        this.id = id;
        this.shopId = shopId;
        this.photoId = base.id;
        this.name = base.name;
        this.category = base.category;
        this.price = Math.round(base.price * factor / 1000) * 1000;
        this.oldPrice = base.oldPrice > 0 ? Math.round(base.oldPrice * factor / 1000) * 1000 : 0;
        this.description = base.description;
        this.calories = base.calories;
        this.prepMinutes = base.prepMinutes;
        this.spicy = base.spicy;
    }
}
