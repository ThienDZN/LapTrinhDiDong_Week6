package com.example.thien;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.List;

// Màn hình ADMIN — QUẢN LÝ MÃ GIẢM GIÁ (chỉ tài khoản admin mở được):
// tạo mã mới (%, giảm tối đa, đơn tối thiểu, hạn dùng); bấm 1 mã để tạm ngưng / kích hoạt lại / hủy mã.
public class AdminVoucherActivity extends AppCompatActivity {

    private final List<VoucherManager.Voucher> vouchers = new ArrayList<>();
    private RecyclerView.Adapter<CartActivity.RowHolder> adapter;

    // Chặn người không phải admin, gắn danh sách và nút tạo mã
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!SessionManager.isAdmin(this)) { finish(); return; }
        setContentView(R.layout.activity_list);
        Ui.setupToolbar(this, R.string.admin_title);
        ((TextView) findViewById(R.id.tvEmpty)).setText(R.string.admin_empty);

        findViewById(R.id.bottomBar).setVisibility(View.VISIBLE);
        ((TextView) findViewById(R.id.btnBottom)).setText(R.string.admin_new);
        findViewById(R.id.btnBottom).setOnClickListener(v -> showCreateDialog());

        adapter = new RecyclerView.Adapter<CartActivity.RowHolder>() {
            @NonNull @Override
            public CartActivity.RowHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                return new CartActivity.RowHolder(LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_row, parent, false));
            }

            // 1 mã: tên mã, điều kiện (giảm tối đa, đơn tối thiểu, hạn dùng), trạng thái; bấm để thao tác
            @Override
            public void onBindViewHolder(@NonNull CartActivity.RowHolder h, int position) {
                VoucherManager.Voucher v = vouchers.get(position);
                h.showIcon(R.drawable.ic_receipt, position + 1);
                h.title.setText(v.code);
                h.sub.setText(describe(v));
                h.price.setText(getString(statusText(v)));
                h.delete.setVisibility(View.GONE);
                h.itemView.setOnClickListener(x -> showActions(v));
            }

            @Override public int getItemCount() { return vouchers.size(); }
        };
        RecyclerView rv = findViewById(R.id.rvList);
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(adapter);
        reload();
    }

    // "Giảm 10% · tối đa 30.000 ₫ · đơn từ 100.000 ₫ · HSD 23/12/2026"
    private String describe(VoucherManager.Voucher v) {
        StringBuilder s = new StringBuilder(getString(R.string.voucher_percent, v.percent));
        if (v.maxDiscount > 0) s.append(" · ").append(getString(R.string.voucher_max, Format.money(v.maxDiscount)));
        if (v.minOrder > 0) s.append(" · ").append(getString(R.string.voucher_min, Format.money(v.minOrder)));
        s.append(" · ").append(v.expiresAt == Long.MAX_VALUE ? getString(R.string.voucher_no_expiry)
                : getString(R.string.voucher_expiry, Format.date(v.expiresAt)));
        return s.toString();
    }

    // Trạng thái: Đang dùng / Tạm ngưng / Hết hạn
    private int statusText(VoucherManager.Voucher v) {
        if (v.expired(System.currentTimeMillis())) return R.string.voucher_expired;
        return v.active ? R.string.voucher_on : R.string.voucher_off;
    }

    // Đọc lại tất cả mã từ Firestore
    @SuppressLint("NotifyDataSetChanged")
    private void reload() {
        VoucherManager.getAll(list -> {
            vouchers.clear();
            vouchers.addAll(list);
            adapter.notifyDataSetChanged();
            findViewById(R.id.tvEmpty).setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
            ((TextView) findViewById(R.id.tvBottomLabel)).setText(R.string.admin_title);
            ((TextView) findViewById(R.id.tvTotal)).setText(getString(R.string.admin_count, list.size()));
        });
    }

    // Bấm 1 mã: chọn Tạm ngưng / Kích hoạt lại hoặc Hủy mã
    private void showActions(VoucherManager.Voucher v) {
        String toggle = getString(v.active ? R.string.voucher_pause : R.string.voucher_resume);
        new MaterialAlertDialogBuilder(this)
                .setTitle(v.code)
                .setItems(new String[]{toggle, getString(R.string.voucher_cancel)}, (d, which) -> {
                    if (which == 0) VoucherManager.setActive(v.code, !v.active, this::reload);
                    else confirmDelete(v);
                })
                .show();
    }

    // Hỏi lại trước khi hủy hẳn 1 mã
    private void confirmDelete(VoucherManager.Voucher v) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.voucher_cancel)
                .setMessage(getString(R.string.voucher_cancel_confirm, v.code))
                .setNegativeButton(R.string.btn_keep_code, null)
                .setPositiveButton(R.string.voucher_cancel, (d, w) -> VoucherManager.delete(v.code, () -> {
                    Toast.makeText(this, R.string.voucher_deleted, Toast.LENGTH_SHORT).show();
                    reload();
                }))
                .show();
    }

    // Hộp thoại tạo mã: kiểm tra từng ô rồi lưu lên Firestore (trùng tên mã thì báo lỗi)
    private void showCreateDialog() {
        View form = LayoutInflater.from(this).inflate(R.layout.dialog_voucher, null);
        TextInputLayout tilCode = form.findViewById(R.id.tilCode), tilPercent = form.findViewById(R.id.tilPercent),
                tilMax = form.findViewById(R.id.tilMax), tilMin = form.findViewById(R.id.tilMin),
                tilDays = form.findViewById(R.id.tilDays);

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.admin_new)
                .setView(form)
                .setNegativeButton(R.string.btn_cancel, null)
                .setPositiveButton(R.string.btn_save, null)   // gắn listener bên dưới để giữ dialog khi lỗi
                .create();
        dialog.show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(x -> {
            String code = Ui.text(tilCode).toUpperCase();
            int percent = number(tilPercent), days = number(tilDays);
            boolean ok = check(tilCode, code.matches("[A-Z0-9]{3,15}"), R.string.err_code)
                    & check(tilPercent, percent >= 1 && percent <= 90, R.string.err_percent)
                    & check(tilDays, days >= 1 && days <= 365, R.string.err_days);
            if (!ok) return;
            for (VoucherManager.Voucher v : vouchers) {
                if (v.code.equals(code)) { tilCode.setError(getString(R.string.err_code_taken)); return; }
            }
            VoucherManager.Voucher v = new VoucherManager.Voucher(code, percent, number(tilMax), number(tilMin),
                    System.currentTimeMillis() + days * VoucherManager.DAY, true);
            VoucherManager.save(v, SessionManager.getAccount(this), () -> {
                Toast.makeText(this, R.string.voucher_saved, Toast.LENGTH_SHORT).show();
                reload();
            });
            dialog.dismiss();
        });
    }

    // Hiện / xóa lỗi của 1 ô; trả về điều kiện
    private boolean check(TextInputLayout til, boolean valid, int err) {
        til.setError(valid ? null : getString(err));
        return valid;
    }


    // Số trong 1 ô nhập (trống / sai thì 0)
    private static int number(TextInputLayout til) {
        String s = Ui.text(til);
        return s.matches("[0-9]{1,7}") ? Integer.parseInt(s) : 0;
    }
}
