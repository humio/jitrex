/*
    Copyright (c) Peter Sorotokin, 1998-2000
    Copyright (c) Humio, 2019
    See file "LICENSE.md" for terms of usage and
    redistribution.   
*/
package com.humio.jitrex.jvm;

import com.humio.util.jint.gen.LocalVariable;

public abstract class RLocalAllocator {

    public abstract int alloc();

    public abstract void free(int local);

    public LocalVariable allocVariable(String type) {
        return new LocalVariable(alloc(), type);
    }

    public void free(LocalVariable var) {
        free(var.getIndex());
    }

}
