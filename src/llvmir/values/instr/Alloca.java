package llvmir.values.instr;

import llvmir.DataType;

public class Alloca extends Instruction {
    public Alloca(DataType vt, String name) {
        super(vt, Type.ALLOCA, name);
    }
}
