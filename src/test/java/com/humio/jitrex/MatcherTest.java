package com.humio.jitrex;

import com.humio.jitrex.jvm.Sample;
import com.humio.jitrex.util.Regex;
import com.humio.jitrex.jvm.Sample2;
import com.humio.util.jint.util.CompilerException;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.*;

/*
import java.util.jitrex.Pattern;
import java.util.jitrex.Matcher;
*/
@RunWith(JUnit4.class)
public class MatcherTest {

    Pattern patt;

    @Test
    public void testOpenThings() {

        try {
            Pattern p = Pattern.compile("^[");
            Assert.fail("compile should fail");
        } catch (CompilerException e) {
            // ok //
        }
        
        try {
            Pattern p = Pattern.compile("^(");
            Assert.fail("compile should fail");
        } catch (CompilerException e) {
            // ok //
        }

        Pattern p = Pattern.compile("^{");
        assertEquals(true, p.matcher("{ ... }").find());
    }


    @Test
    public void matches() {
        patt = Pattern.compile("foo");
        assertEquals(true, patt.matcher("foo").matches());
        assertEquals(false, patt.matcher("fo").matches());
    }

    @Test
    public void find() {
        patt = Pattern.compile("foo");
        assertEquals(false, patt.matcher("xxxfoxxx").find());
        assertEquals(true, patt.matcher("xxxfoo").find());
        assertEquals(true, patt.matcher("fooxxx").find());
        assertEquals(true, patt.matcher("foo").find());

        Matcher m = patt.matcher("foo foo foo");
        assertEquals(true, m.find());
        assertEquals(true, m.find());
        assertEquals(true, m.find());
        assertEquals(false, m.find());
    }

    @Test
    public void lookingAt() {
        patt = Pattern.compile("foo");
        assertEquals(false, patt.matcher("xxxfoo").lookingAt());
        assertEquals(true, patt.matcher("fooxxx").lookingAt());
    }

    @Test
    public void namedGroups() {
        patt = Pattern.compile("foo(?<xx>.*)(?<y>.)$");

        Matcher m = patt.matcher("foobarx");
        assertEquals(true, m.find());

        assertEquals(2, m.groupCount());

        assertEquals("foobarx", m.group(0));
        assertEquals("bar", m.group(1));
        assertEquals("x", m.group(2));

        assertEquals("bar", m.group("xx"));
        assertEquals("x", m.group("y"));
    }


    // ignore @Test
    public void embeddedVars() {
        patt = Pattern.compile("foo${bar}(?<xx>baz)");

        Matcher m = patt.matcher("fooxxxbaz");

        Set<String> vars = new HashSet<String>(); vars.add("bar");
        assertEquals( m.variables(), vars);

        assertEquals( false, m.matches() );

        Map<String,String> fields = new HashMap<>(); fields.put("bar", "yy");
        m.reset("fooxxxbaz", fields);
        assertEquals( false, m.matches() );

        Map<String,String> fields2 = new HashMap<>(); fields2.put("bar", "xxx");
        m.reset(fields2);
        assertEquals( true, m.matches() );
        assertEquals( "baz", m.group("xx"));

    }

    @Test
    public void ignoreCaseTest() {
        patt = Pattern.compile("fo\\D\\dBar", Pattern.CASE_INSENSITIVE);

        Matcher m = patt.matcher("Foo3baR");
        assertEquals(true, m.find());

    }


    @Test
    public void dotAllTest() {
        patt = Pattern.compile("foo.bar", Pattern.DOTALL);

        Matcher m = patt.matcher("foo\nbar");
        assertEquals(true, m.matches());

        patt = Pattern.compile("foo.bar", 0);

        m = patt.matcher("foo\nbar");
        assertEquals(false, m.matches());
    }

    @Test
    public void xxxTest() {
        Regex r = new Sample();
        r.init("foo\nbar", 0, 7);
        assertTrue(r.search());
    }

    @Test
    public void multiLineTest() {
        patt = Pattern.compile("foo$", Pattern.MULTILINE);

        Matcher m = patt.matcher("x\nfoo\nbar");
        assertEquals(true, m.find());

        m = patt.matcher("x\nfoo");
        assertEquals(true, m.find());

        patt = Pattern.compile("^bar", Pattern.MULTILINE);

        m = patt.matcher("foo\nbar");
        assertEquals(true, m.find());

        m = patt.matcher("bar\nfoo");
        assertEquals(true, m.find());
    }

