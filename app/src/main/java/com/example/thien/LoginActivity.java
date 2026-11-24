package com.example.thien;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "Lifecycle";


    private EditText edtUsername, edtPassword;

    // Nhận username vừa đăng ký từ RegisterActivity để điền sẵn vào ô đăng nhập
    private final ActivityResultLauncher<Intent> registerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    edtUsername.setText(result.getData().getStringExtra(RegisterActivity.EXTRA_USERNAME));
                    edtPassword.setText("");
                    edtPassword.requestFocus();
                }
            });

    // 1. onCreate: chạy 1 lần khi Activity được tạo -> gắn layout, tìm View, gắn sự kiện
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Đã đăng nhập từ trước (chưa bấm Đăng xuất): vào thẳng Thực đơn
        if (!SessionManager.getAccount(this).isEmpty()) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }
        setContentView(R.layout.activity_login);
        Log.d(TAG, "LoginActivity: onCreate");

        edtUsername = findViewById(R.id.edtUsername);
        edtPassword = findViewById(R.id.edtPassword);
        Button btnLogin = findViewById(R.id.btnLogin);

        btnLogin.setOnClickListener(v -> handleLogin());
        UserStore.createTestAccounts();   // admin/123456 và user/123456 trên Firestore
        findViewById(R.id.tvGoRegister).setOnClickListener(v ->
                registerLauncher.launch(new Intent(this, RegisterActivity.class)));
    }

    // Kiểm tra dữ liệu nhập và tài khoản; đúng thì lưu phiên rồi mở Home
    private void handleLogin() {
        String username = edtUsername.getText().toString().trim();
        String password = edtPassword.getText().toString();

        if (username.isEmpty()) {
            edtUsername.setError("Vui lòng nhập tên đăng nhập");
            return;
        }
        if (password.isEmpty()) {
            edtPassword.setError("Vui lòng nhập mật khẩu");
            return;
        }
        // Kiểm tra tài khoản trên Firestore (chạy nền, xong thì gọi lại)
        findViewById(R.id.btnLogin).setEnabled(false);
        UserStore.login(username, password, user -> {
            findViewById(R.id.btnLogin).setEnabled(true);
            if (user == null) {
                Toast.makeText(this, "Sai tên đăng nhập, mật khẩu hoặc chưa có mạng", Toast.LENGTH_SHORT).show();
                return;
            }
            SessionManager.login(this, user.getId(), user.getString("name"), user.getString("role"));
            startActivity(new Intent(this, MainActivity.class));
            finish();   // đóng Login -> bấm Back ở Home sẽ thoát app, không quay lại Login
        });
    }

    // 2. onStart: Activity bắt đầu hiện ra (nhưng chưa tương tác được)
    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "LoginActivity: onStart");
    }

    // 3. onResume: Activity ở trên cùng, người dùng tương tác được
    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "LoginActivity: onResume");
    }

    // 4. onPause: bị che một phần / sắp chuyển màn hình
    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "LoginActivity: onPause");
    }

    // 5. onStop: không còn nhìn thấy (đã sang màn khác hoặc về Home điện thoại)
    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "LoginActivity: onStop");
    }

    // 6. onRestart: quay lại Activity sau khi đã onStop
    @Override
    protected void onRestart() {
        super.onRestart();
        Log.d(TAG, "LoginActivity: onRestart");
    }

    // 7. onDestroy: Activity bị hủy (finish() hoặc hệ thống đóng) -> "shut down"
    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "LoginActivity: onDestroy");
    }
}
