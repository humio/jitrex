/*
    Copyright (c) Peter Sorotokin, 1998-2000
    Copyright (c) Humio, 2019
    See file "LICENSE.md" for terms of usage and
    redistribution.   
*/
package com.humio.util.jint.util;

import com.humio.util.jint.lang.ArgList;
import com.humio.jitrex.util.Formatter;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Hashtable;

public class CompilerException extends RuntimeException {

    static Hashtable<Object,String> messages;

    static {
        try {
            messages = new Hashtable<>();

            InputStream in = openResource("com/humio/util/jint/util/errors.txt");
            if (in == null)
                in = openResource("com/humio/util/jint/util/mini_errors.txt");

            if (in != null) {
                Class errorCodes1;
                Class errorCodes2;
                try {
                    errorCodes1 = Class.forName("com.humio.util.jint.constants.ErrorCodes");
                    errorCodes2 = Class.forName("com.humio.util.jint.constants.MiniErrorCodes");
                } catch (ClassNotFoundException e) {
                    errorCodes1 = Class.forName("com.humio.util.jint.constants.MiniErrorCodes");
                    errorCodes2 = errorCodes1;
                }
                BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line.startsWith("#"))
                        continue;
                    int index = line.indexOf(' ');
                    int index2 = line.indexOf('\t');
                    if (index < 0 || index > index2)
                        index = index2;
                    if (index < 0)
                        continue;
                    String name = line.substring(0, index);
                    String message = line.substring(index + 1).trim();
                    try {
                        messages.put(errorCodes1.getField(name).get(null), message);
                    } catch (Exception e) {
                        // this is kaffe bug workaround
                        try {
                            messages.put(errorCodes2.getField(name).get(null), message);
                        } catch (Exception e1) {
                            // e.printStackTrace();
                        }
                    }
                }
                reader.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    Exception wrapped;
    int fpos;
    int code;
    Object[] attachments;

    public CompilerException(int code, int fpos, Exception e, Object[] att) {
        super(makeMessage(code, att));
        wrapped = e;
        this.fpos = fpos;
        this.code = code;
        //printStackTrace();
    }

    public CompilerException(int code, int fpos) {
        this(code, fpos, null, null);
    }

    public CompilerException(int code) {
        this(code, -1, null, null);
    }

    public CompilerException(int code, int fpos, Object[] att) {
        this(code, fpos, null, att);
    }

    public CompilerException(int code, int fpos, Exception e) {
        this(code, fpos, e, makeAddInfo(e));
    }

    private static Object[] makeAddInfo(Exception e) {
        Object[] addInfo = {e.toString()};
        return addInfo;
    }

    static String makeMessage(int code, Object[] att) {
        String msg = (String) messages.get(code);
        if (msg != null) {
            if (msg.indexOf('%') >= 0 && att != null)
                msg = Formatter.sprintf(msg, new ArgList(att));
            return msg;
        } else
            return "CompilerError[" + Integer.toHexString(code) + "]";
    }

    private static InputStream openResource(String name) {
        ClassLoader loader = CompilerException.class.getClassLoader();
        if (loader != null)
            return loader.getResourceAsStream(name);
        else
            return ClassLoader.getSystemResourceAsStream(name);
    }

    public Exception getException() {
        return wrapped;
    }

    public int getFilePos() {
        return fpos;
    }

    public void setFilePos(int p) {
        fpos = p;
    }

    public int getCode() {
        return code;
    }

    public int getLine() {
        // for jint, not jitrex
        return fpos >>> 10;
    }

    public int getColumn() {
        // for jint, not jitrex
        return fpos & 0x3FF;
    }

    public String toString() {
        if (wrapped != null)
            return "CompilerException[" + getMessage() + "," + wrapped.toString() + "]";
        else
            return super.toString();
    }

    public void printStackTrace() {
        super.printStackTrace();
        if (wrapped != null) {
            System.err.print("Wrapped Exception:");
            wrapped.printStackTrace();
        }
    }

}
