package com.example.thien;

import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Danh sách quán và thực đơn của từng quán (dữ liệu MÔ PHỎNG cho đồ án): 9 khu vực x 8 quán = 72 quán,
// mỗi khu vực có quán riêng với tên khác nhau. 33 món mẫu có ảnh thật được sao chép cho từng quán bán
// (món của quán nào thì có id riêng, giá chênh nhẹ giữa các quán).
// Lượt bán / lượt thích / giá chỉnh tay của từng món nằm ở Firestore (collection "foods").
public final class Menu {

    public static final String COLLECTION = "foods";
    public static final String[] CATEGORIES = {"Cơm", "Bún & Mì", "Đồ nướng", "Ăn vặt", "Lẩu",
            "Fast food", "Đồ uống", "Tráng miệng"};

    private static final List<Food> BASE = new ArrayList<>();                 // 33 món mẫu (id 1..33)
    private static final List<Food> FOODS = new ArrayList<>();                // món của tất cả các quán
    private static final Map<Integer, Food> FOOD_BY_ID = new HashMap<>();
    private static final List<Shop> SHOPS = new ArrayList<>();

    // 9 nhóm quán: 0 cơm, 1 phở/bún, 2 mì, 3 nướng, 4 ăn vặt, 5 lẩu, 6 fast food, 7 đồ uống, 8 chè/tráng miệng
    private static final int[][] THEME_DISHES = {   // id món mẫu mà mỗi nhóm bán (món chính + ăn kèm + đồ uống)
            {1, 2, 3, 4, 17, 29, 30},                    // cơm
            {5, 6, 9, 10, 20, 19, 27, 30},               // phở / bún
            {7, 8, 18, 20, 16, 26, 28, 29},              // mì
            {11, 12, 13, 14, 17, 18, 26, 28},            // nướng
            {15, 16, 17, 18, 19, 20, 26, 27, 29, 32},    // ăn vặt
            {21, 22, 20, 13, 18, 28, 29, 26},            // lẩu
            {23, 24, 25, 17, 18, 26, 29, 32},            // fast food
            {26, 27, 28, 29, 30, 31, 32, 33},            // đồ uống
            {31, 32, 33, 26, 27, 28, 29, 30}};           // chè / tráng miệng
    private static final String[] THEME_TAGS = {"Cơm tấm · Cơm chiên", "Phở · Bún · Cháo", "Mì cay · Mì xào",
            "Đồ nướng · BBQ", "Ăn vặt · Bánh mì chảo", "Lẩu Thái · Lẩu gà", "Pizza · Burger · Gà rán",
            "Trà sữa · Cà phê · Nước ép", "Chè · Flan · Kem"};
    private static final int[] THEME_COVER = {1, 5, 7, 11, 16, 21, 24, 26, 33};   // món mẫu làm ảnh bìa quán

