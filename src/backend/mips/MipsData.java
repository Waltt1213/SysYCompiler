package backend.mips;

public class MipsData {
    private String name;
    private String type;
    private String init;

    public MipsData(String name, String type, String init) {
        this.name = name;
        this.type = type;
        this.init = init;
    }

    @Override
    public String toString() {
        return String.format("\t%s: %s %s", name, type, init);
    }
}
