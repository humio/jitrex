/*
    Copyright (c) Peter Sorotokin, 1998-2000
    Copyright (c) Humio, 2019
    See file "LICENSE.md" for terms of usage and
    redistribution.   
*/
package com.humio.util.jint.gen;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Hashtable;
import java.util.Vector;

class JVMConstantPool implements JVMCodes {

    int constantPoolSize = 1;
    Vector<CP_Entry> constantPoolAcc = new Vector<>();
    Hashtable<Object,CP_Entry> constantPoolTable = new Hashtable<>();
    Hashtable<CP_UTF8_Entry, CP_Class_Entry> classPoolTable = new Hashtable<>();

    CP_Entry[] entryTable;

    CP_UTF8_Entry lookupUTF8(String s) {
        CP_UTF8_Entry entry = (CP_UTF8_Entry) constantPoolTable.get(s);
        if (entry == null) {
            entry = new CP_UTF8_Entry(s);
            entry.insert();
        }
        return entry;
    }

    CP_UTF8_Entry insertUTF8(String s, CP_UTF8_Entry fill) throws IOException {
        CP_UTF8_Entry entry;
        s = s.intern();
        if (fill != null) {
            entry = fill;
            fill.data = s;
        } else
            entry = new CP_UTF8_Entry(s);
        entry.insert();
        return entry;
    }

    CP_Number_Entry lookupNumber(Number n) {
        CP_Number_Entry entry = (CP_Number_Entry) constantPoolTable.get(n);
        if (entry == null) {
            entry = new CP_Number_Entry(n);
            entry.insert();
        }
        return entry;
    }

    CP_Number_Entry insertNumber(Number n) throws IOException {
        CP_Number_Entry entry = new CP_Number_Entry(n);
        entry.insert();
        return entry;
    }

    CP_String_Entry lookupString(String s) {
        CP_UTF8_Entry key = lookupUTF8(s);
        CP_String_Entry entry = (CP_String_Entry) constantPoolTable.get(key);
        if (entry == null) {
            entry = new CP_String_Entry(key);
            entry.insert();
        }
        return entry;
    }

    CP_String_Entry insertString(CP_UTF8_Entry key) throws IOException {
        CP_String_Entry entry = new CP_String_Entry(key);
        entry.insert();
        return entry;
    }

    CP_Class_Entry lookupClass(String s) {
        CP_UTF8_Entry key = lookupUTF8(s);
        CP_Class_Entry entry = classPoolTable.get(key);
        if (entry == null) {
            entry = new CP_Class_Entry(key);
            entry.insert();
        }
        return entry;
    }

    //------------------------------- constant pool goodies ------

    CP_Class_Entry insertClass(CP_UTF8_Entry key, CP_Class_Entry fill) throws IOException {
        if (fill != null)
            fill.ref = key;
        else
            fill = new CP_Class_Entry(key);
        fill.insert();
        return fill;
    }

    CP_NameAndType_Entry lookupNameAndType(String name, String type) {
        CP_NameAndType_Entry key = new CP_NameAndType_Entry(name, type);
        CP_NameAndType_Entry entry = (CP_NameAndType_Entry) constantPoolTable.get(key);
        if (entry == null) {
            entry = key;
            entry.insert();
        }
        return entry;
    }

    CP_NameAndType_Entry insertNameAndType(CP_UTF8_Entry name, CP_UTF8_Entry type,
                                           CP_NameAndType_Entry fill)
            throws IOException {
        if (fill != null) {
            fill.name = name;
            fill.type = type;
        } else
            fill = new CP_NameAndType_Entry(name, type);
        fill.insert();
        return fill;
    }

    CP_MemberRef_Entry lookupMemberRef(byte tag, String clazz,
                                       String name, String type) {
        CP_MemberRef_Entry key = new CP_MemberRef_Entry(tag, clazz, name, type);
        CP_MemberRef_Entry entry = (CP_MemberRef_Entry) constantPoolTable.get(key);
        if (entry == null) {
            entry = key;
            entry.insert();
        }
        return entry;
    }

    CP_MemberRef_Entry insertMemberRef(byte tag, CP_Class_Entry clazz,
                                       CP_NameAndType_Entry nameAndType) throws IOException {
        CP_MemberRef_Entry fill = new CP_MemberRef_Entry(tag, clazz, nameAndType);
        fill.insert();
        return fill;
    }

    void writeConstantPool(DataOutputStream out) throws IOException {
        out.writeShort(constantPoolSize);
        CP_Entry[] arr = new CP_Entry[constantPoolAcc.size()];
        constantPoolAcc.copyInto(arr);
        for (int i = 0; i < arr.length; i++)
            arr[i].write(out);
    }

