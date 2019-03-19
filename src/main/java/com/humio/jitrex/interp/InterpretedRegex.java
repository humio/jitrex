/*
    Copyright (c) Peter Sorotokin, 1998-2000
    Copyright (c) Humio, 2019
    See file "LICENSE.md" for terms of usage and
    redistribution.   
*/
package com.humio.jitrex.interp;

import com.humio.jitrex.tree.CharClassCodes;
import com.humio.jitrex.util.Regex;
import com.humio.jitrex.util.RegexRefiller;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Stack;

class InterpretedRegex extends Regex implements RInterpCommands, CharClassCodes {
    int nCells;
    Hashtable<String,Variable[]> vars;
    byte[] code;
    int hints;
    int minLength;
    int maxLength;

    CharSequence string;
    byte[] bstring;
    int[] cells;
    int start;
    int end;
    Stack<Object> forks;
    int pc;
    int matchStart;
    int matchEnd;
    int headStart;
    int maxStart;

    CharSequence[] extCells;
    int extVarCells;

    int mfCounter = 0;

    RegexRefiller refiller;

    /**
     * Used to optimize HINT_CHAR_STAR_HEAD. How much headStart
     * can be incremented if matching fails the first attempt at
     * any position.
     */
    int headIncrement = 0;

    /**
     * Used to optimize HINT_CHAR_STAR_HEAD. Needed to calculate
     * headIncrement. It actually equals the minimum number of
     * repetitions for the first subpattern and -1 to indicate that
     * optimization should not be used.
     */
    int addHeadIncrement = 0;

    InterpretedRegex(byte[] code, Hashtable<String,Variable[]> vars, int varCells, int hints,
                     int minLength, int maxLength, int extVarCells) {
        this.code = code;
        this.nCells = varCells;
        this.vars = vars;
        this.hints = hints;
        this.minLength = minLength;
        this.maxLength = maxLength;
        this.extVarCells = extVarCells;
    }

    private static boolean inClass(char c, int charClass) {
        if (c <= 0x7F)
            return false;
        switch (charClass) {
            case CLASS_DISABLED:
            case CLASS_NONE:
                return false;
            case CLASS_ALL:
                return true;
            case CLASS_LETTER:
                return Character.isLetter(c);
            case CLASS_UPPERCASE:
                return Character.isUpperCase(c);
            case CLASS_NONLETTER:
                return !Character.isLetter(c);
            default:
                throw new RuntimeException("Invalid char class: " + charClass);
        }
    }

    private int readCode() {
        int c = code[pc++] & 0xFF;
        if (c <= 0x7F)
            return c;
        c &= 0x7F;
        if (c <= 0x3F)
            return (c << 8) | (code[pc++] & 0xFF);
        if (c != 0x7F)
            throw new RuntimeException("Invalid code encountered: " + c);
        c = code[pc++] << 24;
        c |= ((code[pc++] & 0xFF) << 16);
        c |= ((code[pc++] & 0xFF) << 8);
        c |= (code[pc++] & 0xFF);
        return c;
    }

    private int readLabel() {
        int c = ((code[pc++] & 0xFF) << 8);
        c |= (code[pc++] & 0xFF);
        return c;
    }

    private void fork(int label, int head) {
        Fork f = new Fork();
        f.fork = label;
        f.head = head;
        forks.push(f);
    }

    private void fork(int decrement, int label, int head) {
        MultiFork f = new MultiFork();
        f.decrement = decrement;
        f.counter = -1;
        f.fork = label;
        f.head = head;
        forks.push(f);
    }

    private int assign(int var, int val) {
        Restore r = new Restore();
        r.var = var;
        r.val = cells[var];
        forks.push(r);
        cells[var] = val;
        return val;
    }

    private int fail() {
        while (forks.size() > 0) {
            Object e = forks.pop();
            if (e instanceof Restore) {
                Restore r = (Restore) e;
                cells[r.var] = r.val;
            } else if (e instanceof Fork) {
                Fork f = (Fork) e;
                pc = f.fork;
                return f.head;
            } else {
                MultiFork mf = (MultiFork) e;
                if (mf.virgin) {
                    mf.virgin = false;
                    mf.counter = mfCounter;
                    if (addHeadIncrement >= 0)
                        headIncrement = mfCounter + addHeadIncrement + 1;
                } else
                    mf.counter--;
                if (mf.counter < 0)
                    continue;
                pc = mf.fork;
                forks.push(mf);
                return mf.head + mf.counter * mf.decrement;
            }
        }
        return -1;
    }

