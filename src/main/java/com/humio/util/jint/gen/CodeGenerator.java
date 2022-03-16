/*
    Copyright (c) Peter Sorotokin, 1998-2000
    Copyright (c) Humio, 2019
    See file "LICENSE.md" for terms of usage and
    redistribution.
*/
package com.humio.util.jint.gen;

import com.humio.util.jint.constants.DefinitionConst;
import com.humio.util.jint.constants.TokenConst;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class CodeGenerator implements DefinitionConst, TokenConst {

    int markCounter = 1;

    boolean reachable = false;

    PrintWriter out;

    public CodeGenerator() {
        //out = new PrintWriter( new OutputStreamWriter( System.out ) );
    }

    public CodeGenerator(int acc, String name, String ext) throws IOException {
        out = new PrintWriter(new FileWriter(name.replace('/', '.') + ".dump"));
        out.println("\t.class " + Integer.toHexString(acc) + " " +
                name + " extends " + ext);
    }

    public void flush() throws IOException {
        if (out != null) {
            out.println("\t.endclass");
            out.close();
        }
    }

    /**
     * Add a field to the class.
     *
     * @param accessFlags access flags (ACC_*) for this field, ored together
     * @param name        field name
     * @param type        type <b>descriptor</b> (not class name)
     * @param value       initial value, null for nothing, short numerical
     *                    types represented by Integer
     */
    public void addField(int accessFlags, String name, String type, Object value) {
        out.println("\t.field " + Integer.toHexString(accessFlags) + " " + type
                + " " + name + " = " + value);
    }

    public void setInterfaces(String[] impl) {
        for (int i = 0; i < impl.length; i++)
            out.println("\t.implement " + impl[i]);
    }

    public void startMethod(int accessFlags, String name, String type,
                            String[] exceptions) {
        out.println();
        out.println("\t.method " + Integer.toHexString(accessFlags) + " " + name
                + " " + type);
        if (exceptions != null) {
            out.print("\t.throws");
            for (int i = 0; i < exceptions.length; i++) {
                out.print(' ');
                out.print(exceptions[i]);
            }
            out.println();
        }
        reachable = (accessFlags & (ACC_NATIVE | ACC_ABSTRACT)) == 0;
    }

    public void endMethod() {
        if (reachable)
            throw new RuntimeException("No return at the end of the method!");
        out.println("\t.end");
    }

    public void setSourceFile(String file) {
        out.println("\t.source " + file);
    }

    public void addExceptionHandler(AbstractMark from, AbstractMark to,
                                    AbstractMark jump, String exceptionClass) {
        out.println("\t.handler " + from + ".." + to + "[" + exceptionClass + "]: " + jump);
    }

    public void linenumber(int line) throws IOException {
        out.println("\t.line " + line);
    }

    public void loadConst(Object v) throws IOException {
        if (v instanceof String)
            out.println("\tloadConst \"" + v + "\"");
        else if (v == null)
            out.println("\tloadConst null");
        else {
            String tp = v.getClass().getName();
            int i = tp.lastIndexOf('.');
            char t = tp.charAt(i + 1);
            if (t == 'L')
                t = 'J';
            out.println("\tloadConst " + v + " " + t);
        }
    }

    public void getfield(String clazz, String name, String type) throws IOException {
        out.println("\tgetfield " + clazz + " " + name + " " + type);
    }

    public void getstatic(String clazz, String name, String type) throws IOException {
        out.println("\tgetstatic " + clazz + " " + name + " " + type);
    }

    public void putfield(String clazz, String name, String type) throws IOException {
        out.println("\tputfield " + clazz + " " + name + " " + type);
    }

    public void putstatic(String clazz, String name, String type) throws IOException {
        out.println("\tputstatic " + clazz + " " + name + " " + type);
    }

    public void store(LocalVariable var, String type) throws IOException {
        var.typeCheck(type);
        out.println("\tstore " + var + " " + type);
    }

    public void load(LocalVariable var, String type) throws IOException {
        var.typeCheck(type);
        out.println("\tload " + var + " " + type);
    }

    public void invokevirtual(String clazz, String name, String type) throws IOException {
        out.println("\tinvokevirtual " + clazz + " " + name + " " + type);
    }

    public void invokespecial(String clazz, String name, String type) throws IOException {
        out.println("\tinvokespecial " + clazz + " " + name + " " + type);
    }

    public void invokeinterface(String clazz, String name, String type) throws IOException {
        out.println("\tinvokeinterface " + clazz + " " + name + " " + type);
    }

    public void invokestatic(String clazz, String name, String type) throws IOException {
        out.println("\tinvokestatic " + clazz + " " + name + " " + type);
    }

    public void getelement(String type) throws IOException {
        out.println("\tgetelement " + type);
    }

    public void putelement(String type) throws IOException {
        out.println("\tputelement " + type);
    }

    public void iinc(LocalVariable var, int n) throws IOException {
        var.typeCheck("I");
        out.println("\tiinc " + var + " " + n);
    }

    public void newobject(String clazz) throws IOException {
        out.println("\tnewobject " + clazz);
    }

    public void newarray(String elemType) throws IOException {
        out.println("\tnewarray " + elemType);
    }

    public void newmultiarray(String arrType, int depth) throws IOException {
        out.println("\tnewmultiarray " + arrType + " " + depth);
    }

    public void arraylength() throws IOException {
        out.println("\tarraylength");
    }

    public void monitorenter() throws IOException {
        out.println("\tmonitorenter");
    }

    public void monitorexit() throws IOException {
        out.println("\tmonitorexit");
    }

    public AbstractMark newMark() throws IOException {
        return new MyMark(markCounter++);
    }

    public void mark(AbstractMark m) throws IOException {
        mark(m, false);
    }

    public void mark(AbstractMark m, boolean mayBeUnreachable) throws IOException {
        if (!mayBeUnreachable)
            reachable = true;
        out.print(m + ":");
    }

    public void mark(AbstractMark m, int stack) throws IOException {
        out.print(m + ":");
    }

    public void jump(AbstractMark m) throws IOException {
        reachable = false;
        out.println("\tjump " + m);
    }

    public void jumpIfNot(boolean cmpToZero, int op, String type,
                          AbstractMark target) throws IOException {
        jumpIf(false, cmpToZero, op, type, target);
    }

    public void jumpIf(boolean cmpToZero, int op, String type,
                       AbstractMark target) throws IOException {
        jumpIf(true, cmpToZero, op, type, target);
    }

    public void jumpIf(boolean ifTrue, boolean cmpToZero, int op, String type,
                       AbstractMark target) throws IOException {
        String code = null;
        switch (op) {
            case TOKEN_EE:
                code = "==";
                break;
            case TOKEN_NE:
                code = "!=";
                break;
            case TOKEN_GE:
                code = ">=";
                break;
            case TOKEN_LE:
                code = "<=";
                break;
        }
        if (ifTrue)
            out.print("\tjumpIf ");
        else
            out.print("\tjumpIfNot ");
        if (code != null)
            out.print(code);
        else
            out.print((char) op);
        out.print(cmpToZero ? "0 " : "_ ");
        out.println(target + " " + type);
    }

    public void compare(boolean cmpToZero, String type, boolean gFlag) throws IOException {
        out.println("\tcompare " + (cmpToZero ? "0 " : "_ ") + gFlag + " " + type);
    }

    public void retrn(String type) throws IOException {
        reachable = false;
        out.println("\tretrn " + type);
    }

    public void jsr(AbstractMark mark, int stackChange) throws IOException {
        out.println("\tjsr " + mark + " " + stackChange);
    }

    public void ret(LocalVariable var) throws IOException {
        var.typeCheck("L");
        ret(var.getIndex());
    }

    public void ret(int var) throws IOException {
        reachable = false;
        out.println("\tret " + var);
    }

    public void athrow() throws IOException {
        reachable = false;
        out.println("\tathrow");
    }

    public void lookupswitch(int[] items, AbstractMark[] marks, AbstractMark defaultMark)
            throws IOException {
        out.println("\tlookupswitch");
        for (int i = 0; i < items.length; i++)
            out.println("\t  .case " + items[i] + " : " + marks[i]);
        out.println("\t  .default: " + defaultMark);
    }

    public void tableswitch(int low, AbstractMark[] marks, AbstractMark defaultMark)
            throws IOException {
        out.println("\ttableswitch");
        for (int i = 0; i < marks.length; i++)
            out.println("\t  .case " + (low + i) + " : " + marks[i]);
        out.println("\t  .default: " + defaultMark);
    }

    public void cast(String fromType, String toType) throws IOException {
        out.println("\tcast " + fromType + " " + toType);
    }

    public void instnceof(String clazz) throws IOException {
        out.println("\tinstanceof " + clazz);
    }

    public void pop(String type, int nelem) throws IOException {
        out.println("\tpop " + type + " " + nelem);
    }

    public void swap() throws IOException {
        out.println("\tswap");
    }

    public void dup(String type) throws IOException {
        out.println("\tdup " + type);
    }

    public void insert(String what, String under) throws IOException {
        out.println("\tinsert " + what + " " + under);
    }

    public void op(int op, String type) throws IOException {

        String code = null;

        switch (op) {
            case UNARY_MINUS:
                code = "NEG";
                break;
            case TOKEN_GG:
                code = ">>";
                break;
            case TOKEN_GGG:
                code = ">>>";
                break;
            case TOKEN_LL:
                code = "<<";
                break;
        }

        if (code != null)
            out.println("\top " + code + " " + type);
        else
            out.println("\top " + (char) op + " " + type);
    }

    public boolean isReachable() {
        return reachable;
    }

    public int getStackDepth() {
        return 0; // can return 0 if not checking
    }

    public void close() throws IOException {
        out.close();
    }

    static class MyMark extends AbstractMark {

        String l;

        MyMark(int count) {
            l = "L" + count;
        }

        public String toString() {
            return l;
        }
    }

}

