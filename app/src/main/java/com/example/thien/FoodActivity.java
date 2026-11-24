package com.example.thien;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

// Màn hình CHI TIẾT MÓN: ảnh, tên, đã bán + sao, giá, Thích / Bình luận / Chia sẻ, mô tả + thông tin,
// đánh giá; thanh dưới chọn số lượng rồi thêm vào giỏ (lưu Firestore)
public class FoodActivity extends AppCompatActivity {

    public static final String EXTRA_ID = "food_id";

    private Food food;
    private String account;
    private int qty = 1;
    private boolean liked;
    private List<Review> reviews = new ArrayList<>();
    private ListenerRegistration firestore;

    // Nhận id món, gắn các nút, nghe thay đổi lượt bán / lượt thích từ Firestore
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_food);
        food = Menu.find(getIntent().getIntExtra(EXTRA_ID, -1));
        if (food == null) { finish(); return; }
        account = SessionManager.getAccount(this);

        Ui.setupToolbar(this, 0);
        findViewById(R.id.btnLike).setOnClickListener(v -> toggleLike());
        findViewById(R.id.btnComments).setOnClickListener(v -> findViewById(R.id.scroll)
                .scrollTo(0, findViewById(R.id.tvReviewsTitle).getTop()));
        findViewById(R.id.btnShare).setOnClickListener(v -> startActivity(Intent.createChooser(
                new Intent(Intent.ACTION_SEND).setType("text/plain")
                        .putExtra(Intent.EXTRA_TEXT, getString(R.string.share_text, food.name)), null)));
        findViewById(R.id.btnWriteReview).setOnClickListener(v -> showReviewDialog());
        findViewById(R.id.btnMinus).setOnClickListener(v -> changeQty(-1));
        findViewById(R.id.btnPlus).setOnClickListener(v -> changeQty(1));
        findViewById(R.id.btnAddToCart).setOnClickListener(v -> Ui.addToCart(this, food, qty, null));

        FavoriteManager.isFavorite(account, food.id, fav -> { liked = fav; showInfo(); });
        firestore = Menu.listen(food, this::showInfo);
        showInfo();
        showQty();
        showReviews();
    }

    // Thoát màn hình: ngừng nghe Firestore
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (firestore != null) firestore.remove();
    }

    // Ảnh, tên, đã bán · sao · danh mục, giá, nút Thích, mô tả và bảng thông tin
    private void showInfo() {
        Ui.showFood(findViewById(R.id.cover), food);
        ((TextView) findViewById(R.id.tvName)).setText(food.name);
        TextView shopLink = findViewById(R.id.tvShopLink);   // bấm tên quán -> trang quán
        Shop shop = Menu.findShop(food.shopId);
        shopLink.setText(getString(R.string.shop_of, shop.name));
        shopLink.setOnClickListener(v -> startActivity(new Intent(this, ShopActivity.class)
                .putExtra(ShopActivity.EXTRA_ID, shop.id)));
        ((TextView) findViewById(R.id.tvCredit)).setText(getString(R.string.photo_credit, Menu.photoCredit(food.photoId)));
        ((TextView) findViewById(R.id.tvStats)).setText(getString(R.string.food_stats,
                String.valueOf(food.sold), ratingText(), food.category));
        ((TextView) findViewById(R.id.tvPrice)).setText(Format.money(food.price));
        Ui.showOldPrice(findViewById(R.id.tvOldPrice), food);
        ((TextView) findViewById(R.id.tvDescription)).setText(food.description);

        MaterialButton like = findViewById(R.id.btnLike);
        like.setText(String.valueOf(Math.max(0, food.likes)));
        like.setIconResource(liked ? R.drawable.ic_favorite : R.drawable.ic_favorite_border);

        LinearLayout rows = findViewById(R.id.infoRows);
        rows.removeAllViews();
        Ui.addRow(rows, getString(R.string.label_category), food.category);
        Ui.addRow(rows, getString(R.string.label_calories), getString(R.string.calories_value, food.calories));
        Ui.addRow(rows, getString(R.string.label_prep), getString(R.string.prep_value, food.prepMinutes));
        Ui.addRow(rows, getString(R.string.label_spicy), food.spicy == 0 ? getString(R.string.spicy_none)
                : getResources().getStringArray(R.array.spicy_levels)[food.spicy - 1]);
    }

    // "★ 4.5 (3)" hoặc "Chưa có đánh giá"
    private String ratingText() {
        if (reviews.isEmpty()) return getString(R.string.rating_none);
        double sum = 0;
        for (Review r : reviews) sum += r.rating;
        return getString(R.string.rating_short, sum / reviews.size(), reviews.size());
    }

    // Tăng / giảm số lượng (1..MAX_QTY)
    private void changeQty(int delta) {
        int next = qty + delta;
        if (next > CartManager.MAX_QTY) {
            Toast.makeText(this, getString(R.string.max_qty, CartManager.MAX_QTY), Toast.LENGTH_SHORT).show();
            return;
        }
        qty = Math.max(1, next);
        showQty();
    }

    // Hiện số lượng và nút "Thêm n · tổng tiền"
    private void showQty() {
        ((TextView) findViewById(R.id.tvQty)).setText(String.valueOf(qty));
        ((Button) findViewById(R.id.btnAddToCart)).setText(getString(R.string.btn_add_qty, qty,
                Format.money(food.price * qty)));
    }

    // Thích / bỏ thích món (lưu Firestore, cộng / trừ lượt thích)
    private void toggleLike() {
        liked = !liked;
        food.likes += liked ? 1 : -1;
        FavoriteManager.set(account, food.id, liked);
        showInfo();
        Toast.makeText(this, liked ? R.string.favorite_added : R.string.favorite_removed, Toast.LENGTH_SHORT).show();
    }

    // Đọc đánh giá từ Firestore rồi vẽ tiêu đề + danh sách
    private void showReviews() {
        ReviewManager.getForFood(food.id, list -> {
            reviews = list;
            double sum = 0;
            for (Review r : list) sum += r.rating;
            ((TextView) findViewById(R.id.tvReviewsTitle)).setText(list.isEmpty()
                    ? getString(R.string.reviews_title, 0)
                    : getString(R.string.reviews_title_rating, list.size(), sum / list.size()));
            ((Button) findViewById(R.id.btnComments)).setText(String.valueOf(list.size()));
            showInfo();   // cập nhật số sao ở dòng thống kê

            Ui.showReviews(findViewById(R.id.reviewList), list);
        });
    }

    // Mở hộp thoại viết / sửa đánh giá (điền sẵn đánh giá cũ của mình nếu có)
    private void showReviewDialog() {
        Review mine = null;
        for (Review r : reviews) if (r.account.equals(account)) mine = r;
        ReviewDialog.show(this, food, mine, this::showReviews);
    }
}
