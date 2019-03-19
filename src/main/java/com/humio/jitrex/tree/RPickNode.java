/*
    Copyright (c) Peter Sorotokin, 1998-2000
    Copyright (c) Humio, 2019
    See file "LICENSE.md" for terms of usage and
    redistribution.   
*/
package com.humio.jitrex.tree;

import java.util.Dictionary;

public class RPickNode extends RNode {

    public String name;
    public boolean begin;
    private final int setflags;
    private final int clearflags;
    private final boolean keepFlagsAfter;
    public boolean referenced;
    public RPickNode start;
    private int saveFlags;

    public RPickNode(int pos, String name, boolean begin, int setflags, int clearflags, boolean keepFlagsAfter) {
        super(pos);
        this.name = name;
        this.begin = begin;
        this.setflags = setflags;
        this.clearflags = clearflags;
        this.keepFlagsAfter = keepFlagsAfter;
        minLength = 0;
        maxLength = 0;
    }

    @Override
    public void processFlags(int flags) {
        if (begin) {
            this.saveFlags = flags;
            flags = (flags | setflags) & ~clearflags;
        } else if (!keepFlagsAfter) {
            flags = start.saveFlags;
        }
        super.processFlags(flags);
    }

    public int getNCells() {
        return super.getNCells() + 1;
    }

    public CharSet findPrefix(CharSet tailPrefix) {
        if (tail == null)
            prefix = tailPrefix;
        else {
            tail.findPrefix(tailPrefix);
            prefix = tail.prefix;
        }
        return prefix;
    }

    public boolean isStartAnchored() {
        if (tail == null)
            return false;
        return tail.isStartAnchored();
    }

    public boolean hasPicks() {
        if (name != null)
            return true;
        return super.hasPicks();
    }

    public void collectReferences(Dictionary<String, RSubstNode> refList, Dictionary<String, RPickNode> pickList) {
        if (name.length() > 0)
            pickList.put(name, this);
        super.collectReferences(refList, pickList);
    }

    public RNode markReferenced(Dictionary<String, RSubstNode> refList, Dictionary<String, RPickNode> pickList,
                                boolean collapse) {
        referenced = refList.get(name) != null;
        if (name.length() == 0 || (!referenced && collapse && Character.isDigit(name.charAt(0))))
            if (tail == null)
                return null;
            else
                return tail.markReferenced(refList, pickList, collapse);
        return super.markReferenced(refList, pickList, collapse);
    }

    public Object eval(RContext context) {
        return context.evalRPick(this);
    }
}
