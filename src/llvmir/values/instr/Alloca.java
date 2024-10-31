package llvmir.values.instr;

import llvmir.DataType;
import llvmir.values.BasicBlock;

public class Alloca extends Instruction {
    public Alloca(DataType vt, BasicBlock basicBlock) {
        super(vt, Type.ALLOCA, basicBlock);
    }
}
