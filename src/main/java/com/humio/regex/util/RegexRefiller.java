/*
    Copyright (c) Peter Sorotokin, 1998-2000
    Copyright (c) Humio, 2019
    See file "LICENSE.md" for terms of usage and
    redistribution.   
*/
package com.humio.regex.util;

public abstract class RegexRefiller {
    /**
     * Read more characters into regex's buffer and hand (possibly new) buffer
     * and character range to the Regex. Character range start must remain the
     * same. It also can change regex's refiller (setting it to null if buffer
     * cannot be refilled).
     *
     * @returns new value for buffer boundary
     */
    public abstract int refill(Regex regex, int boundary);
}
