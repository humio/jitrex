/*
    Copyright (c) Humio, 2019
    See file "LICENSE.md" for terms of usage and
    redistribution.
*/
package com.humio.jitrex;

import com.humio.jitrex.compiler.RCompiler;
import com.humio.jitrex.jvm.RJavaClassMachine;
import com.humio.jitrex.parser.RParser;
import com.humio.jitrex.tree.RNode;
import com.humio.jitrex.util.Regex;

import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.*;

public final class Pattern implements Serializable {

    public static final int CASE_INSENSITIVE = Regex.CASE_INSENSITIVE;
    public static final int MULTILINE = Regex.MULTILINE;
    public static final int DOTALL = Regex.DOTALL;

    private final Regex regex;
    private final String pattern;
    private final Map<String, VarEntry> varMap;
    private final Map<String, VarEntry> exts;
    private final int flags;

    public static String quote(String s) {
        return "\\Q" + s + "\\E";
    }

    public String toString() { return pattern(); }

    public int hashCode() { return pattern().hashCode() + flags; }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Pattern) {
            Pattern other = (Pattern) obj;
            if (!other.pattern().equals(pattern()))
                return false;
            return other.flags() == flags();
        }
        return false;
    }

    public int flags() {
        return flags;
    }

    public String replaceFirst(String source, String replacement) {
        return matcher(source).replaceFirst(replacement);
    }

    public String replaceAll(String source, String replacement) {
        return matcher(source).replaceAll(replacement);
    }

    public List<String> findAll(String input, int max) {
        Matcher m = matcher(input);
        List<String> res = new ArrayList<>();
        while ((max == -1 || max-- > 0) && m.find()) {
            res.add( m.group() );
        }
        if (res.size() == 0)
            return null;

        return res;
    }

    public String[] split(CharSequence text, int limit) {
        List<String> res = new ArrayList<>();
        Matcher m = matcher(text);
        int pos = 0;
        while((limit==0 || res.size() != limit-1) && m.find()) {
            String str = text.subSequence(pos, m.start()).toString();
            res.add(str);
            pos = m.end();
        }

        String str = text.subSequence(pos, text.length()).toString();
        res.add(str);

        if (limit == 0) {
            // remove trailing ""s
            while (res.size() > 0 && res.get(res.size()-1).length() == 0) {
                res.remove(res.size()-1);
            }
        }

        return res.toArray(new String[res.size()]);
    }

    public String[] split(CharSequence text) {
        return split(text, 0);
    }

    public String find(CharSequence text) {
        Matcher m = matcher(text);
        if (m.find()) {
            return m.group();
        } else {
            return "";
        }
    }

    static class VarEntry {
         final String name;
         final int start;
         final int end;
         final int ext;

        VarEntry(String name, int start, int end, int ext) {
            this.name = name;
            this.start = start;
            this.end = end;
            this.ext = ext;
        }

        @Override
        public int hashCode() {
            return name.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof VarEntry) {
                VarEntry ve = (VarEntry) obj;

                return start == ve.start && end == ve.end && ext == ve.ext && name.equals(ve.name);
            }

            return false;
        }
    }


    public static Pattern compile(String pattern) {
        return compile(pattern, 0);
    }

    public static Pattern compile(String pattern, int flags) {

        /*
        if (pattern.length() == 0) {
            return empty(flags);
        }
        */

        RNode regex = (new RParser(0, flags)).parse(pattern, false);
        RJavaClassMachine machine = new RJavaClassMachine();

        // machine.setSaveBytecode(true);
        machine.setNoRefiller(true);
        RCompiler comp = new RCompiler(machine);
        comp.compile(regex, pattern);
        Regex re = machine.makeRegex();

        int numCaptures = machine.getNVars();

        Map<String,VarEntry> varMap = new HashMap<>(numCaptures);
        Map<String,VarEntry> extMap = new HashMap<>(numCaptures);
        Enumeration<String> vars = machine.variables();
        while (vars.hasMoreElements()) {
            String var = vars.nextElement();
            int start = machine.getVariableHandle(var, true);
            int end   = machine.getVariableHandle(var, false);
            int ext   = machine.getExtVariableHandle(var);

            if (ext == -1) {
                varMap.put(var, new VarEntry(var, start, end, ext));
            } else {
                extMap.put(var, new VarEntry(var, start, end, ext));
            }
        }

        return new Pattern(re, pattern, flags, varMap, extMap);
    }


    /*
    private static Pattern empty(int flags) {
            return new Pattern(null, "", flags, Map.of(), Map.of()) {
                @Override
                public Matcher matcher(CharSequence input) {

                    return Matcher.empty(this, input);
                }
            };
        }
*/

    private Pattern(Regex re, String pattern, int flags, Map<String,VarEntry> vars, Map<String,VarEntry> exts) {
        this.regex = re;
        this.pattern = pattern;
        this.varMap = vars;
        this.exts = exts;
        this.flags = flags;
    }

    public String pattern() {
        return pattern;
    }

    public Set<String> groups() { return varMap.keySet(); }
    public Set<String> variables() { return exts.keySet(); }

    public Matcher matcher(CharSequence input) {
        return new Matcher(this, input, regex.cloneRegex(), varMap, exts);
    }

    public static boolean matches(String regex, CharSequence input) {
        return compile(regex).matcher(input).matches();
    }

    public boolean matches(CharSequence input) {
        return matcher(input).matches();
    }

    public int groupCount() {
        return  varMap.size();
    }

    protected Object writeReplace() throws ObjectStreamException {
        return new PatternState(pattern, flags);
    }
}

class PatternState implements Serializable {
    private final String pattern;
    private final int flags;

    PatternState(String pattern, int flags) {

        this.pattern = pattern;
        this.flags = flags;
    }

    Object readResolve() throws ObjectStreamException {
        return Pattern.compile(pattern, flags);
    }
}