/*
    Copyright (c) Peter Sorotokin, 1998-2000
    Copyright (c) Humio, 2019
    See file "LICENSE.md" for terms of usage and
    redistribution.   
*/
package com.humio.regex.interp;

import com.humio.regex.compiler.RMachine;
import com.humio.regex.compiler.RVariable;
import com.humio.regex.compiler.RLabel;
import com.humio.regex.tree.CharClassCodes;
import com.humio.regex.util.Regex;

import java.io.ByteArrayOutputStream;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

public class RInterpMachine extends RMachine implements RInterpCommands, CharClassCodes {

    ByteArrayOutputStream buf = new ByteArrayOutputStream();
    Hashtable<String,Variable[]> vars = new Hashtable<>();

    int varCells = 0;
    int extCells = 0;

    int minLength = 0;
    int maxLength = Integer.MAX_VALUE;
    int flags = 0;

    Vector<LabelUse> labelUses = new Vector<>();
    Label mfLabel;

    public RInterpMachine() {
        setExtensions(EXT_HINT | EXT_MULTIFORK | EXT_CONDJUMP | FLAG_DOT_IS_ANY | EXT_SHIFTTBL);
    }

    private void writeLabel(RLabel rl) {
        if (rl == null) {
            buf.write(0xFF);
            buf.write(0xFF);
            return;
        }
        Label label = (Label) rl;
        if (label.offset < 0) {
            LabelUse use = new LabelUse();
            use.label = label;
            use.offset = buf.size();
            labelUses.addElement(use);
        }
        buf.write(label.offset >> 8);
        buf.write(label.offset);
    }

    private void writeCode(int code) {
        if (code <= 0x7F && code >= 0)
            buf.write(code);
        else if (code <= 0x3FFF && code > 0) {
            buf.write((code >> 8) | 0x80);
            buf.write(code);
        } else {
            buf.write(0xFF);
            buf.write(code >> 24);
            buf.write(code >> 16);
            buf.write(code >> 8);
            buf.write(code);
        }
    }

    private void finishLabels(byte[] codes) {
        Enumeration uses = labelUses.elements();
        while (uses.hasMoreElements()) {
            LabelUse use = (LabelUse) uses.nextElement();
            int what = use.label.offset;
            if (what < 0)
                throw new RuntimeException("Label is jumped on but not marked");
            int where = use.offset;
            codes[where++] = (byte) (what >> 8);
            codes[where] = (byte) what;
        }
        labelUses.removeAllElements();
    }

    public Regex makeRegex() {
        writeCode(STOP);
        byte[] codes = buf.toByteArray();
        finishLabels(codes);
    /*
       for( int i = 0 ; i < codes.length ; i++ )
         System.out.println( Integer.toHexString(i) + ": " +
			 Integer.toHexString(codes[i]&0xFF) );
    */
        return new InterpretedRegex(codes, vars, varCells, flags, minLength, maxLength, extCells);
    }

    //-----

    public int getNVars() {
        return vars.size();
    }

    public RVariable newVar(String name, boolean begin) {
        Variable[] list = (Variable[]) vars.get(name);
        if (list == null) {
            Variable v1 = new Variable();
            v1.cell = varCells++;
            Variable v2 = new Variable();
            v2.cell = varCells++;
            Variable[] pair = {v1, v2};
            list = pair;
            vars.put(name, list);
        }
        return list[begin ? 0 : 1];
    }

    //----- basic instructions

    public RLabel newLabel() {
        return new Label();
    }

    public RVariable newTmpVar(int init) {
        Variable v = new Variable();
        v.cell = varCells++;
        writeCode(ASSIGN);
        writeCode(v.cell);
        writeCode(init);
        return v;
    }

    public void mark(RLabel label) {
        ((Label) label).offset = buf.size();
    }

    public void pick(RVariable v) {
        writeCode(PICK);
        writeCode(((Variable) v).cell);
    }

    public void fork(RLabel forkLabel) {
        writeCode(FORK);
        writeLabel(forkLabel);
    }

    public void skip() {
        if ((flags & FLAG_DOT_IS_ANY) != 0)
            writeCode(SKIP);
        else
            writeCode(SKIP_NON_NEWLINE);
    }

    public void boundary(int boundaryClass) {
        writeCode(BOUNDARY);
        writeCode(boundaryClass);
    }

