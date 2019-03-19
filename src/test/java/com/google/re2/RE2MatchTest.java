package com.google.re2;

import com.humio.regex.Pattern;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import static org.junit.Assert.fail;

@RunWith(Parameterized.class)
public class RE2MatchTest {
    @Parameters
    public static FindTest.Test[] matchTests() {
        return FindTest.FIND_TESTS;
    }

    private final FindTest.Test test;

    public RE2MatchTest(FindTest.Test findTest) {
        this.test = findTest;
    }

    @Test
    public void testMatch() {

        Pattern re = Pattern.compile(test.pat);
        boolean m = re.matcher(test.text).find();
        if (m != (test.matches.length > 0)) {
            fail(
                    String.format(
                            "RE2.match failure on %s: %s should be %s", test, m, test.matches.length > 0));
        }
        /*
        // now try bytes
        m = re.matchUTF8(GoTestUtils.utf8(test.text));
        if (m != (test.matches.length > 0)) {
            fail(
                    String.format(
                            "RE2.matchUTF8 failure on %s: %s should be %s", test, m, test.matches.length > 0));
        }
        */
    }

    @Test
    public void testMatchFunction() {
        System.out.println("trying "+test.pat+" to: "+test.text);
        boolean m;
        try {
            m = Pattern.compile(test.pat).matcher(test.text).find();
        } catch (RuntimeException e) {
            throw e;
        }
        if (m != (test.matches.length > 0)) {
            fail(
                    String.format(
                            "RE2.match failure on %s: %s should be %s", test, m, test.matches.length > 0));
        }
    }
}
