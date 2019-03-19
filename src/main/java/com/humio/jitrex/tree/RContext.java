/*
    Copyright (c) Peter Sorotokin, 1998-2000
    Copyright (c) Humio, 2019
    See file "LICENSE.md" for terms of usage and
    redistribution.   
*/
package com.humio.jitrex.tree;

public abstract class RContext {
    public abstract Object evalRAlt(RAltNode regexAlt);

    public abstract Object evalRAny(RAnyNode regexAny);

    public abstract Object evalRBoundary(RBoundaryNode regexBoundary);

    public abstract Object evalRConst(RConstNode regexConst);

    public abstract Object evalRCharClass(RCharClassNode regexCharClass);

    public abstract Object evalRLookAhead(RLookAheadNode regexLookAhead);

    public abstract Object evalRPick(RPickNode regexPick);

    public abstract Object evalRRepeat(RRepeatNode regexRepeat);

    public abstract Object evalRSubst(RSubstNode regexSubst);

    public abstract Object evalREmpty(REmptyNode rEmptyNode);
}
