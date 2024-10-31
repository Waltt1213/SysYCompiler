package llvmir.values.instr;

import llvmir.DataType;
import llvmir.User;
import llvmir.values.BasicBlock;

public class Instruction extends User {
    private final BasicBlock parent;
    private final Type irType;

    public Instruction(DataType vt, Type irType, BasicBlock basicBlock) {
        super(vt);
        this.parent = basicBlock;
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

        public String getValue() {
            return value;
        }

        @Override
        public String toString() {
            return value;
        }
    }

    public BasicBlock getParent() {
        return parent;
    }

    public Type getIrType() {
        return irType;
    }
}
