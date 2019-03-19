package com.humio.jitrex;

import org.junit.Test;

/*
import java.util.jitrex.Pattern;
import java.util.jitrex.Matcher;
*/

public class RegexTest {

    private static final String[] _re = {"^(([^:]+)://)?([^:/]+)(:([0-9]+))?(/.*)", // URL match
            "(([^:]+)://)?([^:/]+)(:([0-9]+))?(/.*)", // URL match without starting ^
            "usd [+-]?[0-9]+.[0-9][0-9]", // Canonical US dollar amount
            "\\b(\\w+)(\\s+\\1)+\\b", // Duplicate words
            "\\{(?:\\d+):(?:(?:[^}](?!-} ))*?)", // this is meant to match against the "some more text and ..." but it causes ORO Matcher
            // to fail, so we won't include this by default... it is also WAY too slow to test
            // we will test large string 10 times
    };


    private static final String[] _str = {
            "http://www.linux.com/",
            "http://www.thelinuxshow.com/main.php3",
            "usd 1234.00",
            "he said she said he said no",
            "same same same",
            "{1:\n" + "this is some more text - and some more and some more and even more\n"
                    + "this is some more text and some more and some more and even more\n"
                    + "this is some more text and some more and some more and even more\n"
                    + "this is some more text and some more and some more and even more\n"
                    + "this is some more text and some more and some more and even more\n"
                    + "this is some more text and some more and some more and even more\n"
                    + "this is some more text and some more and some more and even more\n"
                    + "this is some more text and some more and some more and even more\n"
                    + "this is some more text and some more and some more and even more\n"
                    + "this is some more text and some more and some more and even more\n"
                    + "this is some more text and some more and some more and even more\n"
                    + "this is some more text and some more and some more and even more\n"
                    + "this is some more text and some more and some more and even more\n"
                    + "this is some more text and some more and some more and even more\n"
                    + "this is some more text and some more and some more and even more\n"
                    + "this is some more text and some more and some more and even more\n"
                    + "this is some more text and some more and some more and even more\n"
                    + "this is some more text and some more and some more and even more\n"
                    + "this is some more text and some more and some more and even more\n"
                    + "this is some more text and some more and some more and even more\n"
                    + "this is some more text and some more and some more and even more\n"
                    + "this is some more text and some more and some more and even more\n"
                    + "this is some more text and some more and some more and even more\n"
                    + "this is some more text and some more and some more and even more\n"
                    + "this is some more text and some more and some more and even more\n"
                    + "this is some more text and some more and some more and even more\n"
                    + "this is some more text and some more and some more and even more\n"
                    + "this is some more text and some more and some more and even more\n"
                    + "this is some more text and some more and some more and even more\n"
                    + "this is some more text and some more and some more and even more\n"
                    + "this is some more text and some more and some more and even more\n"
                    + "this is some more text and some more and some more and even more\n"
                    + "this is some more text and some more and some more and even more\n"
                    + "this is some more text and some more and some more and even more\n"
                    + "this is some more text and some more and some more and even more\n"
                    + "this is some more text and some more and some more and even more\n"
                    + "this is some more text and some more and some more and even more\n"
                    + "this is some more text and some more and some more and even more\n"
                    + "this is some more text and some more and some more and even more\n"
                    + "this is some more text and some more and some more and even more\n"
                    + "this is some more text and some more and some more and even more\n"
                    + "this is some more text and some more and some more and even more at the end\n" + "-}\n", // very large bit of text...

    };

    private static boolean[][] expectedMatch = new boolean[_re.length][_str.length];

    static
    {
        expectedMatch[0][0] = true;
        expectedMatch[0][1] = true;
        expectedMatch[0][2] = false;
        expectedMatch[0][3] = false;
        expectedMatch[0][4] = false;
        expectedMatch[0][5] = false;
        expectedMatch[1][0] = true;
        expectedMatch[1][1] = true;
        expectedMatch[1][2] = false;
        expectedMatch[1][3] = false;
        expectedMatch[1][4] = false;
        expectedMatch[1][5] = false;
        expectedMatch[2][0] = false;
        expectedMatch[2][1] = false;
        expectedMatch[2][2] = true;
        expectedMatch[2][3] = false;
        expectedMatch[2][4] = false;
        expectedMatch[2][5] = false;
        expectedMatch[3][0] = false;
        expectedMatch[3][1] = false;
        expectedMatch[3][2] = false;
        expectedMatch[3][3] = false;
        expectedMatch[3][4] = true;
        expectedMatch[3][5] = false;
        expectedMatch[4][0] = false;
        expectedMatch[4][1] = false;
        expectedMatch[4][2] = false;
        expectedMatch[4][3] = false;
        expectedMatch[4][4] = false;
        expectedMatch[4][5] = true;
    }

    private final static int ITERATIONS = 100;
    private final boolean debug = false;
    private final boolean html = false;

