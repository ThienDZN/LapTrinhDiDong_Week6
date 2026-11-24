package com.example.thien;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RatingBar;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

// Màn hình QUÁN: thông tin quán (sao, khoảng cách tới khu vực khách chọn, thời gian giao, giờ mở cửa, địa chỉ)
// và thực đơn của quán. Thêm món vào giỏ ở đây; giỏ chỉ chứa món của 1 quán.
public class ShopActivity extends AppCompatActivity {

    public static final String EXTRA_ID = "shop_id";

    private Shop shop;
    private List<Review> reviews = new ArrayList<>();   // nhận xét thật của khách (không gồm nhận xét mô phỏng)
    private final FoodAdapter adapter = new FoodAdapter(this, this::showCartBar);

    // Nhận id quán, đổ thông tin và danh sách món
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shop);
        shop = Menu.findShop(getIntent().getIntExtra(EXTRA_ID, -1));
        if (shop == null) { finish(); return; }
        Ui.drawBehindStatusBar(this, findViewById(R.id.shopHeader));
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnViewCart).setOnClickListener(v -> startActivity(new Intent(this, CartActivity.class)));

        Ui.showShopCover(findViewById(R.id.imgShop), shop);
        ((TextView) findViewById(R.id.tvShopName)).setText(shop.name);
        ((TextView) findViewById(R.id.tvShopTags)).setText(shop.tags);

        Area area = App.getArea();
        LinearLayout info = findViewById(R.id.shopInfo);
        Ui.addRow(info, getString(R.string.label_distance), Ui.distanceText(this, shop, area));
        Ui.addRow(info, getString(R.string.label_open_hours), getString(R.string.open_hours));
        Ui.addRow(info, getString(R.string.label_address), shop.address);
        Ui.addRow(info, getString(R.string.label_ship_fee), getString(R.string.shop_fee_from,
                Format.money(OrderManager.shippingFee(0, shop.distanceKm(area)))));

        RecyclerView rv = findViewById(R.id.rvFoods);
        rv.setLayoutManager(new GridLayoutManager(this, 2));
        rv.setAdapter(adapter);
        adapter.setFoods(Menu.foodsOf(shop.id));
        ReviewManager.getAverages(adapter::setRatings);   // sao từng món
        findViewById(R.id.btnWriteShopReview).setOnClickListener(v -> showShopReviewDialog());
        showShopReviews();
    }

    // Đọc nhận xét thật của quán từ Firestore, gộp với nhận xét mô phỏng, vẽ danh sách và cập nhật điểm sao quán
    private void showShopReviews() {
        ReviewManager.getShopAverages(map -> ((TextView) findViewById(R.id.tvShopRating)).setText(
                getString(R.string.shop_rating_full, shop.rating(map), Format.count(shop.ratingCount(map)))));
        ReviewManager.getForShop(shop.id, real -> {
            reviews = real;
            List<Review> all = new ArrayList<>(real);
            all.addAll(Menu.sampleReviews(shop.id));
            all.sort((x, y) -> Long.compare(y.createdAt, x.createdAt));
            ((TextView) findViewById(R.id.tvShopReviewsTitle)).setText(getString(R.string.shop_reviews_title, all.size()));

            Ui.showReviews(findViewById(R.id.shopReviewList), all);
        });
    }

    // Hộp thoại viết / sửa nhận xét về quán (điền sẵn nhận xét cũ của mình nếu có)
    private void showShopReviewDialog() {
        String account = SessionManager.getAccount(this);
        Review mine = null;
        for (Review r : reviews) if (r.account.equals(account)) mine = r;
        ReviewDialog.show(this, getString(R.string.shop_review_dialog_title, shop.name), mine,
                (rating, text) -> ReviewManager.saveShop(account, SessionManager.getDisplayName(this), shop.id,
                        rating, text, this::showShopReviews),
                () -> ReviewManager.deleteShop(account, shop.id, this::showShopReviews));
    }

    // Quay lại trang (vừa thêm món ở màn khác): cập nhật thanh giỏ hàng
    @Override
    protected void onResume() {
        super.onResume();
        showCartBar();
    }

    // Thanh "n phần · tổng tiền" ở đáy trang quán (ẩn khi giỏ trống)
    private void showCartBar() {
        CartManager.getItems(SessionManager.getAccount(this), items -> {
            View bar = findViewById(R.id.cartBar);
            bar.setVisibility(items.isEmpty() ? View.GONE : View.VISIBLE);
            ((TextView) findViewById(R.id.tvCartInfo)).setText(getString(R.string.cart_bar,
                    CartManager.count(items), Format.money(CartManager.total(items))));
        });
    }
}
