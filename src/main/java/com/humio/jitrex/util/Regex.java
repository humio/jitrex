/*
    Copyright (c) Peter Sorotokin, 1998-2000
    See file "LICENSE.md" for terms of usage and
    redistribution.   
*/
package com.humio.jitrex.util;

import com.humio.util.jint.lang.CharString;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Enumeration;

/**
 * Regular expression interface definition and convenience methods.
 * Interface focuses on efficiency and convenience methods can be used when
 * simplicity is more important (most of the times).
 * <p>
 * Regex can be used either for <i>search</i> or for <i>matching</i>.
 * Matching determines if string maches jitrex exactly and search determines
 * if there is a substring of the given string that matches the jitrex.
 * There is matchWhole and match methods; matchWhole is "pure" match,
 * it tries to match the whole string; match tries to match beginning of
 * the string.
 * <p>
 * Regex can contain embedded variables of two types. External variables
 * are referenced with dollar sign ($), but not 'at' sign (@). They must be
 * assigned prior to search or matching. Internal variables are either
 * implicit (1, 2, etc) - those refer to unnamed subexpressions in brackets
 * (for example jitrex (.)(.)=\1\2 contains two implicit variables: 1 and 2), or
 * explicit - those are given names (with '@' constructions). Internal variables
 * may be assigned values in the process of matching and their values can be
 * obtained from the jitrex that was used for search or matching.
 * <p>
 * Regex contains some "working space" for the matching process. Therefore
 * between the match/search call and till all information about match/search is
 * obtained jitrex should be used only by a single thread. Use clone() or
 * cloneRegex() to get a separate copy of it.
 * <p>
 * All 'end' or 'final' substring positions or indices in the buffer refer to the first
 * character <b>after</b> substring.
 */
public abstract class Regex implements Cloneable, FilenameFilter {
    private static RegexFactory factory;

    public static final int CASE_INSENSITIVE = 0x01;
    public static final int MULTILINE = 0x02;
    public static final int DOTALL = 0x04;
    public static final int LAZY = 0x08;

    public static final int _OLD_LONG_STRING_HANDLING = 0x10;
    public static final int _SAVE_BYTECODE = 0x20;

    public static Regex createRegex(String re) {
        if (factory == null)
            initFactory();
        return factory.createRegex(re);
    }

    public static Regex createRegex(String re, boolean ignoreCase) {
        if (factory == null)
            initFactory();
        return factory.createRegex(re, ignoreCase);
    }

    public static Regex createFilePattern(String re) {
        if (factory == null)
            initFactory();
        return factory.createFilePattern(re);
    }

    public static Regex createRegex(char[] arr, int off, int len) {
        if (factory == null)
            initFactory();
        return factory.createRegex(arr, off, len, false, false);
    }

    protected static Regex createLowerCaseRegex(String re) {
        if (factory == null)
            initFactory();
        return factory.createLowerCaseRegex(re);
    }

    public static synchronized void setFactory(RegexFactory _factory) {
        factory = _factory;
    }

    private synchronized static void initFactory() {
        if (factory != null)
            return;
        String regexFactoryClass = null;
        String regexFactoryClass1 = null;
        try {
            regexFactoryClass = System.getProperty("kmy.jitrex.factory");
        } catch (Exception e) {
        }
        if (regexFactoryClass == null) {
            regexFactoryClass =  "kmy.jitrex.jvm.JavaClassRegexFactory";
            regexFactoryClass1 = "kmy.jitrex.interp.InterpRegexFactory";
        }
        Class<RegexFactory> factoryClass;
        try {
            factoryClass = (Class<RegexFactory>) Class.forName(regexFactoryClass);
        } catch (ClassNotFoundException e) {
            factoryClass = null;
            if (regexFactoryClass1 != null)
                try {
                    factoryClass = (Class<RegexFactory>) Class.forName(regexFactoryClass1);
                } catch (Exception e1) {
                }
            if (factoryClass == null)
                throw new IllegalArgumentException(
                        "Cannot create default RegexFactory: class not found");
        }
        try {
            factory = factoryClass.getConstructor().newInstance();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Cannot instantiate default RegexFactory: " + e.getMessage());
        }
    }

