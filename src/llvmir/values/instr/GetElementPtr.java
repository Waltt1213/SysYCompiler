package llvmir.values.instr;

import llvmir.DataType;
import llvmir.values.BasicBlock;

public class GetElementPtr extends Instruction {

    public GetElementPtr(DataType vt, BasicBlock basicBlock) {
        super(vt, Type.GETPTR, basicBlock);
    }
}
