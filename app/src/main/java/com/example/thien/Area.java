package com.example.thien;

// 1 khu vực giao hàng (quận) với toạ độ trung tâm; khoảng cách tới quán tính từ điểm này
public class Area {

    public final String name;
    public final double lat, lng;

    public Area(String name, double lat, double lng) {
        this.name = name;
        this.lat = lat;
        this.lng = lng;
    }

    // Các khu vực khách có thể chọn để nhận hàng
    public static final Area[] ALL = {
            new Area("Quận 1", 10.7769, 106.7009),
            new Area("Quận 3", 10.7843, 106.6844),
            new Area("Quận 10", 10.7746, 106.6679),
            new Area("Bình Thạnh", 10.8106, 106.7091),
            new Area("Phú Nhuận", 10.7992, 106.6803),
            new Area("Tân Bình", 10.8014, 106.6526),
            new Area("Gò Vấp", 10.8386, 106.6652),
            new Area("Quận 7", 10.7340, 106.7220),
            new Area("Thủ Đức", 10.8494, 106.7537),
    };

    // Tìm khu vực theo tên, không có thì lấy Quận 1
    public static Area byName(String name) {
        for (Area a : ALL) if (a.name.equals(name)) return a;
        return ALL[0];
    }
}
