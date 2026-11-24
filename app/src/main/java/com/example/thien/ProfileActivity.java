package com.example.thien;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import android.os.Bundle;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

// Màn hình THÔNG TIN CÁ NHÂN: xem và chỉnh sửa họ tên, email, số điện thoại
public class ProfileActivity extends AppCompatActivity {

    // Hiện thông tin cá nhân và gắn nút quay lại, nút chỉnh sửa
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        showProfile();
        Ui.setupToolbar(this, 0);
        findViewById(R.id.btnEditProfile).setOnClickListener(v -> showEditDialog());
    }

    private String email = "", phone = "";   // đọc từ Firestore

    // Đọc hồ sơ từ Firestore (collection users) rồi đổ lên giao diện
    private void showProfile() {
        String account = SessionManager.getAccount(this);
        UserStore.get(account, d -> {
            if (d == null || !d.exists()) return;
            String name = d.getString("name");
            email = d.getString("email") == null ? "" : d.getString("email");
            phone = d.getString("phone") == null ? "" : d.getString("phone");
            ((TextView) findViewById(R.id.tvProfileAvatar)).setText(name.substring(0, 1).toUpperCase());
            ((TextView) findViewById(R.id.tvProfileName)).setText(name);
            ((TextView) findViewById(R.id.tvProfileFullName)).setText(name);
            ((TextView) findViewById(R.id.tvProfileAccount)).setText(account);
            ((TextView) findViewById(R.id.tvProfileEmail)).setText(email.isEmpty() ? getString(R.string.not_updated) : email);
            ((TextView) findViewById(R.id.tvProfilePhone)).setText(phone.isEmpty() ? getString(R.string.not_updated) : phone);
            ((TextView) findViewById(R.id.tvProfileRole)).setText(
                    SessionManager.isAdmin(this) ? R.string.role_admin : R.string.member);
        });
    }

    // Mở hộp thoại sửa họ tên, email, số điện thoại; kiểm tra hợp lệ rồi lưu lên Firestore
    private void showEditDialog() {
        String account = SessionManager.getAccount(this);
        View form = LayoutInflater.from(this).inflate(R.layout.dialog_edit_profile, null);
        EditText edtName = form.findViewById(R.id.edtEditName);
        EditText edtEmail = form.findViewById(R.id.edtEditEmail);
        EditText edtPhone = form.findViewById(R.id.edtEditPhone);
        edtName.setText(SessionManager.getDisplayName(this));
        edtEmail.setText(email);
        edtPhone.setText(phone);

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.edit_profile_title)
                .setView(form)
                .setNegativeButton(R.string.btn_cancel, null)
                .setPositiveButton(R.string.btn_save, null)   // gắn listener bên dưới để giữ dialog khi lỗi
                .create();
        dialog.show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String name = edtName.getText().toString().trim();
            String newEmail = edtEmail.getText().toString().trim();
            String newPhone = edtPhone.getText().toString().trim();

            if (name.isEmpty()) { edtName.setError(getString(R.string.err_fullname)); return; }
            if (!newEmail.isEmpty() && !Patterns.EMAIL_ADDRESS.matcher(newEmail).matches()) {
                edtEmail.setError(getString(R.string.err_email)); return;
            }
            if (!newPhone.isEmpty() && !newPhone.matches("0[0-9]{9}")) {
                edtPhone.setError(getString(R.string.err_phone)); return;
            }

            UserStore.updateProfile(account, name, newEmail, newPhone);
            SessionManager.setDisplayName(this, name);
            showProfile();
            Toast.makeText(this, R.string.profile_saved, Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });
    }
}
