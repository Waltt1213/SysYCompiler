package llvmir.values.instr;

import llvmir.DataType;
import llvmir.values.BasicBlock;

public class BinaryOperator extends Instruction {

    public BinaryOperator(DataType vt, Type type, BasicBlock basicBlock) {
        super(vt, type, basicBlock);
    }
}
