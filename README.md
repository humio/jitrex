[![Build Status](https://cloud.drone.io/api/badges/humio/jitrex/status.svg)](https://cloud.drone.io/humio/jitrex)

# Humio JITrex - Fast JVM Bytecode Regex Engine

This project is based on work done in 2000 by Peter Sorotokin as part 
of the now defunct [Jint Programming Language](http://jint.sourceforge.net).
Peter kindly agreed to relicense the original code under Apache License.
See [background](#background).

A regex engine that has the following properties:

- Compiling a regex with `com.humio.jitrex.Pattern.compile(...)` produces a custom Java class.
  This step is not as fast as the other regex engines. 
- The resulting regex however is fast, especially once HotSpot has jit'ed the code, so you
  should cache the compiled `Pattern` instance.
- Matching (generally) only does allocation on the first invocation, and the execution
  behavior of the matcher has very good locality.
- The regex engine does use backtracking, but inside the buffer allocated in the first application
  of a `Matcher` and without overrunning the stack as can happen in pathological cases 
  with `java.lang.util.Regex`.
- As another consequence of using backtracking, capture groups work faster than e.g. RE2J.
- Very large regular expressions can fail to compile if the resuling byte code does not
  fit the JVM's 64k limit per method.
- At present, it does not understand code points beyond `0xffff`, so `.` does not match 💩,
  which is represented as two 16-bit `char`s.  If however your input regex contains a 💩, then
  it will match just fine.  So in most cases this is not an issue.
  
This is not a regex engine for everyone.  It is (relatively) slow to compile,
but fast at matching if you use the same `Matcher` instance many times.

## Performance

For our use case, the Mark Twain samples provide a good indication of how `jitrex` performs.
Here is a sample run comparing it with `java.util.regex` and `com.google.re2j`.
It runs the following regexes over the 302,278 lines of text (15MB) in the collective Mark 
Twain texts from the Gutenberg project:


regex | #found | `jitrex` | `com.google.re2j` | `java.util.regex`
---   | ---:    | ---:     | ---:              | ---: 
 `/Twain/` | 811 | 54ms | 22ms  (40%) | 42ms (77%)
 `/(?i)Twain/` | 965 | 123ms | 1207ms  (981%) | 120ms (97%)
 `/[a-z]shing/` | 1540 | 155ms | 1043ms  (672%) | 187ms (120%)
 `/Huck[a-zA-Z]+\|Saw[a-zA-Z]+/` | 262 | 146ms | 1293ms  (885%) | 265ms (181%)
 `/\b\w+nn\b/` | 262 | 185ms | 1345ms  (727%) | 347ms (187%)
 `/[a-q][^u-z]{13}x/` | 3021 | 409ms | 2475ms  (605%) | 440ms (107%)
 `/Tom\|Sawyer\|Huckleberry\|Finn/` | 2598 | 185ms | 2501ms  (1351%) | 479ms (258%)
 `/(?i)(Tom\|Sawyer\|Huckleberry\|Finn)/` | 4152 | 311ms | 4755ms  (1528%) | 587ms (188%)
 `/.{0,2}(?:Tom\|Sawyer\|Huckleberry\|Finn)/` | 2598 | 611ms | 3640ms  (595%) | 1735ms (283%)
 `/.{2,4}(Tom\|Sawyer\|Huckleberry\|Finn)/` | 1976 | 1000ms | 4221ms  (422%) | 1807ms (180%)
 `/Tom.{10,25}river\|river.{10,25}Tom/` | 2 | 120ms | 1374ms  (1145%) | 210ms (175%)
 `/[a-zA-Z]+ing/` | 78424 | 178ms | 1275ms  (716%) | 434ms (243%)
 `/\s[a-zA-Z]{0,12}ing\s/` | 45393 | 291ms | 1746ms  (600%) | 484ms (166%)
 `/([A-Za-z]awyer\|[A-Za-z]inn)\s/` | 194 | 291ms | 2190ms  (752%) | 669ms (229%)
 `/["'][^"']{0,30}[?!\.]["']/` | 8072 | 153ms | 1012ms  (661%) | 175ms (114%)
 `/∞\|✓/` | 2 | 109ms | 712ms  (653%) | 375ms (344%)
 `/\p{Sm}/` | 69 | 119ms | 1252ms  (1052%) | 133ms (111%)

Just adding up these numbers provides an overall ~2x speedup relative to java.util.regex,
and 7x speedup relative to com.google.re2j.

## Background

This project is based on work done in 2000 by Peter Sorotokin as part 
of the now defunct [Jint Programming Language](http://jint.sourceforge.net).
Peter kindly agreed to relicense the original code under Apache License.

This project also includes code developed by Google as part of the
[re2j](https://github.com/google/re2j) project, made available under the
Go License.

Based on Peter Sotorokin's work, we have 

- Added a Java-like API (`com.humio.jitrex.Pattern` and `com.humio.jitrex.Matcher`),
- Removed some Perl-style regex features like `<` and `>`, 
- Added many RE2-style regex features such as `(?i:...)` group local flags, `\p{Greek}` unicode classes,
- Made the code pass all relevant parts of the [re2j](https://github.com/google/re2j) junit tests.
- Fixed numerous bugs in the process passing the tests.

