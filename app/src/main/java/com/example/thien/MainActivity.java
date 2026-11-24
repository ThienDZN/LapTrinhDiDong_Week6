package com.example.thien;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Màn hình KHÁM PHÁ (Home) của nền tảng phân phối đồ ăn khuya: khách chọn khu vực nhận hàng, app hiện các quán
// của khu vực đó và GỢI Ý quán ngon / đánh giá cao; khách cũng có thể tự tìm, lọc theo loại món, sắp xếp rồi chọn quán.
public class MainActivity extends AppCompatActivity {

    private final ShopAdapter nearbyAdapter = new ShopAdapter(R.layout.item_shop_row);
    private final ShopAdapter suggestAdapter = new ShopAdapter(R.layout.item_shop_card);
    private String query = "";                 // từ khóa đang tìm
    private String category = null;            // loại món đang lọc, null = tất cả
    private int sort = 0;                      // 0 gợi ý, 1 gần nhất, 2 đánh giá cao, 3 giao nhanh
    private Map<Integer, double[]> reviews = new HashMap<>();   // id quán -> {sao trung bình, số nhận xét thật}

    // Gắn giao diện, thanh công cụ, các danh sách, ô tìm kiếm và chip loại món
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Ui.drawBehindStatusBar(this, findViewById(R.id.header));
        ((TextView) findViewById(R.id.tvWelcome)).setText(
                getString(R.string.welcome_user, SessionManager.getDisplayName(this)));

