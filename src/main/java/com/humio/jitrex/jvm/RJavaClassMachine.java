/*
    Copyright (c) Peter Sorotokin, 1998-2000
    Copyright (c) Humio, 2019
    See file "LICENSE.md" for terms of usage and
    redistribution.   
*/
package com.humio.jitrex.jvm;

import com.humio.jitrex.compiler.RLabel;
import com.humio.jitrex.compiler.RMachine;
import com.humio.jitrex.compiler.RVariable;
import com.humio.jitrex.tree.CharClassCodes;
import com.humio.jitrex.util.Regex;
import com.humio.jitrex.util.RegexRefiller;
import com.humio.util.jint.constants.TokenConst;
import com.humio.util.jint.gen.AbstractMark;
import com.humio.util.jint.gen.CodeGenerator;
import com.humio.util.jint.gen.JVMClassGenerator;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

/**
 * This class is RMachine that compiles jitrex instructions into JVM bytecode.
 */
public class RJavaClassMachine extends RMachine implements CharClassCodes, TokenConst {

    // used by CompilerContext:
    public static int I_HEAD = 0;
    public static int I_STRING = 1;
    public static int I_CELLS = 2;
    public static int I_FORKS = 3;
    public static int I_FORKPTR = 4;
    public static int I_BEGIN = 5;
    public static int I_START = 6;
    public static int I_END = 7;
    public static int I_MAXSTART = 8;
    public static int NVARS = 9;
    static Object counterLock = new Object();
    static int counter = 0;
    static Loader loader = new Loader();
    static String refillSignature = "(L" + Regex.class.getName().replace('.', '/') + ";I)I";
    static String stubClass = JavaClassRegexStub.class.getName().replace('.', '/');
    static String refillerClass = RegexRefiller.class.getName().replace('.', '/');
    static String refillerType = "L" + refillerClass + ";";
    private static Integer ZERO = 0;
    private static Integer ONE = 1;
    private static Integer TWO = 2;
    private static Integer THREE = 3;
    private static Integer MINUS_ONE = -1;
    String stringRep = "***jitrex***";
    Hashtable<String, Variable[]> vars = new Hashtable<>();
    int varCells = 0;
    int extCells = 0;
    int[] extVarRegs;
    int minLength = 0;
    int maxLength = Integer.MAX_VALUE;
    int flags = 0;
    CodeGenerator gen;
    Vector<AbstractMark> switchTable = new Vector<>();
    AbstractMark reallocMark;
    AbstractMark failMark;
    AbstractMark refillMark;
    AbstractMark startMark;
    AbstractMark saveMark;
    AbstractMark mfStartMark;
    AbstractMark mfSaveFailMark;
    int mfHeadDecrement;
    int mfMinCount;
    AbstractMark matchFailedMark;
    AbstractMark matchSucceededMark;
    int initStackDepth;
    // always needed
    int V_HEAD;
    int V_STRING;
    int V_CELLS;
    int V_FORKS;
    int V_FORKPTR;
    int V_END;
    int V_REFILLER = -1; // if not embed; set to RegexRefiller, getting to V_END will cause call to refill
    // can be allocated as needed
    int V_RET1 = -1;
    int V_TMP1 = -1;
    int V_MFCOUNT = -1;
    int V_MFHEAD = -1;
    int V_HEADINC = -1;
    int V_START = -1; // used only for embedded jitrex
    int V_BEGIN = -1; // used only for embedded jitrex
    int V_MAXSTART = -1; // used only for embedded jitrex
    String charType = "C";
    String thisClass;
    String thisType;
    private String charArrType;
    private String charSequenceType;
    private String charSequenceArrType;
    private boolean saveBytecode;
    boolean loadClass = true;
    private Class<? extends JavaClassRegexStub> compiledClass;
    private String compiledFrom = null;
    private boolean embedSearch;
    private boolean embed;
    boolean noRefiller;
    boolean reportPosition;
    int maxLocalVariable;

    private RJavaClassCustomizer customizer;

    private RLocalAllocator allocator = new RLocalAllocator() {
        public int alloc() {
            return ++maxLocalVariable;
        }

        public void free(int loc) {
            if (loc == maxLocalVariable)
                maxLocalVariable--;
            else {
                System.err.println("Cannot free local variable"); // should not happen
                (new Exception()).printStackTrace();
            }
        }
    };

    public RJavaClassMachine() {
        setExtensions(EXT_HINT | EXT_MULTIFORK | EXT_SHIFTTBL | EXT_CONDJUMP);
        charArrType = "[" + charType;
        charSequenceType = "java/lang/CharSequence";
        charSequenceType = "L" + "java/lang/CharSequence" + ";";
        charSequenceArrType = "[" + charSequenceType;
        //reportPosition = true; // for debugging
    }

    private void simpleLocalAlloc() {
        V_HEAD = 1;
        V_STRING = 2;
        V_CELLS = 3;
        V_FORKS = 4;
        V_FORKPTR = 5;
        V_END = 6;

        if (noRefiller)
            maxLocalVariable = 7;
        else {
            V_REFILLER = 7;
            maxLocalVariable = 8;
        }
    }

    public void setNoRefiller(boolean noRefiller) {
        this.noRefiller = noRefiller;
    }

    public void setCustomizer(RJavaClassCustomizer c) {
        customizer = c;
    }

    public void setCodeGenerator(CodeGenerator gen) {
        this.gen = gen;
    }

    public String getClassName() {
        return thisClass;
    }

    public void setClassName(String name) {
        thisClass = name;
    }

    public void setLoadClass(boolean needToLoadClass) {
        loadClass = needToLoadClass;
    }

    public boolean getSaveBytecode() {
        return saveBytecode;
    }

    public void setSaveBytecode(boolean b) {
        saveBytecode = b;
    }

    public boolean getReportPosition() {
        return reportPosition;
    }

    public void setReportPosition(boolean b) {
        reportPosition = b;
    }

    public int getVariableHandle(String var, boolean begin) {
        return (vars.get(var))[begin ? 0 : 1].cell;
    }

    public Enumeration<String> variables() {
        return vars.keys();
    }

    public int getExtVariableHandle(String var) {
        return (vars.get(var))[0].extCell;
    }

    public void embed(boolean search, CodeGenerator codeGen, RLocalAllocator alloc,
                      int[] commVar, int[] extVars, AbstractMark fail, AbstractMark success) {
        embed = true;
        embedSearch = search;
        gen = codeGen;
        allocator = alloc;
        matchFailedMark = fail;
        matchSucceededMark = success;
        V_HEAD = commVar[0];
        V_STRING = commVar[1];
        V_CELLS = commVar[2];
        V_FORKS = commVar[3];
        V_FORKPTR = commVar[4];
        V_BEGIN = commVar[5];
        V_START = commVar[6]; // -1 means it is always 0
        V_END = commVar[7];
        V_MAXSTART = commVar[8];
        extVarRegs = extVars;
        loadClass = false;

        saveBytecode = false;
    }

