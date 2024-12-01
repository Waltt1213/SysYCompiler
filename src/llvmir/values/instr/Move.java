package llvmir.values.instr;

import llvmir.Value;
import llvmir.ValueType;
import llvmir.values.BasicBlock;

import java.util.HashSet;

public class Move extends Instruction {
    private Value dst;
    private Value src;

    public Move(Value dst, Value src, BasicBlock parent) {
        super(new ValueType.Type(ValueType.DataType.VoidTy), Type.MOVE, "");
        this.setParent(parent);
        this.dst = dst;
        this.src = src;
    }

    public Value getDst() {
        return dst;
    }

    public Value getSrc() {
        return src;
    }

    @Override
    public void setVirtualName() {
        dst.setVirtualName();
    }

    @Override
    public HashSet<Value> use() {
        HashSet<Value> use = new HashSet<>();
        use.add(src);
        return use;
    }

    @Override
    public Value def() {
        return dst;
    }

    @Override
    public String toString() {
        return "move " + dst.getFullName() + ", " + src.getFullName();
    }
}
