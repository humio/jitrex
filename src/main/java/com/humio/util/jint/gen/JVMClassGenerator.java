/*
    Copyright (c) Peter Sorotokin, 1998-2000
    Copyright (c) Humio, 2019
    See file "LICENSE.md" for terms of usage and
    redistribution.   
*/
package com.humio.util.jint.gen;

import com.humio.util.jint.constants.DefinitionConst;
import com.humio.util.jint.constants.TokenConst;

import java.io.*;
import java.util.Enumeration;
import java.util.Stack;
import java.util.Vector;

public class JVMClassGenerator extends CodeGenerator implements JVMCodes {

    static final byte[] magic = {(byte) 0xCA, (byte) 0xFE, (byte) 0xBA, (byte) 0xBE,
            (byte) 0x00, (byte) 0x03, (byte) 0x00, (byte) 0x2D};
    static final Integer I_ONE = 1;
    static final Integer I_ZERO = 0;
    static final Long L_ONE = 1L;
    static final Long L_ZERO = 0L;
    static final Float F_ONE = 1f;
    static final Float F_ZERO = (float) 0;

    //------------------------------- interfaces
    static final Double D_ONE = 1d;
    static final Double D_ZERO = (double) 0;
    // setting this to true involves a big perfomance penalty
    public static boolean keepCaller = false;

    //------------------------------ fields
    short accessFlags;
    JVMConstantPool constantPool = new JVMConstantPool();
    JVMConstantPool.CP_Class_Entry thisClass;
    JVMConstantPool.CP_Class_Entry superClass;

    //---------------------------- common goodies
    JVMConstantPool.CP_Class_Entry[] interfaces;
    Vector<FieldEntry> fields;
    JVMConstantPool.CP_UTF8_Entry cpJintType;
    JVMConstantPool.CP_UTF8_Entry cpConstantValue;
    JVMConstantPool.CP_UTF8_Entry cpExceptions;
    JVMConstantPool.CP_UTF8_Entry cpCode;

    //------------------------------ methods
    JVMConstantPool.CP_UTF8_Entry cpLineNumberTable;
    Vector<MethodEntry> methods;
    MethodEntry currentMethod;
    Vector<HandlerEntryTmp> handlers;
    Vector<LineNumber> lineNumberAcc;
    DataOutputStream codeAcc;
    ByteArrayOutputStream codeAccInt = new ByteArrayOutputStream();
    int maxStack;
    int maxLocals;
    int currStack;
    JVMConstantPool.CP_UTF8_Entry cpSourceFile;
    JVMConstantPool.CP_UTF8_Entry cpSourceFileName;
    Stack<TuneJump> toTune = new Stack<>();

    public JVMClassGenerator(int flags, String name, String superName) {
        accessFlags = (short) (flags | DefinitionConst.ACC_SUPER);
        thisClass = constantPool.lookupClass(name);
        superClass = constantPool.lookupClass(superName);
    }

    static void writeSimpleAttribute(DataOutputStream out, short name, short value)
            throws IOException {
        out.writeShort(name);
        out.writeInt(2);
        out.writeShort(value);
    }

    static String makeJVMDescriptorFromJintDescriptor(boolean field, String type) {
        StringBuffer sb = new StringBuffer();
        int i;
        boolean notDone;
        if (field) {
            i = 0;
            notDone = false;
        } else {
            if (type.charAt(0) != '(')
                throw new RuntimeException("Malformed method descriptor: " + type);
            sb.append('(');
            notDone = true;
            i = 1;
        }

        LOOP:
        do {
            char c = type.charAt(i);
            if (c == ')') {
                notDone = false;
                sb.append(')');
                c = type.charAt(++i);
            }
            switch (c) {
                case 'V':
                case 'I':
                case 'S':
                case 'Z':
                case 'B':
                case 'C':
                case 'F':
                case 'D':
                case 'J':
                    sb.append(c);
                    break;
                case 'A':
                    sb.append("Ljava/lang/Object;");
                    break;
                case 'L': {
                    int k = type.indexOf(';', i) + 1;
                    if (k <= 0)
                        throw new RuntimeException("Malformed method descriptor: " + type);
                    sb.append(type, i, k);
                    i = k;
                }
                continue;
                case '[': {
                    sb.append('[');
                    while (true) {
                        if (++i >= type.length())
                            throw new RuntimeException("Malformed method descriptor: " + type);
                        if (type.charAt(i) == '[')
                            sb.append('[');
                        else
                            break;
                    }
                    c = type.charAt(i);
                    if (c == 'L') {
                        int k = type.indexOf(';', i) + 1;
                        if (k > 0) {
                            sb.append(type, i, k);
                            i = k;
                            continue;
                        }
                    } else {
                        if (c == 'A')
                            sb.append("Ljava/lang/Object;");
                        else
                            sb.append(c);
                        break;
                    }
                }
                // fall thru
                default:
                    throw new RuntimeException("Malformed method descriptor: " + type);
            }
            i++;
        }
        while (notDone);
        return sb.toString();
    }

    //------------------------------ class attributes

    static int getArgSize(String type) {
        if (type.charAt(0) != '(')
            throw new RuntimeException("Malformed method descriptor: " + type);
        int i = 1;
        int nWords = 0;
        LOOP:
        while (true) {
            char c;
            try {
                c = type.charAt(i++);
            } catch (StringIndexOutOfBoundsException e) {
                throw new RuntimeException("Malformed method descriptor: " +
                        type + ": " + e.getMessage());
            }
            switch (c) {
                case ')':
                    break LOOP;
                case 'I':
                case 'S':
                case 'Z':
                case 'B':
                case 'C':
                case 'F':
                case 'A':
                    nWords += 1;
                    break;
                case 'D':
                case 'J':
                    nWords += 2;
                    break;
                case 'L':
                    nWords += 1;
                    i = type.indexOf(';', i) + 1;
                    if (i <= 0)
                        throw new RuntimeException("Malformed method descriptor: " + type);
                    break;
                case '[':
                    nWords += 1;
                    while (true) {
                        if (i >= type.length())
                            throw new RuntimeException("Malformed method descriptor: " + type);
                        if (type.charAt(i) == '[')
                            i++;
                        else
                            break;
                    }
                    if (type.charAt(i) == 'L') {
                        i = type.indexOf(';', i) + 1;
                        if (i > 0)
                            break;
                    } else {
                        i++;
                        break;
                    }
                    // fall thru
                default:
                    throw new RuntimeException("Malformed method descriptor: " + type);
            }
        }
        return nWords;
    }

