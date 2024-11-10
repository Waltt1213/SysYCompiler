package backend.mips;

public enum MipsInstrType {
    ADD("add"),
    ADDI("addi"),
    SUB("sub"),
    SUBI("subi"),
    SLL("sll"),
    ORI("ori"),
    ANI("andi"),
    OR("or"),
    AND("and"),
    BEQ("beq"),
    BNE("bne"),
    BGTZ("bgtz"),
    SW("sw"),
    SB("sb"),
    LW("lw"),
    LB("lb"),
    J("j"),
    JAL("jal"),
    JALR("jalr"),
    SYSCALL("syscall");

    private final String value;

    MipsInstrType(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }
}
