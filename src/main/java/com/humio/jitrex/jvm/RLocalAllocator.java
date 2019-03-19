/*
    Copyright (c) Peter Sorotokin, 1998-2000
    Copyright (c) Humio, 2019
    See file "LICENSE.md" for terms of usage and
    redistribution.   
*/
package com.humio.jitrex.jvm;

public abstract class RLocalAllocator {
    public abstract int alloc();

    public abstract void free(int local);
}
