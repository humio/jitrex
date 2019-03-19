/*
    Copyright (c) Peter Sorotokin, 1998-2000
    Copyright (c) Humio, 2019
    See file "LICENSE.md" for terms of usage and
    redistribution.   
*/
package com.humio.jitrex.form;

import java.io.Writer;
import java.math.BigDecimal;

public class RealNumberFormatterSpan extends NumberFormatterSpan {
    int precision;
    boolean sign;

    public RealNumberFormatterSpan(int min, int max, int alignment,
                                   char fillChar, int overflowChar, int precision, boolean sign) {
        super(min, max, alignment, fillChar, overflowChar, null);
        this.precision = precision;
        this.sign = sign;
    }

    public RealNumberFormatterSpan(int min, int alignment, int precision) {
        super(min, alignment);
        this.precision = precision;
    }

    private void append(StringBuffer sb, double v) {
        if (precision == 0)
            if (v >= Long.MAX_VALUE)
                sb.append(v);
            else
                sb.append(Math.round(v));
        else {
            double vm = v;
            for (int i = precision; i > 0; i--)
                vm *= 10;
            if (vm >= Long.MAX_VALUE)
                sb.append(v);
            else {
                String s = Long.toString(Math.round(vm));
                int l = s.length();
                if (l > precision) {
                    sb.append(s, 0, l - precision);
                    sb.append('.');
                    sb.append(s.substring(l - precision));
                } else {
                    sb.append("0.");
                    while (l < precision) {
                        sb.append(0);
                        l++;
                    }
                    sb.append(s);
                }
            }
        }
    }

    public void printf(Writer out, double v) {
        if (Double.isInfinite(v))
            if (v > 0)
                printf(out, "+Inf".toCharArray());
            else
                printf(out, "-Inf".toCharArray());
        else if (Double.isNaN(v))
            printf(out, "NaN".toCharArray());
        else {
            StringBuffer sb = new StringBuffer();
            if (v < 0) {
                sb.append('-');
                v = -v;
            } else if (sign)
                sb.append('+');
            if (padChar == '0' && sb.length() > 0) {
                StringBuffer sb1 = new StringBuffer();
                append(sb1, v);
                for (int i = minChars - sb1.length() - 1; i > 0; i--)
                    sb.append('0');
                sb.append(sb1);
            } else
                append(sb, v);
            printf(out, sb.toString().toCharArray());
        }
    }

    public void printf(Writer out, BigDecimal v) {
        v = v.setScale(precision);
        printf(out, v.toString().toCharArray());
    }

}
