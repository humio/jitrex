package com.humio.regex.jvm;

import com.humio.regex.util.RegexRefiller;

import java.util.Hashtable;

public class Sample2 extends JavaClassRegexStub
{
    private static Hashtable vars;

    protected boolean nextMatchInt() {
        final CharSequence string = super.string;
        final int[] cells = super.cells;
        final int[] forks = super.forks;
        final int end = super.end;
        int i;
        if ((i = super.forkPtr) == 0) {
            int n = super.matchStart = super.headStart;
            Label_0137: {
                if (super.searching) {
                    int n2 = n + 2;
                    while (true) {
                        final int n3 = n2;
                        if (n3 >= end) {
                            super.headStart = n3 + 1;
                            return false;
                        }
                        final int n4 = n3;
                        switch (string.charAt(n3)) {
                            case 'a': {
                                n2 = n4 + 1;
                                continue;
                            }
                            case 'c': {
                                n2 = n4 + 2;
                                continue;
                            }
                            default: {
                                n2 = n4 + 2;
                                continue;
                            }
                            case 't': {
                                n = n4 - 2;
                                super.headStart = n;
                                break Label_0137;
                            }
                        }
                    }
                }
            }
            if (n + 2 < end && string.charAt(n) == 'c') {
                ++n;
                if (string.charAt(n) == 'a') {
                    ++n;
                    if (string.charAt(n) == 't') {
                        ++n;
                        super.forks = forks;
                        super.forkPtr = i;
                        super.matchEnd = n;
                        return true;
                    }
                }
            }
        }
        while (i != 0) {
            --i;
            final int n5 = forks[i];
            --i;
            if (n5 < 0) {
                final int n6 = forks[i];
                this.dumpForks();
                throw new IllegalStateException("Internal error during regex matching");
            }
            cells[n5] = forks[i];
        }
        super.forks = forks;
        super.forkPtr = i;
        ++super.headStart;
        return false;
    }

    public void setRefiller(final RegexRefiller regexRefiller) {
        throw new RuntimeException("This regex was explicitly compiled not to support refilling.");
    }

    static {
        Sample2.vars = new Hashtable();
    }

    public String toString() {
        return "/cat/";
    }

    protected Hashtable getVars() {
        return Sample2.vars;
    }

    public Sample2() {
        super.forks = new int[4];
    }

    public void init(final CharSequence string, final int n, final int n2) {
        super.string = string;
        super.start = n;
        super.end = n + n2;
        super.headStart = n;
        final int end = super.end;
        if (super.refiller == null) {}
        super.maxStart = end;
        super.forkPtr = 0;
    }
}