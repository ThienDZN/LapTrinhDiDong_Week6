package com.example.thien;

import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputLayout;

import java.util.LinkedHashMap;
import java.util.Map;

// Màn hình THANH TOÁN: người nhận, SĐT, địa chỉ giao, cách trả tiền, mã giảm giá (kiểm tra trên Firestore),
// ghi chú. Đặt xong: lưu đơn lên Firestore theo tài khoản, xóa giỏ.
public class CheckoutActivity extends AppCompatActivity {

    private TextInputLayout tilName, tilPhone, tilAddress, tilVoucher, tilNote;
    private final Map<Integer, Integer> cart = new LinkedHashMap<>();
    private String account;
    private VoucherManager.Voucher voucher;   // mã đang áp dụng (null = không có)

    // Gắn giao diện, đọc giỏ + hồ sơ từ Firestore, kiểm tra mã khi rời ô nhập mã
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_checkout);
        Ui.setupToolbar(this, 0);
        account = SessionManager.getAccount(this);
        tilName = findViewById(R.id.tilName);
        tilPhone = findViewById(R.id.tilPhone);
        tilAddress = findViewById(R.id.tilAddress);
        tilVoucher = findViewById(R.id.tilVoucher);
        tilNote = findViewById(R.id.tilNote);

        tilName.getEditText().setText(SessionManager.getDisplayName(this));
        // Điền sẵn SĐT + địa chỉ đã dùng ở đơn trước (lưu trên Firestore)
        UserStore.get(account, d -> {
            if (d == null) return;
            tilPhone.getEditText().setText(d.getString("phone"));
            tilAddress.getEditText().setText(d.getString("address"));
        });
        CartManager.getItems(account, items -> { cart.putAll(items); showSummary(); });

        tilVoucher.getEditText().setOnFocusChangeListener((v, focus) -> { if (!focus) checkVoucher(null); });
        findViewById(R.id.btnPay).setOnClickListener(v -> checkVoucher(this::pay));
    }

    // Kiểm tra mã giảm giá trên Firestore (tồn tại, đang bật, còn hạn, đủ đơn tối thiểu);
    // xong thì chạy next (nếu có và mã hợp lệ hoặc bỏ trống)
    private void checkVoucher(Runnable next) {
        String code = Ui.text(tilVoucher);
        VoucherManager.find(code, v -> {
            String error = voucherError(v, code);
            voucher = error == null ? v : null;
            tilVoucher.setError(error);
            tilVoucher.setHelperText(voucher == null ? null : getString(R.string.voucher_applied, voucher.code,
                    Format.money(VoucherManager.discount(voucher, CartManager.total(cart), System.currentTimeMillis()))));
            showSummary();
            if (next != null && error == null) next.run();
        });
    }

    // Lý do mã không dùng được (null = dùng được hoặc ô trống)
    private String voucherError(VoucherManager.Voucher v, String code) {
        if (code.isEmpty()) return null;
        if (v == null) return getString(R.string.err_voucher);
        if (!v.active) return getString(R.string.err_voucher_off);
        if (v.expired(System.currentTimeMillis())) return getString(R.string.err_voucher_expired);
        if (CartManager.total(cart) < v.minOrder) return getString(R.string.err_voucher_min, Format.money(v.minOrder));
        return null;
    }

    // Tóm tắt: từng món (số lượng × tên) + tạm tính + phí giao + giảm giá + tổng cộng
    private void showSummary() {
        LinearLayout rows = findViewById(R.id.summaryRows);
        rows.removeAllViews();
        Shop shop = CartManager.shopOf(cart);
        Area area = App.getArea();
        if (shop != null) {   // quán nhận đơn + khu vực giao + khoảng cách
            Ui.addRow(rows, getString(R.string.label_shop), shop.name);
            Ui.addRow(rows, getString(R.string.label_deliver_area), area.name + " · " + Ui.distanceText(this, shop, area));
        }
        for (Map.Entry<Integer, Integer> e : cart.entrySet()) {
            Food f = Menu.find(e.getKey());
            Ui.addRow(rows, getString(R.string.item_line, e.getValue(), f.name), Format.money(f.price * e.getValue()));
        }
        double subtotal = CartManager.total(cart);
        double fee = shop == null ? 0 : OrderManager.shippingFee(subtotal, shop.distanceKm(area));
        double discount = VoucherManager.discount(voucher, subtotal, System.currentTimeMillis());
        Ui.addRow(rows, getString(R.string.label_subtotal), Format.money(subtotal));
        Ui.addRow(rows, getString(R.string.label_shipping), fee == 0 ? getString(R.string.free) : Format.money(fee));
        Ui.addRow(rows, getString(R.string.label_discount), "-" + Format.money(discount));
        ((TextView) findViewById(R.id.tvGrandTotal)).setText(Format.money(subtotal + fee - discount));
    }

    // Kiểm tra dữ liệu nhập; hợp lệ thì lưu đơn lên Firestore và báo thành công
    private void pay() {
        String name = Ui.text(tilName), phone = Ui.text(tilPhone), address = Ui.text(tilAddress);
        tilName.setError(name.isEmpty() ? getString(R.string.err_fullname) : null);
        tilPhone.setError(phone.matches("0[0-9]{9}") ? null : getString(R.string.err_phone));
        tilAddress.setError(address.length() >= 10 ? null : getString(R.string.err_address));
        if (tilName.getError() != null || tilPhone.getError() != null || tilAddress.getError() != null) return;
        if (!((MaterialCheckBox) findViewById(R.id.cbAgree)).isChecked()) {
            Toast.makeText(this, R.string.err_agree, Toast.LENGTH_SHORT).show();
            return;
        }
        if (cart.isEmpty()) { finish(); return; }

        Order o = new Order();
        o.receiver = name;
        o.phone = phone;
        o.address = address;
        o.voucher = voucher == null ? "" : voucher.code;
        o.note = Ui.text(tilNote);
        o.payment = paymentName();
        o.subtotal = CartManager.total(cart);
        Shop shop = CartManager.shopOf(cart);
        Area area = App.getArea();
        o.shopId = shop.id;
        o.shopName = shop.name;
        o.area = area.name;
        o.distanceKm = shop.distanceKm(area);
        o.shippingFee = OrderManager.shippingFee(o.subtotal, o.distanceKm);
        o.discount = VoucherManager.discount(voucher, o.subtotal, System.currentTimeMillis());
        o.total = o.subtotal + o.shippingFee - o.discount;

        findViewById(R.id.btnPay).setEnabled(false);
        OrderManager.placeOrder(account, o, cart, id -> {
            findViewById(R.id.btnPay).setEnabled(true);
            if (id == null) { Toast.makeText(this, R.string.error_network, Toast.LENGTH_SHORT).show(); return; }
            o.id = id;
            new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.order_success_title)
                    .setMessage(getString(R.string.order_success_msg, o.code(), Format.money(o.total)))
                    .setCancelable(false)
                    .setPositiveButton(R.string.ok, (d, w) -> finish())
                    .show();
        });
    }

    // Tên phương thức thanh toán đang chọn
    private String paymentName() {
        int id = ((RadioGroup) findViewById(R.id.rgPayment)).getCheckedRadioButtonId();
        if (id == R.id.rbWallet) return getString(R.string.pay_wallet);
        if (id == R.id.rbCard) return getString(R.string.pay_card);
        return getString(R.string.pay_cod);
    }

}
