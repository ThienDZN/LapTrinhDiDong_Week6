package com.example.thien;

import java.io.Serializable;
import java.util.Map;

// 1 quán ăn đối tác trên nền tảng: app chỉ phân phối (kết nối khách với quán), quán tự chuẩn bị món
public class Shop implements Serializable {

    public final int id;
    public final String name;
    public final String tags;           // loại món, ví dụ "Cơm tấm · Cơm chiên"
    public final String address;
    public final String area;           // khu vực (quận) của quán
    public final int theme;             // nhóm quán (0 cơm, 1 phở/bún, ...), dùng để chọn nhận xét mẫu
    public final double lat, lng;
    public final double baseRating;     // điểm sao ban đầu của quán (trước khi có đánh giá trong app)
    public final int baseCount;         // số lượt đánh giá ban đầu
    public final int coverFoodId;       // ảnh bìa quán = ảnh của 1 món mẫu đặc trưng

    public Shop(int id, String name, String tags, String address, String area, int theme,
                double lat, double lng, double baseRating, int baseCount, int coverFoodId) {
        this.id = id;
        this.name = name;
        this.tags = tags;
        this.address = address;
        this.area = area;
        this.theme = theme;
        this.lat = lat;
        this.lng = lng;
        this.baseRating = baseRating;
        this.baseCount = baseCount;
        this.coverFoodId = coverFoodId;
    }

    // Khoảng cách (km) từ khu vực của khách tới quán, theo công thức haversine (đường chim bay)
    public double distanceKm(Area area) {
        double dLat = Math.toRadians(lat - area.lat), dLng = Math.toRadians(lng - area.lng);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) + Math.cos(Math.toRadians(area.lat))
                * Math.cos(Math.toRadians(lat)) * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return 6371 * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    // Thời gian giao dự kiến (phút): 15 phút chuẩn bị + 4 phút mỗi km, làm tròn lên bội số của 5
    public int deliveryMinutes(Area area) {
        return (int) (Math.ceil((15 + 4 * distanceKm(area)) / 5.0) * 5);
    }

    // Điểm sao hiện tại = trung bình có trọng số giữa điểm ban đầu của quán và các nhận xét thật trong app.
    // shopReviews: id quán -> {sao trung bình, số nhận xét}
    public double rating(Map<Integer, double[]> shopReviews) {
        double[] r = shopReviews.get(id);
        if (r == null) return baseRating;
        return (baseRating * baseCount + r[0] * r[1]) / (baseCount + r[1]);
    }

    // Tổng số đánh giá hiện tại (ban đầu + nhận xét thật)
    public int ratingCount(Map<Integer, double[]> shopReviews) {
        double[] r = shopReviews.get(id);
        return baseCount + (r == null ? 0 : (int) r[1]);
    }

    // Điểm gợi ý: quán điểm cao và gần thì điểm cao (mỗi km xa trừ 0,15 sao)
    public double suggestScore(Area area, Map<Integer, double[]> shopReviews) {
        return rating(shopReviews) - 0.15 * distanceKm(area);
    }
}
