/*
    Copyright (c) Peter Sorotokin, 1998-2000
    Copyright (c) Humio, 2019
    See file "LICENSE.md" for terms of usage and
    redistribution.   
*/
package com.humio.jitrex.form;

import java.io.Writer;
import java.math.BigInteger;

public class OctalNumberFormatterSpan extends NumberFormatterSpan {
    public OctalNumberFormatterSpan(int min, int max, int alignment,
                                    char fillChar, int overflowChar) {
        super(min, max, alignment, fillChar, overflowChar, null);
    }

    public OctalNumberFormatterSpan(int min, int alignment, boolean upper) {
        super(min, alignment);
    }

    public void printf(Writer out, int v) {
        String s = Integer.toOctalString(v);
        printf(out, s.toCharArray());
    }

    public void printf(Writer out, char v) {
        String s = Integer.toOctalString(v);
        printf(out, s.toCharArray());
    }

    public void printf(Writer out, long v) {
        String s = Long.toOctalString(v);
        printf(out, s.toCharArray());
    }

    public void printf(Writer out, BigInteger v) {
        String s = v.toString(8);
        printf(out, s.toCharArray());
    }
}
