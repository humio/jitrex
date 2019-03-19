/*
    Copyright (c) Peter Sorotokin, 1998-2000
    Copyright (c) Humio, 2019
    See file "LICENSE.md" for terms of usage and
    redistribution.   
*/
package com.humio.util.jint.lang;

import com.humio.regex.util.Regex;

/**
 * Character array segment. CharString object has roughly the same
 * functionality as String object, but it has its internal structure
 * exposed, so it can be used more efficient. The main difference is that
 * while String is immutable, CharString is mutable, so it requires more
 * careful programming. One cannot automatically assume that CharString
 * will not be changed, like it is done for Strings.
 */
public class CharString {

    private static char[] EMPTY = {};

    public char[] buf;
    public int first;
    public int last;

    public CharString() {
        buf = EMPTY;
    }

    public CharString(String str) {
        buf = str.toCharArray();
        last = buf.length;
    }

    public CharString(Object obj) {
        this(obj.toString());
    }

    public CharString(byte[] arr) {
        this(arr, 0, arr.length);
    }

    public CharString(byte[] arr, int off, int len) {
        char[] b = new char[len];
        for (int i = 0; i < len; i++)
            b[i] = (char) (arr[i + off] & 0xFF);
        buf = b;
        first = 0;
        last = off;
    }

    public CharString(byte[] arr, int off, int len, String encoding)
            throws java.io.UnsupportedEncodingException {
        this(new String(arr, off, len, encoding));
    }

    public CharString(byte[] arr, String encoding)
            throws java.io.UnsupportedEncodingException {
        this(new String(arr, encoding));
    }

    public CharString(char[] b) {
        buf = b;
        last = b.length;
    }

    public CharString(char[] b, int first, int len) {
        buf = b;
        this.first = first;
        last = first + len;
    }

    public static CharString valueOf(char c) {
        return new CharString(String.valueOf(c));
    }

    public static CharString valueOf(double d) {
        return new CharString(String.valueOf(d));
    }

    public static CharString valueOf(float f) {
        return new CharString(String.valueOf(f));
    }

    public static CharString valueOf(int i) {
        return new CharString(String.valueOf(i));
    }

    public static CharString valueOf(long l) {
        return new CharString(String.valueOf(l));
    }

    public static CharString valueOf(Object obj) {
        return new CharString(String.valueOf(obj));
    }

    public static CharString valueOf(boolean b) {
        return new CharString(String.valueOf(b));
    }

    public static CharString valueOf(char[] s) {
        return new CharString(s);
    }

    public static CharString valueOf(char[] s, int off, int len) {
        return new CharString(s, off, len);
    }

    public char charAt(int i) {
        // remember, this class does not do any error checking
        return buf[i + first];
    }

    public int compareTo(Object obj) {
        if (obj == this)
            return 0;
        if (obj.getClass() != CharString.class)
            return -2;
        return compareTo((CharString) obj);
    }

    public int compareTo(String s) {
        return compareTo(new CharString(s));
    }

    public int compareToIgnoreCase(String s) {
        return compareToIgnoreCase(new CharString(s));
    }

    public int compareTo(CharString other) {
        char[] b = buf;
        char[] ob = other.buf;

        int tindex = first;
        int oindex = other.first;

        int tlen = last - tindex;
        int olen = other.last - oindex;
        int len = (tlen > olen ? olen : tlen);

        int max = len + tindex;

        while (tindex < max) {
            int q = b[tindex++] - ob[oindex++];
            if (q != 0)
                return q;
        }

        return tlen - olen;
    }

    public int compareToIgnoreCase(CharString other) {
        char[] b = buf;
        char[] ob = other.buf;

        int tindex = first;
        int oindex = other.first;

        int tlen = last - tindex;
        int olen = other.last - oindex;
        int len = (tlen > olen ? olen : tlen);

        int max = len + tindex;

        while (tindex < max) {
            int q = Character.toLowerCase(b[tindex++]) -
                    Character.toLowerCase(ob[oindex++]);
            if (q != 0)
                return q;
        }

        return tlen - olen;
    }

