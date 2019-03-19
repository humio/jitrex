/*
    Copyright (c) Peter Sorotokin, 1998-2000
    Copyright (c) Humio, 2019
    See file "LICENSE.md" for terms of usage and
    redistribution.   
*/
package com.humio.jitrex.tree;

public class REmptyNode extends RNode {

    public REmptyNode(int pos) {
        super(pos);
        minLength = 0;
        maxLength = 0;
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

    public Object eval(RContext context) {
        return context.evalREmpty(this);
    }
}