    private void readEntry(DataInputStream in) throws IOException {
        int tag = in.read();
        CP_Entry res = entryTable[constantPoolSize];
        if (res != null && res.getTag() != tag)
            throw new IOException("Entry " + constantPoolSize + " is of wrong type: " + tag);
        //System.out.println( "TAG: " + tag );
        switch (tag) {
            case TAG_UTF8:
                res = insertUTF8(in.readUTF(), (CP_UTF8_Entry) res);
                //System.out.println( "UTF: " + res.getData() );
                if (res == null)
                    throw new IOException("Duplicate UTF8 entry in constant pool");
                break;
            case TAG_Integer:
                res = insertNumber(in.readInt());
                //System.out.println( "Number: " + res.getData() );
                break;
            case TAG_Float:
                res = insertNumber(in.readFloat());
                break;
            case TAG_Long:
                res = insertNumber(in.readLong());
                break;
            case TAG_Double:
                res = insertNumber(in.readDouble());
                break;
            case TAG_Class:
                res = insertClass(getOrAllocUTF8Entry(in.readShort()), (CP_Class_Entry) res);
                break;
            case TAG_String:
                res = insertString(getOrAllocUTF8Entry(in.readShort()));
                break;
            case TAG_FieldRef:
            case TAG_MethodRef:
            case TAG_InterfaceMethodRef: {
                int ci = in.readShort();
                int ni = in.readShort();
                res = insertMemberRef((byte) tag, getOrAllocClassEntry(ci),
                        getOrAllocNameAndTypeEntry(ni));
            }
            break;
            case TAG_NameAndType: {
                int ni = in.readShort();
                int ti = in.readShort();
                res = insertNameAndType(getOrAllocUTF8Entry(ni),
                        getOrAllocUTF8Entry(ti), (CP_NameAndType_Entry) res);
            }
            break;
            default:
                throw new IOException("Unknown constant pool entry tag: " + tag);
        }
        entryTable[res.count] = res;
    }

    void readConstantPool(DataInputStream in) throws IOException {
        int size = in.readShort();
        entryTable = new CP_Entry[size];
        while (constantPoolSize < size)
            readEntry(in);
    }

    CP_Entry getEntry(int index) {
        if (index >= entryTable.length || index <= 0)
            throw new RuntimeException("Invalid constant pool index: " + index +
                    ", size = " + entryTable.length);
        if (index >= constantPoolSize)
            throw new RuntimeException("Unassigned constant pool index: " +
                    index + " have only " + constantPoolSize + " entries");
        return entryTable[index];
    }

    CP_UTF8_Entry getOrAllocUTF8Entry(int index) {
        if (index >= entryTable.length || index <= 0)
            throw new RuntimeException("Invalid constant pool index: " + index);
        if (entryTable[index] == null) {
            CP_UTF8_Entry entry = new CP_UTF8_Entry("<not yet read>");
            entryTable[index] = entry;
            entry.count = (short) index;
            return entry;
        } else
            return (CP_UTF8_Entry) entryTable[index];
    }

    CP_Class_Entry getOrAllocClassEntry(int index) {
        if (index >= entryTable.length || index <= 0)
            throw new RuntimeException("Invalid constant pool index: " + index);
        if (entryTable[index] == null) {
            CP_Class_Entry entry = new CP_Class_Entry(null);
            entryTable[index] = entry;
            entry.count = (short) index;
            return entry;
        } else
            return (CP_Class_Entry) entryTable[index];
    }

    CP_NameAndType_Entry getOrAllocNameAndTypeEntry(int index) {
        if (index >= entryTable.length || index <= 0)
            throw new RuntimeException("Invalid constant pool index: " + index);
        if (entryTable[index] == null) {
            CP_NameAndType_Entry entry = new CP_NameAndType_Entry();
            entryTable[index] = entry;
            entry.count = (short) index;
            return entry;
        } else
            return (CP_NameAndType_Entry) entryTable[index];
    }

    class CP_Entry {
        short count;

        void write(DataOutputStream out) throws IOException {
            throw new RuntimeException(getClass().getName() + ".write not implemented");
        }

        Object getKey() {
            throw new RuntimeException(getClass().getName() + ".getKey() not implemented");
        }

        int countInc() {
            return 1;
        }

        void insert() {
            count = (short) constantPoolSize;
            constantPoolSize += countInc();
            if (constantPoolSize > 0xFFFF)
                throw new RuntimeException("Constant pool overflow (probably too large class)");
            constantPoolAcc.addElement(this);
            constantPoolTable.put(getKey(), this);
        }

        java.io.Serializable getData() {
            throw new RuntimeException(getClass().getName() + ".getData() not implemented");
        }

        int getTag() {
            throw new RuntimeException(getClass().getName() + ".getTag() not implemented");
        }


    }

    class CP_UTF8_Entry extends CP_Entry {

        String data;

        CP_UTF8_Entry(String s) {
            data = s;
        }