    @Test
    public void testGroups() {
        patt = Pattern.compile("^([^_]+)_([^_]+)_");
        Matcher m = patt.matcher("H3dOvDVSz3h1BJrkG5gmrQum_1_0");

        assertEquals(true, m.find());
        assertEquals(2, m.groupCount());
    }

    @Test
    public void escapedTest() {

        patt = Pattern.compile("^([^_]+)\\Q_([^_]+)_\\E");

        Matcher m = patt.matcher("H3dOvDVSz3h1BJrkG5gmrQum_1_0");
        assertEquals(false, m.find());
        assertEquals(1, m.groupCount());

        m = patt.matcher("H3dOvDVSz3h1BJrkG5gmrQum_([^_]+)_");
        assertEquals(true, m.find());
        assertEquals(1, m.groupCount());

        patt = Pattern.compile("^(\\Qf\\do\\E).*(\\Qbar\\E)$");
        m = patt.matcher("f\\do    bar");
        assertEquals(true, m.find());
        assertEquals(2, m.groupCount());

    }

    @Test
    public void timeTest() {

        patt = Pattern.compile("(?<name>\\d{2,4}?-\\d{2})-");
        Matcher m = patt.matcher("2016-01-01T00:00:00Z host=original");
        assertTrue( m.find() );
        assertEquals("2016-01",  m.group(1) );
    }
    @Test
    public void charSetTest() {

        patt = Pattern.compile("^[\\]\\\\x]+:2$");
        assertEquals(true, patt.matcher("]x\\]x]x]:2").matches());

        patt = Pattern.compile("([^\\s\\[:]+)\\[");
        Matcher m = patt.matcher("wpa_supplicant[xx]");

        assertEquals(true, m.find());
        assertEquals("wpa_supplicant", m.group(1));

        patt = Pattern.compile("(\\[(?<pid>[^\\]]+)\\]):");
        m = patt.matcher("[1487]: wlp1s0: Associated with c0:8a:de:1d:c1:fc");
        assertEquals(true, m.find());
        assertEquals("[1487]:", m.group(0));
        assertEquals("[1487]",m.group(1));
        assertEquals("1487",m.group(2));
        assertEquals("1487",m.group("pid"));
    }

    @Test
    public void syslogTest() {

        String pattern = "(?<timestamp>\\S+\\s+\\S+\\s+\\S+)\\s+(?<host>\\S+)?\\s+(?<app>[^\\s\\[:]+)?(\\[(?<pid>[^\\]]+)\\])?";

        patt = Pattern.compile(pattern);
        Matcher m = patt.matcher("Feb 10 13:23:00 panda wpa_supplicant[1487]: wlp1s0: Associated with c0:8a:de:1d:c1:fc");

        assertTrue( m.find() );
        assertEquals( "Feb 10 13:23:00", m.group(1));
        assertEquals( "panda", m.group(2));
        assertEquals( "wpa_supplicant", m.group(3));
        assertEquals( "[1487]", m.group(4));
        assertEquals( "1487", m.group(5));

    }

    @Test
    public void simpletest1() {
        patt = Pattern.compile("^(.*?,){11}P");
        Matcher m = patt.matcher("1,2,3,4,5,6,7,8,9,10,11,12");
        assertFalse(m.matches());
    }


    @Test
    public void simpletest2() {
        java.util.regex.Pattern patt = java.util.regex.Pattern.compile("^(.*?,){11}P");
        java.util.regex.Matcher m = patt.matcher("1,2,3,4,5,6,7,8,9,10,11,12");
        assertFalse(m.matches());
    }

    @Test
    public void emptyTest() {
        patt = Pattern.compile("");
        assertTrue(patt.matcher("xxx").find());
        assertTrue(patt.matcher("").matches());
        assertFalse(patt.matcher("xxx").matches());
    }

