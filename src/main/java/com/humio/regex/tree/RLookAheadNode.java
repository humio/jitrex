/*
    Copyright (c) Peter Sorotokin, 1998-2000
    Copyright (c) Humio, 2019
    See file "LICENSE.md" for terms of usage and
    redistribution.   
*/
package com.humio.regex.tree;

import java.util.Dictionary;

public class RLookAheadNode extends RNode {
    public RNode body;
    public boolean positive;

    public RLookAheadNode(int pos, RNode body, boolean positive) {
        super(pos);
        this.body = body;
        this.positive = positive;
    }

    public int getNCells() {
        return super.getNCells() + (body == null ? 0 : body.getNCells());
    }

    public void prepare(int addMaxLeft, int addMinLeft) {
        int addMax = addMaxLeft;
        int addMin = addMinLeft;

        if (tail != null) {
            tail.prepare(addMaxLeft, addMinLeft);
            addMax = tail.maxLeft;
            addMin = tail.minLeft;
        }

        if (body != null) {
            body.prepare(addMaxLeft, addMinLeft);
            if (body.maxLeft < addMax)
                addMax = body.maxLeft;
            if (body.minLeft > minLeft)
                addMin = body.minLeft;
        }

        finishPrepare(addMax, addMin);
    }

    public CharSet findPrefix(CharSet tailPrefix) {
        prefix = tailPrefix;
        return prefix;
    }

    public boolean isStartAnchored() {
        return (tail != null && tail.isStartAnchored()) || (body != null && body.isStartAnchored());
    }

    public boolean hasPicks() {
        if (body != null && body.hasPicks())
            return true;
        return super.hasPicks();
    }

    @Override
    public void processFlags(int flags) {
        super.processFlags(flags);
        if (body != null)
            body.processFlags(flags);
    }

    public boolean hasForks() {
        return true;
    }

    public void collectReferences(Dictionary<String, RSubstNode> refList, Dictionary<String, RPickNode> pickList) {
        if (body != null)
            body.collectReferences(refList, pickList);
        super.collectReferences(refList, pickList);
    }

    public RNode markReferenced(Dictionary<String, RSubstNode> refList, Dictionary<String, RPickNode> pickList,
                                boolean collapse) {
        if (body != null)
            body = body.markReferenced(refList, pickList, collapse);
        return super.markReferenced(refList, pickList, collapse);
    }

    public Object eval(RContext context) {
        return context.evalRLookAhead(this);
    }
}

