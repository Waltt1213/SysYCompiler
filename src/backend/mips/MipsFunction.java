package backend.mips;

import java.util.ArrayList;

public class MipsFunction {
    private String name;
    private ArrayList<MipsInstruction> instructions;

    public MipsFunction(String name) {
        this.name = name;
        instructions = new ArrayList<>();
    }

    public void addInstr(MipsInstruction instruction) {
        instructions.add(instruction);
    }

}
