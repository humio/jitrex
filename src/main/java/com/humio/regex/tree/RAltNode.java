/*
    Copyright (c) Peter Sorotokin, 1998-2000
    Copyright (c) Humio, 2019
    See file "LICENSE.md" for terms of usage and
    redistribution.   
*/
package com.humio.regex.tree;

import java.util.Dictionary;

public class RAltNode extends RNode {
    public RNode alt1;
    public RNode alt2;

    public RAltNode(int pos, RNode alt1, RNode alt2) {
        super(pos);
        this.alt1 = alt1;
        this.alt2 = alt2;
    }

    public int getNCells() {
        return super.getNCells() + (alt1 == null ? 0 : alt1.getNCells()) + (alt2 == null ? 0 : alt2.getNCells());
    }

    public void prepare(int addMaxLeft, int addMinLeft) {
        if (tail != null) {
            tail.prepare(addMaxLeft, addMinLeft);
            addMaxLeft = tail.maxLeft;
            addMinLeft = tail.minLeft;
        }

        if (alt1 != null)
            alt1.prepare(addMaxLeft, addMinLeft);

        if (alt2 != null)
            alt2.prepare(addMaxLeft, addMinLeft);

        int aMax1 = maxTotalLength(alt1);
        int aMax2 = maxTotalLength(alt2);
        maxLength = (aMax1 > aMax2 ? aMax1 : aMax2);

        int aMin1 = minTotalLength(alt1);
        int aMin2 = minTotalLength(alt2);
        minLength = (aMin1 < aMin2 ? aMin1 : aMin2);

        finishPrepare(addMaxLeft, addMinLeft);
    }

    public CharSet findPrefix(CharSet tailPrefix) {
        if (tail != null)
            tailPrefix = tail.findPrefix(tailPrefix);
        CharSet set1 = (alt1 == null ? tailPrefix : alt1.findPrefix(tailPrefix));
        CharSet set2 = (alt2 == null ? tailPrefix : alt2.findPrefix(tailPrefix));
        prefix = CharSet.merge(set1, set2);
        return prefix;
    }

    public boolean isStartAnchored() {
        return alt1 != null && alt2 != null && alt1.isStartAnchored() && alt2.isStartAnchored();
    }

    public boolean isEndAnchored() {
        if (tail != null)
            return tail.isEndAnchored();
        return alt1 != null && alt2 != null && alt1.isEndAnchored() && alt2.isEndAnchored();
    }

    public boolean hasPicks() {
        if (alt1 != null && alt1.hasPicks())
            return true;
        if (alt2 != null && alt2.hasPicks())
            return true;
        return super.hasPicks();
    }

    public void processFlags(int flags) {
        super.processFlags(flags);
        if (alt1 != null)
            alt1.processFlags(flags);
        if (alt2 != null)
            alt2.processFlags(flags);
    }

    public boolean hasForks() {
        return true;
    }

    public void collectReferences(Dictionary<String, RSubstNode> refList, Dictionary<String, RPickNode> pickList) {
        if (alt1 != null)
            alt1.collectReferences(refList, pickList);
        if (alt2 != null)
            alt2.collectReferences(refList, pickList);
        super.collectReferences(refList, pickList);
    }

    public RNode markReferenced(Dictionary<String, RSubstNode> refList, Dictionary<String, RPickNode> pickList,
                                boolean collapse) {
        if (alt1 != null)
            alt1 = alt1.markReferenced(refList, pickList, collapse);
        if (alt2 != null)
            alt2 = alt2.markReferenced(refList, pickList, collapse);
        return super.markReferenced(refList, pickList, collapse);
    }

    public Object eval(RContext context) {
        return context.evalRAlt(this);
    }
}