    public CharString concat(CharString str) {
        int len1 = last - first;
        int len2 = str.last - str.first;
        char[] buf1 = new char[len1 + len2];
        System.arraycopy(buf, 0, buf1, 0, len1);
        System.arraycopy(str.buf, 0, buf1, len1, len2);
        return new CharString(buf1);
    }

    public boolean endsWith(CharString str) {
        int ol = str.last - str.first;
        return regionMatches(false, last - first - ol, str, 0, ol);
    }

    public boolean equals(Object obj) {
        if (obj == this)
            return true;
        if (obj == null)
            return false;
        if (obj.getClass() != CharString.class)
            return false;
        CharString cs = (CharString) obj;
        char[] otherBuf = cs.buf;
        int i = first;
        int j = cs.first;
        int l = last;
        if (l - i != cs.last - j)
            return false;
        char[] b = buf;
        while (i < l)
            if (b[i++] != otherBuf[j++])
                return false;
        return true;
    }

    public boolean equalsIgnoreCase(CharString s) {
        if (s == this)
            return true;
        return regionMatches(true, 0, s, 0, last - first);
    }

    public byte[] getBytes() {
        return toString().getBytes();
    }

    public byte[] getBytes(String enc)
            throws java.io.UnsupportedEncodingException {
        return toString().getBytes(enc);
    }

    public void getChars(int srcBegin, int srcEnd, char[] dest, int destBegin) {
        System.arraycopy(buf, first + srcBegin, dest, destBegin, srcEnd - srcBegin);
    }

    public int hashCode() {
        int acc = 0;
        int f = first;
        char[] b = buf;
        int i31 = 1;
        for (int k = last - 1; k >= f; k--) {
            acc += buf[k] * i31;
            i31 = i31 * 31;
        }
        return acc;
    }

    public int indexOf(int c) {
        return indexOf(c, 0);
    }

    public int indexOf(int c, int fromIndex) {
        int l = last;
        char[] b = buf;
        int k = fromIndex;
        if (k < 0)
            k = first;
        else
            k += first;
        for (; k < l; k++) {
            if (b[k] == c)
                return k;
        }
        return -1;
    }

    public int indexOf(CharString other) {
        return indexOf(other, 0);
    }

    public int indexOf(CharString other, int fromIndex) {
        char[] b = buf;
        int of = other.first;
        int ol = other.last - of;
        if (ol <= 0)
            return fromIndex;
        char[] ob = other.buf;
        int k = fromIndex;
        int f = first;
        int max = last - f - ol;
        if (k < 0)
            k = f;
        else
            k += f;
        char fc = ob[of];
        LOOP:
        for (; k < max; k++) {
            if (b[k] == fc) {
                for (int i = 1; i < ol; i++) {
                    if (b[k + i] != ob[of + i])
                        continue LOOP;
                }
                return k;
            }
        }
        return -1;
    }

    public int lastIndexOf(int c) {
        return lastIndexOf(c, last - first - 1);
    }

    public int lastIndexOf(int c, int fromIndex) {
        int f = first;
        char[] b = buf;
        int len = last - f;
        if (fromIndex >= len)
            fromIndex = len - 1;
        for (int k = fromIndex; k >= 0; k--) {
            if (b[k + f] == c)
                return k;
        }
        return -1;
    }

    public int lastIndexOf(CharString other) {
        return lastIndexOf(other, Integer.MAX_VALUE);
    }

    public int lastIndexOf(CharString other, int fromIndex) {
        int f = first;
        char[] b = buf;
        int of = other.first;
        char[] ob = other.buf;
        int ol = (other.last - of);
        int maxFrom = last - f - ol;
        if (fromIndex > maxFrom)
            fromIndex = maxFrom;
        char fc = ob[of];
        LOOP:
        for (int k = fromIndex; k >= 0; k--) {
            if (b[k + f] == fc) {
                for (int i = 1; i < ol; i++) {
                    if (b[k + i] != ob[of + i])
                        continue LOOP;
                }
                return k;
            }
        }
        return -1;
    }

