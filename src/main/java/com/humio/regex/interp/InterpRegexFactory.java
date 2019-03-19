/*
    Copyright (c) Peter Sorotokin, 1998-2000
    Copyright (c) Humio, 2019
    See file "LICENSE.md" for terms of usage and
    redistribution.   
*/
package com.humio.regex.interp;

import com.humio.regex.compiler.RCompiler;
import com.humio.regex.compiler.RMachine;
import com.humio.regex.parser.RParser;
import com.humio.regex.tree.RNode;
import com.humio.regex.util.Regex;
import com.humio.regex.util.RegexFactory;

public class InterpRegexFactory extends RegexFactory {

    public InterpRegexFactory() {
    }

    protected Regex createRegex(char[] arr, int off, int len,
                                boolean lowerCase, boolean filePattern) {
        RParser parser = new RParser(
                (filePattern ? RParser.FILE_PATTERN_SYNTAX : 0), lowerCase ? Regex.CASE_INSENSITIVE : 0);
        RNode regexNode = parser.parse(arr, off, len, false);
        RMachine machine = new RInterpMachine();
        RCompiler comp = new RCompiler(machine);
        String name;
        if (len < 28)
            name = new String(arr, off, len);
        else
            name = new String(arr, off, 28);
        comp.compile(regexNode, name);
        return machine.makeRegex();
    }
}
