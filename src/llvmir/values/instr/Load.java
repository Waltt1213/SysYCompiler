package llvmir.values.instr;

import llvmir.TypeId;

public class Load extends Instr {

    public Load(TypeId vt, String name) {
        super(vt, name);
    }
}
