/*
    Copyright (c) Peter Sorotokin, 1998-2000
    Copyright (c) Humio, 2019
    See file "LICENSE.md" for terms of usage and
    redistribution.   
*/
package com.humio.jitrex.compiler;

import java.io.PrintStream;

/**
 * RDebugMachine is used to dump jitrex instruction sequence
 * to stdout (or other output stream).
 */
public class RDebugMachine extends RMachine {

    PrintStream out = System.out;
    int count = 1;

    public RDebugMachine() {
        setExtensions(EXT_HINT | EXT_MULTIFORK | EXT_CONDJUMP | EXT_SHIFTTBL);
    }

    private String str(boolean neg, RLabel l) {
        if (l == null)
            return " -> fail";
        if (neg)
            return " -> " + l; // jump if OK
        else
            return " [" + l + "]"; // jump if FAILED
    }

    private void printRange(char[] ranges) {
        for (int i = 0; i < ranges.length; i += 2) {
            if (i != 0)
                out.print(",");
            char cf = ranges[i];
            char cl = ranges[i + 1];
            if (cf > ' ' && cf <= '~')
                out.print(cf);
            else
                out.print("\\u" + Integer.toHexString(cf));
            if (cf != cl) {
                out.print('-');
                if (cl > ' ' && cl <= '~')
                    out.print(cl);
                else
                    out.print("\\u" + Integer.toHexString(cl));
            }
        }
    }

    private RVariable newVarInt(final String nm) {
        return new RVariable() {
            public String toString() {
                return nm;
            }
        };
    }

    public RVariable newVar(String name, boolean begin) {
        return newVarInt(name + "." + (begin ? "b" : "e"));
    }

    public RLabel newLabel() {
        final String nm = "L" + (count++);
        return new RLabel() {
            public String toString() {
                return nm;
            }
        };
    }

    //----- basic instructions

    public void mark(RLabel label) {
        out.print(label + ":");
    }

    public void pick(RVariable v) {
        out.println("\tpick $" + v);
    }

    public void fail() {
        out.println("\tfail");
    }

    public void fork(RLabel forkLabel) {
        out.println("\tfork " + forkLabel);
    }

    public void skip() {
        out.println("\tskip");
    }

    public void boundary(int boundaryClass) {
        out.println("\tboundary " + boundaryClass);
    }

    public void assert2(char[] constStr) {
        out.print("\tassert '");
        for (int i = 0; i < constStr.length; i++)
            out.print(constStr[i]);
        out.println("'");
    }

    public void assert2(int charClass, char[] ranges) {
        out.print("\tassert class[" + charClass + "] ");
        printRange(ranges);
        out.println();
    }

    public void assert2(String ref, boolean picked) {
        out.println("\tassert $" + ref + " " + picked);
    }

    public void hardAssign(RVariable v, int value) {
        out.println("\thardAssign " + v + " " + value);
    }

    public RVariable newTmpVar(int init) {
        RVariable var = newVarInt("V" + (count++));
        out.println("\t.tmp " + var + " = " + init);
        return var;
    }

    public void decjump(RVariable var, RLabel label) {
        out.println("\tdecjump " + var + " -> " + label);
    }

    public void decfail(RVariable var) {
        out.println("\tdecfail " + var);
    }

    public void forget(RVariable var) {
        out.println("\t.forget " + var);
    }

    public void jump(RLabel label) {
        out.println("\tjump " + label);
    }

    //---- extended instructions ----

    //----- EXT_HINT extension

    public void hint(int flags, int minLength, int maxLength) {
        out.print("\t.hint " + Integer.toHexString(flags) + " " + minLength + " ");
        if (maxLength == Integer.MAX_VALUE)
            out.println("*");
        else
            out.println(maxLength);
    }

    //----- EXT_MULTIFORK extension

    public void mfStart(int headDecrement, int minCount) {
        out.println("\tmfStart " + headDecrement +
                " (" + minCount + ")");
    }

    public void mfEnd(int maxCount) {
        out.println("\tmfEnd " + (maxCount < Integer.MAX_VALUE ? "" + maxCount : "*"));
    }

    //----- EXT_CONDJUMP extension

    public void condJump(char[] ranges, RLabel label) {
        out.print("\tjumpIfNotRange ");
        printRange(ranges);
        out.println(str(true, label));
    }

    public void condJump(int atLeastCharLeft, int atMostCharLeft, RLabel label) {
        out.print("\tjumpIfNotCharLeft ");
        if (atLeastCharLeft <= 0)
            out.print("*");
        else
            out.print(atLeastCharLeft);
        out.print(" ");
        if (atMostCharLeft < 0 || atMostCharLeft == Integer.MAX_VALUE)
            out.print("*");
        else
            out.print(atMostCharLeft);
        out.println(str(true, label));
    }

    public void condJump(char c, RLabel label) {
        out.print("\tjumpIfNotChar '" + c + "'");
        out.println(str(true, label));
    }

    //----- EXT_SHIFTTBL extension

    public void shiftTable(boolean beginning, int charsAhead, char[] chars, int[] shifts) {
        out.println("\tshiftTable " + (beginning ? "B" : "E") + " " + charsAhead);
        for (int i = 0; i < chars.length; i++)
            out.println("\t    '" + chars[i] + "' -> " + shifts[i]);
    }

}

