package llvmir.values.instr;

import llvmir.DataType;

public class Alloca extends Instr {
    public Alloca(DataType vt, String name) {
        super(vt, name);
    }
}
