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
 `/Twain/` | 811 | 54ms | 23ms  (-58%) | 42ms (-23%)
 `/(?i)Twain/` | 965 | 119ms | 1181ms  (892%) | 121ms (1%)
 `/[a-z]shing/` | 1540 | 155ms | 931ms  (500%) | 180ms (16%)
 `/Huck[a-zA-Z]+\|Saw[a-zA-Z]+/` | 262 | 148ms | 1233ms  (733%) | 251ms (69%)
 `/\b\w+nn\b/` | 262 | 183ms | 1378ms  (653%) | 348ms (90%)
 `/[a-q][^u-z]{13}x/` | 3021 | 393ms | 2592ms  (559%) | 462ms (17%)
 `/Tom\|Sawyer\|Huckleberry\|Finn/` | 2598 | 183ms | 2361ms  (1190%) | 461ms (151%)
 `/(?i)(Tom\|Sawyer\|Huckleberry\|Finn)/` | 4152 | 300ms | 4656ms  (1452%) | 567ms (89%)
 `/.{0,2}(?:Tom\|Sawyer\|Huckleberry\|Finn)/` | 2598 | 618ms | 3469ms  (461%) | 1836ms (197%)
 `/.{2,4}(Tom\|Sawyer\|Huckleberry\|Finn)/` | 1976 | 978ms | 4162ms  (325%) | 1831ms (87%)
 `/Tom.{10,25}river\|river.{10,25}Tom/` | 2 | 131ms | 1292ms  (886%) | 213ms (62%)
 `/[a-zA-Z]+ing/` | 78424 | 175ms | 1170ms  (568%) | 438ms (150%)
 `/\s[a-zA-Z]{0,12}ing\s/` | 45393 | 279ms | 1602ms  (474%) | 467ms (67%)
 `/([A-Za-z]awyer\|[A-Za-z]inn)\s/` | 194 | 277ms | 1622ms  (485%) | 672ms (142%)
 `/["'][^"']{0,30}[?!\.]["']/` | 8072 | 145ms | 921ms  (535%) | 181ms (24%)
 `/∞\|✓/` | 2 | 119ms | 699ms  (487%) | 377ms (216%)
 `/\p{Sm}/` | 69 | 117ms | 731ms  (524%) | 149ms (27%)

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

