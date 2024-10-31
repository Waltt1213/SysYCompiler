package llvmir.values.instr;

import llvmir.DataType;
import llvmir.values.BasicBlock;

public class Branch extends Instruction {

    public Branch(DataType vt, BasicBlock basicBlock) {
        super(vt, Type.BR, basicBlock);
    }
}
