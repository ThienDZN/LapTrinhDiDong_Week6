package com.example.thien;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

// Định dạng tiền và thời gian theo kiểu Việt Nam
public final class Format {

    private static final Locale VI = Locale.forLanguageTag("vi-VN");

    // Class tiện ích, không tạo object
    private Format() { }

    // 149000 -> "149.000 ₫"
    public static String money(double v) { return NumberFormat.getCurrencyInstance(VI).format(v); }

    // Thời gian -> "23/11/2026 14:05"
    public static String dateTime(long millis) {
        return new SimpleDateFormat("dd/MM/yyyy HH:mm", VI).format(new Date(millis));
    }

    // Số lượt đánh giá gọn: 860 -> "860", 1200 -> "1,2k"
    public static String count(int n) {
        return n < 1000 ? String.valueOf(n) : String.format(VI, "%.1fk", n / 1000.0);
    }

    // Ngày -> "23/11/2026"
    public static String date(long millis) {
        return new SimpleDateFormat("dd/MM/yyyy", VI).format(new Date(millis));
    }
}
