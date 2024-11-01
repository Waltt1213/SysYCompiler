package llvmir.values.instr;

import llvmir.DataType;

public class Store extends Instruction {

    public Store(DataType vt, String name) {
        super(vt, Type.STORE, name);
    }
}
