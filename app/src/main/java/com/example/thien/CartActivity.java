package com.example.thien;

import android.widget.Button;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// Màn hình GIỎ HÀNG (đọc từ Firestore): tăng / giảm số lượng, xóa món, tổng tiền, đặt món
public class CartActivity extends AppCompatActivity {

    private final List<int[]> rows = new ArrayList<>();   // {id món, số lượng}
    private RecyclerView.Adapter<RowHolder> adapter;
    private String account;
    private MaterialToolbar toolbar;

    // Gắn toolbar, danh sách và nút đặt món
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list);
        account = SessionManager.getAccount(this);
        toolbar = Ui.setupToolbar(this, R.string.cart_title);
        ((TextView) findViewById(R.id.tvEmpty)).setText(R.string.cart_empty);
        findViewById(R.id.btnBottom).setOnClickListener(v -> startActivity(new Intent(this, CheckoutActivity.class)));

        adapter = new RecyclerView.Adapter<RowHolder>() {
            @NonNull @Override
            public RowHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                return new RowHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_row, parent, false));
            }

            // 1 món: ảnh, tên, đơn giá, thành tiền, nút - / + và xóa
            @Override
            public void onBindViewHolder(@NonNull RowHolder h, int position) {
                Food f = Menu.find(rows.get(position)[0]);
                int qty = rows.get(position)[1];
                h.showFood(f);
                h.title.setText(f.name);
                h.sub.setText(getString(R.string.unit_price, Format.money(f.price)));
                h.price.setText(Format.money(f.price * qty));
                h.qtyBox.setVisibility(View.VISIBLE);
                h.qty.setText(String.valueOf(qty));
                h.minus.setOnClickListener(v -> CartManager.setQty(account, f.id, qty - 1, CartActivity.this::reload));
                h.plus.setOnClickListener(v -> CartManager.setQty(account, f.id, qty + 1, CartActivity.this::reload));
                h.delete.setOnClickListener(v -> CartManager.setQty(account, f.id, 0, CartActivity.this::reload));
                h.itemView.setOnClickListener(v -> startActivity(new Intent(CartActivity.this, FoodActivity.class)
                        .putExtra(FoodActivity.EXTRA_ID, f.id)));
            }

            @Override public int getItemCount() { return rows.size(); }
        };
        RecyclerView rv = findViewById(R.id.rvList);
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(adapter);
    }

    // Mỗi lần hiện lại (ví dụ vừa đặt món xong) thì đọc lại giỏ
    @Override
    protected void onResume() {
        super.onResume();
        reload();
    }

    // Đọc giỏ từ Firestore, cập nhật danh sách, tổng tiền, trạng thái trống
    @SuppressLint("NotifyDataSetChanged")
    private void reload() {
        CartManager.getItems(account, items -> {
            rows.clear();
            for (Map.Entry<Integer, Integer> e : items.entrySet()) rows.add(new int[]{e.getKey(), e.getValue()});
            adapter.notifyDataSetChanged();
            boolean empty = rows.isEmpty();
            findViewById(R.id.tvEmpty).setVisibility(empty ? View.VISIBLE : View.GONE);
            findViewById(R.id.bottomBar).setVisibility(empty ? View.GONE : View.VISIBLE);
            ((TextView) findViewById(R.id.tvTotal)).setText(Format.money(CartManager.total(items)));
            Shop shop = CartManager.shopOf(items);   // giỏ chỉ có món của 1 quán: hiện tên quán dưới tiêu đề
            toolbar.setSubtitle(shop == null ? null : shop.name);
        });
    }

    // Giữ sẵn các view của 1 dòng item_row (dùng chung cho mọi màn danh sách)
    static class RowHolder extends RecyclerView.ViewHolder {
        final View iconBox, qtyBox, minus, plus;
        final Button action;
        final ImageView icon, photo, delete;
        final TextView title, sub, price, qty;

        RowHolder(@NonNull View v) {
            super(v);
            iconBox = v.findViewById(R.id.iconBox);
            icon = v.findViewById(R.id.imgIcon);
            photo = v.findViewById(R.id.imgRowFood);
            delete = v.findViewById(R.id.btnRowDelete);
            title = v.findViewById(R.id.tvRowTitle);
            sub = v.findViewById(R.id.tvRowSub);
            price = v.findViewById(R.id.tvRowPrice);
            qtyBox = v.findViewById(R.id.qtyBox);
            action = v.findViewById(R.id.btnRowAction);
            qty = v.findViewById(R.id.tvQty);
            minus = v.findViewById(R.id.btnMinus);
            plus = v.findViewById(R.id.btnPlus);
        }

        // Ô bên trái hiện ảnh món trên nền màu của món
        void showFood(Food f) {
            Ui.tint(iconBox, f.id);
            iconBox.setClipToOutline(true);
            icon.setVisibility(View.GONE);
            photo.setVisibility(View.VISIBLE);
            photo.setImageResource(Ui.photoRes(photo.getContext(), f.photoId));
        }

        // Ô bên trái hiện 1 icon (ví dụ hóa đơn, mã giảm giá)
        void showIcon(int iconRes, int colorId) {
            Ui.tint(iconBox, colorId);
            icon.setVisibility(View.VISIBLE);
            icon.setImageResource(iconRes);
            photo.setVisibility(View.GONE);
        }
    }
}
