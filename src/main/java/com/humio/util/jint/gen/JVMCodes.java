/*
    Copyright (c) Peter Sorotokin, 1998-2000
    Copyright (c) Humio, 2019
    See file "LICENSE.md" for terms of usage and
    redistribution.   
*/
package com.humio.util.jint.gen;

interface JVMCodes {
    int nop = 0x00;

    int aconst_null = 0x01;

    int iconst_m1 = 0x02;
    int iconst_0 = 0x03;
    int iconst_1 = 0x04;
    int iconst_2 = 0x05;
    int iconst_3 = 0x06;
    int iconst_4 = 0x07;
    int iconst_5 = 0x08;

    int lconst_0 = 0x09;
    int lconst_1 = 0x0A;

    int fconst_0 = 0x0B;
    int fconst_1 = 0x0C;
    int fconst_2 = 0x0D;

    int dconst_0 = 0x0E;
    int dconst_1 = 0x0F;

    int bipush = 0x10;
    int sipush = 0x11;

    int ldc = 0x12;
    int ldc_w = 0x13;
    int ldc2_w = 0x14;

    int iload = 0x15;
    int lload = 0x16;
    int fload = 0x17;
    int dload = 0x18;
    int aload = 0x19;

    int iload_0 = 0x1A;
    int iload_1 = 0x1B;
    int iload_2 = 0x1C;
    int iload_3 = 0x1D;

    int lload_0 = 0x1E;
    int lload_1 = 0x1F;
    int lload_2 = 0x20;
    int lload_3 = 0x21;

    int fload_0 = 0x22;
    int fload_1 = 0x23;
    int fload_2 = 0x24;
    int fload_3 = 0x25;

    int dload_0 = 0x26;
    int dload_1 = 0x27;
    int dload_2 = 0x28;
    int dload_3 = 0x29;

    int aload_0 = 0x2A;
    int aload_1 = 0x2B;
    int aload_2 = 0x2C;
    int aload_3 = 0x2D;

    int iaload = 0x2E;
    int laload = 0x2F;
    int faload = 0x30;
    int daload = 0x31;
    int aaload = 0x32;
    int baload = 0x33;
    int caload = 0x34;
    int saload = 0x35;

    int istore = 0x36;
    int lstore = 0x37;
    int fstore = 0x38;
    int dstore = 0x39;
    int astore = 0x3A;

    int istore_0 = 0x3B;
    int istore_1 = 0x3C;
    int istore_2 = 0x3D;
    int istore_3 = 0x3E;

    int lstore_0 = 0x3F;
    int lstore_1 = 0x40;
    int lstore_2 = 0x41;
    int lstore_3 = 0x42;

    int fstore_0 = 0x43;
    int fstore_1 = 0x44;
    int fstore_2 = 0x45;
    int fstore_3 = 0x46;

    int dstore_0 = 0x47;
    int dstore_1 = 0x48;
    int dstore_2 = 0x49;
    int dstore_3 = 0x4A;

    int astore_0 = 0x4B;
    int astore_1 = 0x4C;
    int astore_2 = 0x4D;
    int astore_3 = 0x4E;

    int iastore = 0x4F;
    int lastore = 0x50;
    int fastore = 0x51;
    int dastore = 0x52;
    int aastore = 0x53;
    int bastore = 0x54;
    int castore = 0x55;
    int sastore = 0x56;

    int pop = 0x57;
    int pop2 = 0x58;

    int dup = 0x59;
    int dup_x1 = 0x5A;
    int dup_x2 = 0x5B;
    int dup2 = 0x5C;
    int dup2_x1 = 0x5D;
    int dup2_x2 = 0x5E;

    int swap = 0x5F;

