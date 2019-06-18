package com.humio.jitrex;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;


@RunWith(JUnit4.class)
public class LargeInputTest {

    private String makeInput(int repetitions, int constantLen) {
        StringBuilder sb = new StringBuilder(repetitions * constantLen);
        for (int i = 0; i < repetitions; i++) {
            sb.append("\\Q");
            for (int j = 0; j < constantLen; j++) {
                sb.append((char)('A' + (j % 26)));
            }
            sb.append("\\E.*");
        }
        return sb.toString();
    }

    @Test
    public void testInputWithLargeConstantParts() {
        for (int i = 0; i < 100; i++) {
            String s = makeInput(i, 500);
            try {
                Pattern.compile(s);
            } catch (Throwable e) {
                throw new RuntimeException("Failed at input length=" + s.length() + " from i=" + i, e);
            }
        }
    }
}