        ((MaterialToolbar) findViewById(R.id.toolbar)).setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_cart) startActivity(new Intent(this, CartActivity.class));
            if (item.getItemId() == R.id.action_settings) startActivity(new Intent(this, SettingsActivity.class));
            return true;
        });
        setupBottomNav();

        RecyclerView rvSuggest = findViewById(R.id.rvSuggest);
        rvSuggest.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvSuggest.setAdapter(suggestAdapter);
        RecyclerView rvShops = findViewById(R.id.rvShops);
        rvShops.setLayoutManager(new LinearLayoutManager(this));
        rvShops.setAdapter(nearbyAdapter);

        // Chip "Tất cả" + 1 chip cho mỗi loại món
        ChipGroup chips = findViewById(R.id.chipCategories);
        addChip(chips, getString(R.string.filter_all), null).setChecked(true);
        for (String c : Menu.CATEGORIES) addChip(chips, c, c);

        // Gõ tìm kiếm -> lọc lại
        ((EditText) findViewById(R.id.edtSearch)).addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void afterTextChanged(Editable e) {
                query = e.toString().trim().toLowerCase();
                showShops();
            }
        });

        findViewById(R.id.rowArea).setOnClickListener(v -> showAreaDialog());
        findViewById(R.id.tvSort).setOnClickListener(v -> showSortDialog());
        findViewById(R.id.tvCartSub).setOnClickListener(v -> startActivity(new Intent(this, CartActivity.class)));
        findViewById(R.id.hero).setOnClickListener(v -> Ui.showAbout(this));

        VoucherManager.createDefault();   // tạo sẵn mã giảm giá mẫu nếu chưa có
        showShops();
    }

    // Quay lại màn hình: cập nhật giỏ hàng, tab đang chọn và điểm sao các quán
    @Override
    protected void onResume() {
        super.onResume();
        showCart();
        ((BottomNavigationView) findViewById(R.id.bottomNav)).setSelectedItemId(R.id.tab_menu);
        ReviewManager.getShopAverages(map -> {
            reviews = map;
            showShops();
        });
    }

    // Thanh điều hướng dưới: mở màn hình tương ứng (tab Khám phá là màn hiện tại)
    private void setupBottomNav() {
        BottomNavigationView nav = findViewById(R.id.bottomNav);
        nav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.tab_favorites) startActivity(new Intent(this, FavoritesActivity.class));
            else if (id == R.id.tab_cart) startActivity(new Intent(this, CartActivity.class));
            else if (id == R.id.tab_account) startActivity(new Intent(this, AccountActivity.class));
            return id == R.id.tab_menu;
        });
    }

    // Dòng "Giỏ hàng: x phần · tổng tiền" (ẩn nếu trống)
    private void showCart() {
        CartManager.getItems(SessionManager.getAccount(this), items -> {
            TextView cart = findViewById(R.id.tvCartSub);
            cart.setVisibility(items.isEmpty() ? View.GONE : View.VISIBLE);
            cart.setText(getString(R.string.cart_count, CartManager.count(items), Format.money(CartManager.total(items))));
        });
    }

    // Tạo 1 chip lọc; bấm vào thì lọc quán có món thuộc loại đó
    private Chip addChip(ChipGroup group, String text, String categoryName) {
        Chip chip = new Chip(this);
        chip.setText(text);
        chip.setCheckable(true);
        chip.setOnClickListener(v -> {
            category = categoryName;
            showShops();
        });
        group.addView(chip);
        return chip;
    }

    // Hộp thoại chọn khu vực nhận hàng; đổi khu vực thì tính lại khoảng cách và gợi ý
    private void showAreaDialog() {
        String[] names = new String[Area.ALL.length];
        int current = 0;
        for (int i = 0; i < names.length; i++) {
            names[i] = Area.ALL[i].name;
            if (names[i].equals(App.getArea().name)) current = i;
        }
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.pick_area)
                .setSingleChoiceItems(names, current, (d, which) -> {
                    App.setArea(names[which]);
                    showShops();
                    d.dismiss();
                })
                .show();
    }

    // Hộp thoại chọn cách sắp xếp danh sách quán
    private void showSortDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.sort_title)
                .setSingleChoiceItems(R.array.sort_options, sort, (d, which) -> {
                    sort = which;
                    showShops();
                    d.dismiss();
                })
                .show();
    }

    // Lọc quán theo từ khóa (tên quán, loại món, tên món) + loại món, sắp xếp rồi hiển thị;
    // dải "Gợi ý" chỉ hiện khi khách chưa tìm / lọc
    private void showShops() {
        Area area = App.getArea();
        ((TextView) findViewById(R.id.rowArea)).setText(getString(R.string.deliver_to, area.name));
        ((TextView) findViewById(R.id.tvSort)).setText(getResources().getStringArray(R.array.sort_options)[sort]);

        List<Shop> result = new ArrayList<>();
        for (Shop s : Menu.shopsIn(area.name)) if (matches(s)) result.add(s);   // chỉ quán của khu vực đã chọn
        result.sort((a, b) -> {
            if (sort == 1) return Double.compare(a.distanceKm(area), b.distanceKm(area));
            if (sort == 2) return Double.compare(b.rating(reviews), a.rating(reviews));
            if (sort == 3) return Integer.compare(a.deliveryMinutes(area), b.deliveryMinutes(area));
            return Double.compare(b.suggestScore(area, reviews), a.suggestScore(area, reviews));
        });
        nearbyAdapter.setData(result, area, reviews);
        findViewById(R.id.tvEmpty).setVisibility(result.isEmpty() ? View.VISIBLE : View.GONE);

        boolean browsing = query.isEmpty() && category == null;   // đang xem mặc định -> hiện gợi ý
        List<Shop> suggest = Menu.shopsIn(area.name);
        suggest.sort((a, b) -> Double.compare(b.suggestScore(area, reviews), a.suggestScore(area, reviews)));
        suggestAdapter.setData(suggest.subList(0, Math.min(3, suggest.size())), area, reviews);
        int vis = browsing ? View.VISIBLE : View.GONE;
        findViewById(R.id.tvSuggestTitle).setVisibility(vis);
        findViewById(R.id.tvSuggestSub).setVisibility(vis);
        findViewById(R.id.rvSuggest).setVisibility(vis);
    }

    // Quán có khớp từ khóa và loại món đang chọn không
    private boolean matches(Shop s) {
        boolean textOk = query.isEmpty() || (s.name + " " + s.tags).toLowerCase().contains(query);
        boolean categoryOk = category == null;
        for (Food f : Menu.foodsOf(s.id)) {
            if (!textOk && f.name.toLowerCase().contains(query)) textOk = true;   // tìm theo tên món
            if (category != null && f.category.equals(category)) categoryOk = true;
        }
        return textOk && categoryOk;
    }
}