    @Test
    public void lookaheadTest() {
        patt = Pattern.compile("\\{(\\d+):([^}](?!-}\\n))*");

        Matcher m = patt.matcher(

                "{1:\n"
                        + "this is some more text - and some more and some more and even more\n"
                        + "this is some more text - and some more and some more and even more\n"
                        + "this is some more text - and some more and some more and even more\n"
                        + "-}\n"

        );

        assertTrue(m.find());
        assertFalse(m.matches());
    }

    @Test
    public void lookaheadTest2() {
        java.util.regex.Pattern patt = java.util.regex.Pattern.compile("\\{(\\d+):([^}](?!-}\\n)*)");

        java.util.regex.Matcher m = patt.matcher(

                "{1:\n"
                        + "this is some more text - and some more and some more and even more\n"
                        + "this is some more text - and some more and some more and even more\n"
                        + "this is some more text - and some more and some more and even more\n"
                        + "-}\n"

        );

        assertTrue(m.find());
        assertFalse(m.matches());
    }


    @Test
    public void perfWorstCase() {

        int max = 10000;

        Pattern[] ps = new Pattern[max];
        Matcher[] ms = new Matcher[max];
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < max; i++) {
            sb.append('a');
        }


        for (int n = 1; n < max; n += 100) {

            ps[n] = Pattern.compile("(?:a*){" + n + "}a{" + n + "}");
            ms[n] = ps[n].matcher(sb.substring(0, n));
            assertTrue(ms[n].matches());

        }


        for (int n = 1; n < max; n += 100) {
            ms[n].reset(sb.substring(0, n));

            long before = System.nanoTime();
            assertTrue(ms[n].matches());
            long after = System.nanoTime();

            System.out.println("["+n+"]="+(after-before));
        }



    }

    // This example is documented in the com.google.re2j package.html.
    @Test
    public void testDocumentedExample() {
        Pattern p = Pattern.compile("b(an)*(.)");
        Matcher m = p.matcher("by, band, banana");
        assertTrue(m.lookingAt());
        m.reset();
        assertTrue(m.find());
        assertEquals("by", m.group(0));
        assertEquals(null, m.group(1));
        assertEquals("y", m.group(2));
        assertTrue(m.find());
        assertEquals("band", m.group(0));
        assertEquals("an", m.group(1));
        assertEquals("d", m.group(2));
        assertTrue(m.find());
        assertEquals("banana", m.group(0));
        assertEquals("an", m.group(1));
        assertEquals("a", m.group(2));
        assertFalse(m.find());
    }



    @Test
    public void matchStartTest() {
        Regex r = new Sample2();
        String s = "one cat two cats in the yard";
        r.init(s, 0, s.length());
        assertTrue(r.search());
        assertEquals(r.getMatchStart(), 4);
    }


    @Test
    public void appendReplacementTest() {

        Pattern p = Pattern.compile("cat");
        Matcher m = p.matcher("one cat two cats in the yard");
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            m.appendReplacement(sb, "dog");
        }
        m.appendTail(sb);

        assertEquals("one dog two dogs in the yard", sb.toString());
    }

    @Test
    public void replaceTest() {
        // /[a-c]*$/.replaceAll(abc,x) = xx; want x

        Pattern p = Pattern.compile("[a-c]*$");
        String res = p.replaceAll("abc", "x");
        assertEquals(res, "x");

        p = Pattern.compile("[a-c]*");
        res = p.replaceAll("f", "x");
        assertEquals("xfx", res);

        // /[a-c]*/.replaceAll(abcbcdcdedef,x) = xxdxxdxexdxexfx; want xdxdxexdxexfx
        p = Pattern.compile("[a-c]*");
        res = p.replaceAll("abcbcdcdedef", "x");
        assertEquals("xdxdxexdxexfx", res);


    }

    @Test
    public void gcregexTest() {
        // Pattern p = Pattern.compile("(\\S+):.*?(\\S*) secs\\]");
        Pattern p = Pattern.compile("2+:c*?x* ");
        Matcher m = p.matcher("22:22.058+0000][gc,cpu ] GC(1969) User=0.06s Sys=0.00s Real=0.01s");

        assertFalse(m.find());
    }

    @Test
    public void dsbRegex() {
        String str = "^\\w+$";
        Pattern p = Pattern.compile(str);
        Matcher m = p.matcher("W3SVC91");

        assertTrue(m.find());
    }
}