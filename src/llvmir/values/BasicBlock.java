package llvmir.values;

import llvmir.Value;
import llvmir.ValueType;
import llvmir.values.instr.Instruction;

import java.util.ArrayList;
import java.util.LinkedList;

public class BasicBlock extends Value {
    private final LinkedList<Instruction> instructions;
    private ArrayList<Label> preds;
    private Function parent;
    private boolean isTerminator;

    public BasicBlock(String name) {
        super(new ValueType.Type(ValueType.DataType.LabelTy), name);
        instructions = new LinkedList<>();
        preds = new ArrayList<>();
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

    public void addPred(Label label) {
        preds.add(label);
    }

    @Override
    public String getDef() {
        return super.getDef();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n");
        sb.append(getName()).append(": ");
        sb.append("\n");
        for (Instruction instr: instructions) {
            sb.append("\t").append(instr.toString()).append("\n");
        }
        return sb.toString();
    }
}
