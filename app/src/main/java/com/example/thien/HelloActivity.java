package com.example.thien;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

// Tuần 1 - Bài 4: đếm số lần bấm + Toast (chuyển từ Bai1-5_HelloAndroid/MainActivity.java)
public class HelloActivity extends AppCompatActivity {

    private int clickCount = 0;

    // Gắn layout, nút đếm số lần bấm, nút Toast và nút quay lại
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hello);

        TextView tvCount = findViewById(R.id.tvCount);
        Button btnClick = findViewById(R.id.btnClick);
        Button btnHello = findViewById(R.id.btnHello);

        btnClick.setOnClickListener(v -> {
            clickCount++;
            tvCount.setText(String.valueOf(clickCount));   // số hiện trong vòng tròn
        });

        // Nút quay lại: đóng màn hình -> về Home
        Ui.setupToolbar(this, 0);

        btnHello.setOnClickListener(v ->
                Toast.makeText(this, R.string.hello_toast, Toast.LENGTH_SHORT).show());
    }
}
