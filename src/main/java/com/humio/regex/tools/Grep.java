/*
    Copyright (c) Peter Sorotokin, 1998-2000
    See file "LICENSE.md" for terms of usage and
    redistribution.   
*/
package com.humio.regex.tools;

import com.humio.regex.interp.InterpRegexFactory;
import com.humio.util.jint.util.CompilerException;
import com.humio.regex.util.CaseInsensitiveRegex;
import com.humio.regex.util.Regex;

import java.io.*;
import java.util.Enumeration;
import java.util.Vector;

public class Grep {

    private static void processInput(BufferedReader in, String prefix, Regex regex,
                                     boolean revert, boolean numberLines, String[] vars)
            throws IOException {
        String line;
        int count = 1;
        while ((line = in.readLine()) != null) {
            if (regex.searchOnce(line) != revert) {
                if (prefix != null)
                    System.out.print(prefix);
                if (numberLines)
                    System.out.print(count + ":");
                System.out.println(line);
                if (vars != null && !revert) {
                    for (int i = 0; i < vars.length; i++)
                        System.out.println("\t" + vars[i] + " = \"" + regex.get(vars[i]) + "\"");
                }
            }
            count++;
        }
    }

    public static void main(String[] args) {
        int last = 0;
        boolean compile = true;
        boolean revert = false;
        boolean numberLines = false;
        boolean ignoreCase = false;
        while (last < args.length && args[last].startsWith("-")) {
            String option = args[last++];
            if (option.equals("--"))
                break;
            if (option.equals("--interp"))
                compile = false;
            else if (option.equals("-v") || option.equals("--revert-match"))
                revert = true;
            else if (option.equals("-i") || option.equals("--ignore-case"))
                ignoreCase = true;
            else if (option.equals("-n") || option.equals("--line-number"))
                numberLines = true;
            else
                last = args.length; // to force help info
        }
        if (args.length == last) {
            System.err.println("Usage: java kmy.regex.util.Grep regex [ file ... ]");
            System.err.println("\tuse perl syntax for regular expressions");
            System.err.println();
            System.err.println("Options:");
            System.err.println("\t--interp            - do not compile regex into class (slower)");
            System.err.println("\t-n, --line-number   - print line numbers");
            System.err.println("\t-i, --ignore-case   - ignore case in matching");
            System.err.println("\t-v, --revert-match  - select non-matching lines");
            return;
        }
        Regex regex;
        String str = args[last++];
        if (!compile)
            Regex.setFactory(new InterpRegexFactory());
        try {
            if (ignoreCase)
                regex = new CaseInsensitiveRegex(str);
            else
                regex = Regex.createRegex(str);
        } catch (CompilerException e) {
            System.err.println("\t" + args[0]);
            System.err.print("\t");
            for (int p = e.getFilePos(); p > 0; p--)
                System.err.print(' ');
            System.err.println('^');
            System.err.println("Error: " + e.getMessage());
            return;
        } catch (Exception e) {
            System.err.println("Internal error: " + e.toString());
            return;
        }

        boolean processStdin = last == args.length;
        boolean printFileName = last < args.length - 1;

        String[] vars;
        Vector<String> varAcc = new Vector<String>();
        Enumeration varList = regex.variables();
        while (varList.hasMoreElements()) {
            String name = (String) varList.nextElement();
            if (Character.isLetter(name.charAt(0)))
                varAcc.addElement(name);
        }
        if (varAcc.size() > 0) {
            vars = new String[varAcc.size()];
            varAcc.copyInto(vars);
        } else
            vars = null;

        do {
            try {
                String prefix = null;
                Reader input;
                if (processStdin || args[last].equals("-"))
                    input = new InputStreamReader(System.in);
                else {
                    input = new FileReader(args[last]);
                    if (printFileName)
                        prefix = args[last] + ": ";
                }
                BufferedReader lineReader = new BufferedReader(input);
                processInput(lineReader, prefix, regex, revert, numberLines, vars);
            } catch (IOException e) {
                System.err.println("File reading error: " + e.getMessage());
            }
            last++;
        }
        while (last < args.length);
    }
}
