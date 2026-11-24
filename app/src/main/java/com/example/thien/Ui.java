package com.example.thien;

import android.app.Activity;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputLayout;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// Vài hàm giao diện dùng chung cho nhiều màn hình
public final class Ui {

    private static final int[] COLORS = {R.color.cover_1, R.color.cover_2, R.color.cover_3,
            R.color.cover_4, R.color.cover_5, R.color.cover_6};

    // Class tiện ích, không tạo object
    private Ui() { }

    // Tô màu nền cho 1 view theo id (mỗi món 1 màu, quay vòng 6 màu)
    public static void tint(View v, int id) {
        int color = ContextCompat.getColor(v.getContext(), COLORS[Math.floorMod(id - 1, COLORS.length)]);
        ViewCompat.setBackgroundTintList(v, ColorStateList.valueOf(color));
    }

    // Ảnh thật của món (res/drawable-nodpi/food_<id>.jpg), không có thì 0
    public static int photoRes(Context c, int foodId) {
        return c.getResources().getIdentifier("food_" + foodId, "drawable", c.getPackageName());
    }

    // Vẽ ảnh món (layout view_food_cover): ảnh thật trên nền màu + nhãn % giảm giá
    public static void showFood(View cover, Food f) {
        tint(cover, f.id);
        cover.setClipToOutline(true);
        ((ImageView) cover.findViewById(R.id.imgFood)).setImageResource(photoRes(cover.getContext(), f.photoId));
        TextView sale = cover.findViewById(R.id.tvSale);
        sale.setVisibility(f.oldPrice > f.price ? View.VISIBLE : View.GONE);
        sale.setText("-" + Math.round((f.oldPrice - f.price) * 100 / Math.max(1, f.oldPrice)) + "%");
    }

    // Hiện giá cũ dạng gạch ngang (ẩn nếu không giảm giá)
    public static void showOldPrice(TextView tv, Food f) {
        tv.setVisibility(f.oldPrice > f.price ? View.VISIBLE : View.GONE);
        tv.setText(Format.money(f.oldPrice));
        tv.setPaintFlags(tv.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
    }

    // Cho ảnh nền đầu trang vẽ tràn lên sau thanh trạng thái (giờ, pin); view header tự chừa khoảng trống
    // bằng đúng chiều cao thanh trạng thái để chữ / nút không bị che (dùng ở Thực đơn và Tài khoản)
    public static void drawBehindStatusBar(Activity a, View header) {
        WindowCompat.setDecorFitsSystemWindows(a.getWindow(), false);
        // Lớp mờ tối đặt trên cùng, cao đúng bằng thanh trạng thái: nội dung cuộn dưới đồng hồ / pin vẫn đọc được
        // (Android mới bỏ qua setStatusBarColor nên phải tự vẽ lớp này)
        FrameLayout content = a.findViewById(android.R.id.content);
        View scrim = new View(a);
        scrim.setBackgroundColor(Color.parseColor("#B3121016"));
        content.addView(scrim, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, Gravity.TOP));
        int extra = header.getLayoutParams().height > 0 ? header.getLayoutParams().height : 0;
        ViewCompat.setOnApplyWindowInsetsListener(header, (v, insets) -> {
            int top = insets.getInsets(WindowInsetsCompat.Type.systemBars()).top;
            v.setPadding(0, top, 0, v.getPaddingBottom());
            if (extra > 0) {   // header cao cố định: cộng thêm phần thanh trạng thái
                v.getLayoutParams().height = extra + top;
                v.requestLayout();
            }
            scrim.getLayoutParams().height = top;
            scrim.requestLayout();
            return insets;
        });
    }

