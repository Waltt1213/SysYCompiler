package llvmir.values.instr;

import llvmir.TypeId;

public class Alloca extends Instr {
    public Alloca(TypeId vt, String name) {
        super(vt, name);
    }
}
