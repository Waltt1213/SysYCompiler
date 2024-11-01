package llvmir.values.instr;

import llvmir.DataType;

public class Load extends Instruction {

    public Load(DataType vt, String name) {
        super(vt, Type.LOAD, name);
    }
}
