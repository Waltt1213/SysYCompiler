package middle;

public class SlotTracker {
    private static int num = 0;

    public static String slot() {
        int register = num;
        num++;
        return String.valueOf(register);
    }

    public static void reset() {
        num = 0;
    }
}
