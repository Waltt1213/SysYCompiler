package llvmir.values.instr;

import llvmir.ValueType;

public class GetElementPtr extends Instruction {

    // 传进来的是[]* 或 i_*
    public GetElementPtr(ValueType.Type vt, String name) {
        super(vt, Type.GETPTR, name);
    }

    @Override
    public ValueType.Type getTp() {
        return tp.getInnerType().getAddr();
    }

    @Override
    public String getDef() {
        if (name != null) {
            return super.getDef();
        }
        StringBuilder sb = new StringBuilder();
        sb.append(getTp()).append(" getelementptr inbounds (");
        sb.append(tp.getInnerType()).append(", ");
        sb.append(getOperands().get(0).getDef());
        sb.append(", ").append(getOperands().get(1).getDef());
        if (getOperands().size() > 2) {
            sb.append(", ").append(getOperands().get(2).getDef());
        }
        sb.append(")");
        return sb.toString();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getFullName()).append(" = getelementptr inbounds ");
        sb.append(tp.getInnerType()).append(", ");
        sb.append(getOperands().get(0).getDef());
        sb.append(", ").append(getOperands().get(1).getDef());
        if (getOperands().size() > 2) {
            sb.append(", ").append(getOperands().get(2).getDef());
        }
        return sb.toString();
    }
}
