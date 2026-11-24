package com.example.thien;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

// Tuần 1 - Bài 6: máy tính (chuyển từ Bai6_Calculator/MainActivity.java)
public class CalculatorActivity extends AppCompatActivity {

    private final CalculatorEngine engine = new CalculatorEngine();
    private TextView tvResult;
    private TextView tvExpression;

    // Gắn layout, nối các nút số, phép toán, chức năng với CalculatorEngine
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calculator);

        // Nút quay lại: đóng máy tính -> về Home
        Ui.setupToolbar(this, 0);

        tvResult = findViewById(R.id.tvResult);
        tvExpression = findViewById(R.id.tvExpression);

        int[] digitIds = {R.id.btn0, R.id.btn1, R.id.btn2, R.id.btn3, R.id.btn4,
                R.id.btn5, R.id.btn6, R.id.btn7, R.id.btn8, R.id.btn9};
        View.OnClickListener digitListener = v -> {
            engine.inputDigit(((Button) v).getText().charAt(0));
            updateScreen();
        };
        for (int id : digitIds) findViewById(id).setOnClickListener(digitListener);

        View.OnClickListener operatorListener = v -> {
            engine.inputOperator(((Button) v).getText().charAt(0));
            updateScreen();
        };
        findViewById(R.id.btnAdd).setOnClickListener(operatorListener);
        findViewById(R.id.btnSub).setOnClickListener(operatorListener);
        findViewById(R.id.btnMul).setOnClickListener(operatorListener);
        findViewById(R.id.btnDiv).setOnClickListener(operatorListener);

        findViewById(R.id.btnDot).setOnClickListener(v -> { engine.inputDot(); updateScreen(); });
        findViewById(R.id.btnEquals).setOnClickListener(v -> { engine.equalsPressed(); updateScreen(); });
        findViewById(R.id.btnClear).setOnClickListener(v -> { engine.clear(); updateScreen(); });
        findViewById(R.id.btnSign).setOnClickListener(v -> { engine.toggleSign(); updateScreen(); });
        findViewById(R.id.btnPercent).setOnClickListener(v -> { engine.percent(); updateScreen(); });
        findViewById(R.id.btnBackspace).setOnClickListener(v -> { engine.backspace(); updateScreen(); });

        updateScreen();
    }

    // Hiện kết quả và biểu thức hiện tại lên màn hình
    private void updateScreen() {
        tvResult.setText(engine.getDisplay());
        tvExpression.setText(engine.getExpression());
    }
}
