// Copyright 2012 Google Inc. All Rights Reserved.

package com.humio.jitrex;

import com.google.re2.ApiTestUtils;
import com.humio.util.jint.util.CompilerException;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.*;

import static org.junit.Assert.*;

/**
 * This class checks that the behaviour of Pattern and JDK's Pattern are same, and we expect them
 * that way too.
 *
 * @author afrozm@google.com (Afroz Mohiuddin)
 */
@RunWith(JUnit4.class)
public class PatternTest {

  @Test
  public void testCompile() {
    Pattern p = Pattern.compile("abc");
    assertEquals("abc", p.pattern());
    assertEquals(0, p.flags());
  }

  @Test
  public void testToString() {
    Pattern p = Pattern.compile("abc");
    assertEquals("abc", p.toString());
  }

  @Test
  public void testCompileFlags() {
    Pattern p = Pattern.compile("abc", 5);
    assertEquals("abc", p.pattern());
    assertEquals(5, p.flags());
  }
/*
  @Test
  public void testSyntaxError() {
    boolean caught = false;
    try {
      Pattern.compile("abc(");
      fail("should have thrown");
    } catch (PatternSyntaxException e) {
      assertEquals(-1, e.getIndex());
      assertNotSame("", e.getDescription());
      assertNotSame("", e.getMessage());
      assertEquals("abc(", e.getPattern());
      caught = true;
    }
    assertEquals(true, caught);
  }
*/
  @Test
  public void testMatchesNoFlags() {
    ApiTestUtils.testMatches("ab+c", "abbbc", "cbbba");
    ApiTestUtils.testMatches("ab.*c", "abxyzc", "ab\nxyzc");
    ApiTestUtils.testMatches("^ab.*c$", "abc", "xyz\nabc\ndef");
  }

  @Test
  public void testMatchesWithFlags() {
    ApiTestUtils.testMatchesRE2("ab+c", 0, "abbbc", "cbba");
    ApiTestUtils.testMatchesRE2("ab+c", Pattern.CASE_INSENSITIVE, "abBBc", "cbbba");
    ApiTestUtils.testMatchesRE2("ab.*c", 0, "abxyzc", "ab\nxyzc");
    ApiTestUtils.testMatchesRE2("ab.*c", Pattern.DOTALL, "ab\nxyzc", "aB\nxyzC");
    ApiTestUtils.testMatchesRE2(
        "ab.*c", Pattern.DOTALL | Pattern.CASE_INSENSITIVE, "aB\nxyzC", "z");
    ApiTestUtils.testMatchesRE2("^ab.*c$", 0, "abc", "xyz\nabc\ndef");

    ApiTestUtils.testMatchesRE2("^ab.*c$", Pattern.MULTILINE, "abc", "xyz\nabc\ndef");
    ApiTestUtils.testMatchesRE2("^ab.*c$", Pattern.MULTILINE, "abc", "");
    ApiTestUtils.testMatchesRE2("^ab.*c$", Pattern.DOTALL | Pattern.MULTILINE, "ab\nc", "AB\nc");
    ApiTestUtils.testMatchesRE2(
        "^ab.*c$", Pattern.DOTALL | Pattern.MULTILINE | Pattern.CASE_INSENSITIVE, "AB\nc", "z");
  }

  private void testFind(String regexp, int flag, String match, String nonMatch) {
    assertEquals(true, Pattern.compile(regexp, flag).matcher(match).find());
    assertEquals(false, Pattern.compile(regexp, flag).matcher(nonMatch).find());
  }