    public static void main(String[] args) {
        JVMClassGenerator gen = new JVMClassGenerator(DefinitionConst.ACC_PUBLIC, "AAA", "java/lang/Object");
        gen.setSourceFile("BBB.java");
        String[] interf = {"java/lang/Cloneable", "java/io/Serializable"};
        gen.setInterfaces(interf);
        gen.addField(DefinitionConst.ACC_PUBLIC, "intField", "I", null);
        try {
            gen.startMethod(DefinitionConst.ACC_PUBLIC, "<init>", "()V", null);

            gen.load(0, "L");
            gen.invokespecial("java/lang/Object", "<init>", "()V");

            gen.load(0, "L");
            gen.loadConst(1456789);
            gen.putfield("AAA", "intField", "I");

            gen.loadConst(10);
            gen.store(1, "I");

            AbstractMark m = gen.newMark();
            gen.mark(m);

            gen.getstatic("java/lang/System", "out", "Ljava/io/PrintStream;");
            gen.loadConst("Hello, world!");
            gen.invokevirtual("java/io/PrintStream", "println", "(Ljava/lang/String;)V");

            gen.iinc(1, -1);
            gen.load(1, "I");
            gen.jumpIfNot(true, TokenConst.TOKEN_EE, "I", m);

            gen.retrn("V");
            gen.endMethod();


            gen.startMethod(DefinitionConst.ACC_PUBLIC, "toString", "()Ljava/lang/String;", null);

            gen.loadConst("This is AAA!!!");
            gen.retrn("Ljava/lang/String;");

            gen.endMethod();

            BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream("AAA.class"));
            DataOutputStream dout = new DataOutputStream(out);
            gen.writeTo(dout);
            dout.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setInterfaces(String[] interfaceNames) {
        if (interfaceNames == null)
            interfaces = null;
        else {
            interfaces = new JVMConstantPool.CP_Class_Entry[interfaceNames.length];
            for (int i = 0; i < interfaces.length; i++)
                interfaces[i] = constantPool.lookupClass(interfaceNames[i]);
        }
    }

    public void writeInterfaces(DataOutputStream out) throws IOException {
        if (interfaces == null) {
            out.writeShort(0);
        } else {
            out.writeShort(interfaces.length);
            for (int i = 0; i < interfaces.length; i++)
                out.writeShort(interfaces[i].count);
        }
    }

    //--------------------------------

    /**
     * Add a field to the class.
     *
     * @param accessFlags access flags (ACC_*) for this field, ored together
     * @param name        field name
     * @param type        type <b>descriptor</b> (not class name)
     * @param value       initial value, null for nothing, short numerical
     *                    types represented by Integer
     * @param jintType    if not null gives JintType attribute value
     */
    public void addField(int accessFlags, String name, String jintType, Object value) {
        FieldEntry fe = new FieldEntry();
        String type = makeJVMDescriptorFromJintDescriptor(true, jintType);
        if ((this.accessFlags & DefinitionConst.ACC_INTERFACE) != 0)
            accessFlags |= DefinitionConst.ACC_PUBLIC;
        fe.flags = (short) accessFlags;
        fe.name = constantPool.lookupUTF8(name);
        fe.type = constantPool.lookupUTF8(type);
        if (value != null) {
            if (cpConstantValue == null)
                cpConstantValue = constantPool.lookupUTF8("ConstantValue");
            switch (type.charAt(0)) {
                case 'Z':
                    if (value instanceof Boolean) {
                        fe.init = constantPool.lookupNumber(
                                ((Boolean) value).booleanValue() ? 1 : 0);
                        break;
                    }
                    // fall through
                case 'B':
                case 'C':
                case 'I':
                case 'S':
                    fe.init = constantPool.lookupNumber(
                            ((Number) value).intValue());
                    break;
                case 'L':
                    fe.init = constantPool.lookupString((String) value);
                    break;
                case 'J':
                    fe.init = constantPool.lookupNumber((Long) value);
                    break;
                case 'D':
                    fe.init = constantPool.lookupNumber((Double) value);
                    break;
                default:
                    throw new RuntimeException("Value of type " + type + " not implemented");
            }
        }
        if (jintType != null) {
            if (cpJintType == null)
                cpJintType = constantPool.lookupUTF8("JintType");
            fe.jintType = constantPool.lookupUTF8(jintType);
        }
        if (fields == null)
            fields = new Vector<>();
        fields.addElement(fe);
    }

    //----------------------- assembler -------------

    public void writeFields(DataOutputStream out) throws IOException {
        if (fields == null) {
            out.writeShort(0);
        } else {
            FieldEntry[] feArr = new FieldEntry[fields.size()];
            fields.copyInto(feArr);
            out.writeShort(feArr.length);
            for (int i = 0; i < feArr.length; i++) {
                FieldEntry fe = feArr[i];
                out.writeShort(fe.flags);
                out.writeShort(fe.name.count);
                out.writeShort(fe.type.count);
                int nattr = (fe.init == null ? 0 : 1) +
                        (fe.jintType == null ? 0 : 1);
                out.writeShort(nattr);
                if (fe.init != null)
                    writeSimpleAttribute(out, cpConstantValue.count, fe.init.count);
                if (fe.jintType != null)
                    writeSimpleAttribute(out, cpJintType.count, fe.jintType.count);
            }
        }
    }

    public void startMethod(int accessFlags, String name, String jintType,
                            String[] exceptions) {
        if (currentMethod != null)
            throw new RuntimeException("Too many startMethod calls");
        String type = makeJVMDescriptorFromJintDescriptor(false, jintType);
        if ((this.accessFlags & DefinitionConst.ACC_INTERFACE) != 0)
            accessFlags |= DefinitionConst.ACC_PUBLIC;
        currentMethod = new MethodEntry();
        currentMethod.flags = (short) accessFlags;
        currentMethod.name = constantPool.lookupUTF8(name);
        currentMethod.type = constantPool.lookupUTF8(type);
        if (jintType != null) {
            if (cpJintType == null)
                cpJintType = constantPool.lookupUTF8("JintType");
            currentMethod.jintType = constantPool.lookupUTF8(jintType);
        }
        if (exceptions != null) {
            currentMethod.exceptions = new JVMConstantPool.CP_Class_Entry[exceptions.length];
            for (int i = 0; i < exceptions.length; i++)
                currentMethod.exceptions[i] = constantPool.lookupClass(exceptions[i]);
            if (cpExceptions == null)
                cpExceptions = constantPool.lookupUTF8("Exceptions");
        }
        codeAcc = new DataOutputStream(codeAccInt);
        maxStack = 0;
        maxLocals = ((accessFlags & DefinitionConst.ACC_STATIC) == 0 ? 1 : 0) + getArgSize(type);
        if ((accessFlags & (DefinitionConst.ACC_NATIVE | DefinitionConst.ACC_ABSTRACT)) != 0)
            currStack = -1;
        else
            currStack = 0;
    }

    public void endMethod() {
        if (currStack >= 0)
            throw new RuntimeException("Return missed at the end of the method body");
        if (currentMethod == null)
            throw new RuntimeException("Too many endMethod calls");

        if (lineNumberAcc != null) {
            currentMethod.lineNumbers = new LineNumber[lineNumberAcc.size()];
            lineNumberAcc.copyInto(currentMethod.lineNumbers);
            lineNumberAcc = null;
            if (cpLineNumberTable == null)
                cpLineNumberTable = constantPool.lookupUTF8("LineNumberTable");
        }

        try {
            codeAcc.flush();
        } catch (IOException e) {
        }
        if ((currentMethod.flags & (DefinitionConst.ACC_NATIVE | DefinitionConst.ACC_ABSTRACT)) == 0) {
            currentMethod.code = codeAccInt.toByteArray();
            if (currentMethod.code.length == 0)
                currentMethod.code = null;
            else {
                tuneJumps(currentMethod.code);
                if (maxLocals < 0 || maxLocals > 0xFFFF)
                    throw new RuntimeException("Too many local variables");
                if (maxStack < 0 || maxStack > 0xFFFF)
                    throw new RuntimeException("Too large stack requested");
                currentMethod.maxStack = (short) maxStack;
                currentMethod.maxLocals = (short) maxLocals;
	    /*
	       System.out.println( "Method " + currentMethod.name.data + ": vars = " +
	       maxLocals + " stack = " + maxStack );
	       */
                if (cpCode == null)
                    cpCode = constantPool.lookupUTF8("Code");
            }
        }
        codeAccInt.reset();
        if (handlers != null && handlers.size() > 0) {
            currentMethod.handlers = new HandlerEntry[handlers.size()];
            Enumeration<HandlerEntryTmp> elems = handlers.elements();
            for (int i = 0; elems.hasMoreElements(); i++) {
                HandlerEntry en = new HandlerEntry();
                currentMethod.handlers[i] = en;
                HandlerEntryTmp t = elems.nextElement();
                en.start = t.start;
                en.end = t.end;
                if (t.handler.pc < 0)
                    throw new RuntimeException("Handler label was never marked");
                en.handler = (short) t.handler.pc;
                en.clazz = t.clazz;
            }
            handlers.removeAllElements();
        }
        if (methods == null)
            methods = new Vector<>();
        methods.addElement(currentMethod);
        currentMethod = null;
    }

    public void writeMethods(DataOutputStream out) throws IOException {
        if (methods == null)
            out.writeShort(0);
        else {
            MethodEntry[] meArr = new MethodEntry[methods.size()];
            methods.copyInto(meArr);
            out.writeShort(meArr.length);
            for (int i = 0; i < meArr.length; i++) {
                MethodEntry me = meArr[i];
                out.writeShort(me.flags);
                out.writeShort(me.name.count);
                out.writeShort(me.type.count);
                int nattr = (me.code == null ? 0 : 1) +
                        (me.jintType == null ? 0 : 1) +
                        (me.exceptions == null ? 0 : 1);
                out.writeShort(nattr);
                if (me.exceptions != null) {
                    out.writeShort(cpExceptions.count);
                    out.writeInt(me.exceptions.length * 2 + 2);
                    out.writeShort(me.exceptions.length);
                    for (int k = 0; k < me.exceptions.length; k++)
                        out.writeShort(me.exceptions[k].count);
                }
                if (me.code != null) {
                    out.writeShort(cpCode.count);
                    int nbytes = (me.handlers == null ? 0 : me.handlers.length * 8) +
                            me.code.length + 12 + (me.lineNumbers == null ? 0 : me.lineNumbers.length * 4 + 8);
                    out.writeInt(nbytes);
                    out.writeShort(me.maxStack);
                    out.writeShort(me.maxLocals);
                    out.writeInt(me.code.length);
                    out.write(me.code);
                    int nHandlers = (me.handlers == null ? 0 : me.handlers.length);
                    out.writeShort(nHandlers);
                    for (int k = 0; k < nHandlers; k++) {
                        HandlerEntry he = me.handlers[k];
                        out.writeShort(he.start);
                        out.writeShort(he.end);
                        out.writeShort(he.handler);
                        if (he.clazz == null)
                            out.writeShort(0);
                        else
                            out.writeShort(he.clazz.count);
                    }
                    LineNumber[] lineNumbers = me.lineNumbers;
                    if (lineNumbers == null)
                        out.writeShort(0);
                    else {
                        out.writeShort(1);
                        out.writeShort(cpLineNumberTable.count);
                        out.writeInt(4 * lineNumbers.length + 2);
                        out.writeShort(lineNumbers.length);
                        for (int j = 0; j < lineNumbers.length; j++) {
                            out.writeShort(lineNumbers[j].pc);
                            out.writeShort(lineNumbers[j].line);
                        }
                    }
                }
                if (me.jintType != null)
                    writeSimpleAttribute(out, cpJintType.count, me.jintType.count);
            }
        }
    }

    public void setSourceFile(String file) {
        cpSourceFile = constantPool.lookupUTF8("SourceFile");
        cpSourceFileName = constantPool.lookupUTF8(file);
    }

    public void writeAttributes(DataOutputStream out) throws IOException {
        if (cpSourceFileName != null) {
            out.writeShort(1);
            out.writeShort(cpSourceFile.count);
            out.writeInt(2);
            out.writeShort(cpSourceFileName.count);
        } else
            out.writeShort(0);
    }

    public void writeTo(DataOutputStream out) throws IOException {
        out.write(magic);
        constantPool.writeConstantPool(out);
        out.writeShort(accessFlags);
        out.writeShort(thisClass.count);
        out.writeShort(superClass.count);
        writeInterfaces(out);
        writeFields(out);
        writeMethods(out);
        writeAttributes(out);
        out.close();
    }

    void stackPush(int n) {
        if (currStack < 0)
            throw new RuntimeException("Undefined stack state: probably code unreachable");
        currStack += n;
        if (currStack > maxStack)
            maxStack = currStack;
    }

    void stackPop(int n) {
        if (currStack < 0)
            throw new RuntimeException("Undefined stack state: probably code unreachable");
        currStack -= n;
        if (currStack < 0) {
            currStack = 0;
            throw new RuntimeException("Stack underflow");
        }
    }

    int stackPopArgsPushResult(String type) {
        int nWords = getArgSize(type);
        stackPop(nWords);
        int i = type.indexOf(")");
        if (i < 0)
            throw new RuntimeException("Malformed method descriptor: " + type);
        i++;
        switch (type.charAt(i)) {
            case 'V':
                break;
            case 'I':
            case 'S':
            case 'Z':
            case 'B':
            case 'C':
            case 'F':
            case 'L':
            case 'A':
            case '[':
                stackPush(1);
                break;
            case 'J':
            case 'D':
                stackPush(2);
                break;
            default:
                throw new RuntimeException("Malformed method descriptor: " + type);
        }
        return nWords;
    }

    int getPC() throws IOException {
        codeAcc.flush();
        return codeAccInt.size();
    }

    void addTuneJump(int instrPC, int storePC, Label target) {
        toTune.push(new TuneJump(instrPC, storePC, target));
    }

    void tuneJumps(byte[] code) {
        while (toTune.size() > 0) {
            TuneJump tj = toTune.pop();
            if (tj.target.pc < 0)
                throw new RuntimeException("Mark was never set, but is being jumped on at PC = "
                        + tj.instrPC + " [" + tj.target.caller + "]");
            int offset = tj.target.pc - tj.instrPC;
            code[tj.storePC] = (byte) (offset >> 8);
            code[tj.storePC + 1] = (byte) offset;
        }
    }

    void writeLabel(int instrPC, AbstractMark m) throws IOException {
        Label target = (Label) m;
        int offset;
        if (target.pc < 0) {
            offset = 0;
            addTuneJump(instrPC, getPC(), target);
            target.stackSize = currStack;
        } else {
            offset = target.pc - instrPC;
            if (currStack != target.stackSize)
                throw new RuntimeException("Target stack size is different from current");
        }
        codeAcc.writeShort(offset);
    }

    public void addExceptionHandler(AbstractMark from, AbstractMark to,
                                    AbstractMark jump, String exceptionClass) {
        HandlerEntryTmp t = new HandlerEntryTmp();
        int pc = ((Label) from).pc;
        if (pc < 0)
            throw new RuntimeException("addExceptionHandler: from is not marked!");
        t.start = (short) pc;
        pc = ((Label) to).pc;
        if (pc < 0)
            throw new RuntimeException("addExceptionHandler: from is not marked!");
        t.end = (short) pc;
        Label l = (Label) jump;
        t.handler = l;
        if (l.pc >= 0 && l.stackSize != 1)
            throw new RuntimeException(
                    "Exception handler points to the code were stack size is not 1");
        l.stackSize = 1;
        t.clazz = constantPool.lookupClass(exceptionClass);
        if (handlers == null)
            handlers = new Vector<>();
        handlers.addElement(t);
    }

    public void linenumber(int line) throws IOException {
        if (lineNumberAcc == null)
            lineNumberAcc = new Vector<LineNumber>();
        else {
            LineNumber prev = lineNumberAcc.elementAt(lineNumberAcc.size() - 1);
            if (prev.line == line)
                return;
        }
        int pc = getPC();
        LineNumber n = new LineNumber(pc, line);
        lineNumberAcc.addElement(n);
    }

    public void loadConst(Object v) throws IOException {
        stackPush(1);
        if (v == null) {
            codeAcc.write(aconst_null);
        } else {
            if (v instanceof Character)
                v = (int) ((Character) v).charValue();
            else if (v instanceof Boolean)
                v = (((Boolean) v).booleanValue() ? I_ONE : I_ZERO);

            JVMConstantPool.CP_Entry cp;
            boolean wideFlag = false;

            if (v instanceof Number) {
                Number n = (Number) v;
                if (n instanceof Integer || n instanceof Short ||
                        n instanceof Byte) {
                    int i = n.intValue();
                    if (-1 <= i && i <= 5) {
                        codeAcc.write(iconst_0 + i);
                        return;
                    } else if (Byte.MIN_VALUE <= i && i <= Byte.MAX_VALUE) {
                        codeAcc.write(bipush);
                        codeAcc.write(i);
                        return;
                    } else if (Short.MIN_VALUE <= i && i <= Short.MAX_VALUE) {
                        codeAcc.write(sipush);
                        codeAcc.writeShort(i);
                        return;
                    }
                } else if (n instanceof Float) {
                    float f = n.floatValue();
                    if (f == (float) 0.0 || f == (float) 1.0 || f == (float) 2.0) {
                        codeAcc.write(fconst_0 + (int) f);
                        return;
                    }
                    if (f == (float) (-1.0) || f == (float) (-2.0)) {
                        codeAcc.write(fconst_0 - (int) f);
                        codeAcc.write(fneg);
                        return;
                    }
                } else if (n instanceof Double) {
                    double d = n.doubleValue();
                    stackPush(1); // additional word
                    if (d == 0.0 || d == 1.0) {
                        codeAcc.write(dconst_0 + (int) d);
                        return;
                    }
                    if (d == -1.0) {
                        codeAcc.write(dconst_1);
                        codeAcc.write(dneg);
                        return;
                    }
                    wideFlag = true;
                } else if (n instanceof Long) {
                    long l = n.longValue();
                    stackPush(1); // additional word
                    if (l == 0 || l == 1) {
                        codeAcc.write(lconst_0 + (int) l);
                        return;
                    }
                    if (l == -1) {
                        codeAcc.write(lconst_1);
                        codeAcc.write(lneg);
                        return;
                    }
                    wideFlag = true;
                } else
                    throw new RuntimeException("Unexpected number type " + n.getClass().getName());
                cp = constantPool.lookupNumber(n);
            } else if (v instanceof String)
                cp = constantPool.lookupString((String) v);
            else
                throw new RuntimeException("Cannot insert constant of type " +
                        v.getClass().getName());
            if (wideFlag) {
                codeAcc.write(ldc2_w);
                codeAcc.writeShort(cp.count);
            } else if (cp.count <= 0xFF) {
                codeAcc.write(ldc);
                codeAcc.write(cp.count);
            } else {
                codeAcc.write(ldc_w);
                codeAcc.writeShort(cp.count);
            }
        }
    }

    public void getfield(String clazz, String name, String type) throws IOException {
        type = makeJVMDescriptorFromJintDescriptor(true, type);
        JVMConstantPool.CP_MemberRef_Entry entry = constantPool.lookupMemberRef(TAG_FieldRef, clazz, name, type);
        char c = type.charAt(0);
        stackPop(1); // separate pop to check for stack underflow
        stackPush(c == 'J' || c == 'D' ? 2 : 1);
        codeAcc.write(getfield);
        codeAcc.writeShort(entry.count);
    }

    public void getstatic(String clazz, String name, String type) throws IOException {
        type = makeJVMDescriptorFromJintDescriptor(true, type);
        JVMConstantPool.CP_MemberRef_Entry entry = constantPool.lookupMemberRef(TAG_FieldRef, clazz, name, type);
        char c = type.charAt(0);
        stackPush(c == 'J' || c == 'D' ? 2 : 1);
        codeAcc.write(getstatic);
        codeAcc.writeShort(entry.count);
    }

    public void putfield(String clazz, String name, String type) throws IOException {
        type = makeJVMDescriptorFromJintDescriptor(true, type);
        JVMConstantPool.CP_MemberRef_Entry entry = constantPool.lookupMemberRef(TAG_FieldRef, clazz, name, type);
        char c = type.charAt(0);
        stackPop(c == 'J' || c == 'D' ? 3 : 2);
        codeAcc.write(putfield);
        codeAcc.writeShort(entry.count);
    }

    //-------------- assembler goodies

    public void putstatic(String clazz, String name, String type) throws IOException {
        type = makeJVMDescriptorFromJintDescriptor(true, type);
        JVMConstantPool.CP_MemberRef_Entry entry = constantPool.lookupMemberRef(TAG_FieldRef, clazz, name, type);
        char c = type.charAt(0);
        stackPop(c == 'J' || c == 'D' ? 2 : 1);
        codeAcc.write(putstatic);
        codeAcc.writeShort(entry.count);
    }

    public void store(int var, String type) throws IOException {
        int size;
        int store;
        int store_0;
        switch (type.charAt(0)) {
            case 'I':
            case 'S':
            case 'Z':
            case 'B':
            case 'C':
                store = istore;
                store_0 = istore_0;
                size = 1;
                break;
            case 'D':
                store = dstore;
                store_0 = dstore_0;
                size = 2;
                break;
            case 'J':
                store = lstore;
                store_0 = lstore_0;
                size = 2;
                break;
            case 'F':
                store = fstore;
                store_0 = fstore_0;
                size = 1;
                break;
            case 'L':
            case '[':
            case 'A':
                store = astore;
                store_0 = astore_0;
                size = 1;
                break;
            default:
                throw new RuntimeException("Malformed variable descriptor: " + type);
        }

        if (var <= 3)
            codeAcc.write(store_0 + var);
        else if (var <= 255) {
            codeAcc.write(store);
            codeAcc.write(var);
        } else {
            codeAcc.write(wide);
            codeAcc.write(store);
            codeAcc.writeShort(var);
        }

        stackPop(size);

        int mv = var + size;
        if (mv > maxLocals)
            maxLocals = mv;
    }

    public void load(int var, String type) throws IOException {

        if (var < 0)
            throw new RuntimeException("Negative register: " + var);

        int size;
        int load;
        int load_0;
        switch (type.charAt(0)) {
            case 'I':
            case 'S':
            case 'Z':
            case 'B':
            case 'C':
                load = iload;
                load_0 = iload_0;
                size = 1;
                break;
            case 'D':
                load = dload;
                load_0 = dload_0;
                size = 2;
                break;
            case 'J':
                load = lload;
                load_0 = lload_0;
                size = 2;
                break;
            case 'F':
                load = fload;
                load_0 = fload_0;
                size = 1;
                break;
            case 'L':
            case '[':
            case 'A':
                load = aload;
                load_0 = aload_0;
                size = 1;
                break;
            default:
                throw new RuntimeException("Malformed variable descriptor: " + type);
        }

        if (var <= 3)
            codeAcc.write(load_0 + var);
        else if (var <= 255) {
            codeAcc.write(load);
            codeAcc.write(var);
        } else {
            codeAcc.write(wide);
            codeAcc.write(load);
            codeAcc.writeShort(var);
        }
        stackPush(size);
    }

    public void invokevirtual(String clazz, String name, String type) throws IOException {
        type = makeJVMDescriptorFromJintDescriptor(false, type);
        JVMConstantPool.CP_MemberRef_Entry entry =
                constantPool.lookupMemberRef(TAG_MethodRef, clazz, name, type);
        stackPop(1); // take into account "this"
        stackPopArgsPushResult(type);
        codeAcc.write(invokevirtual);
        codeAcc.writeShort(entry.count);
    }

    public void invokespecial(String clazz, String name, String type) throws IOException {
        type = makeJVMDescriptorFromJintDescriptor(false, type);
        JVMConstantPool.CP_MemberRef_Entry entry =
                constantPool.lookupMemberRef(TAG_MethodRef, clazz, name, type);
        stackPop(1); // take into account "this"
        stackPopArgsPushResult(type);
        codeAcc.write(invokespecial);
        codeAcc.writeShort(entry.count);
    }

    public void invokeinterface(String clazz, String name, String type) throws IOException {
        type = makeJVMDescriptorFromJintDescriptor(false, type);
        JVMConstantPool.CP_MemberRef_Entry entry =
                constantPool.lookupMemberRef(TAG_InterfaceMethodRef, clazz, name, type);
        stackPop(1); // take into account "this"
        int n = stackPopArgsPushResult(type);
        codeAcc.write(invokeinterface);
        codeAcc.writeShort(entry.count);
        codeAcc.write(n + 1);
        codeAcc.write(0);
    }

    public void invokestatic(String clazz, String name, String type) throws IOException {
        type = makeJVMDescriptorFromJintDescriptor(false, type);
        JVMConstantPool.CP_MemberRef_Entry entry =
                constantPool.lookupMemberRef(TAG_MethodRef, clazz, name, type);
        stackPopArgsPushResult(type);
        codeAcc.write(invokestatic);
        codeAcc.writeShort(entry.count);
    }

    public void getelement(String type) throws IOException {
        int size = 1;
        switch (type.charAt(0)) {
            case 'L':
            case '[':
                codeAcc.write(aaload);
                break;
            case 'B':
            case 'Z':
                codeAcc.write(baload);
                break;
            case 'I':
                codeAcc.write(iaload);
                break;
            case 'C':
                codeAcc.write(caload);
                break;
            case 'J':
                size = 2;
                codeAcc.write(laload);
                break;
            case 'D':
                size = 2;
                codeAcc.write(daload);
                break;
            case 'F':
                codeAcc.write(faload);
                break;
            case 'S':
                codeAcc.write(saload);
                break;
            default:
                throw new RuntimeException("Unexpected type: " + type);
        }
        stackPop(2);
        stackPush(size);
    }

    public void putelement(String type) throws IOException {
        int size = 1;
        switch (type.charAt(0)) {
            case 'L':
            case '[':
                codeAcc.write(aastore);
                break;
            case 'B':
            case 'Z':
                codeAcc.write(bastore);
                break;
            case 'I':
                codeAcc.write(iastore);
                break;
            case 'C':
                codeAcc.write(castore);
                break;
            case 'J':
                size = 2;
                codeAcc.write(lastore);
                break;
            case 'D':
                size = 2;
                codeAcc.write(dastore);
                break;
            case 'F':
                codeAcc.write(fastore);
                break;
            case 'S':
                codeAcc.write(sastore);
                break;
            default:
                throw new RuntimeException("Unexpected type: " + type);
        }
        stackPop(2 + size);
    }

    public void iinc(int var, int n) throws IOException {
        if (-128 <= n && n <= 127 && var <= 255) {
            codeAcc.write(iinc);
            codeAcc.write(var);
            codeAcc.write(n);
        } else {
            codeAcc.write(wide);
            codeAcc.write(iinc);
            codeAcc.writeShort(var);
            codeAcc.writeShort(n);
        }
    }

    public void newobject(String clazz) throws IOException {
        JVMConstantPool.CP_Class_Entry entry = constantPool.lookupClass(clazz);
        codeAcc.write(newobject);
        codeAcc.writeShort(entry.count);
        stackPush(1);
    }

    public void newarray(String elemType) throws IOException {
        byte code;
        char t = elemType.charAt(0);
        stackPop(1);
        stackPush(1);
        switch (t) {
            case 'L':
            case '[': {
                if (t == 'L')
                    elemType = elemType.substring(1, elemType.length() - 1);
                JVMConstantPool.CP_Class_Entry entry = constantPool.lookupClass(elemType);
                codeAcc.write(anewarray);
                codeAcc.writeShort(entry.count);
            }
            return;
            case 'I':
                code = 10;
                break;
            case 'Z':
                code = 4;
                break;
            case 'C':
                code = 5;
                break;
            case 'F':
                code = 6;
                break;
            case 'D':
                code = 7;
                break;
            case 'B':
                code = 8;
                break;
            case 'S':
                code = 9;
                break;
            case 'J':
                code = 11;
                break;
            default:
                throw new RuntimeException("Bad type in newarray: " + elemType);
        }
        codeAcc.write(newarray);
        codeAcc.write(code);
    }

    public void newmultiarray(String arrType, int depth) throws IOException {
        stackPop(depth);
        JVMConstantPool.CP_Class_Entry entry = constantPool.lookupClass(arrType);
        codeAcc.write(multianewarray);
        codeAcc.writeShort(entry.count);
        codeAcc.write(depth);
        stackPush(1);
    }

    public void arraylength() throws IOException {
        codeAcc.write(arraylength);
    }

    public void monitorenter() throws IOException {
        stackPop(1);
        codeAcc.write(monitorenter);
    }

    public void monitorexit() throws IOException {
        stackPop(1);
        codeAcc.write(monitorexit);
    }

    public AbstractMark newMark() throws IOException {
        Label l = new Label();
        if (!keepCaller)
            l.caller = "hint: set JVMClassGenerator.keepCaller to true to debug";
        else
            try {
                Exception e = new Exception();
                StringWriter sw = new StringWriter();
                PrintWriter out = new PrintWriter(sw);
                e.printStackTrace(out);
                BufferedReader r = new BufferedReader(new StringReader(sw.toString()));
                String ln = r.readLine();
                if (ln != null) {
                    ln = r.readLine();
                    if (ln != null) {
                        ln = r.readLine();
                        if (ln != null)
                            l.caller = ln.trim();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        return l;
    }

    public void mark(AbstractMark m, int stack) throws IOException {
        if (currStack >= 0 && currStack != stack)
            throw new RuntimeException("My stack size conflicts with yours!");
        currStack = stack;
        mark(m);
    }

    public void mark(AbstractMark m, boolean mayBeUnreachable) throws IOException {
        Label label = ((Label) m);
        label.pc = getPC();
        if (currStack < 0) {
            if (label.stackSize < 0) {
                if (!mayBeUnreachable && label.stackSize == -1)
                    throw new RuntimeException("No jump on this mark and stack size unknown at PC = "
                            + label.pc);
            } else
                currStack = label.stackSize;
        } else if (label.stackSize < 0)
            label.stackSize = currStack;
        else if (label.stackSize != currStack)
            throw new RuntimeException("Current stack size is different from jump stack size, PC="
                    + label.pc + " L=" + label.stackSize + " C=" + currStack);
    }

    public void jump(AbstractMark m) throws IOException {
        if (currStack < 0) {
            Label label = ((Label) m);
            if (label.stackSize == -1)
                label.stackSize = -2; // optimized away
        } else {
            int pc = getPC();
            codeAcc.write(_goto);
            writeLabel(pc, m);
            currStack = -1;
        }
    }

    public void jumpIf(boolean ifTrue, boolean cmpToZero, int op, String type,
                       AbstractMark target) throws IOException {
        int code;
        int t = type.charAt(0);
        switch (t) {
            case 'J':
            case 'D':
            case 'F':
                compare(cmpToZero, type, (op == '<' || op == TokenConst.TOKEN_LE));
                t = 'I';
                cmpToZero = true;
                break;
        }
        switch (t) {
            case 'L':
            case '[':
                if (cmpToZero) {
                    stackPop(1);
                    switch (op) {
                        case TokenConst.TOKEN_EE:
                            code = (ifTrue ? ifnull : ifnonnull);
                            break;
                        case TokenConst.TOKEN_NE:
                            code = (ifTrue ? ifnonnull : ifnull);
                            break;
                        default:
                            throw new RuntimeException("Invalid operation for objects");
                    }
                } else {
                    stackPop(2);
                    switch (op) {
                        case TokenConst.TOKEN_EE:
                            code = (ifTrue ? if_acmpeq : if_acmpne);
                            break;
                        case TokenConst.TOKEN_NE:
                            code = (ifTrue ? if_acmpne : if_acmpeq);
                            break;
                        default:
                            throw new RuntimeException("Invalid operation for objects");
                    }
                }
                break;
            case 'B':
            case 'I':
            case 'S':
            case 'C':
            case 'Z':
                if (cmpToZero) {
                    stackPop(1);
                    switch (op) {
                        case TokenConst.TOKEN_EE:
                            code = (ifTrue ? ifeq : ifne);
                            break;
                        case TokenConst.TOKEN_NE:
                            code = (ifTrue ? ifne : ifeq);
                            break;
                        case '<':
                            code = (ifTrue ? iflt : ifge);
                            break;
                        case '>':
                            code = (ifTrue ? ifgt : ifle);
                            break;
                        case TokenConst.TOKEN_GE:
                            code = (ifTrue ? ifge : iflt);
                            break;
                        case TokenConst.TOKEN_LE:
                            code = (ifTrue ? ifle : ifgt);
                            break;
                        default:
                            throw new RuntimeException("Invalid comparison operation");
                    }
                } else {
                    stackPop(2);
                    switch (op) {
                        case TokenConst.TOKEN_EE:
                            code = (ifTrue ? if_icmpeq : if_icmpne);
                            break;
                        case TokenConst.TOKEN_NE:
                            code = (ifTrue ? if_icmpne : if_icmpeq);
                            break;
                        case '<':
                            code = (ifTrue ? if_icmplt : if_icmpge);
                            break;
                        case '>':
                            code = (ifTrue ? if_icmpgt : if_icmple);
                            break;
                        case TokenConst.TOKEN_GE:
                            code = (ifTrue ? if_icmpge : if_icmplt);
                            break;
                        case TokenConst.TOKEN_LE:
                            code = (ifTrue ? if_icmple : if_icmpgt);
                            break;
                        default:
                            throw new RuntimeException("Invalid comparison operation");
                    }
                }
                break;
            default:
                throw new RuntimeException("Unexpected type in jumpIfNot: " + type);
        }
        int pc = getPC();
        codeAcc.write(code);
        writeLabel(pc, target);
    }

    public void compare(boolean cmpToZero, String type, boolean gFlag) throws IOException {
        switch (type.charAt(0)) {
            case 'D':
                if (cmpToZero) {
                    stackPush(2);
                    codeAcc.write(dconst_0);
                }
                stackPop(4);
                codeAcc.write(gFlag ? dcmpg : dcmpl);
                stackPush(1);
                break;
            case 'F':
                if (cmpToZero) {
                    stackPush(1);
                    codeAcc.write(fconst_0);
                }
                stackPop(2);
                codeAcc.write(gFlag ? fcmpg : fcmpl);
                stackPush(1);
                break;
            case 'J':
                if (cmpToZero) {
                    stackPush(2);
                    codeAcc.write(lconst_0);
                }
                stackPop(4);
                codeAcc.write(lcmp);
                stackPush(1);
                break;
            default:
                throw new RuntimeException("Bad type for compare: " + type);
        }
    }

    public void retrn(String type) throws IOException {
        int code;
        int size;
        switch (type.charAt(0)) {
            case 'I':
            case 'B':
            case 'Z':
            case 'S':
            case 'C':
                size = 1;
                code = ireturn;
                break;
            case 'F':
                size = 1;
                code = freturn;
                break;
            case 'J':
                size = 2;
                code = lreturn;
                break;
            case 'L':
            case '[':
            case 'A':
                size = 1;
                code = areturn;
                break;
            case 'D':
                size = 2;
                code = dreturn;
                break;
            case 'V':
                size = 0;
                code = vreturn;
                break;
            default:
                throw new RuntimeException("Bad type for return: " + type);
        }
        stackPop(size);
        codeAcc.write(code);
        currStack = -1;
    }

    public void jsr(AbstractMark mark, int stackChange) throws IOException {
        Label label = (Label) mark;
        int pc = getPC();
        codeAcc.write(jsr);
        stackPush(1); // return address
        writeLabel(pc, label);
        stackPop(1);
        if (stackChange >= 0)
            stackPush(stackChange);
        else
            stackPop(-stackChange);
    }

    public void ret(int var) throws IOException {
        if (var <= 255) {
            codeAcc.write(ret);
            codeAcc.write(var);
        } else {
            codeAcc.write(wide);
            codeAcc.write(ret);
            codeAcc.writeShort(var);
        }
        currStack = -1;
    }

    public void athrow() throws IOException {
        codeAcc.write(athrow);
        currStack = -1;
    }

    public void lookupswitch(int[] items, AbstractMark[] marks, AbstractMark defaultMark)
            throws IOException {

        // verify that items are sorted
        for (int i = 1; i < items.length; i++)
            if (items[i - 1] >= items[i]) {
                if (items[i - 1] == items[i])
                    throw new IllegalArgumentException("Duplicate case in lookupswitch: " + items[i]);
                else
                    throw new IllegalArgumentException("Unsorted constant list in lookupswitch");
            }

        stackPop(1);
        int pc = getPC();
        codeAcc.write(lookupswitch);
        int padding = 3 - (pc & 0x3);
        while (--padding >= 0)
            codeAcc.write(0);

        // if backref, fill with 0xFFFF
        codeAcc.writeShort(((Label) defaultMark).pc >= 0 ? 0xFFFF : 0);
        writeLabel(pc, defaultMark);

        codeAcc.writeInt(items.length);

        for (int i = 0; i < items.length; i++) {
            codeAcc.writeInt(items[i]);
            // if backref, fill with 0xFFFF, all backrefs are already defined,
            // forward references still have pc == -1
            codeAcc.writeShort(((Label) marks[i]).pc >= 0 ? 0xFFFF : 0);
            writeLabel(pc, marks[i]);
        }
    }

    public void tableswitch(int low, AbstractMark[] marks, AbstractMark defaultMark)
            throws IOException {
        stackPop(1);
        int pc = getPC();
        codeAcc.write(tableswitch);
        int padding = 3 - (pc & 0x3);
        while (--padding >= 0)
            codeAcc.write(0);

        // if backref, fill with 0xFFFF
        codeAcc.writeShort(((Label) defaultMark).pc >= 0 ? 0xFFFF : 0);
        writeLabel(pc, defaultMark);

        codeAcc.writeInt(low);
        codeAcc.writeInt(low + marks.length - 1); // high

        for (int i = 0; i < marks.length; i++) {
            // if backref, fill with 0xFFFF
            codeAcc.writeShort(((Label) marks[i]).pc >= 0 ? 0xFFFF : 0);
            writeLabel(pc, marks[i]);
        }
    }

    public void cast(String fromType, String toType) throws IOException {
        int code = nop;
        int popSize = 0;
        int pushSize = 0;
        char t1 = fromType.charAt(0);
        char t2 = toType.charAt(0);
        switch (fromType.charAt(0)) {
            case 'L':
            case '[':
            case 'A':
                if (fromType.equals(toType))
                    return;
                stackPop(1);
                switch (toType.charAt(0)) {
                    case 'A':
                        return;
                    case 'L':
                        toType = toType.substring(1, toType.length() - 1);
                        // fall thru
                    case '[': {
                        JVMConstantPool.CP_Class_Entry entry = constantPool.lookupClass(toType);
                        stackPush(1);
                        codeAcc.write(checkcast);
                        codeAcc.writeShort(entry.count);
                    }
                    return;
                }
                break;
            case 'I':
            case 'B':
            case 'C':
            case 'S':
                popSize = 1;
                switch (t2) {
                    case 'I':
                    case 'B':
                    case 'C':
                    case 'S':
                        if (t1 == t2)
                            return;
                        code = iconst_0; // it is just a flag, not written into class
                        pushSize = 1;
                        break;
                    case 'F':
                        pushSize = 1;
                        code = i2f;
                        break;
                    case 'J':
                        pushSize = 2;
                        code = i2l;
                        break;
                    case 'D':
                        pushSize = 2;
                        code = i2d;
                        break;
                }
                break;
            case 'D':
                popSize = 2;
                switch (t2) {
                    case 'I':
                    case 'B':
                    case 'C':
                    case 'S':
                        pushSize = 1;
                        code = d2i;
                        break;
                    case 'F':
                        pushSize = 1;
                        code = d2f;
                        break;
                    case 'J':
                        pushSize = 2;
                        code = d2l;
                        break;
                    case 'D':
                        return;
                }
                break;
            case 'F':
                popSize = 1;
                switch (t2) {
                    case 'I':
                    case 'B':
                    case 'C':
                    case 'S':
                        pushSize = 1;
                        code = f2i;
                        break;
                    case 'F':
                        return;
                    case 'J':
                        pushSize = 2;
                        code = f2l;
                        break;
                    case 'D':
                        pushSize = 2;
                        code = f2d;
                        break;
                }
                break;
            case 'J':
                popSize = 2;
                switch (t2) {
                    case 'I':
                    case 'B':
                    case 'C':
                    case 'S':
                        pushSize = 1;
                        code = l2i;
                        break;
                    case 'F':
                        pushSize = 1;
                        code = l2f;
                        break;
                    case 'J':
                        return;
                    case 'D':
                        pushSize = 2;
                        code = l2d;
                        break;
                }
                break;
        }
        if (code != nop) {
            stackPop(popSize);
            if (code != iconst_0) // iconst_0 used as a flag value
                codeAcc.write(code);
            switch (t2) {
                case 'B':
                    codeAcc.write(i2b);
                    break;
                case 'C':
                    codeAcc.write(i2c);
                    break;
                case 'S':
                    codeAcc.write(i2s);
                    break;
            }
            stackPush(pushSize);
        } else
            throw new RuntimeException("Conversion from " + fromType + " to " +
                    toType + " not implemented");
    }

    public void instnceof(String clazz) throws IOException {
        stackPop(1);
        JVMConstantPool.CP_Class_Entry entry = constantPool.lookupClass(clazz);
        codeAcc.write(instnceof);
        codeAcc.writeShort(entry.count);
        stackPush(1);
    }

    public void pop(String type, int nelem) throws IOException {
        char t = type.charAt(0);
        int size = (t == 'J' || t == 'D' || t == '2' ? 2 : 1);
        stackPop(size);
        while (size >= 2) {
            size -= 2;
            codeAcc.write(pop2);
        }
        if (size > 0)
            codeAcc.write(pop);
    }

    public void swap() throws IOException {
        stackPop(2);
        codeAcc.write(swap);
        stackPush(2);
    }

    public void dup(String type) throws IOException {
        char t = type.charAt(0);
        if (t == 'J' || t == 'D' || t == '2') {
            stackPop(2);
            stackPush(4);
            codeAcc.write(dup2);
        } else {
            stackPop(1);
            stackPush(2);
            codeAcc.write(dup);
        }
    }

    public void insert(String what, String under) throws IOException {
        char t = what.charAt(0);
        boolean big1 = (t == 'J' || t == 'D');
        t = under.charAt(0);
        boolean big2 = (t == 'J' || t == 'D' || t == '2');
        int popSize;
        int pushSize;
        int code;
        if (big1) {
            if (big2) {
                code = dup2_x2; // big under big
                popSize = 4;
                pushSize = 6;
            } else {
                code = dup2_x1; // big under small
                popSize = 3;
                pushSize = 5;
            }
        } else {
            if (big2) {
                code = dup_x2; // small under big
                popSize = 3;
                pushSize = 4;
            } else {
                code = dup_x1; // small under small
                popSize = 2;
                pushSize = 3;
            }
        }
        stackPop(popSize);
        codeAcc.write(code);
        stackPush(pushSize);
    }

    public void op(int op, String type) throws IOException {

        char t = type.charAt(0);

        int code;

        switch (t) {
            case 'I':
            case 'C':
            case 'B':
            case 'S':
                code = 0;
                break;
            case 'J':
                code = 1;
                break;
            case 'F':
                code = 2;
                break;
            case 'D':
                code = 3;
                break;
            default:
                throw new RuntimeException("Not a primitive type or boolean in binaryop: " + type);
        }

        int size = (code % 2) + 1;
        int stackPopAmount = (op == TokenConst.UNARY_MINUS ? 1 : 2) * size;

        switch (op) {
            case '+':
            case TokenConst.TOKEN_PE:
            case TokenConst.TOKEN_PP:
                code += iadd;
                break;
            case TokenConst.UNARY_MINUS:
                code += ineg;
                break;
            case '-':
            case TokenConst.TOKEN_ME:
            case TokenConst.TOKEN_MM:
                code += isub;
                break;
            case '*':
            case TokenConst.TOKEN_AE:
                code += imul;
                break;
            case '/':
            case TokenConst.TOKEN_SE:
                code += idiv;
                break;
            case '%':
            case TokenConst.TOKEN_PCE:
                code += irem;
                break;
            default:
                if (code >= 2)
                    throw new RuntimeException("Integer operation applied to " + type);
                switch (op) {
                    case '|':
                    case TokenConst.TOKEN_ORE:
                        code += ior;
                        break;
                    case '&':
                    case TokenConst.TOKEN_ANDE:
                        code += iand;
                        break;
                    case '^':
                    case TokenConst.TOKEN_XE:
                        code += ixor;
                        break;
                    case TokenConst.TOKEN_GG:
                    case TokenConst.TOKEN_GGE:
                        stackPopAmount = 2 + code;
                        code += ishr;
                        break;
                    case TokenConst.TOKEN_GGG:
                    case TokenConst.TOKEN_GGGE:
                        stackPopAmount = 2 + code;
                        code += iushr;
                        break;
                    case TokenConst.TOKEN_LL:
                    case TokenConst.TOKEN_LLE:
                        stackPopAmount = 2 + code;
                        code += ishl;
                        break;
                    case '~':
                        stackPush(size);
                        codeAcc.write(iconst_m1);
                        if (code != 0)
                            codeAcc.write(i2l);
                        code += ixor;
                        break;
                    default:
                        throw new RuntimeException("Unknown operation: " + (char) op);
                }
        }

        stackPop(stackPopAmount);
        codeAcc.write(code);
        stackPush(size);
    }

    public boolean isReachable() {
        return currStack >= 0;
    }

    public int getStackDepth() {
        return currStack;
    }

    static class FieldEntry {
        short flags;
        JVMConstantPool.CP_UTF8_Entry name;
        JVMConstantPool.CP_UTF8_Entry type;
        JVMConstantPool.CP_Entry init;
        JVMConstantPool.CP_UTF8_Entry jintType;
    }

    static class MethodEntry {
        short flags;
        JVMConstantPool.CP_UTF8_Entry name;
        JVMConstantPool.CP_UTF8_Entry type;
        JVMConstantPool.CP_UTF8_Entry jintType;
        JVMConstantPool.CP_Class_Entry[] exceptions;
        short maxStack;
        short maxLocals;
        byte[] code;
        HandlerEntry[] handlers;
        LineNumber[] lineNumbers;
    }

    static class HandlerEntry {
        short start;
        short end;
        short handler;
        JVMConstantPool.CP_Class_Entry clazz;
    }

    static class HandlerEntryTmp {
        short start;
        short end;
        Label handler;
        JVMConstantPool.CP_Class_Entry clazz;
    }

    static class LineNumber {

        int pc;
        int line;
        LineNumber(int pc, int line) {
            this.pc = pc;
            this.line = line;
        }
    }

    static class Label extends AbstractMark {
        int pc = -1;
        int stackSize = -1;
        String caller;
    }

    //----------------------- test ------------------

    static class TuneJump {
        int instrPC;
        int storePC;
        Label target;

        TuneJump(int iPC, int sPC, Label t) {
            instrPC = iPC;
            storePC = sPC;
            target = t;
        }
    }
}
