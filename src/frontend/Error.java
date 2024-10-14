package frontend;

public class Error extends Exception {
    private final ErrorType type;
    private final int lineno;

    public enum ErrorType {
        a, b, c, d, e, f, g, h, i, j, k, l, m, myError;
    }

    public Error(String type, int lineno) {
        this.type = ErrorType.valueOf(type);
        this.lineno = lineno;
    }

    public ErrorType getType() {
        return type;
    }

    public int getLineno() {
        return lineno;
    }

    @Override
    public String toString() {
        return  lineno + " " + type.toString();
    }
}
