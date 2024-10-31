package llvmir.values.instr;

import llvmir.DataType;

public class Load extends Instr {

    public Load(DataType vt, String name) {
        super(vt, name);
    }
}
