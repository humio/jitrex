/*
    Copyright (c) Peter Sorotokin, 1998-2000
    Copyright (c) Humio, 2019
    See file "LICENSE.md" for terms of usage and
    redistribution.   
*/
package com.humio.regex.tree;

import java.util.Dictionary;

public class RSubstNode extends RNode {

    public String var;
    public boolean picked;

    public RSubstNode(int pos, String var) {
        super(pos);
        this.var = var;
    }

    public int getNCells() {
        return super.getNCells() + (picked ? 0 : 2);
    }

    public void collectReferences(Dictionary<String, RSubstNode> refList, Dictionary<String, RPickNode> pickList) {
        RPickNode picker = (RPickNode) pickList.get(var);
        picked = picker != null;
        if (picked && picker.begin)
            throw new RuntimeException("Variable " + var + " is referenced before fully assigned");
        refList.put(var, this);
        super.collectReferences(refList, pickList);
    }

    public RNode markReferenced(Dictionary<String, RSubstNode> refList, Dictionary<String, RPickNode> pickList,
                                boolean collapse) {
        if (!picked && pickList.get(var) != null)
            throw new RuntimeException("Variable " + var + " is referenced before assigned");
        return super.markReferenced(refList, pickList, collapse);
    }

    public Object eval(RContext context) {
        return context.evalRSubst(this);
    }
}
