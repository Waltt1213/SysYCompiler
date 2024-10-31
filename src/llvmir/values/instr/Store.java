package llvmir.values.instr;

import llvmir.DataType;

public class Store extends Instr {

    public Store(DataType vt, String name) {
        super(vt, name);
    }
}
