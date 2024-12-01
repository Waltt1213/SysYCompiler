package backend.mips;

import java.util.ArrayList;

public class MipsFunction {
    private final String name;
    private final ArrayList<MipsInstruction> instructions;

    public MipsFunction(String name) {
        this.name = name;
        instructions = new ArrayList<>();
    }

    public void addInstr(MipsInstruction instruction) {
        instructions.add(instruction);
    }

    public String getName() {
        return name;
    }

    public ArrayList<MipsInstruction> getInstructions() {
        return instructions;
    }
}
