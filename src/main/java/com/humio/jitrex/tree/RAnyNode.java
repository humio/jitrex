/*
    Copyright (c) Peter Sorotokin, 1998-2000
    Copyright (c) Humio, 2019
    See file "LICENSE.md" for terms of usage and
    redistribution.   
*/
package com.humio.jitrex.tree;

public class RAnyNode extends RNode {

    public RAnyNode(int pos) {
        super(pos);
        minLength = 1;
        maxLength = 1;
    }

    public Object eval(RContext context) {
        return context.evalRAny(this);
    }
}
