/*
    Copyright (c) Peter Sorotokin, 1998-2000
    Copyright (c) Humio, 2019
    See file "LICENSE.md" for terms of usage and
    redistribution.   
*/
package com.humio.regex.tree;

import com.humio.regex.util.Regex;

import java.util.Dictionary;

public abstract class RNode {
    public int position; // in the initial regex

    public int maxLeft;
    public int minLeft;

    // by default, it can take from 0 to infinity chars
    public int maxLength = Integer.MAX_VALUE;
    public int minLength = 0;

    public CharSet prefix = CharSet.FULL_CHARSET;

    public RNode tail;
    private int flags;

    public int flags() { return flags; }

    protected RNode(int pos) {
        position = pos;
    }

    public static int minTotalLength(RNode p) {
        int t = 0;
        while (p != null) {
            t += p.minLength;
            if (t < 0)
                throw new RuntimeException("Unexpected integer overflow");
            p = p.tail;
        }
        return t;
    }

    public static int maxTotalLength(RNode p) {
        int t = 0;
        while (p != null) {
            if (p.maxLength == Integer.MAX_VALUE)
                return Integer.MAX_VALUE;
            t += p.maxLength;
            if (t < 0)
                return Integer.MAX_VALUE;
            p = p.tail;
        }
        return t;
    }

    /**
     * Number of variables to be used by this regex. This should be upper bound,
     * but, of cource, it should be as close as possible to what RCompiler will
     * generate.
     */
    public int getNCells() {
        if (tail == null)
            return 0;
        else
            return tail.getNCells();
    }

    public void prepare(int addMaxLeft, int addMinLeft) {
        if (addMaxLeft < 0 || addMinLeft < 0)
            throw new RuntimeException("Invalid internal state addMin="+addMinLeft+" addMax="+addMaxLeft);

        if (tail != null)
            tail.prepare(addMaxLeft, addMinLeft);
        finishPrepare(addMaxLeft, addMinLeft);
    }

    protected void finishPrepare(int addMaxLeft, int addMinLeft) {
        if (addMaxLeft < 0 || addMinLeft < 0)
            throw new RuntimeException("Invalid internal state addMin="+addMinLeft+" addMax="+addMaxLeft);

        maxLeft = (tail == null ? addMaxLeft : tail.maxLeft);
        if (maxLeft < Integer.MAX_VALUE && maxLength < Integer.MAX_VALUE &&
                maxLeft + maxLength >= 0)
            maxLeft += maxLength;
        else
            maxLeft = Integer.MAX_VALUE;
        minLeft = (tail == null ? addMinLeft : tail.minLeft) + minLength;
        if (minLeft < minLength
                || minLength < 0
                || minLeft < 0
                || maxLeft < 0
                || maxLength < 0
                || maxLeft < maxLength)
            throw new RuntimeException("Invalid min/max char counts: " +
                    minLeft + " " + minLength + " " + maxLeft + " " + maxLength + ": " + this);
    }

    public void processFlags(int flags) {
        this.flags = flags;
        if (tail != null)
            tail.processFlags(flags);
    }

    public boolean isLowercase() {
        return (flags & Regex.CASE_INSENSITIVE) == Regex.CASE_INSENSITIVE;
    }

    public CharSet findPrefix(CharSet tailPrefix) {
        if (tail != null)
            tail.findPrefix(tailPrefix);
        return prefix;
    }

    public boolean isStartAnchored() {
        return false;
    }

    public boolean isEndAnchored() {
        if (tail != null)
            return tail.isEndAnchored();
        return false;
    }

    public boolean hasPicks() {
        if (tail != null)
            return tail.hasPicks();
        return false;
    }

    public boolean hasForks() {
        if (tail != null)
            return tail.hasForks();
        return false;
    }

    public void collectReferences(Dictionary<String, RSubstNode> refList, Dictionary<String, RPickNode> pickList) {
        if (tail != null)
            tail.collectReferences(refList, pickList);
    }

    public RNode markReferenced(Dictionary<String, RSubstNode> refList, Dictionary<String, RPickNode> pickList,
                                boolean collapse) {
        if (tail != null)
            tail = tail.markReferenced(refList, pickList, collapse);
        return this;
    }

    public abstract Object eval(RContext context);

    public String toString() {
        RPrintContext pc = new RPrintContext();
        eval(pc);
        return pc.toString();
    }
}