    int iadd = 0x60;
    int ladd = 0x61;
    int fadd = 0x62;
    int dadd = 0x63;
    int isub = 0x64;
    int lsub = 0x65;
    int fsub = 0x66;
    int dsub = 0x67;
    int imul = 0x68;
    int lmul = 0x69;
    int fmul = 0x6A;
    int dmul = 0x6B;
    int idiv = 0x6C;
    int ldiv = 0x6D;
    int fdiv = 0x6E;
    int ddiv = 0x6F;
    int irem = 0x70;
    int lrem = 0x71;
    int frem = 0x72;
    int drem = 0x73;

    int ineg = 0x74;
    int lneg = 0x75;
    int fneg = 0x76;
    int dneg = 0x77;

    int ishl = 0x78;
    int lshl = 0x79;
    int ishr = 0x7A;
    int lshr = 0x7B;
    int iushr = 0x7C;
    int lushr = 0x7D;
    int iand = 0x7E;
    int land = 0x7F;
    int ior = 0x80;
    int lor = 0x81;
    int ixor = 0x82;
    int lxor = 0x83;

    int iinc = 0x84;

    int i2l = 0x85;
    int i2f = 0x86;
    int i2d = 0x87;
    int l2i = 0x88;
    int l2f = 0x89;
    int l2d = 0x8A;
    int f2i = 0x8B;
    int f2l = 0x8C;
    int f2d = 0x8D;
    int d2i = 0x8E;
    int d2l = 0x8F;
    int d2f = 0x90;
    int i2b = 0x91;
    int i2c = 0x92;
    int i2s = 0x93;

    int lcmp = 0x94;
    int fcmpl = 0x95;
    int fcmpg = 0x96;
    int dcmpl = 0x97;
    int dcmpg = 0x98;

    int ifeq = 0x99;
    int ifne = 0x9A;
    int iflt = 0x9B;
    int ifge = 0x9C;
    int ifgt = 0x9D;
    int ifle = 0x9E;

    int if_icmpeq = 0x9F;
    int if_icmpne = 0xA0;
    int if_icmplt = 0xA1;
    int if_icmpge = 0xA2;
    int if_icmpgt = 0xA3;
    int if_icmple = 0xA4;

    int if_acmpeq = 0xA5;
    int if_acmpne = 0xA6;

    int _goto = 0xA7;
    int jsr = 0xA8;
    int ret = 0xA9;

    int tableswitch = 0xAA;
    int lookupswitch = 0xAB;

    int ireturn = 0xAC;
    int lreturn = 0xAD;
    int freturn = 0xAE;
    int dreturn = 0xAF;
    int areturn = 0xB0;
    int vreturn = 0xB1;

    int getstatic = 0xB2;
    int putstatic = 0xB3;
    int getfield = 0xB4;
    int putfield = 0xB5;

    int invokevirtual = 0xB6;
    int invokespecial = 0xB7;
    int invokestatic = 0xB8;
    int invokeinterface = 0xB9;

    int newobject = 0xBB;
    int newarray = 0xBC;
    int anewarray = 0xBD;

    int arraylength = 0xBE;

    int athrow = 0xBF;

    int checkcast = 0xC0;

    int instnceof = 0xC1;

    int monitorenter = 0xC2;
    int monitorexit = 0xC3;

    int wide = 0xC4;

    int multianewarray = 0xC5;

    int ifnull = 0xC6;
    int ifnonnull = 0xC7;

    int goto_w = 0xC8;
    int jsr_w = 0xC9;

    //---- constant pool entry codes

    byte TAG_UTF8 = (byte) 1;
    byte TAG_Integer = (byte) 3;
    byte TAG_Float = (byte) 4;
    byte TAG_Long = (byte) 5;
    byte TAG_Double = (byte) 6;
    byte TAG_Class = (byte) 7;
    byte TAG_String = (byte) 8;
    byte TAG_FieldRef = (byte) 9;
    byte TAG_MethodRef = (byte) 10;
    byte TAG_InterfaceMethodRef = (byte) 11;
    byte TAG_NameAndType = (byte) 12;


}
