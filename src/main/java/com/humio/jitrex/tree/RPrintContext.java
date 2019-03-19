/*
    Copyright (c) Peter Sorotokin, 1998-2000
    Copyright (c) Humio, 2019
    See file "LICENSE.md" for terms of usage and
    redistribution.   
*/
package com.humio.jitrex.tree;

import java.io.CharArrayWriter;
import java.io.PrintWriter;

public class RPrintContext extends RContext {

    CharArrayWriter buffer;
    PrintWriter out;

    public RPrintContext() {
        buffer = new CharArrayWriter();
        out = new PrintWriter(buffer);
    }

    public RPrintContext(PrintWriter out) {
        this.out = out;
    }

    public String toString() {
        if (buffer == null)
            return super.toString();
        out.flush();
        return buffer.toString();
    }

    public Object evalREmpty(REmptyNode empty) {
        return evalTail(empty);
    }


    private Object evalTail(RNode regex) {
        regex = regex.tail;
        if (regex != null) {
            out.print(";");
            regex.eval(this);
        }
        return null;
    }

    public Object evalRConst(RConstNode regexConst) {
        out.print("RConstNode[");
        out.print(regexConst.c);
        out.print("]");
        return evalTail(regexConst);
    }

    public Object evalRCharClass(RCharClassNode regexCharClass) {
        out.println("RCharClassNode[" + regexCharClass.charClass + "]");
        return evalTail(regexCharClass);
    }

    public Object evalRBoundary(RBoundaryNode regexBoundary) {
        out.print("RBoundaryNode[" + regexBoundary.boundaryClass + "]");
        return evalTail(regexBoundary);
    }

    public Object evalRAlt(RAltNode regexAlt) {
        out.print("RAltNode[");
        if (regexAlt.alt1 != null)
            regexAlt.alt1.eval(this);
        out.print(",");
        if (regexAlt.alt2 != null)
            regexAlt.alt2.eval(this);
        out.print("]");
        return evalTail(regexAlt);
    }

    public Object evalRLookAhead(RLookAheadNode regexLookAhead) {
        out.print("RLookAheadNode[");
        if (regexLookAhead.body != null)
            regexLookAhead.body.eval(this);
        out.print(",");
        out.print(regexLookAhead.positive);
        out.print("]");
        return evalTail(regexLookAhead);
    }

    public Object evalRPick(RPickNode regexPick) {
        out.print("RPickNode[");
        out.print(regexPick.name);
        out.print(",");
        out.print(regexPick.begin ? "b" : "e");
        out.print(",");
        out.print(regexPick.referenced);
        out.print("]");
        return evalTail(regexPick);
    }

    public Object evalRAny(RAnyNode regexAny) {
        out.print("RAnyNode[]");
        return evalTail(regexAny);
    }

    public Object evalRRepeat(RRepeatNode regexRepeat) {
        out.print("RRepeatNode[");
        out.print(regexRepeat.min);
        out.print(",");
        if (regexRepeat.max == Integer.MAX_VALUE)
            out.print("*");
        else
            out.print(regexRepeat.max);
        out.print(",");
        regexRepeat.body.eval(this);
        out.print("]");
        return evalTail(regexRepeat);
    }

    public Object evalRSubst(RSubstNode regexSubst) {
        out.print("RSubstNode[");
        out.print(regexSubst.var);
        out.print(",");
        out.print(regexSubst.picked);
        out.print("]");
        return evalTail(regexSubst);
    }

}
