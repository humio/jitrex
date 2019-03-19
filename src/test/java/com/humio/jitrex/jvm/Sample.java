package com.humio.jitrex.jvm;

import com.humio.jitrex.util.RegexRefiller;

import java.util.Hashtable;

public class Sample extends JavaClassRegexStub
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
            Label_0124: {
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
                            case 'f': {
                                n2 = n4 + 2;
                                continue;
                            }
                            default: {
                                n2 = n4 + 2;
                                continue;
                            }
                            case 'o': {
                                n = n4 - 2;
                                super.headStart = n;
                                break Label_0124;
                            }
                        }
                    }
                }
            }
            if (n + 2 < end && string.charAt(n) == 'f') {
                ++n;
                if (string.charAt(n) == 'o') {
                    ++n;
                    if (string.charAt(n) == 'o' && (++n == end || string.charAt(n) == '\n')) {
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
                throw new IllegalStateException("Internal error during jitrex matching");
            }
            cells[n5] = forks[i];
        }
        super.forks = forks;
        super.forkPtr = i;
        ++super.headStart;
        return false;
    }

    public void setRefiller(final RegexRefiller regexRefiller) {
        throw new RuntimeException("This jitrex was explicitly compiled not to support refilling.");
    }

    static {
        Sample.vars = new Hashtable();
    }

    public String toString() {
        return "/foo$/";
    }

    protected Hashtable getVars() {
        return Sample.vars;
    }

    public Sample() {
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