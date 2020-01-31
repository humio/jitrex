/*
    Copyright (c) Peter Sorotokin, 1998-2000
    Copyright (c) Humio, 2019
    See file "LICENSE.md" for terms of usage and
    redistribution.   
*/
package com.humio.jitrex.parser;

import com.humio.jitrex.Pattern;
import com.humio.jitrex.tree.*;
import com.humio.jitrex.util.Regex;
import com.humio.util.jint.constants.MiniErrorCodes;
import com.humio.util.jint.util.CompilerException;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

/**
 * Regular expression parser.
 * <p>
 * TODO:
 * <ul>
 * <li>possibly add syntax flags (perl/sed/POSIX style, etc.)
 * </ul>
 */
public class RParser implements CharClassCodes, MiniErrorCodes {

    private RNode head;
    private RNode tail;
    private int varCounter;
    private Hashtable<String, RSubstNode> refList;
    private Hashtable<String, RPickNode> pickList;
    private int syntax;
    private int flags;
    private String varName;
    private RNode charClassNode;
    private RNode backslashNode;
    private boolean quoted = false;
    private int backslashTranslation;

    public RParser() {
    }

    public RParser(int syntax, int flags) {
        this.syntax = syntax;
        this.flags = flags;
    }


    private void append(RNode node) {
        if (node == null)
            return;

        if (head == null) {
            head = node;
            tail = node;
        } else
            tail.tail = node;

        while (tail.tail != null)
            tail = tail.tail;
    }

    public RNode parse(String regex) {
        return parse(regex.toCharArray());
    }

    public RNode parse(String regex, boolean noimplicit) {
        char[] arr = regex.toCharArray();
        return parse(arr, 0, arr.length, noimplicit);
    }

    public RNode parse(char[] regex) {
        return parse(regex, 0, regex.length, false);
    }

    public RNode parse(char[] regex, int offset, int length, boolean noimplicit) {
        int indexIn = offset + length;
        int index;
            index = parseImpl(regex, offset, indexIn);
        if (index < indexIn)
            throw new CompilerException(ERR_R_EXTRABRACKET, index);
        RNode res = head==null ? new REmptyNode(offset) : head;
        if (res != null)
            res.processFlags(flags);
        refList = new Hashtable<>();
        pickList = new Hashtable<>();
        if (res != null) {
            res.collectReferences(refList, pickList);
            res = res.markReferenced(refList, pickList, noimplicit);
            res.prepare(0, 0);
        }
        head = null;
        tail = null;
        varCounter = 0;
        return res;
    }

    public String[] getRefList() {
        int size = refList.size();
        if (size == 0)
            return null;
        Vector<String> acc = new Vector<>();
        Enumeration<RSubstNode> refs = refList.elements();
        for (int i = 0; i < size; i++) {
            RSubstNode sn = refs.nextElement();
            if (sn.picked || Character.isDigit(sn.var.charAt(0)))
                continue;
            acc.addElement(sn.var);
        }
        if (acc.size() == 0)
            return null;
        String[] res = new String[acc.size()];
        acc.copyInto(res);
        return res;
    }

    public String[] getPickList() {
        int size = pickList.size();
        if (size == 0)
            return null;
        String[] res = new String[size];
        Enumeration<String> picks = pickList.keys();
        for (int i = 0; i < size; i++)
            res[i] = picks.nextElement();
        return res;
    }

