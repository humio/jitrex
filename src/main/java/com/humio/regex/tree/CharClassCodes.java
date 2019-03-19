/*
    Copyright (c) Peter Sorotokin, 1998-2000
    Copyright (c) Humio, 2019
    See file "LICENSE.md" for terms of usage and
    redistribution.   
*/
package com.humio.regex.tree;

public interface CharClassCodes {
    // char classes work only for range 0x0080-0xFFFF,
    // ASCII is always done by ranges
    int CLASS_DISABLED = 0x8;
    int CLASS_NONE = 0;
    int CLASS_ALL = 0x7;
    int CLASS_LETTER = 0x3;
    int CLASS_LOWERCASE = 0x1;
    int CLASS_UPPERCASE = 0x2;
    int CLASS_NONLETTER = 0x4;
    int CLASS_NONLOWERCASE = 0x6;
    int CLASS_NONUPPERCASE = 0x5;
}