        Object getKey() {
            return data;
        }

        public boolean equals(Object other) {
            return other == this;
        }

        public int hashCode() {
            return count;
        }

        void write(DataOutputStream out) throws IOException {
            out.write(TAG_UTF8);
            out.writeUTF(data);
        }

        String getData() {
            return data;
        }

        int getTag() {
            return TAG_UTF8;
        }
    }

    class CP_Number_Entry extends CP_Entry {
        Number data;

        CP_Number_Entry(Number n) {
            data = n;
        }

        Object getKey() {
            return data;
        }

        int countInc() {
            return (data instanceof Long || data instanceof Double ? 2 : 1);
        }

        void write(DataOutputStream out) throws IOException {
            if (data instanceof Integer) {
                out.write(TAG_Integer);
                out.writeInt(data.intValue());
            } else if (data instanceof Float) {
                out.write(TAG_Float);
                out.writeFloat(data.floatValue());
            } else if (data instanceof Double) {
                out.write(TAG_Double);
                out.writeDouble(data.doubleValue());
            } else if (data instanceof Long) {
                out.write(TAG_Long);
                out.writeLong(data.longValue());
            } else
                throw new RuntimeException("Unexpected type: " +
                        data.getClass().getName());
        }

        Number getData() {
            return data;
        }
    }

    class CP_String_Entry extends CP_Entry {
        CP_UTF8_Entry ref;

        CP_String_Entry(CP_UTF8_Entry ref) {
            this.ref = ref;
        }

        Object getKey() {
            return ref;
        }

        void write(DataOutputStream out) throws IOException {
            out.write(TAG_String);
            short cnt = ref.count;
            out.write(cnt >> 8);
            out.write(cnt);
        }

        String getData() {
            return ref.data;
        }

    }

    class CP_Class_Entry extends CP_Entry {
        CP_UTF8_Entry ref;

        CP_Class_Entry(CP_UTF8_Entry ref) {
            this.ref = ref;
        }

        void write(DataOutputStream out) throws IOException {
            out.write(TAG_Class);
            short cnt = ref.count;
            out.writeShort(cnt);
        }

        void insert() {
            count = (short) (constantPoolSize++);
            if (constantPoolSize > 0xFFFF)
                throw new RuntimeException("Constant pool overflow (probably too large class)");
            constantPoolAcc.addElement(this);
            classPoolTable.put(ref, this);
        }

        String getData() {
            return ref.data;
        }

        int getTag() {
            return TAG_Class;
        }
    }

    class CP_NameAndType_Entry extends CP_Entry {
        CP_UTF8_Entry name;
        CP_UTF8_Entry type;

        CP_NameAndType_Entry() {
        }

        CP_NameAndType_Entry(String name, String type) {
            this.name = lookupUTF8(name);
            this.type = lookupUTF8(type);
        }

        CP_NameAndType_Entry(CP_UTF8_Entry name, CP_UTF8_Entry type) {
            this.name = name;
            this.type = type;
        }

        public int hashCode() {
            return name.count + (type.count << 11);
        }

        public boolean equals(Object other) {
            if (other instanceof CP_NameAndType_Entry) {
                CP_NameAndType_Entry nte = (CP_NameAndType_Entry) other;
                return nte.name == name && nte.type == type;
            }
            return false;
        }

        Object getKey() {
            return this;
        }

        void write(DataOutputStream out) throws IOException {
            out.write(TAG_NameAndType);
            out.writeShort(name.count);
            out.writeShort(type.count);
        }

        int getTag() {
            return TAG_NameAndType;
        }

    }

    class CP_MemberRef_Entry extends CP_Entry {
        byte tag;
        CP_Class_Entry clazz;
        CP_NameAndType_Entry signature;

        CP_MemberRef_Entry(byte tag, String clazz, String name, String type) {
            this.tag = tag;
            this.clazz = lookupClass(clazz);
            this.signature = lookupNameAndType(name, type);
        }

        CP_MemberRef_Entry(byte tag, CP_Class_Entry clazz,
                           CP_NameAndType_Entry nameAndType) {
            this.tag = tag;
            this.clazz = clazz;
            this.signature = nameAndType;
        }

        public int hashCode() {
            return clazz.count + (signature.count << 11);
        }

        public boolean equals(Object other) {
            if (other instanceof CP_MemberRef_Entry) {
                CP_MemberRef_Entry mre = (CP_MemberRef_Entry) other;
                return mre.tag == tag && mre.clazz == clazz &&
                        mre.signature == signature;
            }
            return false;
        }

        Object getKey() {
            return this;
        }

        void write(DataOutputStream out) throws IOException {
            out.write(tag);
            out.write(clazz.count >> 8);
            out.write(clazz.count);
            out.write(signature.count >> 8);
            out.write(signature.count);
        }
    }

}
