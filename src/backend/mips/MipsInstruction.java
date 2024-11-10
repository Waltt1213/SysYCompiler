package backend.mips;

public class MipsInstruction {
    private String operand1;
    private String operand2;
    private String operand3;
    private MipsInstrType op;

    public MipsInstruction(MipsInstrType op, String operand1, String operand2, String operand3) {
        this.op = op;
        this.operand1 = operand1;
        this.operand2 = operand2;
        this.operand3 = operand3;
    }

    @Override
    public String toString() {
        return String.format("\t%s %s, %s, %s", op.toString(), operand1, operand2, operand3);
    }
}