    private boolean refill() {
        if (refiller == null)
            return false;

        int end1 = refiller.refill(this, end);
        if (end1 <= end) {
            if (refiller == null)
                return false;
            headStart = Integer.MAX_VALUE;
            throw new AbortException();
        }
        end = end1;

        if ((hints & RInterpMachine.HINT_START_ANCHORED) != 0)
            maxStart = start;
        else
            maxStart = end - minLength;

        return true;
    }

    private boolean nextMatchInt() {
        int head = headStart;

        // System.out.println( "head " + head );

        matchStart = headStart;
        CharSequence str = string;
        char c = 0;
        boolean passed;
        int mfPC = 0;
        headIncrement = 1;
        addHeadIncrement = 0;

        int code;

        if (forks.size() == 0) {
            pc = 0;
            code = readCode();
        } else
            code = FAIL;

        while (head >= 0) {
            //System.out.println( "HEAD " + head + " Code " + Integer.toHexString(code) );
            switch (code) {
                case ASSIGN: {
                    int var = readCode();
                    int value = readCode();
                    assign(var, value);
                }
                break;
                case HARDASSIGN: {
                    int var = readCode();
                    int value = readCode();
                    cells[var] = value;
                }
                break;
                case PICK: {
                    int var = readCode();
                    assign(var, head);
                }
                break;
                case FORK:
                    fork(readLabel(), head);
                    break;
                case SKIP_NON_NEWLINE:
                    while (head >= end && refill())
                        ;
                    if (head >= end || str.charAt(head++) == '\n')
                        head = fail();
                    break;
                case SKIP:
                    while (head >= end && refill())
                        ;
                    if (head >= end)
                        head = fail();
                    head++;
                    break;
                case ASSERT_CHAR: {
                    int len = readCode();
                    while (head + len > end && refill())
                        ;
                    if (head + len > end)
                        head = fail();
                    else {
                        for (int i = 0; i < len; i++) {
                            //System.out.println( "Got " + str[head] );
                            if (str.charAt(head++) != readCode()) {
                                head = fail();
                                break;
                            }
                        }
                    }
                }
                break;
                case ASSERT_CLASS_RANGE:
                case ASSERT_CLASS: {
                    while (head >= end && refill())
                        ;
                    if (head >= end) {
                        head = fail();
                        break;
                    }
                    c = str.charAt(head++);
                    int charClass = readCode();
                    passed = inClass(c, charClass);
                    if (code == ASSERT_CLASS) {
                        if (!passed)
                            head = fail();
                        break;
                    }
                }
                // fall through
                case ASSERT_RANGE: {
                    if (code == ASSERT_RANGE) {
                        while (head >= end && refill())
                            ;
                        if (head >= end) {
                            head = fail();
                            break;
                        }
                        c = str.charAt(head++);
                        passed = false;
                    }
                    int len = readCode();
                    passed = false;
                    for (int i = 0; i < len; i++) {
                        int c1 = readCode();
                        int c2 = readCode();
                        if (c1 <= c && c <= c2)
                            passed = true;
                    }
                    if (!passed)
                        head = fail();
                }
                break;
                case ASSERT_EXT_VAR:
                case ASSERT_VAR: {
                    CharSequence otherBuf = (code == ASSERT_VAR ? str : extCells[readCode()]);
                    int var = readCode();
                    int vs = cells[var];
                    int ve = cells[var + 1];
                    while (end - head < ve - vs && refill())
                        ;
                    if (end - head < ve - vs)
                        head = fail();
                    else {
                        while (vs < ve) {
                            if (str.charAt(head++) != otherBuf.charAt(vs++)) {
                                head = fail();
                                break;
                            }
                        }
                    }
                }
                break;
                case BOUNDARY: {
                    int type = readCode();
                    switch (type) {
                        case '^':
                            passed = head == start;
                            break;
                        case '$':
                            while (refill())
                                ;
                            passed = head == end;
                            break;
                        default: {
                            while (head >= end && refill())
                                ;
                            char c1 = head > start ? str.charAt(head - 1) : ' ';
                            char c2 = head < end ? str.charAt(head) : ' ';
                            switch (type) {
                                case '>':
                                    passed = (Character.isLetterOrDigit(c1) || c1 == '_') &&
                                            !(Character.isLetterOrDigit(c2) || c2 == '_');
                                    break;
                                case '<':
                                    passed = !(Character.isLetterOrDigit(c1) || c1 == '_') &&
                                            (Character.isLetterOrDigit(c2) || c2 == '_');
                                    break;
                                case 'b':
                                case 'B':
                                    passed = (type == 'b') == ((Character.isLetterOrDigit(c1) || c1 == '_') !=
                                            (Character.isLetterOrDigit(c2) || c2 == '_'));
                                    break;
                                default:
                                    throw new RuntimeException("Invalid boundary class: " + type);
                            }
                        }
                    }
                    if (!passed)
                        head = fail();
                }
                break;
                case DECJUMP: {
                    int var = readCode();
                    int label = readLabel();
                    if (assign(var, cells[var] - 1) > 0)
                        pc = label;
                }
                break;
                case DECFAIL: {
                    int var = readCode();
                    if (assign(var, cells[var] - 1) < 0)
                        head = fail();
                }
                break;
                case JUMP:
                    pc = readLabel();
                    break;
                case FAIL:
                    head = fail();
                    break;
                case STOP:
                    matchEnd = head;
                    return true;

                // extensions:
                // EXT_MULTIFORK

                case MFSTART_HEAD: {
                    addHeadIncrement = readCode();
                    if (addHeadIncrement != -1 && headIncrement != 1)
                        throw new RuntimeException("Internal error: MFSTART_HEAD and not first run");
                }
                // fall through
                case MFSTART: {
                    if (code == MFSTART)
                        addHeadIncrement = -1;
                    int dec = readCode();
                    int label = readLabel();
                    mfCounter = 0;
                    fork(dec, label, head);
                    mfPC = pc;
                }
                break;
                case MFEND: {
                    mfCounter++;
                    pc = mfPC;
                }
                break;
                case MFENDLIMIT: {
                    int limit = readCode();
                    mfCounter++;
                    if (mfCounter >= limit) {
                        head = fail();
                        headIncrement = 1;
                    } else
                        pc = mfPC;
                }
                break;

                case JUMP_RANGE: {
                    while (head >= end && refill())
                        ;
                    int label = readLabel();
                    if (head >= end) {
                        pc = label;
                        break;
                    }
                    c = str.charAt(head);
                    int len = readCode();
                    passed = false;
                    for (int i = 0; i < len; i++) {
                        int c1 = readCode();
                        int c2 = readCode();
                        if (c1 <= c && c <= c2)
                            passed = true;
                    }
                    if (!passed)
                        pc = label;
                }
                break;
                case JUMP_MIN_LEFT: {
                    int label = readLabel();
                    int atLeast = readCode();
                    while (head + atLeast > end && refill())
                        ;
                    if (head + atLeast > end)
                        if (label == 0xFFFF)
                            head = fail();
                        else
                            pc = label;
                }
                break;
                case JUMP_MAX_LEFT: {
                    int label = readLabel();
                    int atMost = readCode();
                    if (head + atMost < end)
                        if (label == 0xFFFF)
                            head = fail();
                        else
                            pc = label;
                }
                break;
                case JUMP_CHAR: {
                    while (head >= end && refill())
                        ;
                    int label = readLabel();
                    char test = (char) readCode();
                    if (head >= end) {
                        pc = label;
                        break;
                    }
                    if (test != str.charAt(head))
                        pc = label;
                }
                break;

                case SHIFTTBL: {
                    while (head >= end && refill())
                        ;
                    int lookup = readCode() + head;
                    if (lookup >= end) {
                        headStart = lookup + 1;
                        return false;
                    }
                    char lookupChar = str.charAt(lookup);
                    int shifts = readCode();
                    int i;
                    for (i = 0; i < shifts; i++) {
                        int chr = readCode();
                        int shift = readCode();
                        if (chr == lookupChar) {
                            //System.out.println( "Shift is " + shift );
                            if (shift > 0) {
                                headStart += shift;
                                return false;
                            }
                            break;
                        }
                    }
                    if (i == shifts) {
                        //System.out.println( "Shift is " + (lookup+1) );
                        headStart = lookup + 1;
                        return false;
                    }
                    for (i++; i < shifts; i++) {
                        readCode();
                        readCode();
                    }
                }
                break;
                default:
                    throw new RuntimeException("Invalid instruction: " + code);
            }
            code = readCode();
        }

        headStart += headIncrement;

        return false;
    }

