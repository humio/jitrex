package com.humio.jitrex.jvm;

import junit.framework.TestCase;

public class RJavaClassMachineTest extends TestCase {

    public void testEncodeAsIdentifier() {
        assertEquals(RJavaClassMachine.encodeAsIdentifier(".*"), "_2e_2a");
        assertEquals(RJavaClassMachine.encodeAsIdentifier("a b c"), "a_20b_20c");
        assertEquals(RJavaClassMachine.encodeAsIdentifier("a_b_c"), "a_5fb_5fc");
        assertEquals(RJavaClassMachine.encodeAsIdentifier("funkyChicken"), "funkyChicken");
    }

    public void testCrc32() {
        assertEquals(RJavaClassMachine.crc32(".*"), "9165d205");
        assertEquals(RJavaClassMachine.crc32("funkyChicken"), "4b3f0849");
    }

}
