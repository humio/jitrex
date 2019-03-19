/*
    Copyright (c) Peter Sorotokin, 1998-2000
    Copyright (c) Humio, 2019
    See file "LICENSE.md" for terms of usage and
    redistribution.   
*/
package com.humio.jitrex.form;

import com.humio.util.jint.lang.CharString;

import java.io.Writer;

public class CharFormatterSpan extends FormatterSpan {

    public CharFormatterSpan(int min, int max, int alignment,
                             char fillChar, int overflowChar) {
        super(min, max, alignment, fillChar, overflowChar);
    }

    public CharFormatterSpan(int min, int alignment) {
        super(min, alignment);
    }

    public void printf(Writer out, int v) {
        super.printf(out, (char) v);
    }

    public void printf(Writer out, long v) {
        super.printf(out, (char) v);
    }

    public void printf(Writer out, double v) {
        super.printf(out, (char) v);
    }

    public void printf(Writer out, float v) {
        super.printf(out, (char) v);
    }

    public void printf(Writer out, CharString cs) {
        printf(out, cs.toString());
    }

    public void printf(Writer out, String s) {
        super.printf(out, (char) Double.parseDouble(s));
    }

    protected void printfString(Writer out, String s) {
        super.printf(out, s);
    }

    public void printf(Writer out, Object obj) {
        if (obj instanceof Number)
            super.printf(out, obj);
        else
            printf(out, obj.toString());
    }

}

