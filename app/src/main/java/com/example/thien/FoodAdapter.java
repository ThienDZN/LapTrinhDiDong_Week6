package com.example.thien;

import java.util.Locale;
import android.app.Activity;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

// Adapter cho lưới món (Thực đơn, Món đã thích). Bấm ô -> chi tiết món; bấm + -> thêm 1 phần vào giỏ.
public class FoodAdapter extends RecyclerView.Adapter<FoodAdapter.Holder> {

    private final List<Food> foods = new ArrayList<>();
    private final Activity activity;   // màn hình đang chứa danh sách (để hiện hộp thoại khi thêm món)
    private final Runnable onCartChanged;           // báo màn hình cập nhật thanh giỏ hàng
    private Map<Integer, double[]> ratings = new HashMap<>();   // id món -> {sao trung bình, số đánh giá}

    public FoodAdapter(Activity activity, Runnable onCartChanged) {
        this.activity = activity;
        this.onCartChanged = onCartChanged;
    }

    // Thay danh sách đang hiển thị
    @SuppressLint("NotifyDataSetChanged")
    public void setFoods(List<Food> list) {
        foods.clear();
        foods.addAll(list);
        notifyDataSetChanged();
    }

    // Cập nhật điểm sao để hiện trên thẻ món
    @SuppressLint("NotifyDataSetChanged")
    public void setRatings(Map<Integer, double[]> map) {
        ratings = map;
        notifyDataSetChanged();
    }

    // Tạo View cho 1 ô từ item_food.xml
    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_food, parent, false));
    }

    // Đổ dữ liệu món vào ô + gắn sự kiện bấm
    @Override
    public void onBindViewHolder(@NonNull Holder h, int position) {
        Food f = foods.get(position);
        Ui.showFood(h.cover, f);
        h.name.setText(f.name);
        double[] r = ratings.get(f.id);
        String stars = r == null ? h.itemView.getContext().getString(R.string.rating_new)
                : String.format(Locale.ROOT, "★ %.1f", r[0]);
        h.sub.setText(h.itemView.getContext().getString(R.string.food_sub, stars, (int) f.sold));
        h.price.setText(Format.money(f.price));
        h.itemView.setOnClickListener(v -> v.getContext().startActivity(
                new Intent(v.getContext(), FoodActivity.class).putExtra(FoodActivity.EXTRA_ID, f.id)));
        h.add.setOnClickListener(v -> Ui.addToCart(activity, f, 1, onCartChanged));
    }

    @Override
    public int getItemCount() { return foods.size(); }

    // Giữ sẵn các view của 1 ô
    static class Holder extends RecyclerView.ViewHolder {
        final View cover, add;
        final TextView name, sub, price;

        Holder(@NonNull View v) {
            super(v);
            cover = v.findViewById(R.id.cover);
            add = v.findViewById(R.id.btnQuickAdd);
            name = v.findViewById(R.id.tvFoodName);
            sub = v.findViewById(R.id.tvFoodSub);
            price = v.findViewById(R.id.tvFoodPrice);
        }
    }
}
