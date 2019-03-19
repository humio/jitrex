/*
    Copyright (c) Humio, 2019
    See file "LICENSE.md" for terms of usage and
    redistribution.
*/
package com.humio.jitrex;

import com.humio.jitrex.util.Regex;

import java.util.*;

public class Matcher {
    private final Pattern pattern;
    protected CharSequence input;
    private final Regex re;
    private final Map<String, Pattern.VarEntry> varMap;
    private final Map<String, Pattern.VarEntry> exts;
    private final Pattern.VarEntry[] indexed;

    int lastEnd = 0;

    public Matcher(Pattern p, CharSequence input) {

        if (input == null || p == null) {
            throw new NullPointerException();
        }

        this.pattern = p;
        this.input = input;
        this.re = null;
        this.varMap = new HashMap<>();
        this.exts = new HashMap<>();
        this.indexed = new Pattern.VarEntry[0];
    }

    public Matcher(Pattern pattern, CharSequence input, Regex re, Map<String, Pattern.VarEntry> varMap, Map<String,Pattern.VarEntry> exts) {

        this.pattern = pattern;
        this.input = input;
        this.re = re;
        this.varMap = varMap;
        this.exts = exts;

        indexed = varMap.values().toArray(new Pattern.VarEntry[varMap.size()]);
        Arrays.sort(indexed, new Comparator<Pattern.VarEntry>() {
            @Override
            public int compare(Pattern.VarEntry o1, Pattern.VarEntry o2) {
                return o1.start - o2.start;
            }
        });

        reset( input );
    }

    private static Map<String,String> EMPTY_MAP = new HashMap<>();

    public void reset() {
        reset(input, EMPTY_MAP);
    }

    public void reset(Map<String,String> vars) {
        reset(input, vars);
    }

    public void reset(CharSequence seq) {
        reset(seq, EMPTY_MAP);
    }

    public void reset(CharSequence seq, Map<String,String> vars) {
        this.input = seq;
        this.lastEnd = 0;
        for (String key : vars.keySet()
             ) {
            assign(key, vars.get(key).toLowerCase());
        }

        re.init(seq, 0, seq.length());

        from = 0;
        to = seq.length();
        lastAppend = 0;
        first = -1;
        last = 0;
    }


    public Set<String> groups() { return varMap.keySet(); }
    public Set<String> variables() { return exts.keySet(); }

    boolean matches = false;

    int first = -1, last = 0;

    int from, to;

    int lastAppend = 0;

    private boolean aftermatch(boolean success) {
        matches = success;
        if (success) {
            first = re.getMatchStart();
            last = re.getMatchEnd();
        } else {
            first = -1;
        }

        return success;
    }

    public boolean matches() {
        lastEnd = 0;
        return aftermatch( re.matchesWhole(input) );
    }



    public boolean find() {

        lastEnd = last;

        boolean success = aftermatch( re.search() );

        // for some reason, JINT jitrex may match the empty string at
        // the end of the previous match.
        if (success && last == lastEnd && last != 0) {
            success = aftermatch( re.search() );
        }

        return success;
    }

    public boolean find(int start) {

        if (matches) {
            lastEnd = last;
        }
        int limit = input.length();
        if ((start < 0) || (start > limit))
            throw new IndexOutOfBoundsException("Illegal start index");
        reset();
        return aftermatch( re.search(start) );
    }

    public boolean lookingAt() {
        return aftermatch( re.match() );
    }

    public int start() {
        if (matches == false) {
            throw new IllegalStateException();
        }
        return first;
    }

    public int end() {
        if (matches == false) {
            throw new IllegalStateException();
        }
        return last;
    }

    public int groupCount() {
        return  varMap.size();
    }

    public String group() {
        return group(0);
    }

    public String group(int groupNo) {
        if (!matches)
            throw new IllegalStateException();
        if (groupNo > groupCount())
            throw new IndexOutOfBoundsException();
        int start = start(groupNo);
        if (start == -1)
            return null;
        int end = end(groupNo);
        return input.subSequence(start, end).toString();
    }

    public int start(int groupNo) {
        if (matches == false) {
            throw new IllegalStateException();
        }
        if (groupNo == 0) {
            return start();
        } else {
            return re.getIndex(indexed[groupNo - 1].start);
        }
    }