    /**
     * Returns variable index handle that can be used later to get or set
     * variable value efficiently. If efficiency is not a concern, use
     * get and set methods instead. Every variable has two index handles -
     * 'begin' and 'end' index handle.
     */
    abstract public int getVariableHandle(String var, boolean begin);

    /**
     * Returns external variable buffer handle that can be used later to get or set
     * variable value efficiently. If efficiency is not a concern, use
     * get and set methods instead. Only external variables have buffer
     * handle. This is different from index handles. For internal variables
     * this method returs -1.
     */
    abstract public int getExtVariableHandle(String var);

    /**
     * Enumerates all external and internal variables in this jitrex.
     */
    abstract public Enumeration<String> variables();

    /**
     * Prepares to match or search. Sets up an input buffer and a range of characters in it
     * that is to be searched or matched.
     */
    abstract public void init(CharSequence subject, int off, int len);

    /**
     * Search the buffer (that was set up by init method) for a substring that matches
     * this jitrex. See description of this class for difference between search and match.
     * On success do not discard record of all other possible matches, so if this method
     * is called again, different way of matching can be found for already found substring
     * or a substring overlapping already found substring. Most often, search(),
     * not searchAgain() should be used.
     *
     * @returns value indicating if matching substring is found.
     */
    abstract public boolean searchAgain();

    /**
     * Search the buffer (that was set up by init method) for a substring that matches
     * this jitrex. See description of this class for difference between search and match.
     * All records for other possible matches withing matching substring that was found
     * are destroyed, so if this method is called again, search will start at the first
     * position after previously found substring.
     *
     * @returns value indicating if matching substring is found.
     */
    abstract public boolean search();
    abstract public boolean search(int from);

    /**
     * Match the buffer (that was set up by init method) against
     * this jitrex. See description of this class for difference between search/match/matchWhole.
     *
     * @returns value indicating if matching was successful.
     */
    abstract public boolean matchWhole();

    /**
     * Match the beginning of the buffer (that was set up by init method) against
     * this jitrex. See description of this class for difference between search/match/matchWhole.
     *
     * @returns value indicating if matching was successful.
     */
    abstract public boolean match();

    /**
     * Returns external or internal variable index using its index handle. Every variable
     * has two indices - 'begin' and 'end'. Use 'begin' and 'end' handles to get them.
     * These indices can be used to determine which part of the input buffer (set by init method)
     * corresponding internal variable matched. An external variable 'begin' and 'end' indices
     * point to its own buffer. They are assinged by setIndex and are never changed.
     */
    abstract public int getIndex(int handle);

    //--------- convenience methods ----------------

    /**
     * Returns internal variable index using its index handle. Every variable
     * has two indices - 'begin' and 'end'. Use 'begin' and 'end' handles to assign them.
     * These indices are needed to completely specify value of an external variable.
     */
    abstract public void setIndex(int handle, int value);

    /**
     * Returns external variable buffer by its buffer handle. If -1 is passed as a handle
     * the input buffer that was set up for matching (using init) is returned.
     */
    abstract public CharSequence getCharBuffer(int extHandle);

    /**
     * After successiful search returns matching substring's initial position in
     * the input buffer (that was set up by init method).
     */
    abstract public int getMatchStart();

    /**
     * After successiful search returns matching substring's final position in
     * the input buffer (that was set up by init method).
     */
    abstract public int getMatchEnd();

    /**
     * Assignes external variable buffer by its buffer handle.
     * It is needed to completely specify value of an external variable.
     */
    abstract public void setExtVariableBuffer(int extHandle, CharSequence arr);

    abstract public void setRefiller(RegexRefiller refiller);

    /**
     * Refiller can call this method after refilling the buffer,
     * if buffer has been reallocated.
     */
    abstract public void setRefilledBuffer(CharSequence buffer);