    public int parseImpl(char[] regex, int index, int maxIndex) {
        RNode prev = null;
        String vname = null;
        // Loop through all characters and process them
        // through the big switch statement. Subpatterns (inside
        // brackets) are processed recursively.
        outer: for (int i = index; i < maxIndex; i++) {
            char c = regex[i];
            int pos = i;

            if (quoted) {
                if (c == '\\' && i + 1 < maxIndex && regex[i + 1] == 'E') {
                    i += 1;
                    quoted = false;
                } else {
                    append(prev);
                    prev = new RConstNode(pos, c);
                }
                continue;
            }

            switch (c) {
                case '*':
                case '+':
                case '?': {
                    if (prev == null || lasttail_is_empty(prev)) {
                        Object[] addInfo = {"'" + c + "'"};
                        throw new CompilerException(ERR_R_BADSTART, index, addInfo);
                    }

                    boolean greedy;
                    if (i + 1 >= maxIndex) {
                        greedy = true;
                    } else {
                        if (regex[i+1] == '?') {
                            greedy = false;
                            i++;
                        } else if (regex[i+1] == '+') {
                            Object[] addInfo = {"posessive"};
                            throw new CompilerException(ERR_R_BADSTART, index, addInfo);
                        } else {
                            greedy = true;
                        }
                    }

                    if (c == '?')
                        if (greedy)
                            prev = new RAltNode(pos, prev, null);
                        else
                            prev = new RAltNode(pos, null, prev);
                    else {
                        boolean reallyGreedy = greedy && (this.flags & Regex.LAZY) == 0;
                        prev = new RRepeatNode(pos, (c == '*' ? 0 : 1), Integer.MAX_VALUE, prev, reallyGreedy);
                    }
                }
                continue;
                case '.':
                    append(prev);
                    prev = new RAnyNode(pos);
                    continue;
                case '{': {
                    int curl_at = i;
                    int j = ++i;
                    int comma = -1;
                    while (true) {
                        if (i >= maxIndex) {
                            i = curl_at;
                            append(prev);
                            prev = new RConstNode(pos, c);
                            continue outer;
//                            throw new CompilerException(ERR_R_NOCURLBRACKET, (j - 1));
                        }
                        c = regex[i];
                        if (c == '}')
                            break;
                        if (c == ',')
                            comma = i;
                        i++;
                    }
                    int min;
                    int max;
                    if (comma == -1) {
                        min = Integer.parseInt(new String(regex, j, i - j));
                        max = min;
                    } else {
                        min = Integer.parseInt(new String(regex, j, comma - j));
                        if (comma == i - 1)
                            max = Integer.MAX_VALUE;
                        else
                            max = Integer.parseInt(new String(regex, comma + 1, i - comma - 1));
                    }
                    boolean greedy = i + 1 >= maxIndex || regex[i + 1] != '?';
                    if (!greedy)
                        i++;
                    prev = new RRepeatNode(pos, min, max, prev, greedy);
                }
                break;
                case ')':
                    append(prev);
                    return i;
                    /*
                case '@': {
                    int start = i;
                    i = parseVariable(jitrex, i, maxIndex) + 1;
                    if (i >= maxIndex || jitrex[i] != '(')
                        throw new CompilerException(ERR_R_NOVAREXPR, start);
                    vname = varName;
                }
                */
                // fall through
                case '(': {
                    int start = i;
                    int lookAheadFlag = 0;
                    boolean keepFlagsAfterGroup = false;
                    append(prev);
                    RNode saveHead = head;
                    RNode saveTail = tail;
                    head = null;
                    tail = null;
                    String var;
                    int setflags = 0;
                    int clearflags = 0;
                    if (vname != null) {
                        var = vname;
                        vname = null;
                    } else {
                        // see if extended construction '(?...)'
                        if (i + 1 < maxIndex && regex[i + 1] == '?') {
                            // yes, this is an extended construction
                            i += 2;
                            if (i >= maxIndex)
                                throw new CompilerException(ERR_R_BQUNFINISHED, start);
                            switch (regex[i]) {
                                case ':':
                                    // non-saving grouping
                                    var = "";
                                    break;
                                case '=':
                                case '!':
                                    var = "";
                                    lookAheadFlag = regex[i];
                                    break;
                                case '<':
                                    int start2 = i;
                                    i = parseVariable(regex, i, maxIndex) + 1;
                                    if (i >= maxIndex || regex[i] != '>')
                                        throw new CompilerException(ERR_R_NOVAREXPR, start2);
                                    var = varName;
                                    // i += 1;
                                    break;

                                case 'i':
                                case 's':
                                case 'm':
                                case '-':
                                    boolean seenMinus = false;
                                    while (regex[i] != ':' && regex[i] != ')' && i < maxIndex) {
                                        switch(regex[i]) {
                                            case '-':
                                                if(seenMinus) {
                                                    throw new CompilerException(ERR_R_BQBAD, start);
                                                }
                                                seenMinus = true;
                                                break;

                                            case 'i':
                                                if (seenMinus) {
                                                    clearflags |= Pattern.CASE_INSENSITIVE;
                                                } else {
                                                    setflags |= Pattern.CASE_INSENSITIVE;
                                                }
                                                break;

                                            case 'm':
                                                if (seenMinus) {
                                                    clearflags |= Pattern.MULTILINE;
                                                } else {
                                                    setflags |= Pattern.MULTILINE;
                                                }
                                                break;

                                            case 's':
                                                if (seenMinus) {
                                                    clearflags |= Pattern.DOTALL;
                                                } else {
                                                    setflags |= Pattern.DOTALL;
                                                }
                                                break;

                                            default:
                                                throw new CompilerException(ERR_R_BQBAD, start);

                                        }
                                        i ++;
                                    }
                                    var = "";
                                    if (regex[i] == ')') {
                                        keepFlagsAfterGroup = true;
                                        i--;
                                    }
                                    break;

                                default:
                                    throw new CompilerException(ERR_R_BQBAD, start);
                            }
                        } else
                            var = Integer.toString(++varCounter);
                    }
                    i = parseImpl(regex, i + 1, maxIndex);
                    if (i >= maxIndex || regex[i] != ')')
                        throw new CompilerException(ERR_R_NOBRACKET, start);
                    if (head == null) {
                        append(new REmptyNode(pos));
                    }
                    if (lookAheadFlag != 0) {
                        prev = new RLookAheadNode(pos, head, lookAheadFlag == '=');
                    } else {
                        RPickNode end = new RPickNode(pos, var, false, setflags, clearflags, keepFlagsAfterGroup);
                        append(end);
                        RPickNode begin = new RPickNode(pos, var, true, setflags, clearflags, keepFlagsAfterGroup);
                        prev = begin;
                        end.start = begin;
                        prev.tail = head;
                    }
                    head = saveHead;
                    tail = saveTail;
                }
                continue;
                case '^':
/* don't support < > anchors
                case '<':
                case '>':
 */
                    append(prev);
                    prev = new RBoundaryNode(pos, c);
                    break;
                case '|': {
                    append(prev);
                    RNode alt1 = head;
                    head = null;
                    tail = null;
                    i = parseImpl(regex, i + 1, maxIndex) - 1;
                    prev = new RAltNode(pos, alt1, head);
                    head = null;
                    tail = null;
                }
                continue;
                case '$': {
                    append(prev);
//                    if (i + 1 >= maxIndex || (c = regex[i + 1]) == ')' || c == '|') {
                        prev = new RBoundaryNode(pos, '$');
                        continue;
//                    }
//                    i = parseVariable(regex, i, maxIndex);
//                    prev = new RSubstNode(pos, varName);
                }
//                continue;
                case '[':
                    append(prev);
                    i = parseCharClass(regex, i, maxIndex);
                    prev = charClassNode;
                    continue;
                case '\\': {
                    i = parseBackslash(regex, i, maxIndex, false);
                    if (backslashNode != null) {
                        append(prev);
                        prev = backslashNode;
                    }
                }
                continue;
                default: {
                    append(prev);
                    prev = new RConstNode(pos, c);
                }
                continue;
            }
        }
        append(prev);
        return maxIndex;
    }

