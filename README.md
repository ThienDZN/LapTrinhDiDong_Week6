# Midnight Bites — nền tảng phân phối đồ ăn khuya (Android, Java)

App **phân phối** (kiểu ShopeeFood): không tự nấu mà kết nối khách với các quán đối tác; quán chuẩn bị món, shipper giao tận nơi.
Quán, thực đơn, nhận xét trong app đều là **dữ liệu mô phỏng** cho đồ án. Dữ liệu người dùng lưu trên **Firebase Firestore**.

Tài khoản test: `admin / 123456` (quản trị) · `user / 123456` (khách hàng) — tự tạo trên Firestore khi mở app.
Mã giảm giá mẫu: `DEMKHUYA` (10%, tối đa 30.000 ₫).

## Nhóm thực hiện
| Thành viên | MSSV | Phần phụ trách |
|---|---|---|
| Nguyễn Minh Thiện | 24162120 | Khám phá quán, trang quán, món ăn, đánh giá / nhận xét, món đã thích, nhạc nền + cài đặt, máy tính |
| Nguyễn Song Hoàng Phúc | 24162096 | Đăng nhập / đăng ký, giỏ hàng, thanh toán, đơn hàng (hủy / trả / đặt lại), tài khoản + hồ sơ, admin mã giảm giá |

### Phân công chi tiết (mỗi người khoảng 1.500 dòng Java)
**Nguyễn Minh Thiện — nhóm "Quán & món ăn"**
- Khám phá, tìm kiếm, lọc, sắp xếp, chọn khu vực: `MainActivity`, `ShopAdapter`, `Area`, `Shop`
- Dữ liệu 72 quán và thực đơn: `Menu`
- Trang quán và trang món: `ShopActivity`, `FoodActivity`, `FoodAdapter`, `Food`
- Đánh giá món, nhận xét quán: `ReviewManager`, `ReviewDialog`, `Review`
- Món đã thích: `FavoriteManager`, `FavoritesActivity`
- Nhạc nền và cài đặt: `App`, `SettingsActivity`; Máy tính: `CalculatorActivity`, `CalculatorEngine`
- Giao diện (res/layout): `activity_main`, `activity_shop`, `activity_food`, `item_shop_*`, `item_food*`, `item_review`, `view_food_cover`, `dialog_review`, `activity_settings`, `activity_calculator`

**Nguyễn Song Hoàng Phúc — nhóm "Tài khoản & đặt hàng"**
- Đăng nhập, đăng ký, phiên đăng nhập: `LoginActivity`, `RegisterActivity`, `UserStore`, `SessionManager`
- Giỏ hàng (1 quán / đơn): `CartManager`, `CartActivity`
- Thanh toán, phí giao theo km, mã giảm giá: `CheckoutActivity`, `OrderManager`
- Đơn hàng: `Order`, `OrdersActivity`, `OrderDetailActivity` (hủy, trả hàng, đặt lại)
- Tài khoản, hồ sơ, Hello: `AccountActivity`, `ProfileActivity`, `HelloActivity`
- Admin mã giảm giá: `AdminVoucherActivity`, `VoucherManager`
- Giao diện (res/layout): `activity_login`, `activity_register`, `activity_list`, `activity_checkout`, `activity_order_detail`, `item_row`, `activity_account`, `activity_profile`, `activity_hello`, `item_info_row`, `dialog_edit_profile`, `dialog_voucher`

**Dùng chung (cả hai cùng sửa):** `Ui`, `Format`, `Cloud`, `res/values` (màu, chữ, theme), ảnh trong `res/drawable-nodpi`.

## Nộp bài
- **File giao diện:** `GiaoDien.zip` (layout, drawable, mipmap, values, menu) và thư mục `app/src/main/res`.
- **Source code:** repo này (`app/src/main/java/com/example/thien`).

## Chạy thử
Mở thư mục này bằng Android Studio, dùng file `app/google-services.json` của project Firebase `laptrinhdidong-34e42`
(đã có sẵn trong repo), rồi Sync Gradle và Run. Lệnh kiểm tra: `./gradlew testDebugUnitTest`.

