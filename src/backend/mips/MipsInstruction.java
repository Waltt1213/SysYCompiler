package backend.mips;

import java.util.ArrayList;

public class MipsInstruction {
    private String operand1;
    private String operand2;
    private String operand3;
    private MipsInstrType op;
    private boolean isLabel = false;
    private boolean isMem = false;
    private int operandNum = 3;

    public MipsInstruction(MipsInstrType op, String operand1,
                           String operand2, String operand3) {
        this.op = op;
        this.operand1 = operand1;
        this.operand2 = operand2;
        this.operand3 = operand3;
        this.isMem = op.isMem();
    }

    public MipsInstruction(MipsInstrType op, String operand1, String operand2) {
        this.op = op;
        this.operand1 = operand1;
        this.operand2 = operand2;
        operandNum = 2;
    }

    public MipsInstruction(MipsInstrType op, String operand1) {
        this.op = op;
        this.operand1 = operand1;
        operandNum = 1;
    }

    public MipsInstruction(String label) {
        this.operand1 = label;
        this.isLabel = true;
    }

    public MipsInstruction(MipsInstrType op) {
        this.op = op;
        this.operandNum = 0;
    }

    public MipsInstrType getOp() {
        return op;
    }

    public ArrayList<String> getOperands() {
        ArrayList<String> operands = new ArrayList<>();
        if (operand1 != null) {
            operands.add(operand1);
        }
        if (operand2 != null) {
            operands.add(operand2);
        }
        if (operand3 != null) {
            operands.add(operand3);
        }
        return operands;
    }

    @Override
    public String toString() {
        if (isLabel) {
            return String.format("%s:", operand1);
        }
        if (operandNum == 0) {
            return String.format("\t%s", op.toString());
        } else if (operandNum == 1) {
            return String.format("\t%s %s", op.toString(), operand1);
        } else if (operandNum == 2) {
            return String.format("\t%s %s, %s", op.toString(), operand1, operand2);
        } else {
            if (isMem) {
                return String.format("\t%s %s, %s(%s)", op.toString(), operand1, operand3, operand2);
            }
            return String.format("\t%s %s, %s, %s", op.toString(), operand1, operand2, operand3);
        }
    }
}