    // Quán theo khu vực: "tên khu vực|nhóm|tên quán"
    private static final String[] SHOP_TABLE = {
            // Quận 1
            "Quận 1|0|Cơm Tấm Bụi Đêm",
            "Quận 1|1|Phở Hòa Ban Đêm",
            "Quận 1|2|Mì Cay Đèn Lồng",
            "Quận 1|3|Nướng Phố Cổ Quận 1",
            "Quận 1|4|Ăn Vặt Hẻm Sài Gòn",
            "Quận 1|5|Lẩu Nấm Đêm Mưa",
            "Quận 1|6|Burger Góc Phố",
            "Quận 1|7|Moon Tea & Coffee",
            // Quận 3
            "Quận 3|0|Cơm Chiên Cô Lan",
            "Quận 3|1|Phở & Bún Đêm Lệ",
            "Quận 3|2|Mì Xào Bà Tám",
            "Quận 3|3|Nướng Than Đêm Quận 3",
            "Quận 3|4|Bánh Mì Chảo Cô Tư",
            "Quận 3|5|Lẩu Đêm Hoàng Gia",
            "Quận 3|7|Trà Sữa Hẻm Xanh",
            "Quận 3|8|Chè Nhà Bà Ngoại",
            // Quận 10
            "Quận 10|0|Cơm Sườn Mười Hai",
            "Quận 10|1|Cháo Sườn Đêm Quận 10",
            "Quận 10|2|Mì Cay Hỏa Long",
            "Quận 10|3|Nướng Than Hồng",
            "Quận 10|4|Ăn Vặt Chợ Đêm Hòa Hưng",
            "Quận 10|6|Gà Rán Quận 10",
            "Quận 10|7|Trà Sữa Bông",
            "Quận 10|8|Chè Hạt Đêm",
            // Bình Thạnh
            "Bình Thạnh|0|Cơm Gà Cô Hạnh",
            "Bình Thạnh|1|Bún Bò Đêm Xưa",
            "Bình Thạnh|2|Mì Trộn Đêm Xanh",
            "Bình Thạnh|3|Nướng Đêm 79",
            "Bình Thạnh|5|Lẩu Thái Bình Thạnh",
            "Bình Thạnh|6|Pizza & Burger Bình Thạnh",
            "Bình Thạnh|7|Cà Phê Muối Đêm",
            "Bình Thạnh|8|Chè Bưởi Cô Sáu",
            // Phú Nhuận
            "Phú Nhuận|0|Cơm Tấm Sườn Cọng",
            "Phú Nhuận|1|Cháo Lòng Hẻm Nhỏ",
            "Phú Nhuận|2|Mì Xào Bếp Nhà",
            "Phú Nhuận|3|Nướng Phố Nhuận",
            "Phú Nhuận|4|Ăn Vặt Cô Ba",
            "Phú Nhuận|5|Lẩu Gà Ớt Hiểm",
            "Phú Nhuận|7|Trà Chanh Đêm Nhuận",
            "Phú Nhuận|8|Tiệm Chè Hạt Sen",
            // Tân Bình
            "Tân Bình|0|Cơm Gà Sân Bay",
            "Tân Bình|1|Hủ Tiếu Gõ Đêm",
            "Tân Bình|3|Nướng Đêm Tân Bình",
            "Tân Bình|4|Bánh Tráng Cô Bảy",
            "Tân Bình|5|Lẩu Gà Lá É Bà Sáu",
            "Tân Bình|6|Gà Rán Sấm Sét",
            "Tân Bình|7|Nước Ép Sân Bay",
            "Tân Bình|8|Tiệm Chè Mơ",
            // Gò Vấp
            "Gò Vấp|0|Cơm Gà Ông Năm",
            "Gò Vấp|1|Phở Bò Gò Vấp",
            "Gò Vấp|2|Mì Xào Bếp Lửa",
            "Gò Vấp|3|Nướng Đêm Gò Vấp",
            "Gò Vấp|4|Xiên Que Cô Tư",
            "Gò Vấp|5|Lẩu Mắm Miền Tây",
            "Gò Vấp|6|Pizza Lò Củi Gò Vấp",
            "Gò Vấp|7|Trà Đào Phố Đêm",
            // Quận 7
            "Quận 7|0|Cơm Gà Hải Nam Quận 7",
            "Quận 7|1|Bún Mắm Phú Mỹ",
            "Quận 7|3|Nướng Hồ Đêm",
            "Quận 7|4|Ăn Vặt Phố Đêm Quận 7",
            "Quận 7|5|Lẩu Thái Nam Sài Gòn",
            "Quận 7|6|Pizza & Burger 24h",
            "Quận 7|7|Sinh Tố Cầu Kênh",
            "Quận 7|8|Chè & Kem Phú Mỹ",
            // Thủ Đức
            "Thủ Đức|0|Cơm Sinh Viên Thủ Đức",
            "Thủ Đức|1|Bún Bò Huế O Hà",
            "Thủ Đức|2|Mì Cay Sasin Night",
            "Thủ Đức|3|Nướng Ký Túc",
            "Thủ Đức|4|Bánh Tráng Trộn Làng Đại Học",
            "Thủ Đức|6|Burger Đêm Thủ Đức",
            "Thủ Đức|7|Trà Sữa Mùa Thi",
            "Thủ Đức|8|Chè Khúc Bạch Cô Mai"
    };

