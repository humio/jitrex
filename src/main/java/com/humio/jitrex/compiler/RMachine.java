/*
    Copyright (c) Peter Sorotokin, 1998-2000
    Copyright (c) Humio, 2019
    See file "LICENSE.md" for terms of usage and
    redistribution.   
*/
package com.humio.jitrex.compiler;

import com.humio.jitrex.util.Regex;

/**
 * This class represents "jitrex matching machine" that can be loaded with
 * "RMachine instructions" and used later for jitrex matching. Subclasses
 * of this class are used together with RCompiler (which compiles jitrex
 * parsing tree into RMachine instructions). This class is used in the
 * following fashion:
 * <ul>
 * <li>First, hint method may be called to provide some properties of the
 * jitrex about to be compiled.
 * <li>Next, init() is called.
 * <li>Next, various RMachine "instructions" are inserted (by calling
 * corresponding RMachine methods).
 * <li>Next, finish() is called.
 * <li>Finally, call to makeRegex() returns the result of compilation.
 * </ul>
 * Single RMachine object is expected to be used only to compile a single
 * regular expression.
 * <p>
 * RMachine can have a number of variables, and several character
 * buffers (which a "pre-set" at runtime by a method external to RMachine
 * (see Regex class documentation)). One of this buffers (main buffer) holds
 * the string which this jitrex is being matched against. It also has an implicit
 * character position that points into main buffer, as well as implicit
 * instruction pointer that points to the RMachine instruction about to be executed.
 * There is also a backtracing mechanism. When fork instruction is executed by
 * the machine, it stores the value of the current character position and
 * instruction pointer (that corresponds to the label given to the fork instruction)
 * into special internal <i>backtracing stack</i> as a <i>fork record</i>.
 * Also, for most modifications of RMachine variables (except for
 * the hardAssign instruction), the original value (before assignment) is
 * stored into backtracing stack. When fail instruction is executed, backtracing
 * is performed. Information in backtracing stack is used to reverse assignments,
 * reset character position and jump to the label given to the last fork instruction.
 * Backtracing stack is cleared up to (and including) the last fork record. If
 * not fork record is found in the stack, jitrex matching fails.
 */
public abstract class RMachine {

    public static final int HINT_START_ANCHORED = 1;
    public static final int HINT_END_ANCHORED = 2;
    public static final int HINT_CHAR_STAR_HEAD = 4; // .* a* [a-z]* type head
    public static final int FLAG_DOT_IS_ANY = Regex.DOTALL << 16; // . can by any char, not only non-newline
    public static final int FLAG_MULTILINE  = Regex.MULTILINE << 16; // . can by any char, not only non-newline
    public static final int FLAG_IGNORECASE = Regex.CASE_INSENSITIVE << 16; // . can by any char, not only non-newline

    public static final int EXT_HINT = 0x0001; // hint
    public static final int EXT_MULTIFORK = 0x0002; // multiFork and inc

    //----- basic instructions
    public static final int EXT_CONDJUMP = 0x0008; // condJumps
    public static final int EXT_SHIFTTBL = 0x0010; // shiftTable
    private int extensions;

    public Regex makeRegex() {
        return null;
    }

    public int getNVars() {
        return 0;
    }

    public void init() {
    }

    public void finish() {
    }

    public void setNoRefiller(boolean norefiller) {
    }

    /**
     * Provides string representation of this regular expression. It does
     * not alter jitrex functionality. This string can be returned by resulting
     * Regex toString() method, for example.
     */
    public void tellName(String name) {
    }

    /**
     * Informs RMachine about current character position in the jitrex. It does
     * not alter jitrex functionality. Can be used for debugging.
     */
    public void tellPosition(int pos) {
    }

    /**
     * Creates a new RMachine named variable. Such variable can be used to hold
     * position in the string that jitrex is being matched against. Every variable name
     * actually corresponds to a substring so two positions are needed: one for
     * the beginning of substring and one for the end (points to the first character
     * <b>after</i> substring). Parameter <i>begin</i> is used to tell which variable
     * is needed.
     */
    abstract public RVariable newVar(String name, boolean begin);

