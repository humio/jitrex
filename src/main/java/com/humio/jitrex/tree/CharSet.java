/*
    Copyright (c) Peter Sorotokin, 1998-2000
    Copyright (c) Humio, 2019
    See file "LICENSE.md" for terms of usage and
    redistribution.   
*/
package com.humio.jitrex.tree;

import com.humio.jitrex.re2.Unicode;
import com.humio.jitrex.re2.UnicodeTables;

import java.util.ArrayList;
import java.util.List;

public class CharSet implements CharClassCodes {
    static char[] smallRange = {(char) 0, (char) 0x7F};
    public static CharSet FULL_CHARSET = new CharSet(smallRange, CLASS_ALL);
    static char[] spaceRange = {' ', ' ', '\t', '\t', '\r', '\r', '\n', '\n'};
    public static CharSet SPACE_CHARSET = new CharSet(spaceRange, CLASS_DISABLED);
    static char[] identRange = {'0', '9', 'A', 'Z', 'a', 'z', '_', '_'};
    public static CharSet IDENT_CHARSET = new CharSet(identRange, CLASS_LETTER);
    static char[] wordRange = {'A', 'Z', 'a', 'z', '_', '_'};
    public static CharSet WORD_CHARSET = new CharSet(wordRange, CLASS_LETTER);
    static char[] digitRange = {'0', '9'};
    public static CharSet DIGIT_CHARSET = new CharSet(digitRange, CLASS_DISABLED);
    public static CharSet NONIDENT_CHARSET = IDENT_CHARSET.negate();
    public static CharSet NONSPACE_CHARSET = SPACE_CHARSET.negate();
    public static CharSet NONWORD_CHARSET = WORD_CHARSET.negate();
    public static CharSet NONDIGIT_CHARSET = DIGIT_CHARSET.negate();
    public int charClass;
    public char[] ranges;

    public CharSet(char[] ranges, int charClass) {
        try {
            this.charClass = charClass;
            this.ranges = normalize(ranges);
        } catch (RuntimeException e) {
            e.printStackTrace();
            throw e;
        }
    }

    public CharSet(char[] ranges) {
        this(ranges, CLASS_DISABLED);
    }

    public CharSet(char c) {
        ranges = new char[2];
        ranges[0] = c;
        ranges[1] = c;
        charClass = CLASS_DISABLED;
    }

    public CharSet(int charClass) {
        this(null, charClass);
    }

    public static char[] normalize(char[] range) {
        if (range == null)
            return null;

        // check if it is OK
        int k;
        for (k = 0; k < range.length; k += 2) {
            if (range[k] > range[k + 1])
                throw new IllegalArgumentException("Inversed range ["+k+"] = " + ((int)range[k]) + " > "+ ((int)range[k+1]));
            if (k + 2 < range.length && range[k + 1] >= range[k + 2] - 1)
                break;
        }
        if (k == range.length)
            return range;

        char[] r = new char[range.length];
        System.arraycopy(range, 0, r, 0, r.length);

        // sort it first
        for (int i = 0; i < r.length; i += 2)
            for (int j = i + 2; j < r.length; j += 2)
                if (r[i] > r[j]) {
                    char tmp = r[i];
                    r[i] = r[j];
                    r[j] = tmp;
                    tmp = r[i + 1];
                    r[i + 1] = r[j + 1];
                    r[j + 1] = tmp;
                }

        // remove overlaps
        int j = 0;
        int i = 0;
        while (i < r.length) {
            r[j++] = r[i++];
            char c = r[i++]; // end of this span
            while (i < r.length && c + 1 >= r[i]) {
                i++;
                c = (c > r[i] ? c : r[i]); // end of the next span
                i++;
            }
            r[j++] = c;
            //System.out.println( "Range: " + (int)r[j-2] + "-" + (int)r[j-1] );
        }

        if (j != r.length) {
            char[] newRange = new char[j];
            System.arraycopy(r, 0, newRange, 0, j);
            r = newRange;
        }

        return r;
    }

    public CharSet toLowerCase() {

        if (ranges.length == 0) {
            return this;
        } else if (charClass != CLASS_DISABLED) {

            switch (charClass) {
                case CLASS_ALL:
                    return this;
                case CLASS_LETTER:
                    return this;
                case CLASS_LOWERCASE:
                    return this;
                case CLASS_UPPERCASE:
                    return new CharSet(CLASS_LOWERCASE);
                case CLASS_NONLETTER:
                    return this;
                case CLASS_NONLOWERCASE:
                case CLASS_NONUPPERCASE:
                    return new CharSet(CLASS_NONLETTER);

                default:
                    throw new IllegalStateException("unknown charClass");
            }

        }

        List<char[]> out = new ArrayList<>();

        char[] range = new char[2];
        range[0] = range[1] = Character.toLowerCase(ranges[0]);
        for (int i = 0; i < ranges.length; i += 2) {
            for (char c = ranges[i]; c <= ranges[i+1]; c++) {
                char lc = Character.toLowerCase(c);
                if (lc == range[1]+1) {
                    range[1] = lc;
                    if (lc == 0xffff) {
                        out.add(range);
                        break;
                    }
                } else {
                    out.add(range);
                    range = new char[2];
                    range[0] = range[1] = lc;
                }
            }
        }
        out.add(range);

        char[] all = new char[out.size()*2];
        for (int i = 0; i < out.size(); i += 1) {
            all[i*2]   = out.get(i)[0];
            all[i*2+1] = out.get(i)[1];
        }

        return new CharSet(all, CLASS_DISABLED);
    }

