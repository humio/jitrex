package com.humio.jitrex;

import com.humio.jitrex.jvm.JavaClassRegexStub;
import org.junit.Assert;
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
        for (int i = 0; i < 1000; i++) {
            String s = makeInput(i, 50);
            try {
                Pattern.compile(s);
            } catch (IllegalRegexException e) {
                System.out.println("testInputWithLargeConstantParts failed with input length="+i);
                return;
            } catch (Throwable e) {
                throw new RuntimeException("Failed at input length=" + s.length() + " from i=" + i, e);
            }
        }
    }



    private String makeDotAllInput(int repetitions, int constantLen) {
        StringBuilder sb = new StringBuilder(repetitions * constantLen);
        for (int i = 0; i < repetitions; i++) {
            sb.append("(");
            for (int j = 0; j < constantLen; j++) {
                sb.append('.');
            }
            sb.append(")");
        }
        return sb.toString();
    }

    @Test
    public void testInputWithLargeAnyParts() {
        for (int i = 0; i < 1000; i++) {
            String s = makeDotAllInput(i, 50);
            try {
                Pattern.compile(s);
            } catch (IllegalRegexException e) {
                System.out.println("testInputWithLargeAnyParts failed with input length="+i);
                return;
            } catch (Throwable e) {
                throw new RuntimeException("Failed at input length=" + s.length() + " from i=" + i, e);
            }
        }
    }


    private String makeManyGlobsInput(int repetitions, int multiplier) {
        StringBuilder sb = new StringBuilder(repetitions * multiplier);
        for (int i = 0; i < repetitions; i++) {
            for (int j = 0; j < multiplier; j++) {
                sb.append("(.*),");
            }
        }
        return sb.toString();
    }

    @Test
    public void testInputWithManyGlobs() {
        for (int i = 0; i < 1000; i++) {
            String s = makeManyGlobsInput(i, 1);
            try {
                Pattern.compile(s);
            } catch (IllegalRegexException e) {
                System.out.println("testInputWithManyGlobs failed with input length="+i);
                return;
            } catch (Throwable e) {
                throw new RuntimeException("Failed at input length=" + s.length() + " from i=" + i, e);
            }
        }
    }


    @Test
    public void testSlowRegex() {

        String input =            "{1:\n" + "this is some more text - and some more and some more and even more\n"
                + "this is some more text and some more and some more and even more\n"
                + "this is some more text and some more and some more and even more\n"
                + "this is some more text and some more and some more and even more\n"
                + "this is some more text and some more and some more and even more\n"
                + "this is some more text and some more and some more and even more\n"
                + "this is some more text and some more and some more and even more\n"
                + "this is some more text and some more and some more and even more\n"
                + "this is some more text and some more and some more and even more\n"
                + "this is some more text and some more and some more and even more\n"
                + "this is some more text and some more and some more and even more\n"
                + "this is some more text and some more and some more and even more\n"
                + "this is some more text and some more and some more and even more\n"
                + "this is some more text and some more and some more and even more\n"
                + "this is some more text and some more and some more and even more\n"
                + "this is some more text and some more and some more and even more\n"
                + "this is some more text and some more and some more and even more\n"
                + "this is some more text and some more and some more and even more\n"
                + "this is some more text and some more and some more and even more\n"
                + "this is some more text and some more and some more and even more\n"
                + "this is some more text and some more and some more and even more\n"
                + "this is some more text and some more and some more and even more\n"
                + "this is some more text and some more and some more and even more\n"
                + "this is some more text and some more and some more and even more\n"
                + "this is some more text and some more and some more and even more\n"
                + "this is some more text and some more and some more and even more\n"
                + "this is some more text and some more and some more and even more\n"
                + "this is some more text and some more and some more and even more\n"
                + "this is some more text and some more and some more and even more\n"
                + "this is some more text and some more and some more and even more\n"
                + "this is some more text and some more and some more and even more\n"
                + "this is some more text and some more and some more and even more\n"
                + "this is some more text and some more and some more and even more\n"
                + "this is some more text and some more and some more and even more\n"
                + "this is some more text and some more and some more and even more\n"
                + "this is some more text and some more and some more and even more\n"
                + "this is some more text and some more and some more and even more\n"
                + "this is some more text and some more and some more and even more\n"
                + "this is some more text and some more and some more and even more\n"
                + "this is some more text and some more and some more and even more\n"
                + "this is some more text and some more and some more and even more\n"
                + "this is some more text and some more and some more and even more at the end\n" + "-}\n";

        String pattern = "(([^:]+)://)?([^:/]+)(:([0-9]+))?(/.*)";

        Pattern p = Pattern.compile(pattern);

        Matcher m = p.matcher(input);

        Assert.assertFalse ( m.find() );

        Assert.assertEquals( m.getBacktrackLimit(), Integer.MAX_VALUE );
        Assert.assertEquals( m.getBacktrackCount(), 26376743 );

        m.setBacktrackLimit(1000000);

        try {
            m.reset(input);
            Assert.assertFalse ( m.find() );
            Assert.assertTrue("should throw exception", false);
        } catch (RegexRuntimeLimitException e) {
            Assert.assertEquals( e.getBacktrackCount(), 1000000);
        }

    }
}