  @Test
  public void testFind() {
    testFind("ab+c", 0, "xxabbbc", "cbbba");
    testFind("ab+c", Pattern.CASE_INSENSITIVE, "abBBc", "cbbba");
    testFind("ab.*c", 0, "xxabxyzc", "ab\nxyzc");
    testFind("ab.*c", Pattern.DOTALL, "ab\nxyzc", "aB\nxyzC");
    testFind("ab.*c", Pattern.DOTALL | Pattern.CASE_INSENSITIVE, "xaB\nxyzCz", "z");
    testFind("^ab.*c$", 0, "abc", "xyz\nabc\ndef");
    testFind("^ab.*c$", Pattern.MULTILINE, "xyz\nabc\ndef", "xyz\nab\nc\ndef");
    testFind("^ab.*c$", Pattern.DOTALL | Pattern.MULTILINE, "xyz\nab\nc\ndef", "xyz\nAB\nc\ndef");
    testFind(
        "^ab.*c$",
        Pattern.DOTALL | Pattern.MULTILINE | Pattern.CASE_INSENSITIVE,
        "xyz\nAB\nc\ndef",
        "z");
  }

  @Test
  public void testSplit() {
    ApiTestUtils.testSplit("/", "abcde", new String[] {"abcde"});
    ApiTestUtils.testSplit("/", "a/b/cc//d/e//", new String[] {"a", "b", "cc", "", "d", "e"});
    ApiTestUtils.testSplit("/", "a/b/cc//d/e//", 3, new String[] {"a", "b", "cc//d/e//"});
    ApiTestUtils.testSplit("/", "a/b/cc//d/e//", 4, new String[] {"a", "b", "cc", "/d/e//"});
    ApiTestUtils.testSplit("/", "a/b/cc//d/e//", 5, new String[] {"a", "b", "cc", "", "d/e//"});
    ApiTestUtils.testSplit("/", "a/b/cc//d/e//", 6, new String[] {"a", "b", "cc", "", "d", "e//"});
    ApiTestUtils.testSplit(
        "/", "a/b/cc//d/e//", 7, new String[] {"a", "b", "cc", "", "d", "e", "/"});
    ApiTestUtils.testSplit(
        "/", "a/b/cc//d/e//", 8, new String[] {"a", "b", "cc", "", "d", "e", "", ""});
    ApiTestUtils.testSplit(
        "/", "a/b/cc//d/e//", 9, new String[] {"a", "b", "cc", "", "d", "e", "", ""});

    // The tests below are listed at
    // http://docs.oracle.com/javase/1.5.0/docs/api/java/util/regex/Pattern.html#split(java.lang.CharSequence, int)

    String s = "boo:and:foo";
    String regexp1 = ":";
    String regexp2 = "o";

    ApiTestUtils.testSplit(regexp1, s, 2, new String[] {"boo", "and:foo"});
    ApiTestUtils.testSplit(regexp1, s, 5, new String[] {"boo", "and", "foo"});
    ApiTestUtils.testSplit(regexp1, s, -2, new String[] {"boo", "and", "foo"});
    ApiTestUtils.testSplit(regexp2, s, 5, new String[] {"b", "", ":and:f", "", ""});
    ApiTestUtils.testSplit(regexp2, s, -2, new String[] {"b", "", ":and:f", "", ""});
    ApiTestUtils.testSplit(regexp2, s, 0, new String[] {"b", "", ":and:f"});
    ApiTestUtils.testSplit(regexp2, s, new String[] {"b", "", ":and:f"});
  }

  @Test
  public void testGroupCount() {
    // It is a simple delegation, but still test it.
    ApiTestUtils.testGroupCount("(.*)ab(.*)a", 2);
    ApiTestUtils.testGroupCount("(.*)(ab)(.*)a", 3);
    ApiTestUtils.testGroupCount("(.*)((a)b)(.*)a", 4);
    ApiTestUtils.testGroupCount("(.*)(\\(ab)(.*)a", 3);
    ApiTestUtils.testGroupCount("(.*)(\\(a\\)b)(.*)a", 3);
  }

  @Test
  public void testQuote() {
    ApiTestUtils.testMatchesRE2(Pattern.quote("ab+c"), 0, "ab+c", "abc");
  }

