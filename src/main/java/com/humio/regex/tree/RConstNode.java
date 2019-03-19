/*
    Copyright (c) Peter Sorotokin, 1998-2000
    Copyright (c) Humio, 2019
    See file "LICENSE.md" for terms of usage and
    redistribution.   
*/
package com.humio.regex.tree;

public class RConstNode extends RNode {

    public char c;

    public RConstNode(int pos, char c) {
        super(pos);
        this.c = c;
        minLength = 1;
        maxLength = 1;
        prefix = new CharSet(c);
    }

    @Override
    public void processFlags(int flags) {
        super.processFlags(flags);
        if (isLowercase()) {
            char c1 = Character.toLowerCase(c);
            if (c1 != c) {
                c = c1;
                prefix = new CharSet(c);
            }
        }
    }

    public Object eval(RContext context) {
        return context.evalRConst(this);
    }
}
