package com.example.thien;

import java.math.BigDecimal;

/**
 * Logic máy tính đơn giản, tách riêng khỏi giao diện để dễ kiểm thử.
 * Tính theo kiểu máy tính bỏ túi: mỗi lần bấm phép toán sẽ tính kết quả trước đó.
 */
public class CalculatorEngine {

    public static final String ERROR = "Lỗi";

    private String display = "0";
    private Double firstOperand = null;
    private char pendingOperator = 0;
    private boolean startNewNumber = true;

    // Chuỗi đang hiện ở dòng kết quả
    public String getDisplay() {
        return display;
    }

    // Biểu thức chờ tính, ví dụ "5 +" (rỗng nếu chưa chọn phép toán)
    public String getExpression() {
        if (pendingOperator == 0 || firstOperand == null) return "";
        return format(firstOperand) + " " + pendingOperator;
    }

    // Nhập một chữ số (tối đa 15 chữ số)
    public void inputDigit(char digit) {
        if (ERROR.equals(display)) clear();
        if (startNewNumber) {
            display = String.valueOf(digit);
            startNewNumber = false;
        } else if (display.equals("0")) {
            display = String.valueOf(digit);
        } else if (display.replace("-", "").replace(".", "").length() < 15) {
            display += digit;
        }
    }

    // Nhập dấu thập phân (mỗi số chỉ có 1 dấu chấm)
    public void inputDot() {
        if (ERROR.equals(display)) clear();
        if (startNewNumber) {
            display = "0.";
            startNewNumber = false;
        } else if (!display.contains(".")) {
            display += ".";
        }
    }

    // Chọn phép toán; nếu đang có phép toán chờ thì tính kết quả trước
    public void inputOperator(char op) {
        if (ERROR.equals(display)) return;
        if (firstOperand != null && pendingOperator != 0 && !startNewNumber) {
            calculate();
            if (ERROR.equals(display)) return;
        }
        firstOperand = Double.parseDouble(display);
        pendingOperator = op;
        startNewNumber = true;
    }

    // Bấm "=": tính phép toán đang chờ
    public void equalsPressed() {
        if (firstOperand == null || pendingOperator == 0 || ERROR.equals(display)) return;
        calculate();
        firstOperand = null;
        pendingOperator = 0;
    }

    // Đổi dấu số đang hiện (+/-)
    public void toggleSign() {
        if (ERROR.equals(display) || display.equals("0")) return;
        display = display.startsWith("-") ? display.substring(1) : "-" + display;
    }

    // Chia số đang hiện cho 100
    public void percent() {
        if (ERROR.equals(display)) return;
        display = format(Double.parseDouble(display) / 100);
        startNewNumber = true;
    }

    // Xóa chữ số cuối cùng
    public void backspace() {
        if (ERROR.equals(display) || startNewNumber) return;
        display = display.length() > 1 ? display.substring(0, display.length() - 1) : "0";
        if (display.equals("-")) display = "0";
    }

    // Xóa hết, về trạng thái ban đầu
    public void clear() {
        display = "0";
        firstOperand = null;
        pendingOperator = 0;
        startNewNumber = true;
    }

    // Tính a (phép toán) b; chia cho 0 hoặc kết quả vô hạn thì hiện "Lỗi"
    private void calculate() {
        double a = firstOperand;
        double b = Double.parseDouble(display);
        double result;
        switch (pendingOperator) {
            case '+': result = a + b; break;
            case '-': result = a - b; break;
            case '×': result = a * b; break;
            case '÷':
                if (b == 0) {
                    display = ERROR;
                    firstOperand = null;
                    pendingOperator = 0;
                    startNewNumber = true;
                    return;
                }
                result = a / b;
                break;
            default: return;
        }
        display = Double.isInfinite(result) || Double.isNaN(result) ? ERROR : format(result);
        firstOperand = result;
        startNewNumber = true;
    }

    /** Hiển thị gọn: 5.0 -> "5", 0.1+0.2 -> "0.3". */
    static String format(double value) {
        BigDecimal bd = new BigDecimal(value).round(new java.math.MathContext(12)).stripTrailingZeros();
        String s = bd.abs().compareTo(new BigDecimal("1e15")) >= 0 ? bd.toString() : bd.toPlainString();
        return s.equals("-0") ? "0" : s;
    }
}
