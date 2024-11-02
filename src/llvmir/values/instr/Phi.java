package llvmir.values.instr;

import llvmir.ValueType;

public class Phi extends Instruction {
    public Phi(ValueType.Type vt, String name) {
        super(vt, Type.PHI, name);
    }
}
