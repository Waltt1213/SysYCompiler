package backend.mips;

import java.util.ArrayList;

public class MipsModule {

    private ArrayList<MipsData> dataSegment;
    private ArrayList<MipsFunction> textSegment;

    public MipsModule() {
        dataSegment = new ArrayList<>();
        textSegment = new ArrayList<>();
    }

    public void addGlobalData(MipsData data) {
        dataSegment.add(data);
    }

    public void addFunction(MipsFunction function) {
        textSegment.add(function);
    }
}
