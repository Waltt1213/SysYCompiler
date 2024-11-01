package llvmir.values.instr;

import llvmir.DataType;

public class Phi extends Instruction {
    public Phi(DataType vt, String name) {
        super(vt, Type.PHI, name);
    }
}