## Chức năng
| # | Chức năng | File chính | Người làm |
|---|-----------|-----------|-----------|
| 1 | Đăng nhập / đăng ký (Firestore `users`, mật khẩu băm SHA-256 + salt, vai trò admin/user), tự đăng nhập lại | `LoginActivity`, `RegisterActivity`, `UserStore`, `SessionManager` | Phúc |
| 2 | Khám phá: chọn khu vực (9 khu, mỗi khu 8 quán riêng), gợi ý quán đánh giá cao, tìm kiếm, lọc loại món, sắp xếp | `MainActivity`, `ShopAdapter`, `Area`, `Shop`, `Menu` | Thiện |
| 3 | Trang quán: sao, khoảng cách, thời gian giao, giờ mở cửa, thực đơn riêng của quán | `ShopActivity`, `FoodAdapter` | Thiện |
| 4 | Nhận xét về quán (kèm nhận xét mô phỏng) + đánh giá, thích, chia sẻ từng món | `ReviewManager`, `ReviewDialog`, `FoodActivity` | Thiện |
| 5 | Giỏ hàng có số lượng, mỗi đơn chỉ 1 quán (đổi quán thì hỏi xóa giỏ) | `CartManager`, `CartActivity`, `Ui.addToCart` | Phúc |
| 6 | Thanh toán: người nhận, SĐT, địa chỉ, phương thức, mã giảm giá, ghi chú, phí giao theo km | `CheckoutActivity`, `OrderManager` | Phúc |
| 7 | Lịch sử đơn theo tài khoản + trạng thái giao + hủy đơn + trả hàng / hoàn tiền + đánh giá món + đặt lại | `OrdersActivity`, `OrderDetailActivity`, `Order` | Phúc |
| 8 | Tài khoản kiểu Shopee: đơn mua theo 5 trạng thái, lối tắt, đăng xuất | `AccountActivity` | Phúc |
| 9 | Món đã thích (Firestore `favorites`) | `FavoriteManager`, `FavoritesActivity` | Thiện |
| 10 | Admin: tạo mã (% giảm, giảm tối đa, đơn tối thiểu, hạn dùng), tạm ngưng / kích hoạt lại / hủy mã | `AdminVoucherActivity`, `VoucherManager` | Phúc |
| 11 | Cài đặt: bật/tắt nhạc nền, chỉnh âm lượng, quản trị (admin), giới thiệu | `SettingsActivity`, `App` | Thiện |
| 12 | Hồ sơ cá nhân, Máy tính, Hello (bài tuần 1) | `ProfileActivity`, `CalculatorActivity`, `HelloActivity` | Thiện (Máy tính) · Phúc (Hồ sơ, Hello) |

## Dữ liệu
- **Firestore** (project `laptrinhdidong-34e42`): `users`, `carts`, `orders`, `reviews` (đánh giá món), `shopReviews` (nhận xét quán),
  `favorites`, `vouchers`, `foods` (lượt bán / lượt thích / giá chỉnh tay của từng món, tự tạo khi có người mua hoặc thích).
- **Quán và món**: viết sẵn trong `Menu.java` — 72 quán (9 khu vực x 8), mỗi quán bán 7–10 món; 33 món mẫu có ảnh thật được sao chép cho từng quán bán.
  Id món = id quán x 100 + id món mẫu. Phí giao: 12.000 ₫ cho 2 km đầu, +4.000 ₫ mỗi km thêm, miễn phí từ 200.000 ₫.

## Ảnh và nhạc
- Ảnh món: `res/drawable-nodpi/food_<id món mẫu>.jpg` từ **Wikimedia Commons** (CC BY / CC BY-SA / CC0 / Public domain), đã cắt vuông;
  tác giả + giấy phép ghi trong `Menu.PHOTO_CREDITS` và hiện ở trang chi tiết món.
- Ảnh nền: `res/drawable-nodpi/bg_city.jpg` — ảnh nhóm tự cung cấp (chưa rõ nguồn / giấy phép gốc, xác nhận lại trước khi công bố),
  đã chỉnh màu: vùng xanh kéo về đen, giữ ánh đèn cam, làm nét.
- Nhạc nền: `res/raw/bgm.mp3` — "Đêm Thành Phố" (Lope Dope) do nhóm cung cấp, đã xử lý cho loa điện thoại (cắt bass dưới 70 Hz, tăng độ lớn
  có giới hạn đỉnh, MP3 256 kbps; bản gốc giữ ở `~/Backups`). Bản tải từ YouTube chưa phải bản có quyền phân phối: chỉ dùng cho đồ án.

## Lưu ý
Firestore đang để chế độ test (ai có app đều đọc/ghi được) — chỉ phù hợp đồ án, không dùng cho sản phẩm thật.
