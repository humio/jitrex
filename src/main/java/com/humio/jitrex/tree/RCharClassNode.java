/*
    Copyright (c) Peter Sorotokin, 1998-2000
    Copyright (c) Humio, 2019
    See file "LICENSE.md" for terms of usage and
    redistribution.   
*/
package com.humio.jitrex.tree;

public class RCharClassNode extends RNode {

    public CharSet charClass;

    public RCharClassNode(int pos, CharSet set) {
        super(pos);
        this.charClass = set;
        prefix = set;
        minLength = 1;
        maxLength = 1;
    }

    public RCharClassNode(int pos, boolean neg, char[] ranges, CharSet[] more) {
        super(pos);
        charClass = new CharSet(ranges);

        for (int i = 0; i < more.length; i++) {
            charClass = CharSet.merge(charClass, more[i]);
        }

        if (neg)
            charClass = charClass.negate();
        prefix = charClass;
        minLength = 1;
        maxLength = 1;
    }

    @Override
    public void processFlags(int flags) {
        super.processFlags(flags);
        if (isLowercase()) {
            charClass = charClass.toLowerCase();
        }
    }

    public Object eval(RContext context) {
        return context.evalRCharClass(this);
    }
}
