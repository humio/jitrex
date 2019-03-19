/*
    Copyright (c) Peter Sorotokin, 1998-2000
    Copyright (c) Humio, 2019
    See file "LICENSE.md" for terms of usage and
    redistribution.   
*/
package com.humio.regex.form;

import java.io.IOException;
import java.io.Writer;

public class NewLineSpan extends Span {

    static char[] newline = {'\n'};

    static {
        try {
            newline = System.getProperty("line.separator").toCharArray();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void print(Writer out, int[] argPtr, Object[] args) {
        try {
            out.write(newline);
        } catch (IOException e) {
            throw new RuntimeException("IOException: " + e.getMessage());
        }
    }

}