    private boolean lasttail_is_empty(RNode prev) {
        if (prev == null)
            return true;

        if (prev.tail != null) {
            prev = prev.tail;
            while (prev.tail != null) {
                prev = prev.tail;
            }
        }

        return prev.is_empty_node();

    }

    private int parseVariable(char[] regex, int i, int maxIndex) {
        int start = ++i;
        if (i >= maxIndex)
            throw new CompilerException(ERR_R_NOVARNAME, (i - 1));
        char c = regex[i];
        if (c != '{') {
            while (true) {
                if (i >= maxIndex)
                    break;
                if (!Character.isLetterOrDigit(regex[i])
                        && regex[i] != '_'
                        && regex[i] != '-'
                        && regex[i] != '#'
                        && regex[i] != '@'
                        && regex[i] != '.'
                        && regex[i] != '['
                        && regex[i] != ']'
                )
                    break;
                i++;
            }
            varName = new String(regex, start, i - start);
        } else {
            do {
                c = regex[i++];
                if (i >= maxIndex)
                    throw new CompilerException(ERR_R_NOCURLBRACKET, start);
            }
            while (c != '}');
            varName = new String(regex, start + 1, i - start - 2);
        }
        return i - 1;
    }

    private int parseCharClass(char[] regex, int i, int maxIndex) {
        boolean neg = false;
        int pos = i;
        char c;
        int j = ++i;
        if (j < maxIndex && regex[j] == '^') {
            neg = true;
            j++;
            i++;
        }
        do {
            if (i < maxIndex && regex[i] == '\\')
            {
                i += 2;
            } else {
                i += 1;
            }

            if (i >= maxIndex)
                throw new CompilerException(ERR_R_NOSQBRACKET, j);

        } while (regex[i] != ']');
        char[] buf = new char[2 * (i - j)];
        ArrayList<CharSet> more_classes = new ArrayList<>();
        char ri = 0;
        while (j < i) {
            c = regex[j];
            if (c == '\\') {
                j = parseBackslash(regex, j, maxIndex, true) + 1;
                if (backslashNode instanceof RCharClassNode) {
                    RCharClassNode node = (RCharClassNode) backslashNode;
                    more_classes.add(node.charClass);
                    continue;
                }
                if (backslashTranslation < 0)
                    continue;
                c = (char) backslashTranslation;
            } else
                j++;
            buf[ri++] = c;
            if (regex[j] != '-') {
                buf[ri++] = c;
                continue;
            }
            j++;
            boolean translated = false;
            while (true) {
                c = regex[j];
                if (c == '\\') {
                    j = parseBackslash(regex, j, maxIndex, true) + 1;
                    if (backslashTranslation < 0)
                        continue;
                    c = (char) backslashTranslation;
                    translated = true;
                } else
                    j++;
                break;
            }
            if (c == ']' && !translated) {
                c = buf[ri - 1];
                buf[ri++] = c;
                buf[ri++] = '-';
                buf[ri++] = '-';
                break;
            }
            buf[ri++] = c;
        }
        if (ri < buf.length) {
            char[] newbuf = new char[ri];
            System.arraycopy(buf, 0, newbuf, 0, ri);
            buf = newbuf;
        }
        CharSet[] more = more_classes.toArray( new CharSet[ more_classes.size()] );
        charClassNode = new RCharClassNode(pos, neg, buf, more);
        return i;
    }

