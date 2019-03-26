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


regex | matches | `jitrex` | `re2j` | `jre` | x re2j | x jre 
---   | ---     | ---      | ---    | ---   | ---            | --- 
 `Twain` | 811 | 53ms | 21ms | 39ms | 39% | 73%
 `(?i)Twain` | 965 | 109ms | 1191ms | 118ms | 1092% | 108%
 `[a-z]shing` | 1540 | 145ms | 943ms | 209ms | 650% | 144%
 `Huck[a-zA-Z]+\|Saw[a-zA-Z]+` | 262 | 132ms | 1594ms | 287ms | 1207% | 217%
 `\b\w+nn\b` | 262 | 193ms | 1676ms | 361ms | 868% | 187%
 `[a-q][^u-z]{13}x` | 3021 | 479ms | 2677ms | 503ms | 558% | 105%
 `Tom\|Sawyer\|Huckleberry\|Finn` | 2598 | 193ms | 3048ms | 522ms | 1579% | 270%
 `(?i)(Tom\|Sawyer\|Huckleberry\|Finn)` | 4152 | 321ms | 5463ms | 596ms | 1701% | 185%
 `.{0,2}(?:Tom\|Sawyer\|Huckleberry\|Finn)` | 2598 | 656ms | 3820ms | 1983ms | 582% | 302%
 `.{2,4}(Tom\|Sawyer\|Huckleberry\|Finn)` | 1976 | 1060ms | 4282ms | 2241ms | 403% | 211%
 `Tom.{10,25}river\|river.{10,25}Tom` | 2 | 115ms | 1324ms | 206ms | 1151% | 179%
 `[a-zA-Z]+ing` | 78424 | 166ms | 1235ms | 441ms | 743% | 265%
 `\s[a-zA-Z]{0,12}ing\s` | 45393 | 325ms | 1612ms | 420ms | 496% | 129%
 `([A-Za-z]awyer\|[A-Za-z]inn)\s` | 194 | 286ms | 1601ms | 661ms | 559% | 231%
 `["'][^"']{0,30}[?!\.]["']` | 8072 | 148ms | 930ms | 183ms | 628% | 123%
 `∞\|✓` | 2 | 107ms | 708ms | 403ms | 661% | 376%
 `\p{Sm}` | 69 | 122ms | 781ms | 148ms | 640% | 121%

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