    public int getVariableHandle(String var, boolean begin) {
        return ((Variable[]) vars.get(var))[begin ? 0 : 1].cell;
    }

    public Enumeration<String> variables() {
        return vars.keys();
    }

    public int getExtVariableHandle(String var) {
        return ((Variable[]) vars.get(var))[0].extCell;
    }

    public void setExtVariableBuffer(int extHandle, CharSequence arr) {
        extCells[extHandle] = arr;
    }

    //---- jitrex

    public void init(CharSequence subject, int off, int len) {
        string = subject;
        start = off;
        end = off + len;
        cells = new int[nCells];
        for (int i = 0; i < nCells; i++)
            cells[i] = -1;

        forks = new Stack<>();

        if ((hints & RInterpMachine.HINT_END_ANCHORED) != 0) {
            headStart = end - maxLength;
            if (headStart < start)
                headStart = start;
        } else
            headStart = start;

        //System.out.println( "head start " + headStart );

        if ((hints & RInterpMachine.HINT_START_ANCHORED) != 0)
            maxStart = start;
        else
            maxStart = end - minLength;

        if (extVarCells > 0)
            extCells = new CharSequence[extVarCells];
    }

    public void init(byte[] arr, int off, int len) {
        bstring = arr;
        char[] carr = new char[off + len];
        for (int i = len + off - 1; i >= off; i--)
            carr[i] = (char) (arr[i] & 0xFF);
        init(new String(carr), off, len);
    }

