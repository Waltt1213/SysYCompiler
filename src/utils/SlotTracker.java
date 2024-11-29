package utils;

public class SlotTracker {
    private static int num = 0;
    private static int strNum = 0;

    public static String slot() {
        num++;
        return String.valueOf(num - 1);
    }

    public static String slotStr() {
        if (strNum == 0) {
            strNum++;
            return ".str";
        }
        strNum++;
        return ".str." + (strNum - 1);
    }

    public static void reset() {
        num = 0;
    }
}