    public void init() {
        try {
            // in embedded case
            // user is responsible for supplying correct values
            // in V_HEAD, V_STRING, V_CELLS, V_FORKS, V_FORKPTR, V_BEGIN and V_END
            if (!embed) {
                simpleLocalAlloc();
                if (gen == null) {
                    if (thisClass == null)
                        synchronized (counterLock) {
                            counter++;
                            thisClass = "com/humio/jitrex/jvm/R_tmp" + counter;
                        }
                    gen = new JVMClassGenerator(gen.ACC_PUBLIC, thisClass, stubClass);
                }
                gen.setSourceFile(compiledFrom == null ? "***regexp***" : compiledFrom);

                thisType = "L" + thisClass + ";";

                gen.startMethod(gen.ACC_PROTECTED, "nextMatchInt", "()Z", null);
            }

            initStackDepth = gen.getStackDepth();
            failMark = gen.newMark();

            if ((flags & HINT_CHAR_STAR_HEAD) != 0) {
                if (V_HEADINC < 0)
                    V_HEADINC = allocator.alloc(); // never freed, never reused
                gen.loadConst(1);
                gen.store(V_HEADINC, "I");
            }

            if (!embed) {
                gen.load(0, thisType);
                gen.getfield(stubClass, "string", charSequenceType);
                gen.store(V_STRING, charSequenceType);

                gen.load(0, thisType);
                gen.getfield(stubClass, "cells", "[I");
                gen.store(V_CELLS, "[I");

                gen.load(0, thisType);
                gen.getfield(stubClass, "forks", "[I");
                gen.store(V_FORKS, "[I");

                gen.load(0, thisType);
                gen.getfield(stubClass, "end", "I");
                gen.store(V_END, "I");

                gen.load(0, thisType);
                gen.getfield(stubClass, "forkPtr", "I");
                gen.dup("I");
                gen.store(V_FORKPTR, "I");

                if (V_REFILLER >= 0) {
                    gen.load(0, thisType);
                    gen.getfield(stubClass, "refiller", refillerType);
                    gen.store(V_REFILLER, refillerType);
                }

                // if there are forks, start with them
                gen.jumpIf(true, gen.TOKEN_NE, "I", failMark);

                gen.load(0, thisType);
                gen.load(0, thisType);
                gen.getfield(stubClass, "headStart", "I");
                gen.dup("I");
                gen.store(V_HEAD, "I");
                gen.putfield(stubClass, "matchStart", "I");
            }

            V_RET1 = allocator.alloc();  // never freed
            V_TMP1 = allocator.alloc();  // never freed

            startMark = gen.newMark();
            gen.mark(startMark);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void tellPosition(int pos) {
        if (reportPosition)
            try {
                gen.linenumber(pos);
            } catch (IOException e) {
                e.printStackTrace();
            }
    }

    private void checkSize(int n) throws IOException {
        gen.load(V_FORKS, "[I");
        gen.arraylength();
        gen.load(V_FORKPTR, "I");
        gen.op('-', "I");
        gen.loadConst(n);
        AbstractMark cont = gen.newMark();
        gen.jumpIf(false, '>', "I", cont);
        if (reallocMark == null)
            reallocMark = gen.newMark();
        gen.jsr(reallocMark, 0);
        gen.mark(cont);
    }

    private void saveFields() throws IOException {
        if (!embed) {
            gen.load(0, thisType);
            gen.load(V_FORKS, "[I");
            gen.putfield(stubClass, "forks", "[I");
            gen.load(0, thisType);
            gen.load(V_FORKPTR, "I");
            gen.putfield(stubClass, "forkPtr", "I");
        }
    }

    private void simpleJumpIfInRange(char[] ranges, AbstractMark inRange,
                                     AbstractMark notInRange) throws IOException {
        boolean myIn = inRange == null;
        boolean myNot = notInRange == null;
        if (myNot)
            notInRange = gen.newMark();
        if (myIn)
            inRange = gen.newMark();
        for (int i = 0; i < ranges.length; i += 2) {
            char c1 = ranges[i];
            char c2 = ranges[i + 1];
            if (c1 == c2) {
                gen.load(V_TMP1, charType);
                gen.loadConst((int) c1);
                gen.jumpIf(false, gen.TOKEN_EE, charType, inRange);
            } else {
                if (c1 != 0) {
                    if (c2 != 0xFFFF) {
                        gen.load(V_TMP1, charType);
                        gen.loadConst((int) c1);
                        gen.jumpIf(false, '<', charType, notInRange);
                        gen.load(V_TMP1, charType);
                        gen.loadConst((int) c2);
                        gen.jumpIf(false, gen.TOKEN_LE, charType, inRange);
                    } else {
                        gen.load(V_TMP1, charType);
                        gen.loadConst((int) c1);
                        gen.jumpIf(false, gen.TOKEN_GE, charType, inRange);
                    }
                } else {
                    if (c2 == 0xFFFF) {
                        gen.jump(inRange);
                    } else {
                        gen.load(V_TMP1, charType);
                        gen.loadConst((int) c2);
                        gen.jumpIf(false, gen.TOKEN_LE, charType, inRange);
                    }
                }
            }
        }
        if (myNot)
            gen.mark(notInRange);
        else
            gen.jump(notInRange);

        if (myIn)
            gen.mark(inRange);
    }

    private void maskJumpIfInRange(char[] ranges, int first, int last,
                                   AbstractMark inRange, AbstractMark notInRange) throws IOException {
        int fval = ranges[first];
        int mask = 0;
        for (int i = first; i <= last; i += 2) {
            int rb = ranges[i];
            int re = ranges[i + 1];
            for (int j = rb; j <= re; j++) {
                int bit = 1 << (j - fval);
                mask |= bit;
            }
        }
        gen.load(V_TMP1, charType);
        gen.loadConst(fval);
        gen.op('-', "I");
        gen.dup("I");
        int V_TMP2 = V_RET1; // we can reuse V_RET1
        gen.store(V_TMP2, "I");
        gen.loadConst(0xFFFFFFE0);
        gen.op('&', "I");

        gen.jumpIf(true, gen.TOKEN_NE, "I", notInRange);

        gen.loadConst(1);
        gen.load(V_TMP2, "I");
        gen.op(gen.TOKEN_LL, "I");
        gen.loadConst(mask);
        gen.op('&', "I");

        if (inRange != null) {
            gen.jumpIf(true, gen.TOKEN_NE, "I", inRange);
            if (notInRange != null)
                gen.jump(notInRange);
        } else
            gen.jumpIf(true, gen.TOKEN_EE, "I", notInRange);
    }

    private void jumpIfInRange(char[] ranges, AbstractMark inRange,
                               AbstractMark notInRange) throws IOException {
        int first = 0;
        int last = ranges.length - 1;
        boolean negate = (ranges[0] == 0 && ranges[last] == 0xFFFF);
        if (negate) {
            first++;
            last--;
        }
        int size = ranges[last] - ranges[first];
        if (size < 32 && last - first >= 6)
            if (negate)
                maskJumpIfInRange(ranges, first, last, notInRange, inRange);
            else
                maskJumpIfInRange(ranges, first, last, inRange, notInRange);
        else
            simpleJumpIfInRange(ranges, inRange, notInRange);
    }

    private void refillIfHaveTo(AbstractMark refilled) throws IOException {
        refillIfHaveTo(gen.TOKEN_GE, refilled, failMark);
    }

    private void refillIfHaveTo(int op, AbstractMark refilled, AbstractMark onFail) throws IOException {
        if (embed || V_REFILLER < 0)
            gen.jumpIf(false, op, "I", onFail);
        else {
            int invOp;
            switch (op) {
                case TOKEN_GE:
                    invOp = '<';
                    break;
                case '>':
                    invOp = TOKEN_LE;
                    break;
                case TOKEN_EE:
                    invOp = TOKEN_NE;
                    break;
                case TOKEN_NE:
                    invOp = TOKEN_EE;
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported op: " + op);
            }
            AbstractMark contMark = gen.newMark();
            gen.jumpIf(false, invOp, "I", contMark);
            gen.load(V_REFILLER, refillerType);
            gen.jumpIf(true, gen.TOKEN_EE, refillerType, onFail);
            if (refillMark == null)
                refillMark = gen.newMark();
            gen.jsr(refillMark, 0);
            gen.jump(refilled);
            gen.mark(contMark);
        }
    }

    private void genRealloc() throws IOException {
        gen.load(V_FORKS, "[I");
        gen.arraylength();
        gen.loadConst(2);
        gen.op('*', "I");
        gen.newarray("I");
        gen.dup("[I");
        gen.load(V_FORKS, "[I");
        gen.swap();
        gen.loadConst(0);
        gen.swap();
        gen.loadConst(0);
        gen.load(V_FORKS, "[I");
        gen.arraylength();
        gen.invokestatic("java/lang/System", "arraycopy",
                "(Ljava/lang/Object;ILjava/lang/Object;II)V");
        gen.store(V_FORKS, "[I");
    }

    //----- finish

    public void finish() {
        try {
            saveFields();

            if (embed)
                gen.jump(matchSucceededMark);
            else {
                gen.load(0, thisType);
                gen.load(V_HEAD, "I");
                gen.putfield(stubClass, "matchEnd", "I");

                if (customizer != null)
                    customizer.customSuccessAction(gen, V_STRING, V_CELLS);

                gen.loadConst(Boolean.TRUE);
                gen.retrn("Z");
            }

            if (reallocMark != null) {
                gen.mark(reallocMark);
                gen.store(V_RET1, "L");
                genRealloc();
                gen.ret(V_RET1);
            }

            if (refillMark != null) {

                // TODO: modify maxStart correctly

                gen.mark(refillMark);
                gen.store(V_RET1, "L");
                gen.load(V_REFILLER, refillerType);
                gen.load(0, thisType);
                gen.load(V_END, "I");
                gen.invokevirtual(refillerClass, "refill", refillSignature);

                gen.load(0, thisType);
                gen.getfield(stubClass, "refiller", refillerType);
                gen.store(V_REFILLER, refillerType);

                gen.dup("I");

                gen.load(V_END, "I");
                AbstractMark contMark = gen.newMark();
                gen.jumpIf(false, '>', "I", contMark);
                gen.load(V_REFILLER, refillerType);
                gen.jumpIf(true, gen.TOKEN_EE, refillerType, contMark);

                gen.load(0, thisType);
                gen.loadConst(Integer.MAX_VALUE);
                gen.putfield(stubClass, "headStart", "I");
                gen.loadConst(Boolean.FALSE);
                gen.retrn("Z");

                gen.mark(contMark);

                gen.store(V_END, "I");

                gen.load(0, thisType);
                gen.load(V_END, "I");
                gen.putfield(stubClass, "end", "I");

                gen.load(0, thisType);
                gen.getfield(stubClass, "string", charSequenceType);
                gen.store(V_STRING, charSequenceType);

                if ((flags & HINT_START_ANCHORED) == 0) {
                    gen.load(0, thisType);
                    gen.load(V_END, "I");
                    if (minLength > 0)
                        if (V_REFILLER < 0) {
                            gen.loadConst(minLength);
                            gen.op('-', "I");
                        } else {
                            gen.load(V_REFILLER, refillerType);
                            AbstractMark hasRefiller = gen.newMark();
                            gen.jumpIf(true, gen.TOKEN_NE, refillerType, hasRefiller);
                            gen.loadConst(minLength);
                            gen.op('-', "I");
                            gen.mark(hasRefiller);
                        }
                    gen.putfield(stubClass, "maxStart", "I");
                }

                gen.ret(V_RET1);
            }

            if (saveMark != null) {
                gen.mark(saveMark);
                gen.store(V_RET1, "L");
                gen.store(V_TMP1, "I");
                // checkSize( 2 )
                gen.load(V_FORKS, "[I");
                gen.arraylength();
                gen.load(V_FORKPTR, "I");
                gen.op('-', "I");
                gen.loadConst(2);
                AbstractMark cont = gen.newMark();
                gen.jumpIf(false, '>', "I", cont);
                genRealloc();
                gen.mark(cont);

                // save value
                gen.load(V_FORKS, "[I");
                gen.load(V_FORKPTR, "I");
                gen.load(V_CELLS, "[I");
                gen.load(V_TMP1, "I");
                gen.getelement("I");
                gen.putelement("I");
                // save cell number
                gen.iinc(V_FORKPTR, 1);
                gen.load(V_FORKS, "[I");
                gen.load(V_FORKPTR, "I");
                gen.load(V_TMP1, "I");
                gen.putelement("I");
                gen.iinc(V_FORKPTR, 1);
                gen.ret(V_RET1);
            }


            AbstractMark start = gen.newMark();
            AbstractMark forkMark = gen.newMark();

            gen.mark(start, initStackDepth);

            gen.iinc(V_FORKPTR, -1);
            gen.load(V_FORKS, "[I");
            gen.load(V_FORKPTR, "I");
            gen.getelement("I");
            gen.iinc(V_FORKPTR, -1);

            if (V_CELLS >= 0) {
                gen.dup("I");
                gen.jumpIf(true, '<', "I", forkMark);

                gen.load(V_CELLS, "[I");
                gen.swap();
                gen.load(V_FORKS, "[I");
                gen.load(V_FORKPTR, "I");
                gen.getelement("I");
                gen.putelement("I");
            } else
                gen.jump(forkMark);

            gen.mark(failMark, initStackDepth);
            gen.load(V_FORKPTR, "I");
            gen.jumpIf(true, gen.TOKEN_NE, "I", start);

            saveFields();

            // TODO: increment it more intelligently
            if (embed) {
                if (embedSearch && !(V_MAXSTART < 0 || V_MAXSTART == V_START)) {
                    if ((flags & HINT_CHAR_STAR_HEAD) != 0) {
                        gen.load(V_BEGIN, "I");
                        gen.load(V_HEADINC, "I");
                        gen.op('+', "I");
                        gen.dup("I");
                        gen.store(V_BEGIN, "I");
                    } else {
                        gen.iinc(V_BEGIN, 1);
                        gen.load(V_BEGIN, "I");
                    }
                    gen.dup("I");
                    gen.store(V_HEAD, "I");
                    gen.load(V_MAXSTART, "I");
                    gen.jumpIf(false, gen.TOKEN_LE, "I", startMark);
                }
                gen.jump(matchFailedMark);
            } else {
                gen.load(0, thisType);
                gen.load(0, thisType);
                gen.getfield(stubClass, "headStart", "I");
                if ((flags & HINT_CHAR_STAR_HEAD) != 0)
                    gen.load(V_HEADINC, "I");
                else
                    gen.loadConst(1);
                gen.op('+', "I");
                gen.putfield(stubClass, "headStart", "I");
                gen.loadConst(Boolean.FALSE);
                gen.retrn("Z");
            }

            gen.mark(forkMark); // fork id is on stack

            // restore head
            gen.load(V_FORKS, "[I");
            gen.load(V_FORKPTR, "I");
            gen.getelement("I");
            gen.store(V_HEAD, "I");

            if (switchTable.size() != 0) {
                AbstractMark[] arr = new AbstractMark[switchTable.size()];
                switchTable.copyInto(arr);
                for (int i = 0; 2 * i < arr.length; i++) {
                    AbstractMark tmp = arr[i];
                    arr[i] = arr[arr.length - 1 - i];
                    arr[arr.length - 1 - i] = tmp;
                }
                int min = -switchTable.size();
                AbstractMark error = gen.newMark();
                gen.tableswitch(min, arr, error);
                gen.mark(error);
            }
            if (!embed) {
                gen.load(0, thisType);
                gen.invokevirtual(stubClass, "dumpForks", "()V");
            }
            gen.newobject("java/lang/IllegalStateException");
            gen.dup("Ljava/lang/IllegalStateException;");
            gen.loadConst("Internal error during jitrex matching");
            gen.invokespecial("java/lang/IllegalStateException", "<init>", "(Ljava/lang/String;)V");
            gen.athrow();

            if (embed)
                return;

            gen.endMethod();

            gen.addField(gen.ACC_STATIC | gen.ACC_PRIVATE, "vars", "Ljava/util/Hashtable;", null);

            if (noRefiller) {
                gen.startMethod(gen.ACC_PUBLIC, "setRefiller", "(" + refillerType + ")V", null);
                gen.newobject("java/lang/RuntimeException");
                gen.dup("Ljava/lang/RuntimeException;");
                gen.loadConst("This regex was explicitly compiled not to support refilling.");
                gen.invokespecial("java/lang/RuntimeException", "<init>", "(Ljava/lang/String;)V");
                gen.athrow();
                gen.endMethod();
            }

            gen.startMethod(gen.ACC_STATIC, "<clinit>", "()V", null);
            gen.newobject("java/util/Hashtable");
            gen.dup("Ljava/util/Hashtable;");
            gen.invokespecial("java/util/Hashtable", "<init>", "()V");
            Enumeration<String> keys = vars.keys();
            while (keys.hasMoreElements()) {
                String key = keys.nextElement();
                Variable[] v = vars.get(key);
                gen.dup("Ljava/util/Hashtable;");
                gen.loadConst(key);
                gen.loadConst(THREE);
                gen.newarray("I");
                gen.dup("[I");
                gen.loadConst(ZERO);
                gen.loadConst(v[0].cell);
                gen.putelement("I");
                gen.dup("[I");
                gen.loadConst(ONE);
                gen.loadConst(v[1].cell);
                gen.putelement("I");
                gen.dup("[I");
                gen.loadConst(TWO);
                gen.loadConst(v[0].extCell);
                gen.putelement("I");
                gen.invokevirtual("java/util/Hashtable", "put",
                        "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;");
                gen.pop("Ljava/lang/Object;", 1);
            }
            gen.putstatic(thisClass, "vars", "Ljava/util/Hashtable;");
            gen.retrn("V");
            gen.endMethod();

            gen.startMethod(gen.ACC_PUBLIC, "toString", "()Ljava/lang/String;", null);
            gen.loadConst(stringRep);
            gen.retrn("Ljava/lang/String;");
            gen.endMethod();

            gen.startMethod(gen.ACC_PROTECTED, "getVars", "()Ljava/util/Hashtable;", null);
            gen.getstatic(thisClass, "vars", "Ljava/util/Hashtable;");
            gen.retrn("Ljava/util/Hashtable;");
            gen.endMethod();


            String initSig;
            if (customizer != null)
                initSig = customizer.customConstructorSignature();
            else
                initSig = "()V";
            gen.startMethod(gen.ACC_PUBLIC, "<init>", initSig, null);
            gen.load(0, thisType);
            gen.invokespecial(stubClass, "<init>", "()V");

            if (varCells != 0) {
                //--> cells = new int[nCells];
                gen.load(0, thisType);
                gen.loadConst(varCells);
                gen.newarray("I");
                gen.putfield(stubClass, "cells", "[I");
            }

            //--> forks = new int[4];
            gen.load(0, thisType);
            gen.loadConst(4);
            gen.newarray("I");
            gen.putfield(stubClass, "forks", "[I");

            if (extCells != 0) {
                //--> extCells = new char[nExtCells][];
                gen.load(0, thisType);
                gen.loadConst(extCells);
                gen.newarray("[C");
                gen.putfield(stubClass, "extCells", "[Ljava/lang/CharSequence;");
            }

            if (customizer != null)
                customizer.customConstructorAction(gen);
            gen.retrn("V");
            gen.endMethod();

            //--> public void init( char[] arr, int off, int len )
            gen.startMethod(gen.ACC_PUBLIC, "init", "(Ljava/lang/CharSequence;II)V", null);

            //--> this.string = arr
            gen.load(0, thisType);
            gen.load(1, "Ljava/lang/CharSequence;");
            gen.putfield(stubClass, "string", "Ljava/lang/CharSequence;");

            //--> this.start = off
            gen.load(0, thisType);
            gen.load(2, "I");
            gen.putfield(stubClass, "start", "I");

            //--> this.end = off + len;
            gen.load(0, thisType);
            gen.load(2, "I");
            gen.load(3, "I");
            gen.op('+', "I");
            gen.putfield(stubClass, "end", "I");

            // initialize all internal variables to "unset"
            boolean loadCells = true;
            Enumeration<Variable[]> varList = vars.elements();
            while (varList.hasMoreElements()) {
                Variable[] pair = varList.nextElement();
                if (pair[0].extCell >= 0)
                    continue;

                for (int q = 0; q < 2; q++) {
                    if (loadCells) {
                        loadCells = false;
                        gen.load(0, thisType);
                        gen.getfield(stubClass, "cells", "[I");
                        gen.store(1, "[I");
                        loadCells = false;
                    }
                    gen.load(1, "[I");
                    gen.loadConst(pair[q].cell);
                    gen.loadConst(MINUS_ONE);
                    gen.putelement("I");
                }
            }

            gen.load(0, thisType);
            if ((flags & HINT_END_ANCHORED) != 0 && maxLength < 2048 && (this.getExtensions() & FLAG_MULTILINE) == 0) {
                gen.load(0, thisType);
                gen.getfield(stubClass, "end", "I");
                gen.loadConst(maxLength);
                gen.op('-', "I");
                gen.dup("I");
                gen.load(2, "I");
                AbstractMark skip = gen.newMark();
                gen.jumpIf(false, gen.TOKEN_GE, "I", skip);
                gen.pop("I", 1);
                gen.load(2, "I");
                gen.mark(skip);
            } else {
                gen.load(2, "I");
            }
            gen.putfield(stubClass, "headStart", "I");

            if ((flags & HINT_START_ANCHORED) != 0 && (this.getExtensions() & FLAG_MULTILINE) == 0) {
                gen.load(0, thisType);
                gen.load(2, "I");
                gen.putfield(stubClass, "maxStart", "I");
            } else {
                gen.load(0, thisType);
                gen.load(0, thisType);
                gen.getfield(stubClass, "end", "I");
                if (minLength > 0) {
                    gen.load(0, thisType);
                    gen.getfield(stubClass, "refiller", refillerType);
                    AbstractMark hasRefiller = gen.newMark();
                    gen.jumpIf(true, gen.TOKEN_NE, refillerType, hasRefiller);
                    gen.loadConst(minLength);
                    gen.op('-', "I");
                    gen.mark(hasRefiller);
                }
                gen.putfield(stubClass, "maxStart", "I");
            }

	/*
	//--> forks[0] = headStart;
	gen.load( 0, thisType );
	gen.getfield( stubClass, "forks", "[I" );
	gen.dup( "[I" );
	gen.loadConst( ZERO );
	gen.load( 0, thisType );
	gen.getfield( stubClass, "headStart", "I" );
	gen.putelement( "I" );

	//--> forks[0] = -1; // -1 is jitrex start
	gen.loadConst( ONE );
	gen.loadConst( MINUS_ONE );
	gen.putelement( "I" );

	//--> forkPtr = 2;
	gen.load( 0, thisType );
	gen.loadConst( TWO );
	gen.putfield( stubClass, "forkPtr", "I" );
	*/

            //--> forkPtr = 0;
            gen.load(0, thisType);
            gen.loadConst(ZERO);
            gen.putfield(stubClass, "forkPtr", "I");

            if (customizer != null)
                customizer.customInitAction(gen);

            gen.retrn("V");
            gen.endMethod();

            if (customizer != null)
                customizer.customMembers(gen);

            gen.flush();

            if (gen instanceof JVMClassGenerator) {

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                DataOutputStream dos = new DataOutputStream(baos);
                ((JVMClassGenerator) gen).writeTo(dos);
                dos.close();

                byte[] body = baos.toByteArray();

                if (saveBytecode) {
                    String name = thisClass + ".class";
                    FileOutputStream fos = new FileOutputStream(name);
                    fos.write(body);
                    fos.close();
                    System.out.println("*** WRITTEN " + name + " ***");
                }
                if (loadClass) {
                    compiledClass = (Class<JavaClassRegexStub>) loader.makeClass(thisClass, body);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Regex makeRegex() {
        try {
            return compiledClass.getConstructor().newInstance();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
        return null;
    }

    public int getNVars() {
        return vars.size();
    }

    public void tellName(String name) {
        if (name != null)
            if (name.length() < 28)
                compiledFrom = name;
            else
                compiledFrom = name.substring(0, 25) + "...";
        stringRep = "/" + name + "/";
    }

    //----- basic instructions

    public RVariable newVar(String name, boolean begin) {
        Variable[] list = vars.get(name);
        if (list == null) {
            Variable v1 = new Variable();
            v1.cell = varCells++;
            Variable v2 = new Variable();
            v2.cell = varCells++;
            Variable[] pair = {v1, v2};
            list = pair;
            vars.put(name, list);
        }
        return list[begin ? 0 : 1];
    }

    public RLabel newLabel() {
        Label l = new Label();
        try {
            l.mark = gen.newMark();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return l;
    }

    public RVariable newTmpVar(int init) {
        Variable v = new Variable();
        v.cell = varCells++;
        try {
            gen.loadConst(v.cell);
            if (saveMark == null)
                saveMark = gen.newMark();
            gen.jsr(saveMark, -1);
            gen.load(V_CELLS, "[I");
            gen.loadConst(v.cell);
            gen.loadConst(init);
            gen.putelement("I");
        } catch (IOException e) {
            e.printStackTrace();
        }
        return v;
    }

    public void hardAssign(RVariable v, int value) {
        try {
            gen.load(V_CELLS, "[I");
            gen.loadConst(((Variable) v).cell);
            gen.loadConst(value);
            gen.putelement("I");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void mark(RLabel label) {
        try {
            gen.mark(((Label) label).mark, initStackDepth);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void pick(RVariable v) {
        try {

            gen.loadConst(((Variable) v).cell);
            if (saveMark == null)
                saveMark = gen.newMark();
            gen.jsr(saveMark, -1);
            gen.load(V_CELLS, "[I");
            gen.loadConst(((Variable) v).cell);
            gen.load(V_HEAD, "I");
            gen.putelement("I");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void fork(RLabel forkLabel) {
        try {
            switchTable.addElement(((Label) forkLabel).mark);
            int index = switchTable.size();

            checkSize(2);

            // save head
            gen.load(V_FORKS, "[I");
            gen.load(V_FORKPTR, "I");
            gen.load(V_HEAD, "I");
            gen.putelement("I");

            // save fork location
            gen.iinc(V_FORKPTR, 1);
            gen.load(V_FORKS, "[I");
            gen.load(V_FORKPTR, "I");
            gen.loadConst(-index);
            gen.putelement("I");

            gen.iinc(V_FORKPTR, 1);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void genCharAt() {
        try {
            if ((getExtensions() & FLAG_IGNORECASE) == 0) {
                gen.invokestatic(stubClass, "charAt", "(Ljava/lang/CharSequence;I)C");
            } else {
                gen.invokestatic(stubClass, "lowerCaseCharAt", "(Ljava/lang/CharSequence;I)C");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void skip() {
        try {
            AbstractMark againMark = gen.newMark();
            gen.mark(againMark);
            gen.load(V_HEAD, "I");
            gen.load(V_END, "I");
            refillIfHaveTo(againMark);

            if ((this.getExtensions() & FLAG_DOT_IS_ANY) == 0) {
                gen.load(V_STRING, charSequenceType);
                gen.load(V_HEAD, "I");
                gen.invokeinterface("java/lang/CharSequence", "charAt", "(I)C");
                gen.invokestatic(stubClass, "cmpLineTerminator", "(C)I");
                gen.jumpIf(true, gen.TOKEN_EE, "I", failMark);
//                gen.loadConst((int) '\n');
//                gen.jumpIf(false, gen.TOKEN_EE, "I", failMark);
            }

            gen.iinc(V_HEAD, 1);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void boundary(int type) {
        boolean multiline = (this.getExtensions() & FLAG_MULTILINE) == FLAG_MULTILINE;
        try {
            switch (type) {
                case '^':
                case 'A':

                    gen.load(V_HEAD, "I");
                    if (embed) {
                        if (V_START < 0)
                            gen.jumpIf(true, gen.TOKEN_NE, "I", failMark);
                        else {
                            gen.load(V_START, "I");
                            gen.jumpIf(false, gen.TOKEN_NE, "I", failMark);
                        }
                    } else {
                        gen.load(0, thisType);
                        gen.getfield(stubClass, "start", "I");
                        if (multiline) {
                            AbstractMark okMark = gen.newMark();
                            gen.jumpIf(false, gen.TOKEN_EE, "I", okMark);

                            gen.load(V_STRING, charSequenceType);

                            gen.load(V_HEAD, "I");
                            gen.loadConst(1);
                            gen.op('-', "I");

                            gen.invokeinterface("java/lang/CharSequence", "charAt", "(I)C");
                            gen.invokestatic(stubClass, "cmpLineTerminator", "(C)I");
                            gen.jumpIf(true, gen.TOKEN_NE, "I", failMark);
                          //  gen.loadConst( '\n' );
                          //  gen.jumpIf(false, gen.TOKEN_NE, "I", failMark);

                            gen.mark(okMark);
                        } else {
                            gen.jumpIf(false, gen.TOKEN_NE, "I", failMark);
                        }
                    }
                    break;
                case '$':
                case 'z':
                case 'Z': {
                    // we consider it to be end-of-string only if refiller is null,
                    // if not and HEAD == END, refill and try again

                    AbstractMark againMark = gen.newMark();
                    gen.mark(againMark);
                    gen.load(V_HEAD, "I");
                    gen.load(V_END, "I");
                    if ((multiline && type == '$') || type == 'Z') {

                        AbstractMark okMark = gen.newMark();
                        gen.jumpIf(false, gen.TOKEN_EE, "I", okMark);

                        gen.load(V_STRING, charSequenceType);
                        gen.load(V_HEAD, "I");
                        gen.invokeinterface("java/lang/CharSequence", "charAt", "(I)C");

                        gen.invokestatic(stubClass, "cmpLineTerminator", "(C)I");
                        gen.jumpIf(true, gen.TOKEN_NE, "I", failMark);

                        // gen.loadConst( '\n' );
                        // gen.jumpIf(false, gen.TOKEN_NE, "I", failMark);

                        gen.mark(okMark);

                    } else {
                        gen.jumpIf(false, gen.TOKEN_NE, "I", failMark);
                    }

                    if (V_REFILLER >= 0) {
                        AbstractMark contMark = gen.newMark();
                        gen.load(V_REFILLER, refillerType);
                        gen.jumpIf(true, gen.TOKEN_EE, refillerType, contMark);
                        if (refillMark == null)
                            refillMark = gen.newMark();
                        gen.jsr(refillMark, 0);
                        gen.jump(againMark);
                        gen.mark(contMark);
                    }
                }
                break;
                case '>':
                case '<':
                case 'b':
                case 'B': {
                    AbstractMark notWord;
                    AbstractMark isWord;
                    AbstractMark cont;

                    // analyzing next character
                    cont = gen.newMark();
                    if (type == '>') {
                        notWord = cont;
                        isWord = failMark;
                    } else if (type == '<') {
                        notWord = failMark;
                        isWord = cont;
                    } else {
                        notWord = gen.newMark();
                        isWord = gen.newMark();
                    }

                    AbstractMark againMark = gen.newMark();
                    gen.mark(againMark);

                    gen.load(V_HEAD, "I");
                    gen.load(V_END, "I");

                    refillIfHaveTo(gen.TOKEN_EE, againMark, notWord);

                    gen.load(V_STRING, charSequenceType);
                    gen.load(V_HEAD, "I");
                    genCharAt();
                    gen.dup("I");
                    gen.store(V_TMP1, "I");
                    gen.invokestatic("java/lang/Character", "isLetterOrDigit", "(C)Z");
                    gen.jumpIf(true, gen.TOKEN_NE, "Z", isWord);
                    gen.load(V_TMP1, "I");
                    gen.loadConst((int) '_');
                    if (type == '<')
                        gen.jumpIf(false, gen.TOKEN_NE, charType, failMark);
                    else {
                        gen.jumpIf(false, gen.TOKEN_EE, charType, isWord);
                        if (type != '>') {
                            gen.mark(notWord);
                            gen.loadConst(1);
                            gen.jump(cont);
                            gen.mark(isWord);
                            gen.loadConst(0);
                        }
                    }
                    gen.mark(cont);

                    // analyzing previous character
                    cont = gen.newMark();
                    if (type == '<') {
                        notWord = cont;
                        isWord = failMark;
                    } else if (type == '>') {
                        notWord = failMark;
                        isWord = cont;
                    } else {
                        notWord = gen.newMark();
                        isWord = gen.newMark();
                    }

                    gen.load(V_HEAD, "I");
                    if (embed) {
                        if (V_START < 0)
                            gen.jumpIf(true, gen.TOKEN_EE, "I", notWord);
                        else {
                            gen.load(V_START, "I");
                            gen.jumpIf(false, gen.TOKEN_EE, "I", notWord);
                        }
                    } else {
                        gen.load(0, thisType);
                        gen.getfield(stubClass, "start", "I");
                        gen.jumpIf(false, gen.TOKEN_EE, "I", notWord);
                    }
                    gen.load(V_STRING, charSequenceType);
                    gen.load(V_HEAD, "I");
                    gen.loadConst(1);
                    gen.op('-', "I");
                    genCharAt();
                    gen.dup("I");
                    gen.store(V_TMP1, "I");
                    gen.invokestatic("java/lang/Character", "isLetterOrDigit", "(C)Z");
                    gen.jumpIf(true, gen.TOKEN_NE, "Z", isWord);
                    gen.load(V_TMP1, "I");
                    gen.loadConst((int) '_');
                    if (type == '>')
                        gen.jumpIf(false, gen.TOKEN_NE, charType, failMark);
                    else {
                        gen.jumpIf(false, gen.TOKEN_EE, charType, isWord);
                        if (type != '<') {
                            gen.mark(notWord);
                            gen.loadConst(1);
                            gen.jump(cont);
                            gen.mark(isWord);
                            gen.loadConst(0);
                        }
                    }
                    gen.mark(cont);

                    if (type == 'b')
                        gen.jumpIf(false, gen.TOKEN_EE, "I", failMark);
                    else if (type == 'B')
                        gen.jumpIf(false, gen.TOKEN_NE, "I", failMark);
                }
                break;
                default:
                    throw new RuntimeException("Invalid boundary class: " + type);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void assert2(int charClass, char[] ranges) {
        try {
            AbstractMark againMark = gen.newMark();
            gen.mark(againMark);
            gen.load(V_HEAD, "I");
            gen.load(V_END, "I");
            refillIfHaveTo(againMark);

            // go through troubles only if not all possible characters
            if (ranges == null || ranges.length > 2 ||
                    ranges[0] != 0 || ranges[1] != 0xFFFF) {
                gen.load(V_STRING, charSequenceType);
                gen.load(V_HEAD, "I");
                genCharAt();
                if (ranges != null)
                    gen.store(V_TMP1, charType);
                AbstractMark inRange = gen.newMark();
                if (ranges != null)
                    if (charClass == CLASS_DISABLED || charClass == CLASS_NONE)
                        jumpIfInRange(ranges, null, failMark);
                    else
                        jumpIfInRange(ranges, inRange, null);
                switch (charClass) {
                    case CLASS_LETTER:
                    case CLASS_NONLETTER:
                        if (ranges != null)
                            gen.load(V_TMP1, charType);
                        gen.invokestatic("java/lang/Character", "isLetter", "(C)Z");
                        gen.jumpIf(charClass == CLASS_LETTER, true, gen.TOKEN_EE, "Z", failMark);
                        break;
                    case CLASS_UPPERCASE:
                    case CLASS_NONUPPERCASE:
                        if (ranges != null)
                            gen.load(V_TMP1, charType);
                        gen.invokestatic("java/lang/Character", "isUpperCase", "(C)Z");
                        gen.jumpIf(charClass == CLASS_UPPERCASE, true, gen.TOKEN_EE, "Z", failMark);
                        break;
                    case CLASS_LOWERCASE:
                    case CLASS_NONLOWERCASE:
                        if (ranges != null)
                            gen.load(V_TMP1, charType);
                        gen.invokestatic("java/lang/Character", "isLowerCase", "(C)Z");
                        gen.jumpIf(charClass == CLASS_LOWERCASE, true, gen.TOKEN_EE, "Z", failMark);
                        break;
                    default:
                        if (ranges == null) {
                            gen.pop(charType, 1);
                            if (charClass != CLASS_ALL)
                                gen.jump(failMark);
                        }
                        break;
                }
                gen.mark(inRange, initStackDepth);
            }
            gen.iinc(V_HEAD, 1);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void assert2(char[] constStr) {
        try {
            AbstractMark againMark = gen.newMark();
            gen.mark(againMark);
            gen.load(V_HEAD, "I");
            if (constStr.length == 1) {
                gen.load(V_END, "I");
            } else {
                gen.loadConst(constStr.length - 1);
                gen.op('+', "I");
                gen.load(V_END, "I");
            }
            refillIfHaveTo(againMark);
            for (int i = 0; i < constStr.length; i++) {
                gen.load(V_STRING, charSequenceType);
                gen.load(V_HEAD, "I");
                genCharAt();
                gen.loadConst((int) constStr[i]);
                gen.jumpIf(false, gen.TOKEN_NE, "I", failMark);
                gen.iinc(V_HEAD, 1);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void assert2(String varName, boolean picked) {
        try {
            Variable[] v = vars.get(varName);
            if (v == null)
                if (picked)
                    throw new RuntimeException("Variable " + varName + " not known!");
                else {
                    Variable v1 = new Variable();
                    v1.cell = varCells++;
                    v1.extCell = extCells;
                    Variable v2 = new Variable();
                    v2.cell = varCells++;
                    v2.extCell = extCells++;
                    Variable[] pair = {v1, v2};
                    v = pair;
                    vars.put(varName, v);
                }

            // we do not check for both variable's begin and end
            // indexes are set (i.e. != -1) because they are either
            // both set or both -1. We will match empty string in the later
            // case, which is our intention

            gen.load(V_CELLS, "[I");
            gen.loadConst(v[0].cell);
            gen.getelement("I");
            gen.store(V_TMP1, "I");

            AbstractMark againMark = gen.newMark();
            gen.mark(againMark);

            gen.load(V_CELLS, "[I");
            gen.loadConst(v[1].cell);
            gen.getelement("I");
            int V_TMP2 = V_RET1; // overlaps with V_RET1
            gen.store(V_TMP2, "I");

            gen.load(V_TMP2, "I");
            gen.load(V_TMP1, "I");
            gen.op('-', "I");
            gen.load(V_END, "I");
            gen.load(V_HEAD, "I");
            gen.op('-', "I");

            refillIfHaveTo('>', againMark, failMark);

            int varStr;
            boolean free = false;

            if (v[0].extCell >= 0) {
                // external variable
                if (embed) {
                    varStr = extVarRegs[v[0].extCell];
                } else {
                    gen.load(0, thisType);
                    gen.getfield(stubClass, "extCells", charSequenceArrType);
                    gen.loadConst(v[0].extCell);
                    gen.getelement(charSequenceType);
                    varStr = allocator.alloc();
                    free = true;
                    gen.store(varStr, charArrType);
                }
            } else
                varStr = V_STRING;

            AbstractMark loopStart = gen.newMark();
            gen.jump(loopStart);

            AbstractMark loopMark = gen.newMark();
            gen.mark(loopMark, initStackDepth);
            gen.load(varStr, charSequenceType);
            gen.load(V_TMP1, "I");
            genCharAt();
            gen.load(V_STRING, charSequenceType);
            gen.load(V_HEAD, "I");
            genCharAt();
            gen.jumpIf(false, gen.TOKEN_NE, charType, failMark);
            gen.iinc(V_HEAD, 1);
            gen.iinc(V_TMP1, 1);

            gen.mark(loopStart);
            gen.load(V_TMP1, "I");
            gen.load(V_TMP2, "I");
            gen.jumpIf(false, '<', "I", loopMark);

            if (free)
                allocator.free(varStr);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void decjump(RVariable var, RLabel label) {
        try {
            Integer cell = ((Variable) var).cell;
            gen.loadConst(cell);
            if (saveMark == null)
                saveMark = gen.newMark();
            gen.jsr(saveMark, -1);
            gen.load(V_CELLS, "[I");
            gen.loadConst(cell);
            gen.dup("2");
            gen.getelement("I");
            gen.loadConst(1);
            gen.op('-', "I");
            gen.insert("I", "2");
            gen.putelement("I");
            gen.jumpIf(true, '>', "I", ((Label) label).mark);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void decfail(RVariable var) {
        try {
            Integer cell = ((Variable) var).cell;
            gen.loadConst(cell);
            if (saveMark == null)
                saveMark = gen.newMark();
            gen.jsr(saveMark, -1);
            gen.load(V_CELLS, "[I");
            gen.loadConst(cell);
            gen.dup("2");
            gen.getelement("I");
            gen.loadConst(1);
            gen.op('-', "I");
            gen.insert("I", "2");
            gen.putelement("I");
            gen.jumpIf(true, '<', "I", failMark);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void forget(RVariable var) {
    }

    public void jump(RLabel label) {
        try {
            gen.jump(((Label) label).mark);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void fail() {
        try {
            gen.jump(failMark);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void hint(int flags, int minLength, int maxLength) {
        this.flags = flags;
        this.minLength = minLength;
        this.maxLength = maxLength;
    }

    //---- extended instructions ----
    //----- EXT_HINT extension

    public void mfStart(int headDecrement, int minCount) {
        if (mfStartMark != null)
            throw new IllegalStateException("Netsed mfStart-mfEnd calls or mfEnd call missing");

        try {

            mfHeadDecrement = headDecrement;
            mfMinCount = minCount;
            mfSaveFailMark = failMark;
            failMark = gen.newMark();

            V_MFCOUNT = allocator.alloc();
            V_MFHEAD = allocator.alloc();

            mfStartMark = gen.newMark();
            gen.load(V_HEAD, "I");
            gen.store(V_MFHEAD, "I");
            gen.loadConst(0);
            gen.store(V_MFCOUNT, "I");
            gen.mark(mfStartMark);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //----- EXT_MULTIFORK extension

    public void mfEnd(int maxCount) {
        if (mfStartMark == null)
            throw new IllegalStateException("mfStart call missing");
        try {
            AbstractMark maxCountFail = null;

            gen.iinc(V_MFCOUNT, 1);
            if (maxCount < Integer.MAX_VALUE) {
                gen.load(V_MFCOUNT, "I");
                gen.loadConst(maxCount);
                gen.jumpIf(false, '<', "I", mfStartMark);
                maxCountFail = gen.newMark();
                gen.jump(maxCountFail);
            } else
                gen.jump(mfStartMark);

            mfStartMark = null;

            gen.mark(failMark);

            if ((flags & HINT_CHAR_STAR_HEAD) != 0) {
                if (mfHeadDecrement != 1)
                    throw new RuntimeException("Internal error: HINT_CHAR_STAR_HEAD, but length != 1");
                gen.load(V_MFCOUNT, "I");
                gen.loadConst(mfMinCount + 1);
                gen.op('+', "I");
                gen.store(V_HEADINC, "I");
            }

            if (maxCountFail != null)
                gen.mark(maxCountFail);
            failMark = mfSaveFailMark;
            // prepare multifork

            gen.load(V_MFCOUNT, "I");
            if (mfHeadDecrement != 1) {
                gen.loadConst(mfHeadDecrement);
                gen.op('*', "I");
            }
            gen.load(V_MFHEAD, "I");
            gen.op('+', "I");
            gen.store(V_HEAD, "I");

            gen.iinc(V_MFCOUNT, -1);


            checkSize(3); // check fork stack size once and for all
            AbstractMark forkPush = gen.newMark();
            gen.jump(forkPush);

            AbstractMark fork = gen.newMark();
            switchTable.addElement(fork);
            int index = switchTable.size();

            gen.mark(fork, initStackDepth);
            //----- read special multifork item
            gen.iinc(V_FORKPTR, -1);
            gen.load(V_FORKS, "[I");
            gen.load(V_FORKPTR, "I");
            gen.getelement("I");
            gen.loadConst(1);
            gen.op('-', "I");
            gen.store(V_MFCOUNT, "I");

            // do not need to check size, will not use more then
            // on the previous fork

            gen.mark(forkPush);
            AbstractMark cont = gen.newMark();
            gen.load(V_MFCOUNT, "I");
            gen.jumpIf(true, '<', "I", cont);

            //----- write special multifork item
            gen.load(V_FORKS, "[I");
            gen.load(V_FORKPTR, "I");
            gen.load(V_MFCOUNT, "I");
            gen.putelement("I");
            gen.iinc(V_FORKPTR, 1);

            //----- write regular fork items, decrement head
            // save head
            gen.load(V_FORKS, "[I");
            gen.load(V_FORKPTR, "I");
            gen.load(V_HEAD, "I");
            gen.loadConst(mfHeadDecrement);
            gen.op('-', "I");
            gen.putelement("I");
            gen.iinc(V_FORKPTR, 1);

            // save fork location
            gen.load(V_FORKS, "[I");
            gen.load(V_FORKPTR, "I");
            gen.loadConst(-index);
            gen.putelement("I");
            gen.iinc(V_FORKPTR, 1);

            gen.mark(cont);

            allocator.free(V_MFHEAD);
            allocator.free(V_MFCOUNT);
            V_MFCOUNT = -1;
            V_MFHEAD = -1;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Jump if char is NOT in range
     */
    public void condJump(char[] ranges, RLabel label) {
        AbstractMark onFail = failMark;
        if (label != null)
            onFail = ((Label) label).mark;
        try {
            AbstractMark againMark = gen.newMark();
            gen.mark(againMark);
            gen.load(V_HEAD, "I");
            gen.load(V_END, "I");
            refillIfHaveTo(gen.TOKEN_GE, againMark, onFail);

            // go through troubles only if not all possible characters
            if (ranges == null || ranges.length > 2 || ranges[0] != 0 || ranges[1] != 0xFFFF) {
                gen.load(V_STRING, charSequenceType);
                gen.load(V_HEAD, "I");
                genCharAt();
                gen.store(V_TMP1, charType);
                AbstractMark inRange = gen.newMark();
                jumpIfInRange(ranges, null, onFail);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //----- EXT_CONDJUMP extension

    /**
     * Jump if less then atLeast or more then atMost chars left. If it is
     * hard to determine how much left, it is OK not to jump.
     */
    public void condJump(int atLeast, int atMost, RLabel label) {
        if (!embed && V_REFILLER >= 0)
            return; // this optimization is not available

        AbstractMark onFail = failMark;
        if (label != null)
            onFail = ((Label) label).mark;
        try {
            if (atLeast > 0) {
                gen.load(V_HEAD, "I");
                gen.loadConst(atLeast);
                gen.op('+', "I");
                gen.load(V_END, "I");
                gen.jumpIf(false, '>', "I", onFail);
            }
	/*
	// this is OK to do ONLY when we are matching, not searching!!!!!
	// so I disable it for now, it probably makes sence to enable this only
	// in case of embedded jitrex, when we know for sure, as checking
	// field "searching" will be too expensive
	if( atMost < 1024 )
	  {
	    gen.load( V_HEAD, "I" );
	    gen.loadConst( new Integer( atMost ) );
	    gen.op( '+', "I" );
	    gen.load( V_END, "I" );
	    gen.jumpIf( false, '<', "I", onFail );
	  }
	*/
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Jump if char is NOT one that is given.
     */
    public void condJump(char c, RLabel label) {
        AbstractMark onFail = failMark;
        if (label != null)
            onFail = ((Label) label).mark;
        try {
            AbstractMark againMark = gen.newMark();
            gen.mark(againMark);
            gen.load(V_HEAD, "I");
            gen.load(V_END, "I");
            refillIfHaveTo(gen.TOKEN_GE, againMark, onFail);
            gen.load(V_STRING, charSequenceType);
            gen.load(V_HEAD, "I");
            genCharAt();
            gen.loadConst(c);
            gen.jumpIf(false, gen.TOKEN_NE, charType, onFail);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void shiftTable(boolean beginning, int charsAhead, char[] chars, int[] shifts) {

        if (embed && !embedSearch)
            return;

        try {
            AbstractMark skipMark = gen.newMark();

            if (!embed) {
                gen.load(0, thisType);
                gen.getfield(stubClass, "searching", "Z");
                gen.jumpIf(true, gen.TOKEN_EE, "I", skipMark);
            }

            gen.load(V_HEAD, "I");
            gen.loadConst(charsAhead);
            gen.op('+', "I");

            AbstractMark tryAgain = gen.newMark();
            gen.mark(tryAgain);
            gen.store(V_TMP1, "I");


            AbstractMark returnPlus1Mark = gen.newMark(); // when nothing left
            AbstractMark returnMark = gen.newMark(); // return becase we are not searching
            AbstractMark shiftMaxMark = gen.newMark(); // return becase we are not searching


	/*
	gen.dup( "I" );
	gen.getstatic( "java/lang/System", "out", "Ljava/io/PrintStream;" );
	gen.swap();
	gen.invokevirtual( "java/io/PrintStream", "println", "(I)V" );
	*/

            AbstractMark afterRefillMark = gen.newMark();
            gen.mark(afterRefillMark);
            gen.load(V_TMP1, "I");
            gen.load(V_TMP1, "I");
            gen.load(V_END, "I");
            refillIfHaveTo(gen.TOKEN_GE, afterRefillMark, returnPlus1Mark);

            //gen.load(V_TMP1, "I");
            gen.load(V_STRING, charSequenceType);
            gen.load(V_TMP1, "I");
            genCharAt();
            // stack now has: lookup lookupChar

            AbstractMark zeroShift = gen.newMark();
            int tableSize = chars[chars.length - 1] - chars[0] + 1;
            if (2 * tableSize < chars.length + 5) {
                // tableswitch
                AbstractMark[] markTable = new AbstractMark[tableSize];
                char caseValue = chars[0];
                int j = 0;
                for (int i = 0; i < tableSize; i++) {
                    if (chars[j] == caseValue) {
                        if (shifts[j++] == 0)
                            markTable[i] = zeroShift;
                        else
                            markTable[i] = gen.newMark();
                    } else
                        markTable[i] = returnPlus1Mark;
                    caseValue++;
                }
                gen.tableswitch(chars[0], markTable, shiftMaxMark);
                caseValue = chars[0];
                j = 0;
                for (int i = 0; i < tableSize; i++) {
                    if (chars[j] == caseValue) {
                        if (shifts[j] != 0) {
                            gen.mark(markTable[i]);
                            gen.loadConst(shifts[j]);
                            gen.op('+', "I");
                            gen.jump(tryAgain);
                        }
                        j++;
                    }
                    caseValue++;
                }
            } else {
                // lookupswitch
                tableSize = chars.length;
                int[] caseValues = new int[tableSize];
                AbstractMark[] markTable = new AbstractMark[tableSize];
                for (int i = 0; i < tableSize; i++) {
                    caseValues[i] = chars[i];
                    if (shifts[i] != 0)
                        markTable[i] = gen.newMark();
                    else
                        markTable[i] = zeroShift;
                }
                gen.lookupswitch(caseValues, markTable, shiftMaxMark);
                for (int i = 0; i < tableSize; i++) {
                    if (shifts[i] != 0) {
                        gen.mark(markTable[i]);
                        gen.loadConst(shifts[i]);
                        gen.op('+', "I");
                        gen.jump(tryAgain);
                    }
                }
            }

            gen.mark(shiftMaxMark);
            gen.loadConst(charsAhead);
            gen.op('+', "I");
            gen.jump(tryAgain);

            gen.mark(returnPlus1Mark);
            if (embed) {
                gen.load(V_TMP1, "I");
                gen.loadConst(1);
                gen.op('+', "I");
                gen.store(V_BEGIN, "I");
                gen.jump(matchFailedMark);
            } else {
                gen.load(0, thisType);
                gen.load(V_TMP1, "I");
                gen.loadConst(1);
                gen.op('+', "I");
                gen.putfield(stubClass, "headStart", "I");
                gen.loadConst(Boolean.FALSE);
                gen.retrn("Z");
            }

            gen.mark(zeroShift);
            // adjust V_HEAD and headStart
            gen.loadConst(charsAhead);
            gen.op('-', "I");
            gen.store(V_HEAD, "I");
            if (embed) {
                gen.load(V_HEAD, "I");
                gen.store(V_BEGIN, "I");
            } else {
                gen.load(0, thisType);
                gen.load(V_HEAD, "I");
                gen.putfield(stubClass, "headStart", "I");
            }
            gen.mark(skipMark);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //----- EXT_SHIFTTBL extension

    static class Label extends RLabel {
        AbstractMark mark;
    }

    //----- loader

    static class Loader extends ClassLoader {

        ClassLoader chain;

        Loader() {
            chain = getClass().getClassLoader();
        }

        Class makeClass(String name, byte[] body) {
            try {
                name = name.replace('/', '.');
                Class result = defineClass(name, body, 0, body.length);
                resolveClass(result);
                return result;
            } catch (ClassFormatError e) {
                throw new RuntimeException("Internal error: class format error: " + e.getMessage());
            }
        }

        public Class loadClass(String name, boolean resolveIt)
                throws ClassNotFoundException {
            if (chain != null)
                return chain.loadClass(name);
            else
                return super.findSystemClass(name);
        }

        public Class loadClass(String name)
                throws ClassNotFoundException {
            return loadClass(name, true);
        }

        public java.io.InputStream getResourceAsStream(String resource) {
            if (chain != null)
                return chain.getResourceAsStream(resource);
            else
                return getSystemResourceAsStream(resource);
        }

        public java.net.URL getResource(String name) {
            if (chain != null)
                return chain.getResource(name);
            else
                return getSystemResource(name);
        }

    }

}
