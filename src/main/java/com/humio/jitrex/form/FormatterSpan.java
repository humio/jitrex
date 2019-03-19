/*
    Copyright (c) Peter Sorotokin, 1998-2000
    Copyright (c) Humio, 2019
    See file "LICENSE.md" for terms of usage and
    redistribution.   
*/
package com.humio.jitrex.form;

import com.humio.util.jint.lang.CharString;

import java.io.IOException;
import java.io.Writer;
import java.math.BigDecimal;
import java.math.BigInteger;

public class FormatterSpan extends Span {

    public static final int ALIGN_LEFT = 1;
    public static final int ALIGN_RIGHT = 2;
    public static final int ALIGN_CENTER = 3;
    public static int ALIGN_MASK = 3;
    static char[] space = new char[32];

    static {
        for (int i = 0; i < space.length; i++)
            space[i] = ' ';
    }

    int minChars;
    int maxChars;
    int align;
    char padChar;
    int overflowChar;

    public FormatterSpan() {
        minChars = 0;
        maxChars = Integer.MAX_VALUE;
        align = ALIGN_RIGHT;
        padChar = ' ';
        overflowChar = '*';
    }

    public FormatterSpan(int min, int max, int alignment) {
        minChars = min;
        maxChars = max;
        align = alignment;
        padChar = ' ';
        overflowChar = '*';
    }

    public FormatterSpan(int min, int alignment) {
        minChars = min;
        maxChars = Integer.MAX_VALUE;
        align = alignment;
        padChar = ' ';
        overflowChar = '*';
    }

    public FormatterSpan(int min, int max, int alignment, char padChar, int overflowChar) {
        this.minChars = min;
        this.maxChars = max;
        this.align = alignment;
        this.padChar = padChar;
        this.overflowChar = overflowChar;
    }

    void pad(Writer out, int howMany) throws IOException {
        char f = padChar;
        if (f == ' ') {
            int l = space.length;
            while (howMany > l) {
                howMany -= l;
                out.write(space, 0, l);
            }
            if (howMany > 0)
                out.write(space, 0, howMany);
        } else {
            while (howMany > 0) {
                out.write(f);
                howMany--;
            }
        }
    }

    public void printf(Writer out, char[] arr, int first, int len) {
        try {
            if (len < minChars) {
                int diff = minChars - len;
                int before = 0;
                switch (align) {
                    case ALIGN_RIGHT:
                        before = diff;
                        break;
                    case ALIGN_CENTER:
                        before = diff / 2;
                        break;
                }
                if (before > 0)
                    pad(out, before);
                out.write(arr, first, len);
                int after = diff - before;
                if (after > 0)
                    pad(out, after);
            } else if (len > maxChars) {
                if (overflowChar > 0) {
                    char oc = (char) overflowChar;
                    for (int i = maxChars; i > 0; i--)
                        out.write(oc);
                } else {
                    int diff = len - maxChars;
                    switch (align) {
                        case ALIGN_RIGHT:
                            first += diff;
                            break;
                        case ALIGN_CENTER:
                            first += diff / 2;
                            break;
                    }
                    len = maxChars;
                }
            } else
                out.write(arr, first, len);
        } catch (IOException e) {
            throw new RuntimeException("IOException: " + e.getMessage());
        }
    }

    public void printf(Writer out, char[] arr) {
        printf(out, arr, 0, arr.length);
    }

    public void printf(Writer out, CharString cs) {
        int f = cs.first;
        printf(out, cs.buf, f, cs.last - f);
    }

    public void printf(Writer out, String s) {
        char[] ca = s.toCharArray();
        printf(out, ca, 0, ca.length);
    }

    public void printf(Writer out, Object obj) {
        if (obj instanceof CharString)
            printf(out, (CharString) obj);
        else if (obj instanceof String)
            printf(out, (String) obj);
        else if (obj instanceof Number) {
            if (obj instanceof Integer)
                printf(out, ((Number) obj).intValue());
            else if (obj instanceof Double)
                printf(out, ((Number) obj).doubleValue());
            else if (obj instanceof Long)
                printf(out, ((Number) obj).longValue());
            else if (obj instanceof Short)
                printf(out, ((Number) obj).shortValue());
            else if (obj instanceof Byte)
                printf(out, ((Number) obj).byteValue());
            else if (obj instanceof Float)
                printf(out, ((Number) obj).floatValue());
            else if (obj instanceof BigDecimal)
                printf(out, ((BigDecimal) obj));
            else if (obj instanceof BigInteger)
                printf(out, ((BigInteger) obj));
            else
                printf(out, obj.toString());
        } else if (obj instanceof Boolean)
            printf(out, ((Boolean) obj).booleanValue());
        else if (obj instanceof Character)
            printf(out, ((Character) obj).charValue());
        else if (obj == null)
            printf(out, "null");
        else
            printf(out, obj.toString());
    }

    public void printf(Writer out, BigDecimal d) {
        printf(out, d.toString().toCharArray());
    }

    public void printf(Writer out, BigInteger i) {
        printf(out, i.toString().toCharArray());
    }

    public void printf(Writer out, int v) {
        printf(out, Integer.toString(v).toCharArray());
    }

    public void printf(Writer out, double v) {
        printf(out, Double.toString(v).toCharArray());
    }

    public void printf(Writer out, char v) {
        char[] arr = {v};
        printf(out, arr, 0, arr.length);
    }

    public void printf(Writer out, boolean v) {
        printf(out, (v ? "true" : "false").toCharArray());
    }

    public void printf(Writer out, long v) {
        printf(out, Long.toString(v).toCharArray());
    }

    public void printf(Writer out, byte v) {
        printf(out, (int) v);
    }

    public void printf(Writer out, short v) {
        printf(out, (int) v);
    }

    public void print(Writer out, int[] argPtr, Object[] args) {
        int i = argPtr[0];
        argPtr[0] = i + 1;
        printf(out, args[i]);
    }

}
