package com.example.thien;

import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

// Màn hình ĐĂNG KÝ: tạo tài khoản mới rồi trả username về Login
public class RegisterActivity extends AppCompatActivity {

    public static final String EXTRA_USERNAME = "username";

    private EditText edtFullName, edtUsername, edtPassword, edtConfirm;

    // Gắn layout và các nút quay lại, đăng ký, về đăng nhập
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        edtFullName = findViewById(R.id.edtFullName);
        edtUsername = findViewById(R.id.edtRegUsername);
        edtPassword = findViewById(R.id.edtRegPassword);
        edtConfirm = findViewById(R.id.edtRegConfirm);

        Ui.setupToolbar(this, 0);
        findViewById(R.id.btnRegister).setOnClickListener(v -> handleRegister());
        findViewById(R.id.tvGoLogin).setOnClickListener(v -> finish());
    }

    // Kiểm tra dữ liệu nhập; hợp lệ thì tạo tài khoản và trả username về Login
    private void handleRegister() {
        String fullName = edtFullName.getText().toString().trim();
        String username = edtUsername.getText().toString().trim();
        String password = edtPassword.getText().toString();
        String confirm = edtConfirm.getText().toString();

        if (fullName.isEmpty()) { edtFullName.setError(getString(R.string.err_fullname)); return; }
        if (username.length() < 3) { edtUsername.setError(getString(R.string.err_username_short)); return; }
        if (password.length() < 6) { edtPassword.setError(getString(R.string.err_password_short)); return; }
        if (!password.equals(confirm)) { edtConfirm.setError(getString(R.string.err_confirm)); return; }
        // Tạo tài khoản trên Firestore; tên đã tồn tại thì báo lỗi
        UserStore.register(username, fullName, password, ok -> {
            if (!ok) {
                edtUsername.setError(getString(R.string.err_username_taken));
                return;
            }
            Toast.makeText(this, R.string.register_success, Toast.LENGTH_SHORT).show();
            setResult(RESULT_OK, new Intent().putExtra(EXTRA_USERNAME, username));
            finish();
        });
    }
}
