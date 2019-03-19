/*
    Copyright (c) Peter Sorotokin, 1998-2000
    Copyright (c) Humio, 2019
    See file "LICENSE.md" for terms of usage and
    redistribution.   
*/
package com.humio.util.jint.constants;

public interface TokenConst {
    int TOKEN_EOF = 0x10001;
    int TOKEN_WORD = 0x10002;
    int TOKEN_V_INT = 0x10003;
    int TOKEN_V_LONG = 0x10004;
    int TOKEN_V_FLOAT = 0x10010;
    int TOKEN_V_DOUBLE = 0x10011;
    int TOKEN_NONE = 0x10020;
    int TOKEN_V_STRING = '"';
    int TOKEN_V_CHAR = '\'';

    int TOKEN_V_REGEX = '`';  // m`re`
    int TOKEN_V_SUBST = 'a';  // s`re`form`
    int TOKEN_V_TRANS = 't';  // t`rg`rg`
    int TOKEN_V_PATT = 'F';  // p`patt`
    int TOKEN_V_FORM = 'f';  // f`form` - printf-style format string
    int TOKEN_V_BOUND = 'b';  // b`form` - format string with embedded variables

    int TOKEN_GE = 'G'; // >=
    int TOKEN_GG = 'g'; // >>
    int TOKEN_GGG = 'Q'; // >>>
    int TOKEN_GGE = 'k'; // >>=
    int TOKEN_GGGE = 'q'; // >>>=
    int TOKEN_LE = 'L'; // <=
    int TOKEN_LL = 'l'; // <<
    int TOKEN_LLE = 'K'; // <<=
    int TOKEN_PP = 'p'; // ++
    int TOKEN_PE = 'P'; // +=
    int TOKEN_SE = 'S'; // /=
    int TOKEN_EE = 'e'; // ==
    int TOKEN_AE = 'A'; // *=
    int TOKEN_MM = 'm'; // --
    int TOKEN_ME = 'M'; // -=
    int TOKEN_NE = 'N'; // !=
    int TOKEN_PCE = 'C'; // %=
    int TOKEN_OR = 'O'; // ||
    int TOKEN_ORE = 'o'; // |=
    int TOKEN_AND = 'D'; // &&
    int TOKEN_ANDE = 'd'; // &=
    int TOKEN_XE = 'X'; // ^=
    int TOKEN_SS = 's'; // ::
    int TOKEN_TE = 'T'; // ~=

    int UNARY_MINUS = 'U'; // -

    int TOKEN_ABSTRACT = -1;
    int TOKEN_BOOLEAN = -2;
    int TOKEN_BREAK = -3;
    int TOKEN_BYTE = -4;
    int TOKEN_BYVALUE = -5;
    int TOKEN_CASE = -6;
    int TOKEN_CATCH = -7;
    int TOKEN_CHAR = -8;
    int TOKEN_CLASS = -9;
    int TOKEN_CONST = -10;
    int TOKEN_CONTINUE = -11;
    int TOKEN_DEFAULT = -12;
    int TOKEN_DO = -13;
    int TOKEN_DOUBLE = -14;
    int TOKEN_ELSE = -15;
    int TOKEN_EXTENDS = -16;
    int TOKEN_FALSE = -17;
    int TOKEN_FINAL = -18;
    int TOKEN_FINALLY = -19;
    int TOKEN_FLOAT = -20;
    int TOKEN_FOR = -21;
    int TOKEN_GOTO = -22;
    int TOKEN_IF = -23;
    int TOKEN_IMPLEMENTS = -24;
    int TOKEN_IMPORT = -25;
    int TOKEN_INSTANCEOF = -26;
    int TOKEN_INT = -27;
    int TOKEN_INTERFACE = -28;
    int TOKEN_LONG = -29;
    int TOKEN_NATIVE = -30;
    int TOKEN_NEW = -31;
    int TOKEN_NULL = -32;
    int TOKEN_PACKAGE = -33;
    int TOKEN_PRIVATE = -34;
    int TOKEN_PROTECTED = -35;
    int TOKEN_PUBLIC = -36;
    int TOKEN_RETURN = -37;
    int TOKEN_SHORT = -38;
    int TOKEN_STATIC = -39;
    int TOKEN_SUPER = -40;
    int TOKEN_SWITCH = -41;
    int TOKEN_SYNCHRONIZED = -42;
    int TOKEN_THIS = -43;
    int TOKEN_THREADSAFE = -44;
    int TOKEN_THROW = -45;
    int TOKEN_THROWS = -46;
    int TOKEN_TRANSIENT = -47;
    int TOKEN_TRUE = -48;
    int TOKEN_TRY = -49;
    int TOKEN_VOID = -50;
    int TOKEN_VOLATILE = -51;
    int TOKEN_WHILE = -52;

    int TOKEN_WHERE = -53; // unofficial

    int TOK_OPT_GLOBAL = 0x00000001; // g
    int TOK_OPT_IGNORE = 0x00000002; // i
    int TOK_OPT_SINGLELINE = 0x00000004; // s
    int TOK_OPT_MULTILINE = 0x00000008; // m

    int TOK_OPT_BUFFERONLY = 0x00010000; // B
    int TOK_OPT_DECLARE = 0x00020000; // D
    int TOK_OPT_OFFLINE = 0x00040000; // O
    int TOK_OPT_RETAINALL = 0x00080000; // R
    int TOK_OPT_STANDALONE = 0x00100000; // S

}
