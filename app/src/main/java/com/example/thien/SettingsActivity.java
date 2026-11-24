package com.example.thien;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.slider.Slider;

// Màn hình CÀI ĐẶT (cấu hình app): bật / tắt nhạc nền, chỉnh âm lượng, quản lý mã giảm giá (chỉ admin),
// giới thiệu ứng dụng, bài tập tuần 1
public class SettingsActivity extends AppCompatActivity {

    // Gắn công tắc nhạc, thanh âm lượng và các dòng bấm
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        Ui.setupToolbar(this, 0);

        // Nhạc nền: bật / tắt
        MaterialSwitch sw = findViewById(R.id.swMusic);
        sw.setChecked(App.isMusicOn());
        sw.setOnCheckedChangeListener((b, on) -> App.setMusicOn(on));

        // Âm lượng 0..100: kéo tới đâu nghe thay đổi tới đó
        Slider slider = findViewById(R.id.sliderVolume);
        TextView tvVolume = findViewById(R.id.tvVolume);
        slider.setValue(App.getVolume());
        tvVolume.setText(App.getVolume() + "%");
        slider.addOnChangeListener((s, value, fromUser) -> {
            tvVolume.setText((int) value + "%");
            if (fromUser) App.setVolume((int) value);   // chỉ lưu khi người dùng tự kéo
        });

        // Quản trị: chỉ admin thấy
        int admin = SessionManager.isAdmin(this) ? View.VISIBLE : View.GONE;
        findViewById(R.id.tvAdminSection).setVisibility(admin);
        findViewById(R.id.cardAdmin).setVisibility(admin);
        findViewById(R.id.rowAdmin).setOnClickListener(v -> startActivity(new Intent(this, AdminVoucherActivity.class)));

        findViewById(R.id.rowAbout).setOnClickListener(v -> Ui.showAbout(this));
        findViewById(R.id.rowCalculator).setOnClickListener(v -> startActivity(new Intent(this, CalculatorActivity.class)));
        findViewById(R.id.rowHello).setOnClickListener(v -> startActivity(new Intent(this, HelloActivity.class)));
    }
}
