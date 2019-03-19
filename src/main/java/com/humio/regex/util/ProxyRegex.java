/*
    Copyright (c) Peter Sorotokin, 1998-2000
    See file "LICENSE.md" for terms of usage and
    redistribution.   
*/
package com.humio.regex.util;

import java.util.Enumeration;

public class ProxyRegex extends Regex {

    Regex regex;

    public ProxyRegex(Regex orig) {
        regex = orig;
    }

    public int getVariableHandle(String var, boolean begin) {
        return regex.getVariableHandle(var, begin);
    }

    public int getExtVariableHandle(String var) {
        return regex.getExtVariableHandle(var);
    }

    public Enumeration<String> variables() {
        return regex.variables();
    }

    public void init(CharSequence subject, int off, int len) {
        regex.init(subject, off, len);
    }

    public boolean searchAgain() {
        return regex.searchAgain();
    }

    public boolean search() {
        return regex.search();
    }

    public boolean search(int from) {
        return regex.search(from);
    }

    public boolean match() {
        return regex.match();
    }

    public boolean matchWhole() {
        return regex.matchWhole();
    }

    public int getIndex(int handle) {
        return regex.getIndex(handle);
    }

    public void setIndex(int handle, int value) {
        regex.setIndex(handle, value);
    }

    public CharSequence getCharBuffer(int extHandle) {
        return regex.getCharBuffer(extHandle);
    }

    public int getMatchStart() {
        return regex.getMatchStart();
    }

    public int getMatchEnd() {
        return regex.getMatchEnd();
    }

    public void setExtVariableBuffer(int extHandle, CharSequence arr) {
        regex.setExtVariableBuffer(extHandle, arr);
    }

    public void setRefiller(RegexRefiller refiller) {
        regex.setRefiller(refiller);
    }

    public void setRefilledBuffer(CharSequence buffer) {
        regex.setRefilledBuffer(buffer);
    }

    public Regex cloneRegex() {
        ProxyRegex clone = (ProxyRegex) super.cloneRegex();
        clone.regex = regex.cloneRegex();
        return clone;
    }

    public String toString() {
        return regex.toString();
    }

}
