package com.example.thien;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

// Kiểm tra logic thuần Java: thực đơn, tổng tiền giỏ, phí giao, giảm giá, trạng thái đơn
public class StoreLogicTest {

    // Tất cả quán (duyệt qua từng khu vực) và tất cả món của mọi quán
    private static List<Shop> allShops() {
        List<Shop> list = new ArrayList<>();
        for (Area a : Area.ALL) list.addAll(Menu.shopsIn(a.name));
        return list;
    }

    private static List<Food> allFoods() {
        List<Food> list = new ArrayList<>();
        for (Shop s : allShops()) list.addAll(Menu.foodsOf(s.id));
        return list;
    }

    @Test
    public void thucDon_daDangVaKhongTrungId() {
        Set<Integer> ids = new HashSet<>();
        for (Food f : allFoods()) assertTrue("trùng id " + f.id, ids.add(f.id));
        assertTrue(allFoods().size() >= 500);
        for (String c : Menu.CATEGORIES) {
            boolean found = false;
            for (Food f : allFoods()) if (f.category.equals(c)) found = true;
            assertTrue("danh mục trống: " + c, found);
        }
    }

    @Test
    public void moiMonDeuCoNguonAnh() {
        for (Food f : allFoods()) assertTrue(Menu.photoCredit(f.photoId).contains("Wikimedia"));
    }

    @Test
    public void tongTienGio_giaNhanSoLuong() {
        List<Food> menu = Menu.foodsOf(1);                                    // 2 món đầu của quán số 1
        Map<Integer, Integer> cart = new LinkedHashMap<>();
        cart.put(menu.get(0).id, 2);
        cart.put(menu.get(1).id, 1);
        assertEquals(menu.get(0).price * 2 + menu.get(1).price, CartManager.total(cart), 0.001);
        assertEquals(3, CartManager.count(cart));
    }

    @Test
    public void phiGiao_theoKhoangCachVaMienPhiTu200k() {
        assertEquals(12000, OrderManager.shippingFee(100000, 1.0), 0.001);    // trong 2 km đầu
        assertEquals(12000, OrderManager.shippingFee(100000, 2.0), 0.001);
        assertEquals(16000, OrderManager.shippingFee(100000, 2.5), 0.001);    // km thứ 3 (làm tròn lên) +4.000
        assertEquals(24000, OrderManager.shippingFee(100000, 5.0), 0.001);    // 12.000 + 3 km x 4.000
        assertEquals(0, OrderManager.shippingFee(200000, 5.0), 0.001);        // đơn từ 200.000 miễn phí
    }

    @Test
    public void moiKhuVucCoQuanRiengTenKhacNhau() {
        Set<String> names = new HashSet<>();
        for (Area a : Area.ALL) {
            assertEquals(a.name, 8, Menu.shopsIn(a.name).size());             // mỗi khu vực 8 quán
            for (Shop s : Menu.shopsIn(a.name)) {
                assertTrue("trùng tên quán: " + s.name, names.add(s.name));   // không quán nào trùng tên ở khu vực khác
                assertTrue(s.distanceKm(a) < 3.0);                            // quán nằm gần trung tâm khu vực
            }
        }
        assertEquals(72, names.size());
        Set<Double> ratings = new HashSet<>();
        for (Shop s : allShops()) { assertTrue(s.baseRating >= 4.2 && s.baseRating <= 4.8); ratings.add(s.baseRating); }
        assertTrue("điểm sao phải khác nhau giữa các quán", ratings.size() >= 5);
    }

    @Test
    public void quan_goiYVaDiemSao() {
        Map<Integer, double[]> none = new java.util.HashMap<>();
        Area q1 = Area.byName("Quận 1");
        Shop near = null, far = null;
        for (Shop s : Menu.shopsIn("Quận 1")) if (near == null || s.distanceKm(q1) < near.distanceKm(q1)) near = s;
        for (Shop s : allShops()) if (far == null || s.distanceKm(q1) > far.distanceKm(q1)) far = s;
        assertTrue(near.distanceKm(q1) < far.distanceKm(q1));
        assertTrue(near.deliveryMinutes(q1) <= far.deliveryMinutes(q1));
        assertTrue(near.suggestScore(q1, none) > far.suggestScore(q1, none) - 1.0);
        for (Shop s : allShops()) assertTrue(s.name, Menu.foodsOf(s.id).size() >= 7);   // mỗi quán bán từ 7 món
        for (Food f : allFoods()) assertNotNull(Menu.findShop(f.shopId));
    }

