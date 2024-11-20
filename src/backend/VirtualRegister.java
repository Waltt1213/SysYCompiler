package backend;

import java.util.Objects;

public class VirtualRegister {
    private final String name;
    private int start;
    private int end;

    private boolean isConst = false;

    public static VirtualRegister vSp = new VirtualRegister("$sp");
    public static VirtualRegister vfp = new VirtualRegister("$fp");
    public static VirtualRegister vra = new VirtualRegister("$ra");
    public static VirtualRegister vZero = new VirtualRegister("$zero");

    public VirtualRegister(String name) {
        this.name = name;
    }

    public void setStart(int start) {
        this.start = start;
    }

    public void setEnd(int end) {
        this.end = end;
    }

    public int getStart() {
        return start;
    }

    public int getEnd() {
        return end;
    }

    public void setConst(boolean aConst) {
        isConst = aConst;
    }

    public boolean isConst() {
        return isConst;
    }

    public String getName() {
        return name;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof VirtualRegister)) {
            return false;
        }
        VirtualRegister o = (VirtualRegister) obj;
        return o.name.equals(this.name);
    }
}
