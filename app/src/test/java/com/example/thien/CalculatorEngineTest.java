package com.example.thien;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class CalculatorEngineTest {

    private CalculatorEngine type(String keys) {
        CalculatorEngine e = new CalculatorEngine();
        for (char c : keys.toCharArray()) {
            if (Character.isDigit(c)) e.inputDigit(c);
            else if (c == '.') e.inputDot();
            else if (c == '=') e.equalsPressed();
            else e.inputOperator(c);
        }
        return e;
    }

    @Test public void add() { assertEquals("5", type("2+3=").getDisplay()); }
    @Test public void subtractNegative() { assertEquals("-7", type("3-10=").getDisplay()); }
    @Test public void multiply() { assertEquals("42", type("6×7=").getDisplay()); }
    @Test public void divide() { assertEquals("2.5", type("5÷2=").getDisplay()); }
    @Test public void decimals() { assertEquals("0.3", type("0.1+0.2=").getDisplay()); }
    @Test public void chain() { assertEquals("20", type("2+3×4=").getDisplay()); }
    @Test public void divideByZero() { assertEquals(CalculatorEngine.ERROR, type("8÷0=").getDisplay()); }
    @Test public void recoverAfterError() { assertEquals("4", type("8÷0=4").getDisplay()); }

    @Test public void clearAndBackspace() {
        CalculatorEngine e = type("123");
        e.backspace();
        assertEquals("12", e.getDisplay());
        e.clear();
        assertEquals("0", e.getDisplay());
    }

    @Test public void signAndPercent() {
        CalculatorEngine e = type("50");
        e.toggleSign();
        assertEquals("-50", e.getDisplay());
        e.percent();
        assertEquals("-0.5", e.getDisplay());
    }
}
