/*
    Copyright (c) Peter Sorotokin, 1998-2000
    Copyright (c) Humio, 2019
    See file "LICENSE.md" for terms of usage and
    redistribution.   
*/
package com.humio.jitrex.form;

import com.humio.util.jint.constants.MiniErrorCodes;
import com.humio.util.jint.util.CompilerException;

import java.util.Vector;

public class FParser implements MiniErrorCodes {

    private static int parseVariable(char[] buf, int i, int maxIndex, Vector<String> varAcc) {
        int start = i;
        if (i >= maxIndex)
            throw new CompilerException(ERR_F_NOVARNAME);
        char c = buf[i];
        if (c != '{') {
            while (true) {
                if (i >= maxIndex)
                    break;
                if (!Character.isLetterOrDigit(buf[i]) && buf[i] != '_')
                    break;
                i++;
            }
            varAcc.addElement(new String(buf, start, i - start));
            return i;
        } else {
            do {
                c = buf[i++];
                if (i >= maxIndex)
                    throw new CompilerException(ERR_F_NOCURLBRACKET);
            }
            while (c != '}' && c != '%');
            varAcc.addElement(new String(buf, start + 1, i - start - 2));
            return i - 1;
        }
    }

    public static void parse(Vector<Span> acc, char[] buf, int off, int last, Vector<String> varAcc) {
        int stringStart = off;
        char c = (char) 0;
        while (true) {
            if (off < last && (c = buf[off]) != '%' && (varAcc == null || (c != '$' && c != '\\'))) {
                off++;
                continue;
            }
            if (off > stringStart)
                acc.addElement(new StringSpan(new String(buf, stringStart, off - stringStart)));
            if (off >= last)
                break;
            off++;
            if (off >= last) {
                Object[] addInfo = {"'" + c + "'"};
                throw new CompilerException(ERR_F_BADEND, -1, addInfo);
            }

            boolean closeBracket = false;
            if (c != '%') {
                // '$' or \
                closeBracket = buf[off] == '{';
                off = parseVariable(buf, off, last, varAcc);
                if (off >= last || buf[off] != '%') {
                    stringStart = off;
                    acc.addElement(new FormatterSpan(0,
                            Integer.MAX_VALUE, FormatterSpan.ALIGN_RIGHT, ' ', '*'));
                    continue;
                }
                off++;
                if (off >= last) {
                    Object[] addInfo = {"%"};
                    throw new CompilerException(ERR_F_BADEND, -1, addInfo);
                }
                c = buf[off];
            } else {
                c = buf[off];
                if (c == '%') {
                    stringStart = off;
                    continue;
                }
                if (varAcc != null && c != 'n')
                    varAcc.addElement(null);
            }

            String javaFormat = null;
            boolean sharp = false;
            boolean done = false;
            boolean hadDot = false;
            boolean uppercase = false;
            boolean exact = false;
            boolean sign = false;
            int width = 0;
            int precision = 0;
            int align = FormatterSpan.ALIGN_RIGHT;
            char padWith = ' ';
            while (!done) {
                switch (c) {
                    case '#':
                        sharp = true;
                        break;
                    case '-':
                        align = FormatterSpan.ALIGN_LEFT;
                        break;
                    case '+':
                        sign = true;
                        break;
                    case '=':
                        exact = true;
                        break;
                    case '^':
                        align = FormatterSpan.ALIGN_CENTER;
                        break;
                    case '.':
                        hadDot = true;
                        break;
                    case 'n': // newline
                        done = true;
                        break;
                    case 'X':
                    case 'E':
                    case 'G':
                        uppercase = true;
                        done = true;
                        break;
                    case 'd':
                    case 'i':
                    case 'o':
                    case 'c':
                    case 'x':
                    case 'g':
                    case 'f':
                    case 'e':
                    case 's':
                    case 't': // date&time
                    case 'T': // date&time
                        done = true;
                        break;
                    case '[':
                        // java.text.* formats access
                    {
                        int start = ++off;
                        while (off < last && buf[off] != ']')
                            off++;
                        if (off >= last) {
                            Object[] addInfo = {"%...["};
                            throw new CompilerException(ERR_F_BADEND, -1, addInfo);
                        }
                        javaFormat = new String(buf, start, off - start);
                    }
                    break;
                    case '0':
                        if (width == 0 && !hadDot) {
                            padWith = '0';
                            break;
                        }
                        // fall thru
                    default: {
                        if ('0' <= c && c <= '9') {
                            if (hadDot)
                                precision = (precision * 10 + (c - '0'));
                            else
                                width = (width * 10 + (c - '0'));
                            break;
                        }
                        Object[] addInfo = {"'" + c + "'"};
                        throw new CompilerException(ERR_F_BADFORMATCHAR, -1, addInfo);
                    }
                }
                if (!done) {
                    off++;
                    if (off >= last)
                        throw new CompilerException(ERR_F_INCOMPLETE);
                    c = buf[off];
                }
            }

            Span span;
            int maxWidth = (exact ? width : Integer.MAX_VALUE);
            if (precision == 0)
                precision = 6;
            if (align != FormatterSpan.ALIGN_RIGHT)
                padWith = ' ';

            if (javaFormat != null || c == 't' || c == 'T') {
                switch (c) {
                    case 'd':
                    case 'i':
                    case 'g':
                    case 'G':
                    case 'f':
                    case 'F':
                    case 'e':
                    case 'E':
                        span = new NumberFormatterSpan(width, maxWidth, align, ' ', '*', javaFormat);
                        break;
                    case 'T':
                    case 't':
                        span = new DateTimeFormatterSpan(width, maxWidth, align, ' ', '*',
                                c == 'T', javaFormat);
                        break;
                    default: {
                        String[] addInfo = {"'" + c + "'"};
                        throw new CompilerException(ERR_F_NOTWITHJAVA, -1, addInfo);
                    }
                }
            } else if (c == 'd' || c == 'i')
                span = new DecimalNumberFormatterSpan(width, maxWidth, align, padWith, '*', sign);
            else if (c == 'o')
                span = new OctalNumberFormatterSpan(width, maxWidth, align, padWith, '*');
            else if (c == 'c')
                span = new CharFormatterSpan(width, maxWidth, align, padWith, '*');
            else if (c == 'x' || c == 'X')
                span = new HexNumberFormatterSpan(width, maxWidth, align, padWith, '*', uppercase);
            else if (c == 'g' || c == 'G' || c == 'f' || c == 'F' || c == 'e' || c == 'E')
                span = new RealNumberFormatterSpan(width, maxWidth, align, padWith, '*', precision, sign);
            else if (c == 'n')
                span = new NewLineSpan();
            else
                span = new FormatterSpan(width, maxWidth, align, ' ', '*');
            acc.addElement(span);
            off++;
            if (closeBracket) {
                if (off >= last || buf[off++] != '}')
                    throw new CompilerException(ERR_F_NOCURLBRACKET);
            }
            stringStart = off;
        }
    }

    public static Span[] parse(String form, Vector<String> varAcc) {
        char[] arr = form.toCharArray();
        Vector<Span> acc = new Vector<Span>();
        parse(acc, arr, 0, arr.length, varAcc);
        Span[] res = new Span[acc.size()];
        acc.copyInto(res);
        return res;
    }
}