    public static CharSet merge(CharSet set1, CharSet set2) {
        int charClass = set1.charClass | set2.charClass;
        if (charClass != CLASS_DISABLED)
            charClass &= ~CLASS_DISABLED;
        char[] ranges = merge(set1.ranges, set2.ranges);
        CharSet m = new CharSet(ranges, charClass);
        //System.out.println( "Merged " + set1 + " " + set2 + " -> " + m );
        return m;
    }

    public static char[] merge(char[] range1, char[] range2) {
        if (range1 == null)
            return range2;
        if (range2 == null)
            return range1;

        char[] range = new char[range1.length + range2.length];

        int i = 0;
        int i1 = 0;
        int i2 = 0;

        while (i1 < range1.length && i2 < range2.length) {
            int b;
            int e;
            char b1 = range1[i1];
            char e1 = range1[i1 + 1];
            char b2 = range2[i2];
            char e2 = range2[i2 + 1];
            if (b1 < b2) {
                b = b1;
                i1 += 2;
                if (e1 < b2)
                    e = e1;
                else {
                    e = (e1 > e2 ? e1 : e2);
                    i2 += 2;
                }
            } else {
                // b1 >= b2
                b = b2;
                i2 += 2;
                if (e2 < b1)
                    e = e2;
                else {
                    e = (e1 > e2 ? e1 : e2);
                    i1 += 2;
                }
            }
            range[i++] = (char) b;
            range[i++] = (char) e;
        }

        if (i1 < range1.length) {
            System.arraycopy(range1, i1, range, i, range1.length - i1);
            i += (range1.length - i1);
        } else if (i2 < range2.length) {
            System.arraycopy(range2, i2, range, i, range2.length - i2);
            i += (range2.length - i2);
        }

        if (i != range.length) {
            char[] newRange = new char[i];
            System.arraycopy(range, 0, newRange, 0, i);
            range = newRange;
        }

        return range;
    }

    static class Pair<A,B> {
        private final A first;
        private final B second;

        Pair(A a, B b) {
            this.first = a;
            this.second = b;
        }

        public static <X,Y> Pair<X,Y> of(X a, Y b) {
            return new Pair<>(a,b);
        }
    }

    public static CharSet decode(String name, boolean negate) {
        Pair<int[][], int[][]> pair = unicodeTable(name);
        if (pair == null) {
            throw new IllegalArgumentException("unknown unicode class "+name);
        }
        int[][] tab = pair.first;
        int[][] fold = pair.second; // fold-equivalent table

        char[] ranges = makeRange( tab, negate );
        return new CharSet(ranges);
    }

    private static char[] makeRange(int[][] tab, boolean negate) {
        List<Character> res;
        if (negate) {
            res = appendNegatedTable(tab);
        } else {
            res = appendTable(tab);
        }
        char[] out = new char[res.size()];
        for (int i = 0; i < out.length; i++) {
            out[i] = res.get(i);
        }
        return out;
    }



    // appendTable() appends the Unicode range table |table| to this CharClass.
    // Does not mutate |table|.
    static List<Character> appendTable(int[][] table) {
        List<Character> res = new ArrayList<>();
        for (int[] triple : table) {
            int lo = triple[0], hi = triple[1], stride = triple[2];
            if (stride == 1) {
                appendRange(res, lo, hi);
                continue;
            }
            for (int c = lo; c <= hi; c += stride) {
                appendRange(res, c, c);
            }
        }
        return res;
    }

    private static void appendRange(List<Character> res, int c, int c1) {

        if (c > 0xffff || c1 > 0xffff)
            return;

        res.add( (char)c );
        res.add( (char)c1 );
    }