    public void assert2(int charClass, char[] ranges) {
        if (ranges != null && ranges.length > 0)
            if (charClass != CLASS_DISABLED)
                writeCode(ASSERT_CLASS_RANGE);
            else
                writeCode(ASSERT_RANGE);
        else if (charClass != CLASS_DISABLED)
            writeCode(ASSERT_CLASS);
        else
            throw new RuntimeException("Empty char class");
        if (charClass != CLASS_DISABLED)
            writeCode(charClass);
        if (ranges != null && ranges.length > 0) {
            writeCode(ranges.length / 2);
            for (int i = 0; i < ranges.length; i++)
                writeCode(ranges[i]);
        }
    }

    public void assert2(char[] constStr) {
        writeCode(ASSERT_CHAR);
        writeCode(constStr.length);
        for (int i = 0; i < constStr.length; i++)
            writeCode(constStr[i] & 0xFFFF);
    }

    public void assert2(String ref, boolean picked) {
        Variable[] v = (Variable[]) vars.get(ref);
        if (picked)
            writeCode(ASSERT_VAR);
        else {
            writeCode(ASSERT_EXT_VAR);
            if (v == null) {
                Variable v1 = new Variable();
                v1.cell = varCells++;
                v1.extCell = extCells;
                Variable v2 = new Variable();
                v2.cell = varCells++;
                v2.extCell = extCells++;
                Variable[] pair = {v1, v2};
                v = pair;
                vars.put(ref, v);
            }
            writeCode(v[0].extCell);
        }
        if (v == null)
            throw new RuntimeException("Variable " + ref + " not known!");
        writeCode(v[0].cell);
    }

    public void hardAssign(RVariable v, int value) {
        writeCode(HARDASSIGN);
        writeCode(((Variable) v).cell);
        writeCode(value);
    }

    public void decjump(RVariable var, RLabel label) {
        writeCode(DECJUMP);
        writeCode(((Variable) var).cell);
        writeLabel(label);
    }

    public void decfail(RVariable var) {
        writeCode(DECFAIL);
        writeCode(((Variable) var).cell);
    }

    public void forget(RVariable var) {
    }

    public void jump(RLabel label) {
        writeCode(JUMP);
        writeLabel(label);
    }

    public void fail() {
        writeCode(FAIL);
    }

    public void hint(int flags, int minLength, int maxLength) {
        this.flags = flags;
        this.minLength = minLength;
        this.maxLength = maxLength;
    }

    //---- extended instructions ----

    //----- EXT_HINT extension

    public void mfStart(int headDecrement, int min) {
        if (mfLabel != null)
            throw new IllegalStateException("Netsed mfStart-mfEnd calls or mfEnd call missing");
        if (min >= 0) {
            writeCode(MFSTART_HEAD);
            writeCode(min);
        } else
            writeCode(MFSTART);
        writeCode(headDecrement);
        mfLabel = new Label();
        writeLabel(mfLabel);
    }

    //----- EXT_MULTIFORK extension

    public void mfEnd(int max) {
        if (mfLabel == null)
            throw new IllegalStateException("mfStart call missing");
        if (max < Integer.MAX_VALUE) {
            writeCode(MFENDLIMIT);
            writeCode(max);
        } else {
            writeCode(MFEND);
        }
        mark(mfLabel);
        mfLabel = null;
    }

    /**
     * Jump if char is NOT in range
     */
    public void condJump(char[] ranges, RLabel label) {
        writeCode(JUMP_RANGE);
        writeLabel(label);
        writeCode(ranges.length / 2);
        for (int i = 0; i < ranges.length; i++)
            writeCode(ranges[i]);
    }

    /**
     * Jump if less then atLeast or more then atMost chars left
     */
    public void condJump(int atLeastCharLeft, int atMostCharLeft, RLabel label) {
        if (atLeastCharLeft > 0) {
            writeCode(JUMP_MIN_LEFT);
            writeLabel(label);
            writeCode(atLeastCharLeft);
        }
        if (atMostCharLeft < 2048) {
            writeCode(JUMP_MAX_LEFT);
            writeLabel(label);
            writeCode(atMostCharLeft);
        }
    }

    //----- EXT_CONDJUMP extension

    //----- EXT_CONDJUMP extension

    /**
     * Jump if char is NOT one that is given.
     */
    public void condJump(char c, RLabel label) {
        writeCode(JUMP_CHAR);
        writeLabel(label);
        writeCode(c);
    }

    public void shiftTable(boolean beginning, int charsAhead, char[] chars, int[] shifts) {
        if (!beginning)
            return;
        writeCode(SHIFTTBL);
        writeCode(charsAhead);
        writeCode(chars.length);
        for (int i = 0; i < chars.length; i++) {
            writeCode(chars[i]);
            writeCode(shifts[i]);
        }
    }

    static class Label extends RLabel {
        int offset = -1;
    }

    //----- EXT_SHIFTTBL extension

    static class LabelUse {
        Label label;
        int offset;
    }

}

