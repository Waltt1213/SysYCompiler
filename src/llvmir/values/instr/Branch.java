package llvmir.values.instr;

import llvmir.DataType;

public class Branch extends Instruction {

    public Branch(DataType vt, String name) {
        super(vt, Type.BR, name);
    }
}
