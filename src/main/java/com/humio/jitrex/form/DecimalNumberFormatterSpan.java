/*
    Copyright (c) Peter Sorotokin, 1998-2000
    Copyright (c) Humio, 2019
    See file "LICENSE.md" for terms of usage and
    redistribution.   
*/
package com.humio.jitrex.form;

import java.io.Writer;

public class DecimalNumberFormatterSpan extends NumberFormatterSpan {

    boolean sign;

    public DecimalNumberFormatterSpan(int min, int max, int alignment,
                                      char fillChar, int overflowChar, boolean sign) {
        super(min, max, alignment, fillChar, overflowChar, null);
        this.sign = sign;
    }

    public DecimalNumberFormatterSpan(int min, int alignment, boolean upper) {
        super(min, alignment);
    }

    public void printf(Writer out, int v) {
        if ((sign || v < 0) && padChar == '0') {
            StringBuffer sb = new StringBuffer();
            String s;
            // Remember, Integer.MIN_VALUE cannot be negated!
            if (v < 0) {
                sb.append('-');
                s = Integer.toString(v).substring(1);
            } else {
                sb.append('+');
                s = Integer.toString(v);
            }
            for (int i = minChars - s.length() - 1; i > 0; i--)
                sb.append('0');
            sb.append(s);
            printfString(out, sb.toString());
        } else {
            if (sign && v >= 0)
                printfString(out, "+" + v);
            else
                printfString(out, Integer.toString(v));
        }
    }

    public void printf(Writer out, long v) {
        if (javaFormat != null) {
            printfString(out, javaFormat.format(v));
            return;
        }
        if ((sign || v < 0) && padChar == '0') {
            StringBuffer sb = new StringBuffer();
            String s;
            // Remember, Long.MIN_VALUE cannot be negated!
            if (v < 0) {
                sb.append('-');
                s = Long.toString(v).substring(1);
            } else {
                sb.append('+');
                s = Long.toString(v);
            }
            for (int i = minChars - s.length() - 1; i > 0; i--)
                sb.append('0');
            sb.append(s);
            printfString(out, sb.toString());
        } else {
            if (sign && v >= 0)
                printfString(out, "+" + v);
            else
                printfString(out, Long.toString(v));
        }
    }
}