    // Nhận xét mô phỏng: mỗi nhóm quán có 3 nhận xét mẫu {tác giả, số sao, nội dung, số ngày trước}
    private static final Object[][][] SAMPLE_REVIEWS = {
            {{"Ngọc Lan", 5, "Sườn nướng thơm, cơm dẻo, giao nhanh lúc 1h sáng.", 2}, {"Quang Minh", 5, "Bì chả ngon, nước mắm vừa miệng.", 6}, {"Thu Hòa", 4, "Giờ cao điểm hơi đông nên chờ lâu một chút.", 11}},
            {{"Anh Tuấn", 5, "Nước dùng ngọt thanh, bò mềm, ăn khuya cực hợp.", 1}, {"Đăng Khoa", 5, "Bún bò Huế đậm vị, cay đúng ý.", 5}, {"Tường Vy", 4, "Cháo lòng nóng hổi nhưng quẩy hơi nguội.", 9}},
            {{"Gia Bảo", 4, "Cấp độ 3 là vừa, hải sản tươi.", 3}, {"Mỹ Linh", 4, "Nước dùng cay nồng, hơi mặn một chút.", 8}, {"Hoàng Nam", 5, "Mì xào thơm, phần ăn đầy đặn.", 14}},
            {{"Thanh Tâm", 5, "Xiên nướng thơm khói, chấm muối ớt rất ngon.", 2}, {"Văn Hậu", 4, "Bạch tuộc sa tế giòn nhưng rất cay.", 7}, {"Bảo Ngọc", 5, "Đóng gói kỹ, tới nơi vẫn còn nóng.", 10}},
            {{"Khánh Linh", 5, "Bánh mì chảo đầy topping, trứng chín vừa.", 1}, {"Đức Anh", 5, "Bánh tráng trộn giống ngoài hàng, ngon.", 4}, {"Phương Thảo", 4, "Há cảo hơi ít nhân.", 12}},
            {{"Minh Châu", 5, "Nước lẩu Thái chua cay chuẩn vị, hải sản tươi.", 3}, {"Trọng Nghĩa", 5, "Lẩu gà lá é thơm, nước ngọt tự nhiên.", 6}, {"Hải Yến", 4, "Giao hơi lâu vì đơn ở xa.", 13}},
            {{"Quốc Bảo", 4, "Pizza hải sản nhiều topping, phô mai kéo sợi.", 2}, {"Yến Nhi", 5, "Burger bò mọng, khoai chiên giòn.", 5}, {"Thành Đạt", 3, "Gà rán hơi béo so với khẩu vị.", 9}},
            {{"Bích Phượng", 5, "Trà sữa trân châu dẻo, ít ngọt đúng ý.", 1}, {"Tuấn Kiệt", 5, "Cà phê muối béo mặn, lạ miệng.", 4}, {"Diễm My", 4, "Trà đào cam sả thơm mát.", 8}},
            {{"Hồng Nhung", 5, "Chè khúc bạch mát, ngọt thanh.", 2}, {"Việt Hùng", 5, "Flan mềm mịn, caramel vừa vặn.", 6}, {"Kim Ngân", 4, "Kem dừa đẹp mắt, hơi tan khi giao tới.", 10}},
    };

    private static final String[] AUTHORS = {"Ngọc Lan", "Quang Minh", "Thu Hòa", "Anh Tuấn", "Đăng Khoa", "Tường Vy",
            "Gia Bảo", "Mỹ Linh", "Hoàng Nam", "Thanh Tâm", "Văn Hậu", "Bảo Ngọc"};

