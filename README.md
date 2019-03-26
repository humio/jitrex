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


regex | matches | `jitrex` | `com.google.re2j` | `java.util.regex`
---   | ---     | ---      | ---    | ---  
 `/Twain/` | 811 | 53ms | 23ms  (43%) | 44ms (83%)
 `/(?i)Twain/` | 965 | 121ms | 1199ms  (990%) | 120ms (99%)
 `/[a-z]shing/` | 1540 | 163ms | 921ms  (565%) | 187ms (114%)
 `/Huck[a-zA-Z]+\|Saw[a-zA-Z]+/` | 262 | 149ms | 1318ms  (884%) | 259ms (173%)
 `/\b\w+nn\b/` | 262 | 183ms | 1399ms  (764%) | 329ms (179%)
 `/[a-q][^u-z]{13}x/` | 3021 | 404ms | 2479ms  (613%) | 471ms (116%)
 `/Tom\|Sawyer\|Huckleberry\|Finn/` | 2598 | 178ms | 2449ms  (1375%) | 497ms (279%)
 `/(?i)(Tom\|Sawyer\|Huckleberry\|Finn)/` | 4152 | 301ms | 4752ms  (1578%) | 581ms (193%)
 `/.{0,2}(?:Tom\|Sawyer\|Huckleberry\|Finn)/` | 2598 | 608ms | 3646ms  (599%) | 1764ms (290%)
 `/.{2,4}(Tom\|Sawyer\|Huckleberry\|Finn)/` | 1976 | 960ms | 4289ms  (446%) | 1816ms (189%)
 `/Tom.{10,25}river\|river.{10,25}Tom/` | 2 | 129ms | 1313ms  (1017%) | 219ms (169%)
 `/[a-zA-Z]+ing/` | 78424 | 168ms | 1218ms  (725%) | 437ms (260%)
 `/\s[a-zA-Z]{0,12}ing\s/` | 45393 | 283ms | 1648ms  (582%) | 469ms (165%)
 `/([A-Za-z]awyer\|[A-Za-z]inn)\s/` | 194 | 284ms | 1648ms  (580%) | 586ms (206%)
 `/["'][^"']{0,30}[?!\.]["']/` | 8072 | 146ms | 892ms  (610%) | 180ms (123%)
 `/∞\|✓/` | 2 | 116ms | 688ms  (593%) | 380ms (327%)
 `/\p{Sm}/` | 69 | 115ms | 797ms  (693%) | 152ms (132%)

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

- Added a Java-like API (`com.humio.regex.Pattern` and `com.humio.regex.Matcher`),
- Removed some Perl-style regex features like `<` and `>`, 
- Added many RE2-style regex features such as `(?i:...)` group local flags, `\p{Greek}` unicode classes,
- Made the code pass all relevant parts of the [re2j](https://github.com/google/re2j) junit tests.
- Fixed numerous bugs in the process passing the tests.