    /**
     * Creates a new RMachine label. Label can be used to mark RMachine
     * instruction and can be jumped to.
     */
    abstract public RLabel newLabel();

    /**
     * Creates a new RMachine temporary variable. Such variable can be used to hold
     * loop counter or any other integer.
     * Adds an assignment-reversion record to backtracing stack.
     *
     * @param init initial value for the variable
     */
    abstract public RVariable newTmpVar(int init);

    /**
     * Makes the given label to refer to the <b>next</i> RMachine instruction. Once
     * a label is marked, it cannot be marked again.
     */
    abstract public void mark(RLabel label);

    /**
     * Store current character position (in the main buffer) into a given variable.
     * Adds an assignment-reversion record to backtracing stack.
     */
    abstract public void pick(RVariable v);

    /**
     * Add a fork record to backtracing stack. If subsequent fail transfers control
     * to this record, instruction pointer will be set to the given label.
     */
    abstract public void fork(RLabel forkLabel);

    /**
     * Increment current position by 1 (skip a character).
     */
    abstract public void skip();

    /**
     * Check if current position is on the certain type of boundary given by.
     * <i>boundaryClass</i>. Boundary types:
     * <ul>
     * <li>'^' or 'A' - beginning of the string being matched.
     * <li>'$' or 'Z' - end of the string being matched.
     * <li>'&lt;' - word beginning.
     * <li>'&gt;' - word end.
     * <li>'b' - word beginning or end.
     * <li>'B' - neither word beginning nor end.
     * </ul>
     */
    abstract public void boundary(int boundaryClass);

    /**
     * Make sure that current character belongs to the given character class. If
     * it does, increment current char position by 1, otherwise fail.
     * See kmy.jitrex.tree.CharSet and kmy.jitrex.tree.CharClassCodes.
     */
    abstract public void assert2(int charClass, char[] ranges);

    //---- extended instructions ----

    abstract public void assert2(char[] constStr);

    abstract public void assert2(String varName, boolean picked);

    abstract public void hardAssign(RVariable v, int value);

    abstract public void decjump(RVariable var, RLabel label);

    abstract public void decfail(RVariable var);

    abstract public void forget(RVariable var);

    abstract public void jump(RLabel label);

    abstract public void fail();

    public int getExtensions() {
        return extensions;
    }

    public void setExtensions(int ext) {
        extensions = ext;
    }

    //----- EXT_HINT extension

    public void hint(int flags, int minLength, int maxLength) {
        // can ignore
    }

    //----- EXT_MULTIFORK extension

    public void mfStart(int headDecrement, int minCount) {
        throw new IllegalArgumentException("MULTIFORK extension is not implemented");
    }

    /**
     * maxCount got minCount subtracted from it!
     */
    public void mfEnd(int maxCount) {
        throw new IllegalArgumentException("MULTIFORK extension is not implemented");
    }

    //----- EXT_CONDJUMP extension

    /**
     * Jump if char is NOT in range
     */
    public void condJump(char[] ranges, RLabel label) {
        throw new IllegalArgumentException("CONDJUMP extension is not implemented");
    }

    /**
     * Jump if less then atLeast or more then atMost chars left. If it is
     * hard to determine how much left, it is OK not to jump.
     */
    public void condJump(int atLeastCharLeft, int atMostCharLeft, RLabel label) {
        throw new IllegalArgumentException("CONDJUMP extension is not implemented");
    }

    /**
     * Jump if char is NOT one that is given.
     */
    public void condJump(char c, RLabel label) {
        throw new IllegalArgumentException("CONDJUMP extension is not implemented");
    }

    //----- EXT_SHIFTTBL extension

    public void shiftTable(boolean beginning, int charsAhead, char[] chars, int[] shifts) {
        throw new IllegalArgumentException("SHIFTTBL extension is not implemented");
    }

    public void setFlags(int flags) {
        extensions = (extensions & 0xffff) | (flags << 16);
    }
}



