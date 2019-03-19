# Humio JitRex - Fast JVM Bytecode Regex Engine

This project is based on work done in 2000 by Peter Sorotokin as part 
of the now defunct [Jint Programming Language](http://jint.sourceforge.net).
Peter kindly agreed to relicense the original code under Apache License.

A regex engine that has the following properties:

- Compiling a regex with `com.humio.regex.Pattern.compile(...)` produces a custom Java class.
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
  which is represented as two 16-bit `char`s.  
  
This is not a regex engine for everyone.  It is (relatively) slow to compile,
but fast at matching if you use the same `Matcher` instance many times.


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

