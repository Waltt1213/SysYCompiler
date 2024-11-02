package llvmir.values.instr;

import llvmir.ValueType;

public class Compare extends Instruction {
    private final CondType condType;

    public Compare(ValueType.Type vt, String name, String sym) {
        super(vt, Type.ICMP, name);
        condType = CondType.getOp(sym);
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
}
