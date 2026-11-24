package com.example.thien;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.util.LinkedHashMap;
import java.util.Map;

// Màn hình CHI TIẾT ĐƠN (đọc từ Firestore): trạng thái giao, hủy đơn, thông tin giao hàng, các món, tiền, Đặt lại
public class OrderDetailActivity extends AppCompatActivity {

    public static final String EXTRA_ID = "order_id";

    // Đọc đơn theo mã document trên Firestore
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_detail);
        Ui.setupToolbar(this, 0);
        OrderManager.getOrder(getIntent().getStringExtra(EXTRA_ID), o -> {
            if (o == null) { finish(); return; }
            show(o);
        });
    }

    // Hiện toàn bộ đơn: trạng thái + nút, thông tin giao hàng, các món, tiền và nút Đặt lại
    private void show(Order o) {
        int step = o.step(System.currentTimeMillis());
        showStatus(o, step);
        showInfo(o);
        showItems(o, step);
        showTotals(o);
        findViewById(R.id.btnReorder).setOnClickListener(v -> reorder(o));
    }

    // Trạng thái giao + thanh tiến độ + nút Hủy đơn / Trả hàng (chỉ hiện khi đủ điều kiện)
    private void showStatus(Order o, int step) {
        ((TextView) findViewById(R.id.tvStatus)).setText(OrdersActivity.STATUS[step]);
        LinearProgressIndicator pb = findViewById(R.id.pbStatus);
        boolean ended = step == Order.CANCELLED || step == Order.RETURNED;   // đơn đã hủy / đang trả: ẩn thanh tiến độ
        pb.setVisibility(ended ? View.GONE : View.VISIBLE);
        findViewById(R.id.tvStatusSteps).setVisibility(ended ? View.GONE : View.VISIBLE);
        pb.setProgress(step);

        // Hủy đơn: chỉ hiện khi quán còn đang chuẩn bị, hỏi lại trước khi hủy
        View cancel = findViewById(R.id.btnCancelOrder);
        cancel.setVisibility(o.canCancel(System.currentTimeMillis()) ? View.VISIBLE : View.GONE);
        cancel.setOnClickListener(v -> new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.btn_cancel_order)
                .setMessage(getString(R.string.cancel_order_confirm, o.code()))
                .setNegativeButton(R.string.btn_keep, null)
                .setPositiveButton(R.string.btn_cancel_order, (d, w) -> OrderManager.cancel(o, () -> {
                    Toast.makeText(this, R.string.order_cancelled, Toast.LENGTH_SHORT).show();
                    recreate();   // vẽ lại màn hình với trạng thái "Đã hủy"
                }))
                .show());

        // Trả hàng: chỉ hiện khi đã giao và còn trong 3 ngày; khách chọn lý do rồi gửi yêu cầu
        View ret = findViewById(R.id.btnReturnOrder);
        ret.setVisibility(o.canReturn(System.currentTimeMillis()) ? View.VISIBLE : View.GONE);
        ret.setOnClickListener(v -> showReturnDialog(o));
    }

    // Thông tin quán, người nhận, thanh toán, voucher, ghi chú (và yêu cầu trả hàng nếu có)
    private void showInfo(Order o) {
        LinearLayout info = findViewById(R.id.infoRows);
        if (!o.shopName.isEmpty()) Ui.addRow(info, getString(R.string.label_shop), o.shopName);
        Ui.addRow(info, getString(R.string.label_order_code), o.code());
        Ui.addRow(info, getString(R.string.label_time), Format.dateTime(o.createdAt));
        Ui.addRow(info, getString(R.string.label_receiver), o.receiver + " · " + o.phone);
        Ui.addRow(info, getString(R.string.label_address), o.address + (o.area.isEmpty() ? "" : ", " + o.area));
        Ui.addRow(info, getString(R.string.label_payment), o.payment);
        if (!o.voucher.isEmpty()) Ui.addRow(info, getString(R.string.label_voucher), o.voucher);
        if (!o.note.isEmpty()) Ui.addRow(info, getString(R.string.label_note), o.note);
        if (o.returnRequested) {   // thông tin yêu cầu trả hàng đã gửi
            Ui.addRow(info, getString(R.string.label_return_time), Format.dateTime(o.returnAt));
            Ui.addRow(info, getString(R.string.label_return_reason), o.returnReason);
            if (!o.returnNote.isEmpty()) Ui.addRow(info, getString(R.string.label_return_note), o.returnNote);
        }
    }

    // Các món: dùng lại item_row, bấm để mở chi tiết món; đã nhận hàng thì có nút đánh giá
    private void showItems(Order o, int step) {
        LinearLayout list = findViewById(R.id.itemList);
        for (Order.Item item : o.items) {
            View row = LayoutInflater.from(this).inflate(R.layout.item_row, list, false);
            CartActivity.RowHolder h = new CartActivity.RowHolder(row);
            Food f = Menu.find(item.foodId);
            if (f != null) h.showFood(f);
            h.title.setText(getString(R.string.item_line, item.qty, item.name));
            h.sub.setText(getString(R.string.unit_price, Format.money(item.price)));
            h.price.setText(Format.money(item.price * item.qty));
            h.delete.setVisibility(View.GONE);
            row.setOnClickListener(v -> startActivity(new Intent(this, FoodActivity.class)
                    .putExtra(FoodActivity.EXTRA_ID, item.foodId)));
            if (f != null && (step == 2 || step == Order.RETURNED)) showReviewButton(h, f);
            list.addView(row);
        }
    }

    // Tạm tính, phí ship, giảm giá, tổng cộng
    private void showTotals(Order o) {
        LinearLayout totals = findViewById(R.id.totalRows);
        Ui.addRow(totals, getString(R.string.label_subtotal), Format.money(o.subtotal));
        Ui.addRow(totals, getString(R.string.label_shipping), o.shippingFee == 0 ? getString(R.string.free) : Format.money(o.shippingFee));
        Ui.addRow(totals, getString(R.string.label_discount), "-" + Format.money(o.discount));
        Ui.addRow(totals, getString(R.string.label_grand_total), Format.money(o.total));
    }

    // Đặt lại: thay giỏ bằng toàn bộ món của đơn này (cùng 1 quán) rồi mở giỏ
    private void reorder(Order o) {
        Map<Integer, Integer> items = new LinkedHashMap<>();
        for (Order.Item item : o.items) if (Menu.find(item.foodId) != null) items.put(item.foodId, item.qty);
        CartManager.replaceAll(SessionManager.getAccount(this), items, () -> {
            Toast.makeText(this, R.string.reordered, Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, CartActivity.class));
        });
    }

    // Hộp thoại trả hàng: chọn 1 lý do (bắt buộc) + ghi chú thêm (không bắt buộc), hỏi lại bằng nút Gửi yêu cầu
    private void showReturnDialog(Order o) {
        String[] reasons = getResources().getStringArray(R.array.return_reasons);
        int[] chosen = {-1};
        EditText note = new EditText(this);
        note.setHint(R.string.hint_return_note);
        note.setMaxLines(3);
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(64, 8, 64, 0);
        box.addView(note);

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.btn_return_order)
                .setSingleChoiceItems(reasons, -1, (d, which) -> chosen[0] = which)
                .setView(box)
                .setNegativeButton(R.string.btn_cancel, null)
                .setPositiveButton(R.string.btn_send_return, null)   // gắn listener bên dưới để giữ hộp thoại khi thiếu lý do
                .create();
        dialog.show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            if (chosen[0] < 0) { Toast.makeText(this, R.string.err_return_reason, Toast.LENGTH_SHORT).show(); return; }
            OrderManager.requestReturn(o, reasons[chosen[0]], note.getText().toString().trim(), () -> {
                Toast.makeText(this, R.string.return_sent, Toast.LENGTH_LONG).show();
                recreate();   // vẽ lại với trạng thái "Đang xử lý trả hàng"
            });
            dialog.dismiss();
        });
    }

    // Nút "Đánh giá" dưới mỗi món của đơn đã giao; đã đánh giá rồi thì đổi thành "Sửa đánh giá · n★"
    private void showReviewButton(CartActivity.RowHolder h, Food f) {
        String account = SessionManager.getAccount(this);
        h.action.setVisibility(View.VISIBLE);
        h.action.setText(R.string.btn_review);
        ReviewManager.getMine(account, f.id, mine -> {
            if (mine != null) h.action.setText(getString(R.string.btn_review_edit, mine.rating));
            h.action.setOnClickListener(v -> ReviewDialog.show(this, f, mine, () -> showReviewButton(h, f)));
        });
    }
}
