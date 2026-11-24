package com.example.thien;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

// Màn hình TÀI KHOẢN (giống trang "Tôi" của Shopee): thông tin người dùng, ĐƠN MUA chia theo trạng thái
// (bấm để xem đúng loại đơn), lối tắt tới đã thích / giỏ hàng / hồ sơ / cài đặt, đăng xuất
public class AccountActivity extends AppCompatActivity {

    private static final int[] BOXES = {R.id.box0, R.id.box1, R.id.box2, R.id.box3, R.id.box4};
    private static final int[] COUNTS = {R.id.tvCount0, R.id.tvCount1, R.id.tvCount2, R.id.tvCount3, R.id.tvCount4};

    // Gắn các nút bấm (mỗi nút mở 1 màn hình)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account);
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        Ui.drawBehindStatusBar(this, findViewById(R.id.headerBox));

        findViewById(R.id.headerProfile).setOnClickListener(v -> open(ProfileActivity.class));
        findViewById(R.id.rowProfile).setOnClickListener(v -> open(ProfileActivity.class));
        findViewById(R.id.rowFavorites).setOnClickListener(v -> open(FavoritesActivity.class));
        findViewById(R.id.rowCart).setOnClickListener(v -> open(CartActivity.class));
        findViewById(R.id.rowSettings).setOnClickListener(v -> open(SettingsActivity.class));
        findViewById(R.id.rowAllOrders).setOnClickListener(v -> open(OrdersActivity.class));
        for (int step = 0; step < BOXES.length; step++) {
            int s = step;
            findViewById(BOXES[step]).setOnClickListener(v -> startActivity(new Intent(this, OrdersActivity.class)
                    .putExtra(OrdersActivity.EXTRA_STEP, s)));
        }
        findViewById(R.id.btnLogout).setOnClickListener(v -> {
            SessionManager.logout(this);
            startActivity(new Intent(this, LoginActivity.class).addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));   // xóa hết màn cũ
        });
    }

    // Mỗi lần quay lại: cập nhật tên (có thể vừa sửa hồ sơ) và số đơn theo từng trạng thái
    @Override
    protected void onResume() {
        super.onResume();
        String name = SessionManager.getDisplayName(this);
        ((TextView) findViewById(R.id.tvAvatar)).setText(name.isEmpty() ? "?" : name.substring(0, 1).toUpperCase());
        ((TextView) findViewById(R.id.tvName)).setText(name);
        ((TextView) findViewById(R.id.tvRole)).setText(SessionManager.isAdmin(this) ? R.string.role_admin : R.string.member);

        OrderManager.getOrders(SessionManager.getAccount(this), orders -> {
            int[] counts = new int[BOXES.length];
            long now = System.currentTimeMillis();
            for (Order o : orders) if (!o.items.isEmpty()) counts[o.step(now)]++;
            for (int i = 0; i < COUNTS.length; i++) {   // huy hiệu số đơn: ẩn khi 0, tối đa hiện "99+"
                TextView badge = findViewById(COUNTS[i]);
                badge.setVisibility(counts[i] == 0 ? View.GONE : View.VISIBLE);
                badge.setText(counts[i] > 99 ? "99+" : String.valueOf(counts[i]));
            }
        });
    }

    // Mở 1 màn hình
    private void open(Class<?> screen) { startActivity(new Intent(this, screen)); }
}