    public boolean matches(CharSequence arr, int off, int len) {
        init(arr, off, len);
        return match();
    }

    public boolean matches(CharSequence s) {
        init(s, 0, s.length());
        return match();
    }

    public boolean matchesWhole(CharSequence arr, int off, int len) {
        init(arr, off, len);
        return matchWhole();
    }

    public boolean matchesWhole(CharSequence s) {
        init(s, 0, s.length());
        return matchWhole();
    }

    public boolean searchOnce(CharSequence arr, int off, int len) {
        init(arr, off, len);
        return search();
    }

    public boolean searchOnce(String s) {
        init(s, 0, s.length());
        return search();
    }

    public boolean searchOnce(CharString s) {
        String ss = new String(s.buf, s.first, s.last - s.first);
        init(ss, 0, ss.length());
        return search();
    }

    public boolean searchOnce(Object obj) {
        if (obj instanceof CharString) {
            CharString s = (CharString) obj;
            return searchOnce(s);
        } else if (obj instanceof char[]) {
            char[] s = (char[]) obj;
            return searchOnce(new String(s));
        } else if (obj instanceof CharSequence) {
            return searchOnce((CharSequence)obj);
        } else {
            String s = obj.toString();
            return searchOnce(s);
        }
    }

    public void setExtVariable(int extHandle, int beginHandle, int endHandle, String v) {
        setExtVariableBuffer(extHandle, v);
        setIndex(beginHandle, 0);
        setIndex(endHandle, v.length());
    }

    public void setExtVariable(int extHandle, int beginHandle, int endHandle, CharString v) {
        setExtVariableBuffer(extHandle, new String(v.buf));
        setIndex(beginHandle, v.first);
        setIndex(endHandle, v.last);
    }

    public void setExtVariable(int extHandle, int beginHandle, int endHandle, char[] carr) {
        setExtVariableBuffer(extHandle, new String(carr));
        setIndex(beginHandle, 0);
        setIndex(endHandle, carr.length);
    }

    public void setExtVariable(int extHandle, int beginHandle, int endHandle, Object v) {
        if (v instanceof String)
            setExtVariable(extHandle, beginHandle, endHandle, (String) v);
        else if (v instanceof CharString)
            setExtVariable(extHandle, beginHandle, endHandle, (CharString) v);
        else if (v instanceof char[])
            setExtVariable(extHandle, beginHandle, endHandle, (char[]) v);
        else
            setExtVariable(extHandle, beginHandle, endHandle, v.toString());
    }

    public CharSequence getMatch() {
        CharSequence cbuf = getCharBuffer(-1);
        int start = getMatchStart();
        return cbuf.subSequence(start, getMatchEnd());
    }

    public String getMatchString() {
        return getMatch().toString();
    }

    public String get(String var) {
        int begin = getIndex(getVariableHandle(var, true));
        int end = getIndex(getVariableHandle(var, false));
        if (begin < 0 || end < 0 || end < begin)
            return null;
        int ext = getExtVariableHandle(var);
        return getCharBuffer(ext).toString();
    }

    public void set(String var, String val) {
        int ext = getExtVariableHandle(var);
        if (ext < 0)
            throw new IllegalArgumentException("No external variable " + var);
        int begin = getVariableHandle(var, true);
        int end = getVariableHandle(var, false);
        setExtVariableBuffer(ext, val);
        setIndex(begin, 0);
        setIndex(end, val.length());
    }

    public Regex cloneRegex() {
        try {
            return (Regex) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException("Internal error: " + e);
        }
    }

    public void init(char[] arr) {
        init(new String(arr), 0, arr.length);
    }

    public void init(String s) {
        init(s, 0, s.length());
    }

    public Object clone() {
        return cloneRegex();
    }

    public boolean accept(File dir, String name) {
        return matchesWhole(name);
    }

    public abstract void setBackTrackLimit(int i);
    public abstract int getBackTrackLimit();
    public abstract int getBackTrackCount();

}
