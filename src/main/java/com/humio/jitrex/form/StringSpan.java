/*
    Copyright (c) Peter Sorotokin, 1998-2000
    Copyright (c) Humio, 2019
    See file "LICENSE.md" for terms of usage and
    redistribution.   
*/
package com.humio.jitrex.form;

import java.io.IOException;
import java.io.Writer;

public class StringSpan extends Span {

    String str;

    public StringSpan(String s) {
        str = s;
    }

    public void print(Writer out, int[] argPtr, Object[] args) {
        try {
            out.write(str);
        } catch (IOException e) {
            throw new RuntimeException("IOException: " + e.getMessage());
        }
    }

}