    private int parseBackslash(char[] regex, int i, int maxIndex, boolean inRange) {
        int pos = i;
        i++;
        if (i >= maxIndex)
            throw new CompilerException(ERR_R_STRAYBSLASH, i);
        char c = regex[i];
        backslashNode = null;
        backslashTranslation = -1;
        top: switch (c) {
            case '\n':
                return i;
            case 'c':
                if (++i >= maxIndex)
                    throw new CompilerException(ERR_R_CTLUNFINISHED, i);
                c = regex[i];
                if ('@' > c || '_' < c)
                    throw new CompilerException(ERR_R_CTLINVALID, i);
                c -= '@';
                break;
            case 'n':
                c = '\n';
                break;
            case 't':
                c = '\t';
                break;
            case 'r':
                c = '\r';
                break;
            case 'f':
                c = (char) 0x0c;
                break;
            case 'a':
                c = (char) 0x07;
                break;
            case 'v':
                c = (char) 0x0b;
                break;

            case 'p':
            case 'P': {
                boolean negate = c=='P';
                if (i+1 < maxIndex && regex[i+1] == '{') {
                    int start = i+2;
                    if (start < maxIndex && regex[start] == '^') {
                        negate = !negate;
                        start += 1;
                    }
                    for (int j = start; j < maxIndex; j++) {
                        if (regex[j] == '}') {
                            int ndigit = j-start;
                            String name = new String(regex, start, ndigit);
                            backslashNode = new RCharClassNode(pos, CharSet.decode(name, negate));
                            i = j;
                            break top;
                        }
                    }
                } else if (i+1 < maxIndex) {
                    String name = new String(regex, i+1, 1);
                    backslashNode = new RCharClassNode(pos, CharSet.decode(name, negate));
                    i += 1;
                    break top;
                }
                // TODO: make a new error for this
                throw new CompilerException(ERR_R_HEXBADNUMBER, i);
            }


            case 'x':
                if (i+1 < maxIndex && regex[i+1] == '{') {
                    int start = i+2;
                    for (int j = start; j < maxIndex; j++) {
                        if (regex[j] == '}') {
                            int ndigit = j-start;
                            String num = new String(regex, start, ndigit);
                            try {
                                c = (char) Integer.parseInt(num, 16);
                                i = j;
                                break top;
                            } catch (NumberFormatException e) {
                            }
                        }
                    }
                    throw new CompilerException(ERR_R_HEXBADNUMBER, i);
                }
            case 'u':
             {
                int ndigit = (c == 'x' ? 2 : 4);
                if (i + ndigit >= maxIndex)
                    throw new CompilerException(ERR_R_HEXUNFINISHED, i);
                String num = new String(regex, i + 1, ndigit);
                try {
                    c = (char) Integer.parseInt(num, 16);
                } catch (NumberFormatException e) {
                    Object[] addInfo = {Integer.toString(ndigit), num};
                    throw new CompilerException(ERR_R_HEXBADNUMBER, i);
                }
                i += ndigit;
            }
            break;
            case 'w':
            case 'W':
            case 's':
            case 'S':
            case 'd':
            case 'D':
                backslashNode = new RCharClassNode(pos, CharSet.decode(c));
                return i;
            case 'b':
            case 'B':
            case 'A':
            case 'Z':
            case 'z':
                if (!inRange) {
                    backslashNode = new RBoundaryNode(pos, c);
                    return i;
                } else if (c == 'b')
                    c = (char) 0x08; // backspace
                break;
            case 'Q':
                if (!inRange) {
                    quoted = true;
                    return i;
                }
            default:
                if ('0' <= c && c <= '9') {
                    if (c == '0')
                        i++; // backreference cannot start with 0
                    else {
                        if (!inRange) {
                            // see if backreference
                            int first = i; // first digit character
                            while (++i < maxIndex) {
                                char c1 = regex[i];
                                if ('0' > c1 || '9' < c1)
                                    break;
                            }
                            String decimalString = new String(regex, first, i - first);
                            int decimal = Integer.parseInt(decimalString);
                            if (decimal <= varCounter) {
                                // yes, it is backreference
                                backslashNode = new RSubstNode(pos, decimalString);
                                return i - 1;
                            } else
                                i = first;
                        }
                        if (c > '7') {
                            if (!inRange)
                                throw new CompilerException(ERR_R_BADBACKREF, i);
                            // \9 in character class would be just 9
                            break;
                        }
                    }
                    c = 0;
                    int j = 0;
                    while (i < maxIndex && j < 3) {
                        char chr = regex[i];
                        if ('0' <= chr && chr <= '7') {
                            c = (char) ((c << 3) + (chr - '0'));
                            i++;
                        } else
                            break;
                    }
                    i--;
                }
                break;
        }
        if (inRange)
            backslashTranslation = c;
        else
            backslashNode = new RConstNode(pos, c);
        return i;
    }

}