    private static final String[] PHOTO_CREDITS = {
            "Christopher Crouzet · CC BY-SA 4.0",
            "LN9267 · CC BY-SA 4.0",
            "Misaochan · CC BY 4.0",
            "Ralff Nestor Nacor · CC BY-SA 4.0",
            "Kritzolina · CC BY-SA 4.0",
            "Kham Tran - www.khamtran.com · CC BY 3.0",
            "Mobius6 · CC BY-SA 4.0",
            "Dion Hinchcliffe · CC BY-SA 2.0",
            "Tonbi ko · CC BY-SA 4.0",
            "hongnhung106 · CC BY 2.0",
            "PattayaPatrol · CC BY-SA 4.0",
            "Phương Huy · CC0",
            "Daderot · CC0",
            "jeffreyw · CC BY 2.0",
            "Vinnie Cartabiano · CC BY 2.0",
            "Memberofc1 · CC BY-SA 3.0",
            "Tony Webster from Minneapolis, Minnesota, United States · CC BY-SA 2.0",
            "jeffreyw · CC BY 2.0",
            "Aterux at Vietnamese Wikipedia · CC BY-SA 3.0",
            "sfllaw of Flickr · CC BY-SA 2.0",
            "Vee Satayamas · CC BY 4.0",
            "Newone · CC BY-SA 4.0",
            "投稿者 · Public domain",
            "Acabashi · CC BY-SA 4.0",
            "Vyacheslav Argenberg · CC BY 4.0",
            "Oqmilteashop · CC BY-SA 3.0",
            "Shreveport-Bossier Convention and Tourist Bureau · CC BY 2.0",
            "Geoffreyrabbit · CC BY-SA 4.0",
            "Agency of the United States Department of Agriculture Edited · Public domain",
            "Mr.ちゅらさん · CC BY-SA 4.0",
            "Syced · CC0",
            "BokuAlec · CC BY-SA 3.0",
            "Anilakeo · CC BY-SA 4.0"
    };


