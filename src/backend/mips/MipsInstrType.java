package backend.mips;

public enum MipsInstrType {
    LI("li"),
    LA("la"),
    MOVE("move"),
    ADD("add"),
    ADDI("addi"),
    ADDU("addu"),
    SUB("sub"),
    SUBI("subi"),
    SUBU("subu"),
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
    JR("jr"),
    SYSCALL("syscall");

    private final String value;

    MipsInstrType(String value) {
        this.value = value;
    }

    public boolean isMem() {
        return this == SW || this == SB || this == LW || this == LB;
    }

    @Override
    public String toString() {
        return value;
    }
}
