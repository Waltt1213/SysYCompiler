package llvmir.values.instr;

import llvmir.DataType;
import llvmir.values.BasicBlock;

public class Phi extends Instruction {
    public Phi(DataType vt, BasicBlock basicBlock) {
        super(vt, Type.PHI, basicBlock);
    }
}
