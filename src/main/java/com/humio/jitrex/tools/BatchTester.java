/*
    Copyright (c) Peter Sorotokin, 1998-2000
    See file "LICENSE.md" for terms of usage and
    redistribution.   
*/
package com.humio.jitrex.tools;

import com.humio.jitrex.compiler.RCompiler;
import com.humio.jitrex.compiler.RDebugMachine;
import com.humio.jitrex.compiler.RMachine;
import com.humio.jitrex.interp.RInterpMachine;
import com.humio.jitrex.parser.RParser;
import com.humio.jitrex.util.RegexRefiller;
import com.humio.jitrex.jvm.RJavaClassMachine;
import com.humio.jitrex.tree.CharSet;
import com.humio.jitrex.tree.RNode;
import com.humio.jitrex.util.Regex;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.util.Enumeration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class is used to batch-test regular expression. Input is assumed to be in
 * file jitrex.txt.
 */
public class BatchTester {
    /**
     * This is pattern testing method.
     */
    static void test(String patt) {
        RNode regex = (new RParser()).parse(patt);
        System.out.println(regex);
        System.out.println("\tmin = " + regex.minLeft + " max = " +
                (regex.maxLeft == Integer.MAX_VALUE ? "*" : "" + regex.maxLeft));
        System.out.println("\tprefix: " + regex.findPrefix(CharSet.FULL_CHARSET));
        System.out.println();
        RDebugMachine debug = new RDebugMachine();
        RCompiler dcomp = new RCompiler(debug);
        dcomp.compile(regex, patt);
        System.out.println("\t.stop");

        Regex re = null;

        try {
            RJavaClassMachine jmachine = new RJavaClassMachine();
            jmachine.setSaveBytecode(true);
            RCompiler jcomp = new RCompiler(jmachine);
            jcomp.compile(regex, patt);
            re = jmachine.makeRegex();
        } catch (Exception e) {
            e.printStackTrace();
        }

        //re = null;

        if (re == null) {
            System.out.println("Using interpreter!!!!");
            RInterpMachine machine = new RInterpMachine();
            RCompiler comp = new RCompiler(machine);
            comp.compile(regex, patt);
            re = machine.makeRegex();
        }

        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
            while (true) {
                String str = in.readLine();
                if (str == null)
                    break;
                char[] test = str.toCharArray();
                System.out.println("--> " + re.searchOnce(str));
                re.init(test);
                try {
                    re.set("a", "kmy");
                } catch (Exception e) {
                    e.printStackTrace();
                }
                while (re.search()) {
                    System.out.print("\tMATCH");
                    Enumeration vars = re.variables();
                    while (vars.hasMoreElements()) {
                        String nm = (String) vars.nextElement();
                        int vb = re.getVariableHandle(nm, true);
                        int ve = re.getVariableHandle(nm, false);
                        vb = re.getIndex(vb);
                        ve = re.getIndex(ve);
                        CharSequence vbuf = re.getCharBuffer(re.getExtVariableHandle(nm));
                        String val = (ve >= 0 && vb >= 0 ? vbuf.subSequence(vb, ve).toString() : "");
                        System.out.print(" $" + nm + "=" + val);
                    }
                    System.out.println();
                }
                System.out.println("\tDONE");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        if (args.length > 0 && !args[0].startsWith("-")) {
            try {
                test(args[0]);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return;
        }
        boolean verbose = args.length > 0 && args[0].indexOf('v') >= 0;
        boolean perf = args.length > 0 && args[0].indexOf('p') >= 0;
        boolean nomultifork = args.length > 0 && args[0].indexOf('f') >= 0;
        boolean nohints = args.length > 0 && args[0].indexOf('h') >= 0;
        boolean nocharleft = args.length > 0 && args[0].indexOf('j') >= 0; // cond jump
        boolean compile = args.length > 0 && args[0].indexOf('c') >= 0;
        boolean noshifttbl = args.length > 0 && args[0].indexOf('s') >= 0;
        boolean precompile = args.length > 0 && args[0].indexOf('P') >= 0;
        boolean noimplicit = args.length > 0 && args[0].indexOf('I') >= 0;
        boolean norefiller = args.length > 0 && args[0].indexOf('R') >= 0;
        boolean userefiller = args.length > 0 && args[0].indexOf('r') >= 0;

        long total = 0;
        long jtotal = 0;

        RegexRefiller refiller = new RegexRefiller() {
            public int refill(Regex regex, int boundary) {
                //System.out.println( "Refill: " + jitrex + " " + boundary );
                CharSequence buffer = regex.getCharBuffer(-1);
                if (buffer.length() <= boundary) {
                    regex.setRefiller(null);
                    return boundary; // returning -1 here means "exit right away"
                }
                return boundary + 1;
            }
        };

        try {
            BufferedReader in = new BufferedReader(new FileReader("jitrex.txt"));
            Regex re = null;
            Pattern pattern = null;
            int nvars = 0;
            while (true) {
                String str = in.readLine();
                if (str == null)
                    break;
                if (str.startsWith("#") || str.length() == 0)
                    continue;
                if (!str.startsWith(";")) {
                    if (verbose)
                        System.out.println("------- " + str);
                    pattern = Pattern.compile(str);
                    RNode regex = (new RParser()).parse(str, perf && noimplicit);
                    CharSet prefix = regex.findPrefix(CharSet.FULL_CHARSET);
                    RMachine machine;
                    if (compile) {
                        RJavaClassMachine jmachine = new RJavaClassMachine();
                        jmachine.setSaveBytecode(true);
                        machine = jmachine;
                    } else
                        machine = new RInterpMachine();



                    if (nohints)
                        machine.setExtensions(machine.getExtensions() & ~RMachine.EXT_HINT);
                    if (nomultifork)
                        machine.setExtensions(machine.getExtensions() & ~RMachine.EXT_MULTIFORK);
                    if (nocharleft)
                        machine.setExtensions(machine.getExtensions() & ~RMachine.EXT_CONDJUMP);
                    if (noshifttbl)
                        machine.setExtensions(machine.getExtensions() & ~RMachine.EXT_SHIFTTBL);
                    machine.setNoRefiller(norefiller);
                    RCompiler comp = new RCompiler(machine);
                    comp.compile(regex, str);
                    re = machine.makeRegex();
                    nvars = machine.getNVars();

                    if (perf && compile && precompile) {
                        // remove JIT overhead by pre-running
                        for (int r = 0; r < 10000; r++) {
                            re.init("123UUU");
                            re.search();
                        }
                        System.gc();
                        Thread.sleep(2000); // give HotSpot a chance
                    }
                } else {
                    int next = str.indexOf(';', 1);
                    if (next < 0) {
                        System.out.println("What's that: " + str + "?");
                        continue;
                    }
                    String test = str.substring(1, next);
                    int start = next + 1;
                    next = str.indexOf(';', start);
                    if (next < 0)
                        next = str.length();
                    boolean expect =  Boolean.parseBoolean(str.substring(start, next));
                    String[] vals = new String[4];
                    int i;
                    for (i = 0; i < vals.length && next < str.length(); i++) {
                        start = next + 1;
                        next = str.indexOf(';', start);
                        if (next < 0)
                            next = str.length();
                        vals[i] = str.substring(start, next);
                    }
                    for (; i < vals.length; i++)
                        vals[i] = "";
                    char[] testArr = test.toCharArray();
                    if (perf) {
                        {
                            int count = (compile ? 5000 : 1000);
                            long time = System.nanoTime();
                            for (int r = 0; r < count; r++) {
                                if (userefiller) {
                                    re.init(test, 0, 0);
                                    re.setRefiller(refiller);
                                } else
                                    re.init(testArr);
                                re.search();
                            }
                            int result = (int) ((System.nanoTime() - time) / count);
                            if (verbose)
                                System.out.print(test + ":\t");
                            System.out.println(result);
                            total += result;
                            // continue;
                        }
                        {
                            int count = (compile ? 5000 : 1000);
                            long time = System.nanoTime();
                            Matcher matcher = pattern.matcher("");
                            for (int r = 0; r < count; r++) {
                                matcher.reset(test);
                                matcher.find();
                            }
                            int result = (int) ((System.nanoTime() - time) / count);
                            if (verbose)
                                System.out.print(test + ": [j]\t");
                            System.out.println(result);
                            jtotal += result;
                            continue;
                        }
                    }
                    if (userefiller) {
                        re.init(test, 0, 0);
                        re.setRefiller(refiller);
                    } else
                        re.init(testArr);
                    if (re.search()) {
                        if (expect) {
                            for (i = 0; i < nvars && i < vals.length; i++) {
                                String nm = Integer.toString(i + 1);
                                int vb = re.getVariableHandle(nm, true);
                                int ve = re.getVariableHandle(nm, false);
                                vb = re.getIndex(vb);
                                ve = re.getIndex(ve);
                                try {
                                    String val =
                                            (ve >= 0 && vb >= 0 ? new String(testArr, vb, ve - vb) : "");
                                    if (!val.equals(vals[i])) {
                                        System.out.println("FAILED: " + test + " $" + nm + "='" + val +
                                                "', expected '" + vals[i] + "'");
                                    }
                                } catch (Exception e) {
                                    System.out.println("Unexpected exception: " + e);
                                    System.out.println(" nm = $" + nm + " ve = " + ve + " vb = " + vb +
                                            " testArr.length = " + testArr.length);
                                }
                            }
                            if (i == nvars && verbose)
                                System.out.println("passed: " + test);
                        } else
                            System.out.println("FAILED: " + test + " [matching]");
                    } else {
                        if (expect)
                            System.out.println("FAILED: " + test + " [not matching]");
                        else if (verbose)
                            System.out.println("passed: " + test + " [not matching]");
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (perf) {
            System.out.println("----------------");
            System.out.println("Total: " + total);
            System.out.println("Total: " + jtotal);
        }
    }
}
