/*
    Copyright (c) Peter Sorotokin, 1998-2000
    Copyright (c) Humio, 2019
    See file "LICENSE.md" for terms of usage and
    redistribution.   
*/
package com.humio.util.jint.constants;

public interface DefinitionConst {
    int ACC_PUBLIC = 0x0001;
    int ACC_PRIVATE = 0x0002;
    int ACC_PROTECTED = 0x0004;
    int ACC_STATIC = 0x0008;
    int ACC_FINAL = 0x0010;
    int ACC_SUPER = 0x0020; // for classes: new invokespecial semantics
    int ACC_SYNCHRONIZED = 0x0020; // for methods
    int ACC_VOLATILE = 0x0040;
    int ACC_TRANSIENT = 0x0080;
    int ACC_NATIVE = 0x0100;
    int ACC_INTERFACE = 0x0200;
    int ACC_ABSTRACT = 0x0400;

    int PUBLIC_FLAG = ACC_PUBLIC;
    int PRIVATE_FLAG = ACC_PRIVATE;
    int ABSTRACT_FLAG = ACC_ABSTRACT;
    int FINAL_FLAG = ACC_FINAL;
    int INTERFACE_FLAG = ACC_INTERFACE;
    int PROTECTED_FLAG = ACC_PROTECTED;
    int STATIC_FLAG = ACC_STATIC;
    int SYNCHRONIZED_FLAG = ACC_SYNCHRONIZED;
    int NATIVE_FLAG = ACC_NATIVE;
    int VOLATILE_FLAG = ACC_VOLATILE;
    int TRANSIENT_FLAG = ACC_TRANSIENT;

    int STRICT_FLAG = 0x00010000;
    int EXPLICIT_FLAG = 0x00020000;
    int WHERE_FLAG = 0x00040000;

    int MUST_BE_CLASS_FLAG = 0x00100000;
    int SELF_USED_FLAG = 0x00200000;
    int METHOD_SCOPE_FLAG = 0x00400000;
    int WORLD_FLAG = 0x00800000;

    int CONST_FLAG = 0x01000000;
    int SCRIPT_STYLE_FLAG = 0x02000000;
    int PACKAGE_PRIVATE_FLAG = 0x04000000;

    int ACC_MASK = 0x0000FFFF;

}
