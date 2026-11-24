package com.example.thien;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

// Màn hình MÓN ĐÃ THÍCH (đọc từ Firestore): lưới các món đã bấm Thích
public class FavoritesActivity extends AppCompatActivity {

    private final FoodAdapter adapter = new FoodAdapter(this, null);

    // Gắn toolbar và lưới món
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list);
        Ui.setupToolbar(this, R.string.nav_favorites);
        RecyclerView rv = findViewById(R.id.rvList);
        rv.setLayoutManager(new GridLayoutManager(this, 2));
        rv.setAdapter(adapter);
    }

    // Quay lại từ chi tiết món (có thể vừa bỏ thích): đọc lại danh sách
    @Override
    protected void onResume() {
        super.onResume();
        FavoriteManager.getAll(SessionManager.getAccount(this), list -> {
            adapter.setFoods(list);
            TextView empty = findViewById(R.id.tvEmpty);
            empty.setText(R.string.favorites_empty);
            empty.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
        });
    }
}
