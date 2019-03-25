package com.humio.jitrex;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@RunWith(JUnit4.class)
public class PerformanceTest {


    static String [] regexes = {
            "Twain",
            "(?i)Twain",
            "[a-z]shing",
            "Huck[a-zA-Z]+|Saw[a-zA-Z]+",
            "\\b\\w+nn\\b",
            "[a-q][^u-z]{13}x",
            "Tom|Sawyer|Huckleberry|Finn",
            "(?i)Tom|Sawyer|Huckleberry|Finn",
            ".{0,2}(Tom|Sawyer|Huckleberry|Finn)",
            ".{2,4}(Tom|Sawyer|Huckleberry|Finn)",
            "Tom.{10,25}river|river.{10,25}Tom",
            "[a-zA-Z]+ing",
            "\\s[a-zA-Z]{0,12}ing\\s",
            "([A-Za-z]awyer|[A-Za-z]inn)\\s",
            "[\"'][^\"']{0,30}[?!\\.][\"']",
            "\u221E|\u2713",
            "\\p{Sm}"                               // any mathematical symbol
    };


    static String[] input;

    static {
        try {
            input = init();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String[] init() throws IOException {
        InputStream in = PerformanceTest.class.getResourceAsStream( "3200.txt" );
        BufferedReader br = new BufferedReader( new InputStreamReader(in, StandardCharsets.UTF_8 ));
        List<String> out = new ArrayList<>();
        String line;
        while ((line = br.readLine()) != null) {
            out.add(line);
        }
        return out.toArray(new String[out.size()]);
    }


    @Test
    public void testIt() {

        com.humio.jitrex.Pattern[] patterns1 = new com.humio.jitrex.Pattern[ regexes.length ];
        for (int i = 0; i < regexes.length; i++) {
            patterns1[i] = com.humio.jitrex.Pattern.compile(regexes[i]);
        }

        java.util.regex.Pattern[] patterns2 = new java.util.regex.Pattern[ regexes.length ];
        for (int i = 0; i < regexes.length; i++) {
            patterns2[i] = java.util.regex.Pattern.compile(regexes[i]);
        }


        int ITERS = 3;

        for (int iter = 0; iter < ITERS; iter++) {

            System.out.println("---------------- ITER: " +iter+" -----------------");

            double total1 = 0L;
            double total2 = 0L;

            for (int i = 0; i < regexes.length; i++) {

                int matches1 = 0;
                long before1 = System.nanoTime();
                com.humio.jitrex.Matcher m1 = patterns1[i].matcher("");

                for (int l = 0; l < input.length; l++) {

                    m1.reset(input[l]);

                    while(m1.find()) {
                        matches1 += 1;
                    }

                }
                long after1 = System.nanoTime();


                int matches2 = 0;
                long before2 = System.nanoTime();

                java.util.regex.Matcher m2 = patterns2[i].matcher("");

                for (int l = 0; l < input.length; l++) {

                    m2.reset(input[l]);

                    while(m2.find()) {
                        matches2 += 1;
                    }

                }
                long after2 = System.nanoTime();

                double humio_jitrex = (after1-before1)/1000000000.0;
                double java_util = (after2-before2)/1000000000.0;

                total1 += humio_jitrex;
                total2 += java_util;

                System.out.println("regex["+i+"], matches="+matches1+"|"+matches2+"  jitrex:"+humio_jitrex+"sec; java:"+java_util+"sec"+"; speedup: "+(java_util/humio_jitrex)+"x");

            }

            System.out.println("END --- jitrex:"+total1+"sec; java:"+total2+"sec"+"; speedup: "+(total2/total1)+"x ---");

        }

    }

}