    public int length() {
        return last - first;
    }

    public boolean regionMatches(int tindex, CharString other, int oindex, int len) {
        return regionMatches(false, tindex, other, oindex, len);
    }

    public boolean regionMatches(boolean ignoreCase, int tindex,
                                 CharString other, int oindex, int len) {
        char[] b = buf;
        char[] ob = other.buf;

        if (tindex < first || oindex < other.first ||
                tindex + len > last || oindex + len > other.last)
            return false;

        len += tindex;

        if (ignoreCase) {
            while (tindex < len)
                if (Character.toLowerCase(b[tindex++]) !=
                        Character.toLowerCase(ob[oindex++]))
                    return false;
        } else {
            while (tindex < len)
                if (b[tindex++] != ob[oindex++])
                    return false;
        }
        return true;
    }

    public CharString replace(char origChar, char newChar) {
        int f = first;
        int len = last - f;
        char[] b = buf;
        char[] b1 = new char[len];
        for (int i = 0; i < len; i++) {
            char c = b[i];
            b1[i] = (c == origChar ? newChar : c);
        }
        return new CharString(b1);
    }

    //---------- Extra! ----------

    public boolean startsWith(CharString other) {
        return regionMatches(false, 0, other, 0, other.length());
    }

    public boolean startsWith(CharString other, int off) {
        return regionMatches(false, off, other, 0, other.length());
    }

    public CharString substring(int off) {
        int f = first + off;
        if (f > last || off < 0)
            throw new IndexOutOfBoundsException(String.valueOf(off));
        return new CharString(buf, f, last);
    }

    public CharString substring(int begin, int end) {
        int e = first + end;
        if (e > last || begin < 0 || end < begin)
            throw new IndexOutOfBoundsException(begin + ":" + end);
        CharString res = new CharString(buf);
        res.first = first + begin;
        res.last = e;
        return res;
    }

    public char[] toCharArray() {
        int f = first;
        int len = last - f;
        char[] arr = new char[len];
        System.arraycopy(buf, f, arr, 0, len);
        return arr;
    }

    public CharString toLowerCase() {
        int f = first;
        int len = last - f;
        char[] arr = new char[len];
        char[] b = buf;
        for (int i = 0; i < len; i++)
            arr[i] = Character.toLowerCase(b[i + f]);
        return new CharString(arr);
    }

    public java.lang.String toString() {
        return new String(buf, first, last - first);
    }

    public CharString toUpperCase() {
        int f = first;
        int len = last - f;
        char[] arr = new char[len];
        char[] b = buf;
        for (int i = 0; i < len; i++)
            arr[i] = Character.toUpperCase(b[i + f]);
        return new CharString(arr);
    }

    //---------- static stuff ---------- 

    public CharString trim() {
        int f = first;
        int l = last - 1;
        char[] b = buf;

        while (f <= l && b[f] <= ' ')
            f++;

        while (f <= l && b[l] <= ' ')
            l--;

        return new CharString(b, f, l + 1);
    }

    public boolean matches(Regex regex) {
        return regex.matches(new String(buf, first, last - first));
    }

    public boolean matches(Regex regex, int off, int len) {
        if (first + len > last)
            throw new IndexOutOfBoundsException(String.valueOf(first + len));
        return regex.matches(new String(buf, first + off, len));
    }

    public boolean hasMatch(Regex regex) {
        return regex.searchOnce(new String(buf, first, last - first));
    }

    public boolean hasMatch(Regex regex, int off, int len) {
        if (first + len > last)
            throw new IndexOutOfBoundsException(String.valueOf(first + len));
        return regex.searchOnce(new String(buf, first + off, len));
    }

    public int intValue() {
        return Integer.parseInt(toString());
    }

    public long longValue() {
        return (Long.valueOf(toString())).longValue();
    }

    public double doubleValue() {
        return Double.parseDouble(toString());
    }

    public float floatValue() {
        return Float.parseFloat(toString());
    }
}
