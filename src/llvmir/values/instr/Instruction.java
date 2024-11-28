package llvmir.values.instr;

import llvmir.User;
import llvmir.Value;
import llvmir.ValueType;
import llvmir.values.BasicBlock;
import middle.SlotTracker;

import java.util.HashSet;

public class Instruction extends User {
    private final Type irType;
    private BasicBlock parent;
    private boolean needName = false;

    public Instruction(ValueType.Type vt, Type irType, String name) {
        super(vt, name);
        this.irType = irType;
    }

    public enum Type {
        ADD("add"),
        SUB("sub"),
        MUL("mul"),
        SDIV("sdiv"),
        SREM("srem"),
        ICMP("icmp"),
        AND("and"),
        OR("or"),
        CALL("call"),
        ALLOCA("alloca"),
        LOAD("load"),
        STORE("store"),
        GETPTR("getelementptr"),
        PHI("phi"),
        ZEXT("zext"),
        TRUNC("trunc"),
        BR("br"),
        RET("ret"),
        DEFAULT("default");
        private final String value;

        Type(String value) {
            this.value = value;
        }

        public static Type getOp(String op) {
            switch (op) {
                case "+":
                    return ADD;
                case "-":
                    return SUB;
                case "*":
                    return MUL;
                case "/":
                    return SDIV;
                case "%":
                    return SREM;
                case "&&":
                    return AND;
                case "||":
                    return OR;
                default:
                    return DEFAULT;
            }
        }

        public String getValue() {
            return value;
        }

        @Override
        public String toString() {
            return value;
        }
    }

    public Type getIrType() {
        return irType;
    }

    public void setNeedName(boolean needName) {
        this.needName = needName;
    }

    public boolean isNeedName() {
        return needName;
    }

    public void setParent(BasicBlock parent) {
        this.parent = parent;
    }

    public BasicBlock getParent() {
        return parent;
    }

    public Value def() {
        return super.def();
    }

    public HashSet<Value> use() {
        return new HashSet<>(operands);
    }

    public void remove() {
        parent.removeInstr(this);
        parent = null;
        super.remove();
        for (Value operand: operands) {
            operand.removeUser(this);
        }
        operands.clear();
    }

    public void setVirtualName() {
        if (needName) {
            setName(SlotTracker.slot());
        }
    }
}
