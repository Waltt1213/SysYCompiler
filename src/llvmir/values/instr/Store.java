package llvmir.values.instr;

import llvmir.TypeId;

public class Store extends Instr {

    public Store(TypeId vt, String name) {
        super(vt, name);
    }
}
