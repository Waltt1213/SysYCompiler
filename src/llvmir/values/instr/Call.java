package llvmir.values.instr;

import llvmir.DataType;

public class Call extends Instruction {

    public Call(DataType vt, String name) {
        super(vt, Type.CALL, name);
    }
}
