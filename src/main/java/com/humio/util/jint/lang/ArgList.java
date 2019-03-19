/*
    Copyright (c) Peter Sorotokin, 1998-2000
    Copyright (c) Humio, 2019
    See file "LICENSE.md" for terms of usage and
    redistribution.   
*/
package com.humio.util.jint.lang;

import java.util.Vector;

/**
 * An ArgList object represets variable-length parameter list. When the last
 * formal parameter in method is declared as ArgList, Jint compiler
 * allows any number (including 0) of actual parameters of any type to match it.
 * These parameters are wrapped if needed (so <code>int</code> becomes <code>Integer</code>)
 * and combined
 * into single ArgList object that is passed as a value for the last parameter.
 * If the <b>static</b> type of last actual parameter is ArgList, though, <b>and</b>
 * it is the <b>only one</b> that corresponds to ArgList formal parameter it will
 * be passed "as is".
 * <p>
 * EXAMPLE:
 * <pre>
 *  void foo( String form // regular parameter
 *            ArgList a // extra parameters
 *           )
 *  {
 *    int nExtra = a.length();
 *    Object arg1 = a.get(0); // first extra parameter
 *    Object arg1 = a.get(1); // second extra parameter
 *  }
 *  </pre>
 * Note: the first expression is valid only in Jint, the second one has the same meaning
 * in Java and Jint.
 * <pre>
 *  foo( "bar" ) is equivalent to foo( "bar", new ArgList() )
 *  foo( "bar", 1 ) is equivalent to foo( "bar", new ArgList( new Integer(1) ) )
 *  foo( "bar", 2.0, "a" ) is equivalent to foo( "bar", new ArgList( new Double(2.0), "a" ) )
 *  </pre>
 */
public final class ArgList extends Vector<Object> {
    /**
     * Creates ArgList from the array of values.
     */
    public ArgList(Object[] argList) {
        this(argList, 0);
    }

    /**
     * Creates ArgList from the array of values, starting with value argList[offset].
     */
    public ArgList(Object[] argList, int offset) {
        super(argList == null ? 0 : argList.length - offset);
        if (argList != null) {
            for (int i = offset; i < argList.length; i++)
                addElement(argList[i]);
        }
    }

    /**
     * Creates empty ArgList.
     */
    public ArgList() {
    }

    /**
     * Creates empty ArgList optimized to store given number of elements.
     */
    public ArgList(int n) {
        super(n);
    }

    /**
     * Creates ArgList that contains only one value.
     */
    public ArgList(Object arg1) {
        super(1);
        addElement(arg1);
    }

    /**
     * Creates ArgList that contains two values.
     */
    public ArgList(Object arg1, Object arg2) {
        super(2);
        addElement(arg1);
        addElement(arg2);
    }

    /**
     * Creates ArgList that contains three values.
     */
    public ArgList(Object arg1, Object arg2, Object arg3) {
        super(3);
        addElement(arg1);
        addElement(arg2);
        addElement(arg3);
    }

    /**
     * Creates ArgList that contains four values. Use ArgList(Object[]) to
     * create ArgList with more then four values.
     */
    public ArgList(Object arg1, Object arg2, Object arg3, Object arg4) {
        super(4);
        addElement(arg1);
        addElement(arg2);
        addElement(arg3);
        addElement(arg4);
    }

    /**
     * Returns the list of objects in the arglist.
     */
    public Object[] getList() {
        Object[] list = new Object[size()];
        copyInto(list);
        return list;
    }

    /**
     * Returns the number of objects in the arglist
     */
    public int length() {
        return size();
    }

    /**
     * Returns object in the arglist by its index. The first object
     * has index 0.
     */
    public Object get(int i) {
        return elementAt(i);
    }
}
