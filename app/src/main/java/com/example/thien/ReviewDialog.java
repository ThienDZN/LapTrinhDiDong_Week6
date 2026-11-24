package com.example.thien;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

// Hộp thoại chấm sao + viết đánh giá cho 1 món. Dùng chung ở trang chi tiết món và trang chi tiết đơn hàng.
public final class ReviewDialog {

    // Class tiện ích, không tạo object
    private ReviewDialog() { }

    // Hành động "lưu" của hộp thoại: nhận số sao + nội dung (món và quán lưu ở 2 nơi khác nhau)
    public interface SaveAction { void save(int rating, String text); }

    // Hộp thoại đánh giá MÓN (giữ cách gọi cũ)
    public static void show(Activity a, Food food, Review mine, Runnable onChanged) {
        String account = SessionManager.getAccount(a);
        show(a, a.getString(R.string.review_dialog_title, food.name), mine,
                (rating, text) -> ReviewManager.save(account, SessionManager.getDisplayName(a), food.id, rating, text, onChanged),
                () -> ReviewManager.delete(account, food.id, onChanged));
    }

    // Hiện hộp thoại: chọn sao (bắt buộc) + nội dung (tối thiểu 10 ký tự). mine = đánh giá cũ của mình
    // (null nếu chưa có) để điền sẵn và hiện nút Xóa; save / delete do nơi gọi quyết định lưu vào đâu.
    public static void show(Activity a, String title, Review mine, SaveAction save, Runnable delete) {
        View form = LayoutInflater.from(a).inflate(R.layout.dialog_review, null);
        RatingBar rb = form.findViewById(R.id.rbInput);
        TextInputLayout til = form.findViewById(R.id.tilReview);
        TextInputEditText edt = form.findViewById(R.id.edtReview);
        TextView label = form.findViewById(R.id.tvStarLabel);
        String[] labels = a.getResources().getStringArray(R.array.star_labels);
        rb.setOnRatingBarChangeListener((bar, value, fromUser) ->
                label.setText(value < 1 ? a.getString(R.string.star_hint) : labels[Math.round(value) - 1]));
        if (mine != null) { rb.setRating(mine.rating); edt.setText(mine.content); }

        MaterialAlertDialogBuilder b = new MaterialAlertDialogBuilder(a)
                .setTitle(title)
                .setView(form)
                .setNegativeButton(R.string.btn_cancel, null)
                .setPositiveButton(R.string.btn_save, null);   // gắn listener bên dưới để giữ hộp thoại khi nhập sai
        if (mine != null) {
            b.setNeutralButton(R.string.btn_delete, (d, w) -> delete.run());
        }
        AlertDialog dialog = b.create();
        dialog.show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            int rating = Math.round(rb.getRating());
            String text = edt.getText() == null ? "" : edt.getText().toString().trim();
            if (rating < 1) { Toast.makeText(a, R.string.err_rating, Toast.LENGTH_SHORT).show(); return; }
            if (text.length() < 10) { til.setError(a.getString(R.string.err_review_short)); return; }
            save.save(rating, text);
            Toast.makeText(a, R.string.review_saved, Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });
    }
}
