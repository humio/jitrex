/*
    Copyright (c) Peter Sorotokin, 1998-2000
    Copyright (c) Humio, 2019
    See file "LICENSE.md" for terms of usage and
    redistribution.   
*/
package com.humio.jitrex.interp;

public interface RInterpCommands {
    byte ASSIGN = (byte) 0x01; /* ASSIGN var val */
    byte HARDASSIGN = (byte) 0x02; /* HARDASSIGN var val */
    byte PICK = (byte) 0x03; /* PICK var */
    byte FORK = (byte) 0x04; /* FORK label */
    byte SKIP = (byte) 0x05; /* SKIP */
    byte SKIP_NON_NEWLINE = (byte) 0x06; /* SKIP */

    byte ASSERT_CLASS = (byte) 0x10; /* ASSERT_CLASS classID */
    byte ASSERT_CHAR = (byte) 0x11; /* ASSERT_CHAR count char char ... */
    byte ASSERT_RANGE = (byte) 0x12; /* ASSERT_RANGE count, char ... */
    byte ASSERT_CLASS_RANGE = (byte) 0x13; /* ASSERT_CLASS_RANGE classID, count, char ... */
    byte ASSERT_VAR = (byte) 0x14; /* ASSERT_VAR var */
    byte ASSERT_EXT_VAR = (byte) 0x15; /* ASSERT_EXT_VAR extCell var */
    byte BOUNDARY = (byte) 0x16; /* BOUNDARY boundaryID */

    byte SHIFTTBL = (byte) 0x17; /* SHIFTTBL off cnt {chr shft} cnt {chr} */

    byte DECJUMP = (byte) 0x20; /* DECJUMP var label */
    byte DECFAIL = (byte) 0x21; /* DECFAIL var */

    byte JUMP = (byte) 0x30; /* JUMP label */
    byte FAIL = (byte) 0x31; /* FAIL */

    byte MFSTART = (byte) 0x40; /* MFSTART headDec, label */
    byte MFSTART_HEAD = (byte) 0x41; /* MFSTART_HEAD minCount, headDec, label */
    byte MFEND = (byte) 0x42; /* MFEND */
    byte MFENDLIMIT = (byte) 0x43; /* MFENDLIMIT count */

    byte CHARLEFT = (byte) 0x50; /* CHARLEFT count */

    byte JUMP_RANGE = (byte) 0x60; /* JUMP_RANGE label count char ... */
    byte JUMP_CHAR = (byte) 0x61; /* JUMP_CHAR char ... */
    byte JUMP_MIN_LEFT = (byte) 0x62; /* JUMP_MIN_LEFT label count */
    byte JUMP_MAX_LEFT = (byte) 0x63; /* JUMP_MIN_LEFT label count */

    byte STOP = (byte) 0x00; /* STOP */
}
