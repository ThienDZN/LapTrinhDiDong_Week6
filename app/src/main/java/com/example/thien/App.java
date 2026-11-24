package com.example.thien;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import java.io.IOException;

// Lớp Application: chạy 1 lần khi mở app. Dùng để phát NHẠC NỀN "Đêm Thành Phố" (res/raw/bgm.mp3):
//  - bật khi app hiện ra, dừng khi app bị đóng / chuyển sang chạy nền (đếm số màn hình đang hiện)
//  - người dùng bật / tắt và chỉnh âm lượng ở màn Cài đặt (lưu SharedPreferences)
public class App extends Application implements Application.ActivityLifecycleCallbacks {

    private static App instance;
    private MediaPlayer player;
    private boolean ready = false;   // MediaPlayer đã nạp xong file nhạc chưa
    private int visibleScreens = 0;   // số màn hình đang hiện trên màn hình điện thoại
    private final Handler handler = new Handler(Looper.getMainLooper());

    // Mở app: ghi nhớ instance và theo dõi vòng đời của mọi màn hình
    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        registerActivityLifecycleCallbacks(this);
    }

    // Nhạc nền đang được bật trong cài đặt không (mặc định bật)
    public static boolean isMusicOn() {
        return instance.getSharedPreferences("settings", Context.MODE_PRIVATE).getBoolean("music", true);
    }

    // Bật / tắt nhạc nền (gọi từ Cài đặt)
    public static void setMusicOn(boolean on) {
        instance.getSharedPreferences("settings", Context.MODE_PRIVATE).edit().putBoolean("music", on).apply();
        if (on) instance.play();
        else instance.pause();
    }

    // Khu vực nhận hàng khách đã chọn (mặc định Quận 1); lưu lại để lần sau mở app vẫn nhớ
    public static Area getArea() {
        return Area.byName(instance.getSharedPreferences("settings", Context.MODE_PRIVATE).getString("area", "Quận 1"));
    }

    // Đổi khu vực nhận hàng
    public static void setArea(String name) {
        instance.getSharedPreferences("settings", Context.MODE_PRIVATE).edit().putString("area", name).apply();
    }

    // Âm lượng nhạc nền 0..100 (mặc định 80)
    public static int getVolume() {
        return instance.getSharedPreferences("settings", Context.MODE_PRIVATE).getInt("volume", 80);
    }

    // Đổi âm lượng (kéo thanh trượt ở Cài đặt): lưu lại và áp dụng ngay
    public static void setVolume(int percent) {
        instance.getSharedPreferences("settings", Context.MODE_PRIVATE).edit().putInt("volume", percent).apply();
        instance.handler.removeCallbacksAndMessages(null);   // dừng fade-in nếu đang chạy
        if (instance.ready) instance.player.setVolume(percent / 100f, percent / 100f);
    }

    // 1 màn hình hiện ra: nếu là màn đầu tiên (app vừa mở / quay lại) thì phát nhạc
    @Override
    public void onActivityStarted(Activity a) {
        if (visibleScreens++ == 0 && isMusicOn()) play();
    }

    // 1 màn hình bị ẩn: không còn màn nào hiện (đóng app / về Home điện thoại) thì dừng nhạc
    @Override
    public void onActivityStopped(Activity a) {
        if (--visibleScreens == 0) pause();
    }

    // Phát nhạc nền lặp lại, âm lượng tăng dần trong 2 giây (fade-in) cho êm tai.
    // Lần đầu: nạp file nhạc ở luồng nền (prepareAsync) để mở app không bị khựng, xong mới phát.
    private void play() {
        if (player == null) {
            player = new MediaPlayer();
            try (AssetFileDescriptor fd = getResources().openRawResourceFd(R.raw.bgm)) {
                player.setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build());
                player.setDataSource(fd.getFileDescriptor(), fd.getStartOffset(), fd.getLength());
            } catch (IOException e) {
                player = null;
                return;
            }
            player.setLooping(true);
            player.setOnPreparedListener(mp -> {
                ready = true;
                if (visibleScreens > 0 && isMusicOn()) play();   // app vẫn đang mở thì phát luôn
            });
            player.prepareAsync();
            return;
        }
        if (!ready || player.isPlaying()) return;
        player.setVolume(0, 0);
        player.start();
        for (int i = 1; i <= 10; i++) {
            float v = getVolume() / 100f * i / 10;
            handler.postDelayed(() -> player.setVolume(v, v), i * 200L);
        }
    }

    // Tạm dừng
    private void pause() {
        handler.removeCallbacksAndMessages(null);
        if (ready && player.isPlaying()) player.pause();
    }

    // Các sự kiện vòng đời không dùng tới
    @Override public void onActivityCreated(Activity a, Bundle b) { }
    @Override public void onActivityResumed(Activity a) { }
    @Override public void onActivityPaused(Activity a) { }
    @Override public void onActivitySaveInstanceState(Activity a, Bundle b) { }
    @Override public void onActivityDestroyed(Activity a) { }
}