    @Test
    public void thuDuc_coKhuVucVaQuanRieng() {
        assertEquals("Thủ Đức", Area.byName("Thủ Đức").name);                 // không có thì byName trả về Quận 1
        boolean found = false;
        for (Shop s : Menu.shopsIn("Thủ Đức")) if (s.name.equals("Mì Cay Sasin Night")) found = true;
        assertTrue(found);
    }

    @Test
    public void quan_diemSaoGopDanhGiaThat() {
        Shop s = Menu.findShop(2);                                            // quán số 2 (Quận 1)
        Map<Integer, double[]> reviews = new java.util.HashMap<>();
        assertEquals(s.baseRating, s.rating(reviews), 0.0001);
        reviews.put(2, new double[]{1.0, 600});                               // 600 nhận xét 1 sao cho quán -> điểm quán giảm
        assertTrue(s.rating(reviews) < s.baseRating);
        assertEquals(s.baseCount + 600, s.ratingCount(reviews));
    }

    @Test
    public void moiQuanCoNhanXetMoPhong() {
        for (Shop s : allShops()) {
            assertTrue(s.name, Menu.sampleReviews(s.id).size() >= 3);
            for (Review r : Menu.sampleReviews(s.id)) assertTrue(r.rating >= 1 && r.rating <= 5 && r.content.length() > 10);
        }
    }

    @Test
    public void maGiamGia_dieuKien() {
        long now = 1_000_000;
        VoucherManager.Voucher v = new VoucherManager.Voucher("TEST", 10, 30000, 100000, now + 1000, true);
        assertEquals(13000, VoucherManager.discount(v, 130000, now), 0.001);     // 10%
        assertEquals(30000, VoucherManager.discount(v, 500000, now), 0.001);     // chạm mức tối đa
        assertEquals(0, VoucherManager.discount(v, 90000, now), 0.001);          // chưa đủ đơn tối thiểu
        assertEquals(0, VoucherManager.discount(v, 130000, now + 2000), 0.001);  // hết hạn
        VoucherManager.Voucher off = new VoucherManager.Voucher("OFF", 10, 0, 0, now + 1000, false);
        assertEquals(0, VoucherManager.discount(off, 130000, now), 0.001);       // đang tạm ngưng
        assertEquals(0, VoucherManager.discount(null, 130000, now), 0.001);      // không có mã
    }

    @Test
    public void trangThaiDon_theoThoiGian() {
        Order o = new Order();
        o.createdAt = 0;
        assertEquals(0, o.step(4 * Order.MINUTE));
        assertEquals(1, o.step(10 * Order.MINUTE));
        assertEquals(2, o.step(30 * Order.MINUTE));
        assertTrue(o.canCancel(4 * Order.MINUTE));
        o.cancelled = true;
        assertEquals(Order.CANCELLED, o.step(4 * Order.MINUTE));
        assertNotNull(Menu.find(101));                                        // món đầu của quán 1 = 1 x 100 + món mẫu 1
    }

    @Test
    public void traHang_chiKhiDaGiaoVaTrong3Ngay() {
        Order o = new Order();
        o.createdAt = 0;
        long delivered = 25 * Order.MINUTE;
        assertEquals(false, o.canReturn(10 * Order.MINUTE));                              // chưa giao
        assertEquals(true, o.canReturn(delivered + Order.MINUTE));                        // vừa giao
        assertEquals(true, o.canReturn(delivered + Order.RETURN_WINDOW));                 // đúng hạn chót
        assertEquals(false, o.canReturn(delivered + Order.RETURN_WINDOW + Order.MINUTE)); // quá 3 ngày
        o.returnRequested = true;
        assertEquals(Order.RETURNED, o.step(delivered + Order.MINUTE));
        assertEquals(false, o.canReturn(delivered + Order.MINUTE));                       // đã gửi rồi thì không gửi lại
        Order c = new Order();
        c.cancelled = true;
        assertEquals(false, c.canReturn(delivered + Order.MINUTE));                       // đơn đã hủy thì không trả
    }
}
