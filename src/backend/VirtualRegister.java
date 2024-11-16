package backend;

import java.util.Objects;

public class VirtualRegister {
    private final String name;
    private int start;
    private int end;

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