    // appendNegatedTable() returns the result of appending the negation of range
    // table |table| to this CharClass.  Does not mutate |table|.
    static List<Character> appendNegatedTable(int[][] table) {
        List<Character> res = new ArrayList<>();
        int nextLo = 0; // lo end of next class to add
        for (int[] triple : table) {
            int lo = triple[0], hi = triple[1], stride = triple[2];
            if (stride == 1) {
                if (nextLo <= lo - 1) {
                    appendRange(res, nextLo, lo - 1);
                }
                nextLo = hi + 1;
                continue;
            }
            for (int c = lo; c <= hi; c += stride) {
                if (nextLo <= c - 1) {
                    appendRange(res, nextLo, c - 1);
                }
                nextLo = c + 1;
            }
        }
        if (nextLo <= Unicode.MAX_RUNE) {
            appendRange(res, nextLo, Unicode.MAX_RUNE);
        }
        return res;
    }

    static int[][] ANY_TABLE = { {0, Unicode.MAX_RUNE, 1} };

    private static Pair<int[][], int[][]> unicodeTable(String name) {
        // Special case: "Any" means any.
        if (name.equals("Any")) {
            return Pair.of(ANY_TABLE, ANY_TABLE);
        }
        int[][] table = UnicodeTables.CATEGORIES.get(name);
        if (table != null) {
            return Pair.of(table, UnicodeTables.FOLD_CATEGORIES.get(name));
        }
        table = UnicodeTables.SCRIPTS.get(name);
        if (table != null) {
            return Pair.of(table, UnicodeTables.FOLD_SCRIPT.get(name));
        }
        return null;
    }


    public static CharSet decode(char c) {
        switch (c) {
            case 'I':
                return NONIDENT_CHARSET;
            case 'i':
                return IDENT_CHARSET;
            case 'S':
                return NONSPACE_CHARSET;
            case 's':
                return SPACE_CHARSET;
            case 'w':
                return WORD_CHARSET;
            case 'W':
                return NONWORD_CHARSET;
            case 'd':
                return DIGIT_CHARSET;
            case 'D':
                return NONDIGIT_CHARSET;
            default:
                throw new IllegalArgumentException("Unknown char class code: " + c);
        }
    }

    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("CharSet[");
        if ((charClass & CLASS_UPPERCASE) != 0)
            sb.append("upper,");
        if ((charClass & CLASS_LOWERCASE) != 0)
            sb.append("lower,");
        if ((charClass & CLASS_NONLETTER) != 0)
            sb.append("non,");
        if (ranges != null) {
            for (int i = 0; i < ranges.length; i += 2) {
                char cf = ranges[i];
                char cl = ranges[i + 1];
                if (cf > ' ' && cf <= '~')
                    sb.append(cf);
                else
                    sb.append("\\u" + Integer.toHexString(cf));
                if (cf != cl) {
                    sb.append('-');
                    if (cl > ' ' && cl <= '~')
                        sb.append(cl);
                    else
                        sb.append("\\u" + Integer.toHexString(cl));
                }
            }
        }
        sb.append("]");
        return sb.toString();
    }

    public CharSet negate() {
        try {
            char[] in = ranges;
            char[] res;
            int resClass;
            char maxChar;
            if (charClass == CLASS_DISABLED) {
                maxChar = (char) 0xFFFF;
                resClass = CLASS_DISABLED;
            } else {
                maxChar = (char) 0x7F;
                resClass = (~charClass) & CLASS_ALL;
            }
            if (in != null && in.length > 0) {
                if (in[in.length - 1] > maxChar)
                    throw new RuntimeException("Negation of this set is not supported:" + this);
                int size = in.length + (in[0] == 0 ? -1 : 1) + (in[in.length - 1] == maxChar ? -1 : 1);
                if (size == 0)
                    res = null;
                else {
                    if ((size & 1) != 0)
                        throw new RuntimeException("Assertion failed: odd size: " + size);
                    res = new char[size];
                    int i = 0;
                    int j = 0;
                    if (in[i] != 0)
                        res[j++] = 0;
                    else
                        i++;
                    while (i < in.length)
                        if ((i & 1) == 0) {
                            // start of original range - end of new range
                            res[j++] = (char) (in[i++] - 1);
                        } else {
                            // end of original range - start of new range
                            if (in[i] != maxChar)
                                res[j++] = (char) (in[i++] + 1);
                        }
                    if (j < size)
                        res[j++] = maxChar;
                    if (j != size)
                        throw new RuntimeException("Internal error: assertion failed for: " + this);
                }
            } else {
                res = new char[2];
                res[0] = 0;
                res[1] = maxChar;
            }
            return new CharSet(res, resClass);
        } catch (RuntimeException e) {
            e.printStackTrace();
            throw e;
        }
    }

    //------ merging and normalizing

    public CharSet merge(CharSet other) {
        return merge(this, other);
    }

    public int complexity() {
        return (ranges == null ? 0 : ranges.length) +
                (charClass == CLASS_ALL || charClass == CLASS_NONE || charClass == CLASS_DISABLED ? 0 : 10);
    }

    public boolean isSingleChar() {
        return (charClass == CLASS_NONE || charClass == CLASS_DISABLED) && ranges.length == 2 &&
                ranges[0] == ranges[1];
    }
}
