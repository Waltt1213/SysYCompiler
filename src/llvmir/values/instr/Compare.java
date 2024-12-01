package llvmir.values.instr;

import llvmir.ValueType;

public class Compare extends Instruction {
    private CondType condType;

    public Compare(String name, CondType type) {
        super(new ValueType.Type(ValueType.DataType.Integer1Ty), Type.ICMP, name);
        condType = type;
    }

    public CondType getCondType() {
        return condType;
    }

    public void setCondType(CondType condType) {
        this.condType = condType;
    }

    public enum CondType {
        SGT("sgt"),
        SLT("slt"),
        SGE("sge"),
        SLE("sle"),
        EQ("eq"),
        NE("ne"),
        DEFAULT("default");
        private final String value;

        CondType(String value) {
            this.value = value;
        }

        public static CondType getOp(String symbol) {
            switch (symbol) {
                case ">":
                    return SGT;
                case "<":
                    return SLT;
                case ">=":
                    return SGE;
                case "<=":
                    return SLE;
                case "==":
                    return EQ;
                case "!=":
                    return NE;
                default:
                    return DEFAULT;
            }
        }

        @Override
        public String toString() {
            return value;
        }
    }

    @Override
    public String toString() {
        return getFullName() + " = icmp " + condType + " "
                + getOperands().get(0).getDef() + ", "
                + getOperands().get(1).getFullName();
    }
}
