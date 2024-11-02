package llvmir.values.instr;

import llvmir.ValueType;

public class Branch extends Instruction {

    public Branch(ValueType.Type vt, String name) {
        super(vt, Type.BR, name);
    }
}