    @Test
    public void testRegex() {

        long startTime, endTime;
        long[][][] timeTaken = new long[_re.length][ITERATIONS][_str.length];
        boolean[][] matches = new boolean[_re.length][_str.length];

        startTime = System.currentTimeMillis();
        for (int regnum = 0; regnum < _re.length; regnum++)
        {
            try
            {
                java.util.regex.Pattern regexpr = java.util.regex.Pattern.compile(_re[regnum]);
                for (int itter = 0; itter < ITERATIONS; itter++)
                {
                    for (int strnum = 0; strnum < _str.length; strnum++)
                    {
                        if (debug && (itter % 1000) == 0)
                        {
                            System.out.print("Iteration/jitrex number/string number " + itter + "/" + regnum + "/" + strnum + "... ");
                        }

                        if (debug && (itter % 1000) == 0)
                        {
                            System.out.print(_re[regnum] + " against " + _str[strnum] + ":");
                        }

                        long iterStarTime = System.currentTimeMillis();
                        java.util.regex.Matcher m = regexpr.matcher(_str[strnum]);
                        boolean b = m.find();
                        matches[regnum][strnum] = (b == expectedMatch[regnum][strnum]);
                        timeTaken[regnum][itter][strnum] = (System.currentTimeMillis() - iterStarTime);

                        if (debug && (itter % 1000) == 0)
                        {
                            System.out.print(b);
                        }

                        if (debug && (itter % 1000) == 0)
                        {
                            System.out.print(" took " + timeTaken[regnum][itter][strnum] + "ms" + "\n");
                        }
                    }
                }
            }
            catch (Throwable e)
            {
                if (debug)
                {
                    System.out.println(_re[regnum] + "  failed badly");
                }
            }
        }
        endTime = System.currentTimeMillis();
        printResult("java.util.jitrex.Pattern", timeTaken, (endTime - startTime), matches, html);
        // ----------------------//



        startTime = System.currentTimeMillis();
        for (int regnum = 0; regnum < _re.length; regnum++)
        {
            try
            {
                Pattern regexpr = Pattern.compile(_re[regnum]);
                for (int itter = 0; itter < ITERATIONS; itter++)
                {
                    for (int strnum = 0; strnum < _str.length; strnum++)
                    {
                        if (debug && (itter % 1000) == 0)
                        {
                            System.out.print("Iteration/jitrex number/string number " + itter + "/" + regnum + "/" + strnum + "... ");
                        }

                        if (debug && (itter % 1000) == 0)
                        {
                            System.out.print(_re[regnum] + " against " + _str[strnum] + ":");
                        }

                        long iterStarTime = System.currentTimeMillis();
                        Matcher m = regexpr.matcher(_str[strnum]);
                        boolean b = m.find();
                        matches[regnum][strnum] = (b == expectedMatch[regnum][strnum]);
                        timeTaken[regnum][itter][strnum] = (System.currentTimeMillis() - iterStarTime);

                        if (debug && (itter % 1000) == 0)
                        {
                            System.out.print(b);
                        }

                        if (debug && (itter % 1000) == 0)
                        {
                            System.out.print(" took " + timeTaken[regnum][itter][strnum] + "ms" + "\n");
                        }
                    }
                }
            }
            catch (Throwable e)
            {
                if (debug)
                {
                    System.out.println(_re[regnum] + "  failed badly");
                }
            }
        }
        endTime = System.currentTimeMillis();
        printResult("com.humio.jitrex.Pattern", timeTaken, (endTime - startTime), matches, html);
        // ----------------------//

    }



    private static final void printResult(String regexName, long[][][] matrix, long totalTime, boolean[][] matches, boolean html)
    {
        // timeTaken[regnum][itter][strnum]
        if (html)
        {
            System.out.println("<table>");
            System.out.println("<tr><th colspan=\"3\"><h2>Regular expression library:</h2></th><td colspan=\"3\"><h2>" + regexName
                    + "</h2></td></tr>");
        }
        else
        {
            System.out.println("------------------------------------------");
            System.out.println("Regular expression library: " + regexName + "\n");
        }
        for (int re = 0; re < _re.length; re++)
        {
            if (html)
            {
                System.out.println("<tr><th>RE:</th><td colspan=\"5\">" + _re[re] + "</td></tr>");
                System.out
                        .println("<tr><th>MS</th><th>MAX</th><th>AVG</th><th>MIN</th><th>DEV</th><th>INPUT</th><th>MATCH</th></tr>");
            }
            else
            {
                System.out.println("RE: " + _re[re]);
                System.out.println("  MS\tMAX\tAVG\tMIN\tDEV\tMATCH");
            }
            for (int str = 0; str < _str.length; str++)
            {
                long total = 0;
                long sumOfSq = 0;
                long min = Long.MAX_VALUE;
                long max = Long.MIN_VALUE;
                for (int i = 0; i < ITERATIONS; i++)
                {
                    long elapsed = matrix[re][i][str];
                    total += elapsed;
                    sumOfSq += elapsed * elapsed;
                    if (elapsed < min)
                    {
                        min = elapsed;
                    }
                    if (elapsed > max)
                    {
                        max = elapsed;
                    }
                }
                // calc std dev
                long stdDev = (long) java.lang.Math.sqrt((sumOfSq - ((total * total) / ITERATIONS)) / (ITERATIONS - 1));

                if (html)
                {
                    System.out.println("<tr><td>" + total + "</td><td>" + max + "</td><td>" + (double) total / ITERATIONS
                            + "</td><td>" + min + "</td><td>" + stdDev + "</td><td>" + _str[str] + "</td><td>" + matches[re][str]
                            + "</td></tr>");
                }
                else
                {
                    System.out.println("  " + total + "\t" + max + "\t" + (double) total / ITERATIONS + "\t" + min + "\t" + stdDev
                             + "\t'" + matches[re][str] + "'");
                }
            }
        }
        if (html)
        {
            System.out.println("<tr><th colspan=\"3\"><h2>Total time taken:</h2></th><td colspan=\"3\"><h2>" + totalTime
                    + "</h2></td></tr>");
            System.out.println("</table>");
        }
        else
        {
            System.out.println("Total time taken: " + totalTime);
            System.out.println("------------------------------------------");
        }
    }

}