package utils;

import frontend.TokenType;

import java.util.HashMap;

public class Transform {
    private static final HashMap<String, Integer> asciiMap = new HashMap<String, Integer>() {{
        put("\\a", 7);
        put("\\b", 8);
        put("\\t", 9);
        put("\\n", 10);
        put("\\v", 11);
        put("\\f", 12);
        put("\\\"", 34);
        put("\\'", 39);
        put("\\\\", 92);
        put("\\0", 0);
    }};

    public static int str2int(String str) {
        if (str.length() == 1) {
            return str.charAt(0);
        } else {
            if (asciiMap.containsKey(str.substring(1, str.length() - 1))) {
                return asciiMap.get(str.substring(1, str.length() - 1));
            }
            return 0;
        }
    }
}