    public CharSequence getCharBuffer(int extHandle) {
        if (extHandle < 0)
            return string;
        else
            return extCells[extHandle];
    }

    public byte[] getByteBuffer(int extHandle) {
        if (extHandle >= 0) {
            CharSequence cbuf = getCharBuffer(extHandle);
            byte[] buf = new byte[cbuf.length()];
            for (int i = start; i < cbuf.length(); i++)
                buf[i] = (byte) cbuf.charAt(i);
            return buf;
        }
        if (bstring == null) {
            bstring = new byte[end];
            for (int i = start; i < end; i++)
                bstring[i] = (byte) string.charAt(i);
        }
        return bstring;
    }

    public int getMatchStart() {
        return matchStart;
    }

    public int getMatchEnd() {
        return matchEnd;
    }

    public int getIndex(int handle) {
        return cells[handle];
    }

    public void setIndex(int handle, int value) {
        cells[handle] = value;
    }

    public void setRefiller(RegexRefiller refiller) {
        this.refiller = refiller;
    }

    public void setRefilledBuffer(CharSequence buffer) {
        string = buffer;
    }

    public boolean search(int from) {
        this.start = from;
        this.headStart = from;
        return search();
    }

    public boolean search() {
        try {
            while (true) {
                while (headStart > maxStart) {
                    if (refiller == null)
                        return false;
                    refill();
                }
                if (nextMatchInt()) {
                    forks.removeAllElements();
                    if (headStart < matchEnd)
                        headStart = matchEnd;
                    else
                        headStart++;
                    return true;
                }
            }
        } catch (AbortException e) {
        }
        return false;
    }

    public boolean searchAgain() {
        try {
            while (true) {
                while (headStart > maxStart) {
                    if (refiller == null)
                        return false;
                    refill();
                }
                if (nextMatchInt())
                    return true;
                headStart = matchStart + 1; // so when we fail next time, we start at an offset
            }
        } catch (AbortException e) {
        }
        return false;
    }

    public boolean matchWhole() {
        try {
            if (start <= end) {
                if (!refill())
                    return false;
            }
            while (nextMatchInt()) {
                if (matchEnd == end)
                    return true;
                if (forks.size() == 0) // no alternatives left
                    break;
            }
        } catch (AbortException e) {
        }
        return false;
    }

    public boolean match() {
        try {
            if (start <= end) {
                if (!refill())
                    return false;
            }
            return nextMatchInt();
        } catch (AbortException e) {
        }
        return false;
    }

    static class AbortException extends RuntimeException {
    }

    static class Fork {
        int fork;
        int head;
    }

    static class MultiFork {
        boolean virgin = true;
        int fork;
        int counter;
        int head;
        int decrement;
    }

    static class Restore {
        int var;
        int val;
    }

}