    // 33 món mẫu: id, tên, danh mục, giá, giá gốc, kcal, phút chuẩn bị, độ cay, mô tả
    static {
        add(1, "Cơm tấm sườn bì chả", "Cơm", 45000, 0, 750, 10, 0, "Sườn nướng than hồng, bì, chả trứng, mỡ hành, nước mắm chua ngọt.");
        add(2, "Cơm chiên dương châu", "Cơm", 40000, 0, 680, 10, 0, "Cơm chiên tơi với lạp xưởng, tôm, trứng, đậu Hà Lan.");
        add(3, "Cơm gà Hải Nam", "Cơm", 45000, 50000, 650, 12, 0, "Gà luộc da vàng mềm, cơm nấu nước dùng gà, dưa leo, nước chấm gừng ớt.");
        add(4, "Cơm chiên bò sa tế", "Cơm", 50000, 0, 720, 12, 2, "Cơm chiên tơi với thịt bò xào sa tế thơm cay, trứng và hành lá.");
        add(5, "Phở bò tái chín", "Bún & Mì", 50000, 0, 550, 8, 0, "Nước dùng hầm xương 12 tiếng, bò tái, bò chín, hành, ngò gai.");
        add(6, "Bún bò Huế", "Bún & Mì", 50000, 0, 600, 8, 2, "Nước lèo sả ruốc, giò heo, chả cua, thêm sa tế cay nồng.");
        add(7, "Mì cay 7 cấp độ", "Bún & Mì", 49000, 59000, 640, 10, 3, "Mì Hàn Quốc nước dùng kim chi, hải sản, chọn độ cay từ 1 tới 7.");
        add(8, "Mì xào hải sản", "Bún & Mì", 55000, 0, 700, 12, 1, "Mì trứng xào tôm, mực, cải ngọt, sốt dầu hào.");
        add(9, "Hủ tiếu Nam Vang", "Bún & Mì", 45000, 0, 520, 8, 0, "Hủ tiếu dai, tôm, gan, thịt bằm, nước dùng ngọt thanh.");
        add(10, "Cháo lòng nóng", "Bún & Mì", 35000, 0, 480, 6, 0, "Cháo lòng đậm vị, kèm quẩy giòn — món ăn khuya kinh điển.");
        add(11, "Xiên nướng thập cẩm", "Đồ nướng", 60000, 0, 650, 15, 1, "10 xiên: bò cuộn nấm, gà, xúc xích, đậu bắp, nấm kim châm.");
        add(12, "Gà nướng muối ớt", "Đồ nướng", 120000, 140000, 1100, 25, 2, "Nửa con gà ta nướng muối ớt, kèm rau răm và muối tiêu chanh.");
        add(13, "Bạch tuộc nướng sa tế", "Đồ nướng", 90000, 0, 420, 15, 3, "Bạch tuộc tươi nướng sa tế, giòn sần sật.");
        add(14, "Sườn nướng BBQ", "Đồ nướng", 95000, 0, 900, 20, 0, "Sườn heo sốt BBQ mật ong, nướng mềm rụm.");
        add(15, "Bánh mì chảo", "Ăn vặt", 40000, 0, 620, 10, 0, "Trứng ốp la, pate, xúc xích, bò viên trên chảo gang nóng.");
        add(16, "Bánh tráng trộn", "Ăn vặt", 25000, 0, 350, 5, 2, "Bánh tráng, xoài, trứng cút, bò khô, rau răm, sa tế.");
        add(17, "Khoai tây chiên", "Ăn vặt", 30000, 0, 420, 7, 0, "Khoai tây chiên giòn, kèm tương ớt và mayonnaise.");
        add(18, "Xúc xích nướng", "Ăn vặt", 20000, 0, 300, 5, 0, "2 cây xúc xích Đức nướng, chấm tương ớt.");
        add(19, "Trứng vịt lộn rau răm", "Ăn vặt", 15000, 0, 180, 5, 0, "Trứng vịt lộn luộc, rau răm, gừng, muối tiêu chanh.");
        add(20, "Há cảo hấp", "Ăn vặt", 45000, 0, 380, 12, 0, "6 viên há cảo tôm thịt nóng hổi, nước chấm xì dầu.");
        add(21, "Lẩu Thái hải sản", "Lẩu", 250000, 290000, 1600, 20, 2, "Cho 2–3 người: tôm, mực, nghêu, nấm, nước lẩu chua cay.");
        add(22, "Lẩu gà lá é", "Lẩu", 220000, 0, 1500, 20, 1, "Cho 2–3 người: gà ta, lá é Đà Lạt, măng chua, bún tươi.");
        add(23, "Pizza hải sản", "Fast food", 129000, 0, 1400, 18, 0, "Pizza 9 inch: tôm, mực, phô mai mozzarella kéo sợi.");
        add(24, "Burger bò phô mai", "Fast food", 69000, 79000, 780, 10, 0, "Bò Úc nướng, phô mai cheddar, xà lách, sốt đặc biệt.");
        add(25, "Gà rán giòn (3 miếng)", "Fast food", 79000, 0, 900, 12, 1, "Gà rán da giòn, kèm tương ớt và khoai chiên nhỏ.");
        add(26, "Trà sữa trân châu", "Đồ uống", 35000, 0, 320, 5, 0, "Trà đen sữa tươi, trân châu đường đen.");
        add(27, "Cà phê muối", "Đồ uống", 30000, 0, 180, 5, 0, "Cà phê phin đậm vị với lớp kem muối béo mặn.");
        add(28, "Trà đào cam sả", "Đồ uống", 35000, 0, 150, 5, 0, "Trà đào, miếng đào ngâm, cam vàng, sả thơm.");
        add(29, "Nước ép cam", "Đồ uống", 30000, 0, 120, 5, 0, "Cam vắt tươi 100%, không thêm đường.");
        add(30, "Sữa đậu nành nóng", "Đồ uống", 15000, 0, 130, 3, 0, "Sữa đậu nành nấu tại quán, ấm bụng về khuya.");
        add(31, "Chè khúc bạch", "Tráng miệng", 30000, 0, 280, 5, 0, "Khúc bạch phô mai, nhãn, hạnh nhân lát, nước đường thanh.");
        add(32, "Bánh flan caramel", "Tráng miệng", 20000, 0, 220, 3, 0, "Flan mềm mịn, caramel đắng nhẹ, thêm cà phê đá bào.");
        add(33, "Kem dừa", "Tráng miệng", 35000, 0, 300, 3, 0, "Kem dừa trong trái dừa tươi, rắc đậu phộng.");
    }

