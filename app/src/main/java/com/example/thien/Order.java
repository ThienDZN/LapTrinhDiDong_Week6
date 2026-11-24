package com.example.thien;

import java.util.ArrayList;
import java.util.List;

// 1 đơn đồ ăn đã đặt
public class Order {

    public static final long MINUTE = 60_000;

    // 1 món trong đơn: lưu tên + giá lúc đặt và số lượng
    public static class Item {
        public final int foodId;
        public final String name;
        public final double price;
        public final int qty;

        public Item(int foodId, String name, double price, int qty) {
            this.foodId = foodId;
            this.name = name;
            this.price = price;
            this.qty = qty;
        }
    }

    public String id;   // mã document trên Firestore
    public String receiver, phone, address, payment, voucher, note;
    public int shopId;
    public String shopName = "", area = "";   // quán nhận đơn + khu vực giao
    public double distanceKm;
    public double subtotal, shippingFee, discount, total;
    public long createdAt;
    public boolean cancelled;        // khách đã hủy đơn
    public boolean returnRequested;  // khách đã gửi yêu cầu trả hàng
    public String returnReason = "", returnNote = "";
    public long returnAt;
    public final List<Item> items = new ArrayList<>();

    // Mã đơn ngắn dễ đọc: 6 ký tự đầu của mã document, ví dụ "#A1B2C3"
    public String code() { return "#" + id.substring(0, 6).toUpperCase(); }

    public static final int CANCELLED = 3;
    public static final int RETURNED = 4;                      // đã gửi yêu cầu trả hàng
    public static final long RETURN_WINDOW = 3 * 24 * 60 * MINUTE;   // được trả trong 3 ngày kể từ lúc giao xong

    // Trạng thái theo thời gian đã trôi qua (mô phỏng quán + shipper):
    // 0 = đang chuẩn bị (< 5 phút), 1 = đang giao (< 25 phút), 2 = đã giao, 3 = đã hủy, 4 = đã yêu cầu trả hàng
    public int step(long now) {
        if (cancelled) return CANCELLED;
        if (returnRequested) return RETURNED;
        long passed = now - createdAt;
        if (passed < 5 * MINUTE) return 0;
        if (passed < 25 * MINUTE) return 1;
        return 2;
    }

    // Chỉ hủy được khi quán còn đang chuẩn bị
    public boolean canCancel(long now) { return step(now) == 0; }

    // Trả hàng được khi đã giao xong, chưa yêu cầu trả, và còn trong 3 ngày kể từ lúc giao (lúc giao = đặt + 25 phút)
    public boolean canReturn(long now) {
        return step(now) == 2 && now - (createdAt + 25 * MINUTE) <= RETURN_WINDOW;
    }

    // Tổng số phần trong đơn
    public int count() {
        int n = 0;
        for (Item i : items) n += i.qty;
        return n;
    }
}