    // Thêm món vào giỏ. Giỏ đang có món của quán khác thì hỏi khách có muốn xóa giỏ cũ để đặt từ quán này không.
    // onAdded chạy sau khi đã thêm xong.
    public static void addToCart(Activity a, Food food, int qty, Runnable onAdded) {
        String account = SessionManager.getAccount(a);
        CartManager.add(account, food, qty, ok -> {
            if (ok) {
                Toast.makeText(a, a.getString(R.string.added_to_cart, qty, food.name),
                        Toast.LENGTH_SHORT).show();
                if (onAdded != null) onAdded.run();
                return;
            }
            Shop shop = Menu.findShop(food.shopId);
            new MaterialAlertDialogBuilder(a)
                    .setTitle(R.string.other_shop_title)
                    .setMessage(a.getString(R.string.other_shop_msg, shop.name))
                    .setNegativeButton(R.string.btn_keep, null)
                    .setPositiveButton(R.string.other_shop_replace, (d, w) -> {
                        Map<Integer, Integer> fresh = new LinkedHashMap<>();
                        fresh.put(food.id, qty);
                        CartManager.replaceAll(account, fresh, () -> {
                            Toast.makeText(a, a.getString(R.string.added_to_cart, qty, food.name),
                                    Toast.LENGTH_SHORT).show();
                            if (onAdded != null) onAdded.run();
                        });
                    })
                    .show();
        });
    }

    // Ảnh bìa quán = ảnh món đặc trưng, bo góc theo nền
    public static void showShopCover(ImageView img, Shop shop) {
        int res = photoRes(img.getContext(), shop.coverFoodId);
        if (res != 0) img.setImageResource(res);
        img.setClipToOutline(true);
    }

    // "1,2 km · ~25 phút" (khoảng cách tới khu vực khách chọn + thời gian giao dự kiến)
    public static String distanceText(Context c, Shop shop, Area area) {
        return c.getString(R.string.shop_distance, shop.distanceKm(area), shop.deliveryMinutes(area));
    }

    // Thanh công cụ có nút quay lại (đóng màn hình); titleRes != 0 thì đặt luôn tiêu đề. Trả về thanh công cụ để chỉnh thêm.
    public static MaterialToolbar setupToolbar(Activity a, int titleRes) {
        MaterialToolbar bar = a.findViewById(R.id.toolbar);
        if (titleRes != 0) bar.setTitle(titleRes);
        bar.setNavigationOnClickListener(v -> a.finish());
        return bar;
    }

    // Vẽ danh sách đánh giá vào box (xóa cũ, thêm mới). Đánh giá của chính mình có chữ "(Bạn)".
    public static void showReviews(LinearLayout box, List<Review> reviews) {
        Context c = box.getContext();
        String me = SessionManager.getAccount(c);
        box.removeAllViews();
        for (Review r : reviews) {
            View v = LayoutInflater.from(c).inflate(R.layout.item_review, box, false);
            ((TextView) v.findViewById(R.id.tvReviewAvatar)).setText(r.author.substring(0, 1).toUpperCase());
            ((TextView) v.findViewById(R.id.tvReviewAuthor)).setText(
                    r.account.equals(me) ? r.author + " " + c.getString(R.string.you) : r.author);
            ((RatingBar) v.findViewById(R.id.rbReview)).setRating(r.rating);
            ((TextView) v.findViewById(R.id.tvReviewDate)).setText(Format.date(r.createdAt));
            ((TextView) v.findViewById(R.id.tvReviewContent)).setText(r.content);
            box.addView(v);
        }
    }

    // Chữ trong 1 ô nhập (đã bỏ khoảng trắng 2 đầu)
    public static String text(TextInputLayout til) {
        return til.getEditText().getText() == null ? "" : til.getEditText().getText().toString().trim();
    }

    // Hộp thoại giới thiệu ứng dụng (dùng ở banner Thực đơn và trang Cài đặt)
    public static void showAbout(Context c) {
        new MaterialAlertDialogBuilder(c)
                .setTitle(R.string.about_title)
                .setMessage(R.string.about_text)
                .setPositiveButton(R.string.ok, null)
                .show();
    }

    // Thêm 1 dòng "nhãn — giá trị" (layout item_info_row) vào cuối box
    public static void addRow(LinearLayout box, String label, String value) {
        View row = LayoutInflater.from(box.getContext()).inflate(R.layout.item_info_row, box, false);
        ((TextView) row.findViewById(R.id.tvRowLabel)).setText(label);
        ((TextView) row.findViewById(R.id.tvRowValue)).setText(value);
        box.addView(row);
    }
}
