/*
    Copyright (c) Peter Sorotokin, 1998-2000
    Copyright (c) Humio, 2019
    See file "LICENSE.md" for terms of usage and
    redistribution.   
*/
package com.humio.jitrex.interp;

import com.humio.jitrex.compiler.RVariable;

class Variable extends RVariable {
    int cell;
    int extCell = -1;
}

