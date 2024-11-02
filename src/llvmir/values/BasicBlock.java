package llvmir.values;

import llvmir.Value;
import llvmir.ValueType;
import llvmir.values.instr.Instruction;

import java.util.LinkedList;

public class BasicBlock extends Value {
    private final LinkedList<Instruction> instructions;
    private Function parent;
    private boolean isTerminator;

    public BasicBlock(ValueType.Type vt, String name) {
        super(vt, name);
        instructions = new LinkedList<>();
    }

    public void setParent(Function parent) {
        this.parent = parent;
    }

    public Function getParent() {
        return parent;
    }

    public void setTerminator(boolean terminator) {
        isTerminator = terminator;
    }

    public boolean isTerminator() {
        return isTerminator;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void appendInstr(Instruction instr) {
        instructions.add(instr);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n");
        for (Instruction instr: instructions) {
            sb.append("\t").append(instr.toString()).append("\n");
        }
        return sb.toString();
    }
}
