package com.humio.jitrex;

import com.humio.jitrex.util.Regex;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@RunWith(JUnit4.class)
public class PerformanceTest {

    private static abstract class RegexpBackend {

        public abstract int countMatches(List<String> inputs);

    }

    private static class JitrexBackend extends RegexpBackend {

        private final com.humio.jitrex.Pattern pattern;

        public JitrexBackend(String pattern) {
            this(pattern, 0);
        }

        public JitrexBackend(String pattern, int flags) {
            this.pattern = com.humio.jitrex.Pattern.compile(pattern, flags);
        }

        @Override
        public int countMatches(List<String> inputs) {
            int result = 0;
            com.humio.jitrex.Matcher matcher = this.pattern.matcher("");
            for (String s : inputs) {
                matcher.reset(s);
                while (matcher.find()) {
                    result += 1;
                }
            }
            return result;
        }

    }

    private static class JavaBackend extends RegexpBackend {

        private final java.util.regex.Pattern pattern;

        public JavaBackend(String pattern) {
            this.pattern = java.util.regex.Pattern.compile(pattern);
        }

        @Override
        public int countMatches(List<String> inputs) {
            int result = 0;
            java.util.regex.Matcher matcher = pattern.matcher("");
            for (String s : inputs) {
                matcher.reset(s);
                while (matcher.find()) {
                    result += 1;
                }
            }
            return result;
        }

    }

    private static class Re2Backend extends RegexpBackend {

        private final com.google.re2j.Pattern pattern;

        public Re2Backend(String pattern) {
            this.pattern = com.google.re2j.Pattern.compile(pattern);
        }

        @Override
        public int countMatches(List<String> inputs) {
            int result = 0;
            com.google.re2j.Matcher matcher = pattern.matcher("");
            for (String s : inputs) {
                matcher.reset(s);
                while (matcher.find()) {
                    result += 1;
                }
            }
            return result;
        }

    }

    private interface RegexpBackendFactory {
        public RegexpBackend create(String pattern);
    }

    private static final Map<String, RegexpBackendFactory> backendFactories = new HashMap<String, RegexpBackendFactory>() {{
        put("jitrex", (pattern)->new JitrexBackend(pattern, Regex._OLD_LONG_STRING_HANDLING));
        put("jitrex/i", (pattern)->new JitrexBackend(pattern, Regex.CASE_INSENSITIVE | Regex._OLD_LONG_STRING_HANDLING));
        put("jitrex/n", (pattern)->new JitrexBackend(pattern, 0));
        put("jitrex/ni", (pattern)->new JitrexBackend(pattern, Regex.CASE_INSENSITIVE));
        put("java", JavaBackend::new);
        put("re2", Re2Backend::new);
    }};

    static String [] regexes = {
            "Twain",
            "(?i)Twain",
            "[a-z]shing",
            "Huck[a-zA-Z]+|Saw[a-zA-Z]+",
            "\\b\\w+nn\\b",
            "[a-q][^u-z]{13}x",
            "Tom|Sawyer|Huckleberry|Finn",
            "(?i)(Tom|Sawyer|Huckleberry|Finn)",
            ".{0,2}(?:Tom|Sawyer|Huckleberry|Finn)",
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

    private static void runBenchmark(List<String> backendNames, String keyBackend, List<String> patterns, List<String> inputs,
                              int repeatCount) {
        PrintStream out = System.out;
        int keyIndex = backendNames.indexOf(keyBackend);
        List<RegexpBackendFactory> factories = backendNames.stream().map(backendFactories::get).collect(Collectors.toList());
        List<Map<String, RegexpBackend>> backendCache = new ArrayList<>();
        for (String ignored : backendNames)
            backendCache.add(new HashMap<>());
        StringBuilder header = new StringBuilder(String.format("%-48s", "pattern"));
        StringBuilder separator = new StringBuilder("------------------------------------------------");
        for (int i = 0; i < backendNames.size(); i++) {
            header.append(" |");
            separator.append("-+");
            if (keyIndex != -1 && i != keyIndex) {
                header.append("        ");
                separator.append("--------");
            }
            header.append(String.format(" %9s%s", backendNames.get(i), i == keyIndex ? "!" : " "));
            separator.append(String.format("-----------"));
        }

        for (int repeat = 0; repeat < repeatCount; repeat++) {
            out.println("repeat: " + repeat);
            out.println(separator);
            out.println(header);
            out.println(separator);
            for (String pattern : patterns) {
                List<Long> durations = new ArrayList<>();
                long keyDuration = 0;
                for (int i = 0; i < factories.size(); i++) {
                    RegexpBackend backend = backendCache.get(i).get(pattern);
                    if (backend == null) {
                        RegexpBackendFactory factory = factories.get(i);
                        backend = factory.create(pattern);
                        backendCache.get(i).put(pattern, backend);
                    }
                    long start = System.currentTimeMillis();
                    backend.countMatches(inputs);
                    long duration = System.currentTimeMillis() - start;
                    durations.add(duration);
                    if (i == keyIndex)
                        keyDuration = duration;
                }
                StringBuilder output = new StringBuilder(String.format("%-48s", "/" + pattern + "/"));
                for (int i = 0; i < factories.size(); i++) {
                    long duration = durations.get(i);
                    output.append(String.format(" | %8sms", duration));
                    if (keyIndex != -1 && i != keyIndex) {
                        output.append(String.format(" (%4s%%)", 100 * duration / keyDuration));
                    }
                }
                out.println(output.toString());
            }
            out.println(separator);
            out.println();
        }
    }

    private static List<String> allBackendNames() {
        return backendFactories.keySet().stream().sorted().collect(Collectors.toList());
    }

    @Test
    public void testIt() {
        runBenchmark(
                allBackendNames(),
                "jitrex",
                Arrays.asList(regexes),
                Arrays.asList(input),
                3);
    }

    public static void main(String[] args) {
        // The arguments indicate which backends to benchmark. Passing no
        // arguments means run them all. Passing a list of names means run just
        // those, for instance,
        //
        //   main jitrex java
        //
        // means compare jitrex to java. Prefixing a minus means run all except
        // the ones mentioned, so
        //
        //   main -java -re2
        //
        // means run all except java and re2. Finally, suffixing an exclamation
        // point means compare runtimes with that backend, so
        //
        //   main jitrex!
        //
        // means run all and compare the results with jitrex.
        List<String> backends = allBackendNames();
        String keyBackend = "";
        boolean isDefault = true;
        for (String arg : args) {
            boolean isKey = arg.endsWith("!");
            if (isKey) {
                arg = arg.substring(0, arg.length() - 1);
                keyBackend = arg;
                if (isDefault) {
                    continue;
                }
            }
            if (arg.startsWith("-")) {
                arg = arg.substring(1);
                backends.remove(arg);
                isDefault = false;
            } else {
                if (isDefault) {
                    backends = new ArrayList<>();
                    if (!keyBackend.isEmpty())
                        backends.add(keyBackend);
                    isDefault = false;
                }
                if (!backends.contains(arg)) {
                    backends.add(arg);
                }
            }
        }

        runBenchmark(
                backends,
                keyBackend,
                Arrays.asList(regexes),
                Arrays.asList(input),
                3);
    }

}
