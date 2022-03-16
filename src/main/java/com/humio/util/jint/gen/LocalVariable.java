package com.humio.util.jint.gen;

import com.humio.util.jint.constants.MiniErrorCodes;
import com.humio.util.jint.util.CompilerException;

public class LocalVariable {

    private final int index;
    private final String type;

    public LocalVariable(int index, String type) {
        this.index = index;
        this.type = type;
    }

    public int getIndex() {
        return this.index;
    }

    public String getType() {
        return this.type;
    }

    public void typeCheck(String usageType) {
        if (!this.type.equals(usageType)) {
            throw new CompilerException(MiniErrorCodes.ERR_C_LOCVAR_TYPING, -1, null, new Object[] {type, usageType});
        }
    }

}
