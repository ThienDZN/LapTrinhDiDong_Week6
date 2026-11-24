package com.example.thien;

import android.content.Context;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Adapter cho danh sách quán: layout hàng (item_shop_row) cho "Quán gần bạn", layout thẻ (item_shop_card)
// cho dải "Gợi ý cho bạn". Bấm 1 quán -> mở trang quán.
public class ShopAdapter extends RecyclerView.Adapter<ShopAdapter.Holder> {

    private final int layout;
    private final List<Shop> shops = new ArrayList<>();
    private Area area = App.getArea();
    private Map<Integer, double[]> reviews = new HashMap<>();

    public ShopAdapter(int layout) { this.layout = layout; }

    // Cập nhật danh sách quán, khu vực khách chọn và điểm đánh giá, rồi vẽ lại
    @SuppressLint("NotifyDataSetChanged")
    public void setData(List<Shop> list, Area area, Map<Integer, double[]> reviews) {
        shops.clear();
        shops.addAll(list);
        this.area = area;
        this.reviews = reviews;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(LayoutInflater.from(parent.getContext()).inflate(layout, parent, false));
    }

    // Đổ dữ liệu 1 quán vào thẻ
    @Override
    public void onBindViewHolder(@NonNull Holder h, int position) {
        Shop s = shops.get(position);
        Context c = h.itemView.getContext();
        Ui.showShopCover(h.cover, s);
        h.name.setText(s.name);
        h.tags.setText(s.tags);
        h.rating.setText(c.getString(R.string.shop_rating, s.rating(reviews), Format.count(s.ratingCount(reviews))));
        h.distance.setText(Ui.distanceText(c, s, area));
        double fee = OrderManager.shippingFee(0, s.distanceKm(area));
        h.fee.setText(c.getString(R.string.shop_fee_from, Format.money(fee)));
        h.itemView.setOnClickListener(v -> c.startActivity(
                new Intent(c, ShopActivity.class).putExtra(ShopActivity.EXTRA_ID, s.id)));
    }

    @Override
    public int getItemCount() { return shops.size(); }

    // Giữ sẵn các view của 1 thẻ quán
    static class Holder extends RecyclerView.ViewHolder {
        final ImageView cover;
        final TextView name, tags, rating, distance, fee;

        Holder(@NonNull View v) {
            super(v);
            cover = v.findViewById(R.id.imgShop);
            name = v.findViewById(R.id.tvShopName);
            tags = v.findViewById(R.id.tvShopTags);
            rating = v.findViewById(R.id.tvShopRating);
            distance = v.findViewById(R.id.tvShopDistance);
            fee = v.findViewById(R.id.tvShopFee);
        }
    }
}