  private Pattern reserialize(Pattern object) {
    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    try {
      ObjectOutputStream out = new ObjectOutputStream(bytes);
      out.writeObject(object);
      ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()));
      return (Pattern) in.readObject();
    } catch (IOException e) {
      throw new RuntimeException(e);
    } catch (ClassNotFoundException e) {
      throw new RuntimeException(e);
    }
  }

  private void assertSerializes(Pattern p) {
    Pattern reserialized = reserialize(p);
    assertEquals(p.pattern(), reserialized.pattern());
    assertEquals(p.flags(), reserialized.flags());
  }

  // @Test
  public void testSerialize() {
    assertSerializes(Pattern.compile("ab+c"));
    assertSerializes(Pattern.compile("^ab.*c$", Pattern.DOTALL | Pattern.MULTILINE));
    assertFalse(reserialize(Pattern.compile("abc")).matcher("def").find());
  }

  @Test
  public void testEquals() {
    Pattern pattern1 = Pattern.compile("abc");
    Pattern pattern2 = Pattern.compile("abc");
    Pattern pattern3 = Pattern.compile("def");
    Pattern pattern4 = Pattern.compile("abc", Pattern.CASE_INSENSITIVE);
    assertEquals(pattern1, pattern2);
    assertNotEquals(pattern1, pattern3);
    assertEquals(pattern1.hashCode(), pattern2.hashCode());
    assertNotEquals(pattern1.hashCode(), pattern4.hashCode());
  }

  // @Test
  public void testEmpty() {
    Pattern p = Pattern.compile("(?:)+");
    assertTrue( p.matches(""));

    p = Pattern.compile("(?:)*");
    assertTrue( p.matches(""));
    assertFalse( p.matches("xx"));

    p = Pattern.compile("(?:(?:(?:)))");
    assertTrue( p.matcher("xx").find());
    assertTrue( p.matches(""));

  }


  @Test
  public void testRepeat() {
    Pattern.compile("(?:(?:a{1}){0,1})");
  }

  @Test
  public void testRex() {
    Pattern p = Pattern.compile("[/]+");
    assertTrue(p.matches("/"));
    assertTrue(p.matches("////"));
  }

  @Test
  public void testInfiniteLoop() {
    try {
      Pattern p = Pattern.compile(".*?to\\s(.*+)\\s");
      Matcher m = p.matcher("\tto xxx in sss");
      assertEquals(true, m.find());
      assertEquals("xxx in", m.group(1));
    } catch (com.humio.util.jint.util.CompilerException e) {
      // ok
    }

  }

  // @Test
  public void testOOM() {
      Pattern p = Pattern.compile("\\s(?:(?:.*?)+)\\s");
      Matcher m = p.matcher("\tto xxx in sss");
      assertEquals(true, m.find());
      assertEquals("xxx in", m.group(1));
  }

  @Test
  public void testInfiniteLoop2() {
    java.util.regex.Pattern p = java.util.regex.Pattern.compile(".*?to\\s(.*+)\\s");
    java.util.regex.Matcher m = p.matcher("\tto xxx in sss");
    assertEquals(false, m.find());
  }

  @Test
  public void testRepeatOK() {
    com.humio.jitrex.Pattern.compile("(a+|b)+");
  }

  @Test
  public void testEmptyRepeat() {
    try {
      com.humio.jitrex.Pattern p = com.humio.jitrex.Pattern.compile("^*");
      Assert.fail("regex should not compile");
    } catch (CompilerException e) {
      // ok
    }
  }

  @Test
  public void testInfiniteLoop3() {
    try {
      com.google.re2j.Pattern p = com.google.re2j.Pattern.compile(".*?to\\s(.*+)\\s");
      com.google.re2j.Matcher m = p.matcher("\tto xxx in sss");
      assertEquals(true, m.find());
      assertEquals("xxx in", m.group(1));
    } catch (com.google.re2j.PatternSyntaxException e) {
      // ok
    }
  }


  @Test
  public void testLazyFlag1() {
      Pattern p = Pattern.compile(".*foo", Pattern.LAZY);
      Matcher m = p.matcher("foofoo");
      assertEquals(true, m.find());
      assertEquals("foo", m.group(0));
  }

  @Test
  public void testLazyFlag2() {
      Pattern p = Pattern.compile(".*foo", 0);
      Matcher m = p.matcher("foofoo");
      assertEquals(true, m.find());
      assertEquals("foofoo", m.group(0));
  }


}
