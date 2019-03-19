/*
    Copyright (c) Peter Sorotokin, 1998-2000
    See file "LICENSE.md" for terms of usage and
    redistribution.   
*/
package com.humio.jitrex.tools;

import com.humio.jitrex.util.Regex;

import java.io.File;

public class Dir {
    public static void main(String[] args) {
        Regex filter = Regex.createFilePattern(args[0]);
        File f = new File(".");
        String[] list = f.list(filter);
        for (int i = 0; i < list.length; i++)
            System.out.println(list[i]);
    }
}

