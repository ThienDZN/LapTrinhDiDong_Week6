package com.example.thien;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

// Màn hình LỊCH SỬ ĐƠN HÀNG: đọc các đơn của tài khoản từ Firestore (đăng xuất / đăng nhập lại vẫn còn),
// bấm 1 đơn để xem chi tiết
public class OrdersActivity extends AppCompatActivity {

    public static final String EXTRA_STEP = "step";   // chỉ hiện đơn ở 1 trạng thái (-1 = tất cả)
    static final int[] STATUS = {R.string.status_0, R.string.status_1, R.string.status_2, R.string.status_3, R.string.status_4};
    private final List<Order> orders = new ArrayList<>();

    // Gắn toolbar, danh sách; đọc đơn từ Firestore
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list);
        int filter = getIntent().getIntExtra(EXTRA_STEP, -1);
        Ui.setupToolbar(this, filter < 0 ? R.string.nav_orders : STATUS[filter]);
        TextView empty = findViewById(R.id.tvEmpty);

        RecyclerView rv = findViewById(R.id.rvList);
        rv.setLayoutManager(new LinearLayoutManager(this));
        RecyclerView.Adapter<CartActivity.RowHolder> adapter = new RecyclerView.Adapter<CartActivity.RowHolder>() {
            @NonNull @Override
            public CartActivity.RowHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                return new CartActivity.RowHolder(LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_row, parent, false));
            }

            // 1 đơn: ảnh món đầu tiên, mã đơn, thời gian · số phần · trạng thái, tổng tiền
            @Override
            public void onBindViewHolder(@NonNull CartActivity.RowHolder h, int position) {
                Order o = orders.get(position);
                Food first = o.items.isEmpty() ? null : Menu.find(o.items.get(0).foodId);
                if (first != null) h.showFood(first);
                else h.showIcon(R.drawable.ic_receipt, position + 1);
                h.title.setText(o.shopName.isEmpty() ? getString(R.string.order_title, o.code()) : o.shopName);
                h.sub.setText(getString(R.string.order_sub, o.code() + " · " + Format.dateTime(o.createdAt), o.count(),
                        getString(STATUS[o.step(System.currentTimeMillis())])));
                h.price.setText(Format.money(o.total));
                h.delete.setVisibility(View.GONE);
                h.itemView.setOnClickListener(v -> startActivity(new Intent(OrdersActivity.this,
                        OrderDetailActivity.class).putExtra(OrderDetailActivity.EXTRA_ID, o.id)));
            }

            @Override public int getItemCount() { return orders.size(); }
        };
        rv.setAdapter(adapter);

        empty.setText(R.string.loading);
        empty.setVisibility(View.VISIBLE);
        OrderManager.getOrders(SessionManager.getAccount(this), list -> {
            long now = System.currentTimeMillis();
            for (Order o : list) {
                if (o.items.isEmpty()) continue;                      // bỏ đơn cũ không phải đồ ăn
                if (filter >= 0 && o.step(now) != filter) continue;   // lọc theo trạng thái được chọn
                orders.add(o);
            }
            adapter.notifyItemRangeInserted(0, orders.size());
            empty.setText(R.string.orders_empty);
            empty.setVisibility(orders.isEmpty() ? View.VISIBLE : View.GONE);
        });
    }
}
