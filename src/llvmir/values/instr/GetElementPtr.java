package llvmir.values.instr;

import llvmir.DataType;

public class GetElementPtr extends Instruction {

    public GetElementPtr(DataType vt, String name) {
        super(vt, Type.GETPTR, name);
    }
}
