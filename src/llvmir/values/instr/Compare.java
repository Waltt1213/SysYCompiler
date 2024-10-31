package llvmir.values.instr;

import llvmir.DataType;
import llvmir.values.BasicBlock;

public class Compare extends Instruction {

    public Compare(DataType vt, BasicBlock basicBlock) {
        super(vt, Type.ICMP, basicBlock);
    }
}