    // Tạo 72 quán và thực đơn riêng của từng quán (chạy sau khi đã có 33 món mẫu)
    static {
        int shopId = 0;
        for (String row : SHOP_TABLE) {
            if (row.startsWith("//")) continue;
            String[] p = row.split("\\|");
            Area area = Area.byName(p[0]);
            int theme = Integer.parseInt(p[1]);
            shopId++;
            // toạ độ lệch nhẹ quanh trung tâm khu vực; sao và số đánh giá ban đầu khác nhau giữa các quán
            double lat = area.lat + ((shopId * 37) % 9 - 4) * 0.0035;
            double lng = area.lng + ((shopId * 53) % 9 - 4) * 0.0035;
            double rating = 4.2 + (shopId * 3 % 7) / 10.0;
            int count = 300 + shopId * 379 % 2700;
            SHOPS.add(new Shop(shopId, p[2], THEME_TAGS[theme], "Khu ẩm thực đêm, " + area.name, area.name,
                    theme, lat, lng, rating, count, THEME_COVER[theme]));
            for (int baseId : THEME_DISHES[theme]) {
                Food f = new Food(BASE.get(baseId - 1), shopId * 100 + baseId, shopId);
                FOODS.add(f);
                FOOD_BY_ID.put(f.id, f);
            }
        }
    }

    // Class tiện ích, không tạo object
    private Menu() { }

    // Thêm 1 món mẫu
    private static void add(int id, String name, String category, double price, double oldPrice,
                            int calories, int prepMinutes, int spicy, String description) {
        BASE.add(new Food(id, name, category, price, oldPrice, calories, prepMinutes, spicy, description));
    }

    // Dòng ghi nguồn ảnh của món (photoId = id món mẫu; bắt buộc với ảnh giấy phép CC)
    public static String photoCredit(int photoId) {
        return photoId >= 1 && photoId <= PHOTO_CREDITS.length ? PHOTO_CREDITS[photoId - 1] + " · Wikimedia Commons" : "";
    }

    // Các quán của 1 khu vực
    public static List<Shop> shopsIn(String areaName) {
        List<Shop> list = new ArrayList<>();
        for (Shop s : SHOPS) if (s.area.equals(areaName)) list.add(s);
        return list;
    }

    // Tìm quán theo id, không có thì null
    public static Shop findShop(int id) {
        return id >= 1 && id <= SHOPS.size() ? SHOPS.get(id - 1) : null;
    }

    // Các món của 1 quán
    public static List<Food> foodsOf(int shopId) {
        List<Food> list = new ArrayList<>();
        for (Food f : FOODS) if (f.shopId == shopId) list.add(f);
        return list;
    }

    // Tìm món theo id, không có thì null
    public static Food find(int id) { return FOOD_BY_ID.get(id); }

    // Nhận xét mô phỏng của 1 quán (3 nhận xét theo nhóm quán, tác giả xoay vòng theo quán), mới nhất trước
    public static List<Review> sampleReviews(int shopId) {
        Shop shop = findShop(shopId);
        List<Review> list = new ArrayList<>();
        long now = System.currentTimeMillis();
        for (int k = 0; k < 3; k++) {
            Object[] r = SAMPLE_REVIEWS[shop.theme][k];
            String author = AUTHORS[(shopId * 3 + k) % AUTHORS.length];
            int days = 1 + (shopId * 5 + k * 4) % 14;
            list.add(new Review("demo_" + author, author, (Integer) r[1], (String) r[2], now - days * 24L * 60 * 60 * 1000));
        }
        return list;
    }

    // Lắng nghe Firestore của 1 món: sửa tên / giá / mô tả trên console hoặc có người mua / thích thì app cập nhật ngay.
    // Nhớ remove() khi thoát màn hình.
    public static ListenerRegistration listen(Food f, Runnable onChange) {
        return Cloud.db().collection(COLLECTION).document(String.valueOf(f.id)).addSnapshotListener((d, e) -> {
            if (e != null || d == null || !d.exists()) return;
            if (d.getString("name") != null) f.name = d.getString("name");
            if (d.getDouble("price") != null) f.price = d.getDouble("price");
            if (d.getString("description") != null) f.description = d.getString("description");
            if (d.getLong("sold") != null) f.sold = d.getLong("sold");
            if (d.getLong("likes") != null) f.likes = d.getLong("likes");
            onChange.run();
        });
    }

    // Cộng (hoặc trừ) số lượng đã bán của món; tự tạo document nếu chưa có
    public static void addSold(int foodId, int qty) {
        Cloud.db().collection(COLLECTION).document(String.valueOf(foodId))
                .set(Collections.singletonMap("sold", FieldValue.increment(qty)), SetOptions.merge());
    }
}