    public int end(int groupNo) {
        if (matches == false) {
            throw new IllegalStateException();
        }
        if (groupNo == 0) {
            return end();
        } else {
            return re.getIndex(indexed[groupNo - 1].end);
        }
    }

    public String group(String name) {
        Pattern.VarEntry ent = varMap.get(name);
        if (ent == null) {
            return null; // TODO?
        }

        int start = re.getIndex(ent.start);;
        int end = re.getIndex(ent.end);;
        return input.subSequence(start, end).toString();
    }

    private void assign(String variableName, String value) {
        Pattern.VarEntry idx = exts.get(variableName);
        if (idx != null) {
            re.setExtVariable(idx.ext, idx.start, idx.end, value);
        }
    }

    public void appendReplacement(StringBuilder sb, String replacement) {
        appendReplacement(sb, compile(replacement) );
    }


    void appendReplacement(StringBuilder sb, List<Appender> appender) {

        sb.append( input, lastEnd, start());

        for (Appender a : appender) {
            a.append(sb);
        }

        lastAppend = last;
    }

    public void appendTail(StringBuilder sb) {
        sb.append( input, lastAppend, input.length() );
    }


    public void appendReplacement(StringBuffer sb, String replacement) {
        appendReplacement(sb, compile(replacement)) ;
    }


    void appendReplacement(StringBuffer sb, List<Appender> appender) {

        sb.append( input, lastEnd, start());

        for (Appender a : appender) {
            a.append(sb);
        }

        lastAppend = last;
    }

    public void appendTail(StringBuffer sb) {
        sb.append( input, lastAppend, input.length() );
    }



    public String replaceAll(String replacement) {

        List<Appender> compiled = null;

        StringBuilder sb = new StringBuilder();
        while (find()) {
            if (compiled == null) {
                compiled = compile(replacement);
            }
            appendReplacement(sb, compiled);
        }
        appendTail(sb);

        return sb.toString();
    }

    public String replaceFirst(String replacement) {
        StringBuilder sb = new StringBuilder();
        if (find()) {
            appendReplacement(sb, compile(replacement));
        }
        appendTail(sb);

        return sb.toString();
    }

    private List<Appender> compile(String repl) {
        List<Appender> res = new ArrayList<>();

        int length = repl.length();
        for (int i = 0; i < length; i++) {
            switch (repl.charAt(i)) {
                case '$':

                    int end = i+1;
                    while (end < length && Character.isDigit(repl.charAt(end))) {
                        end += 1;
                    }

                    if (end == i+1) {
                        throw new IllegalArgumentException("not a number after $");
                    }

                    int groupNo = Integer.parseInt( repl.substring(i+1, end) );

                    if (groupNo > groupCount()) {
                        throw new IndexOutOfBoundsException();
                    }

                    res.add( new GroupAppender(groupNo));
                    i = end-1;
                    break;

                default:

                    StringBuffer o = new StringBuffer();
                    char c;

                    int start = i;
                    while (i < length && (c = repl.charAt(i)) != '$') {
                        if (c == '\\' && i+1 < length) {
                            o.append( repl.charAt( i + 1));
                            i += 2;
                        } else {
                            o.append(repl.charAt(i));
                            i += 1;
                        }
                    }

                    if (start != i) {
                        res.add(new TextAppender(o.toString()));
                    }

                    if (i != length) {
                        i -= 1;
                    }
            }
        }

        return res;
    }

    abstract static class Appender {
        abstract void append(StringBuffer sb);
        abstract void append(StringBuilder sb);
    }

    static class TextAppender extends Appender {
        String literal;
        TextAppender(String lit) { this.literal = lit; }
        void append(StringBuilder sb) {
            sb.append(literal);
        }
        void append(StringBuffer sb) {
            sb.append(literal);
        }
    }

     class GroupAppender extends Appender {
        int groupNo;
        GroupAppender(int no) { this.groupNo = no; }
         void append(StringBuilder sb) {
             if (groupNo > groupCount())
                 return;
             String g = group(groupNo);
             if (g != null) {
                 sb.append(g);
             }
         }
         void append(StringBuffer sb) {
             if (groupNo > groupCount())
                 return;
             String g = group(groupNo);
             if (g != null) {
                 sb.append(g);
             }
         }
    }



}