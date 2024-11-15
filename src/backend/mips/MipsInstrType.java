package backend.mips;

public enum MipsInstrType {
    LI("li"),
    LA("la"),
    MOVE("move"),
    ADD("add"),
    ADDI("addi"),
    ADDU("addu"),
    ADDIU("addiu"),
    SUB("sub"),
    SUBI("subi"),
    SUBU("subu"),
    SLL("sll"),
    MULT("mult"),
    DIV("div"),
    MFHI("mfhi"),
    MFLO("mflo"),
    ORI("ori"),
    ANDI("andi"),
    OR("or"),
    AND("and"),
    BEQ("beq"),
    BNE("bne"),
    BLT("blt"),
    BLE("ble"),
    BGT("bgt"),
    BGE("bge"),
    BGTZ("bgtz"),
    SW("sw"),
    SB("sb"),
    LW("lw"),
    LB("lb"),
    J("j"),
    JAL("jal"),
    JALR("jalr"),
    JR("jr"),
    SYSCALL("syscall"),
    NOP("nop");

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
