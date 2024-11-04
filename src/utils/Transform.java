package utils;

import frontend.TokenType;

import java.util.ArrayList;
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
            if (asciiMap.containsKey(str)) {
                return asciiMap.get(str);
            }
            return 0;
        }
    }

    public static ArrayList<Integer> str2intList(String str) {
        ArrayList<Integer> integers = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < str.toCharArray().length; i++) {
            if (str.toCharArray()[i] != '\\') {
                sb.append(str.charAt(i));
                if (asciiMap.containsKey(sb.toString())) {
                    integers.add(asciiMap.get(sb.toString()));
                    sb.setLength(0);
                    continue;
                }
                sb.setLength(0);
                integers.add((int) str.toCharArray()[i]);
            } else {
                sb.append(str.charAt(i));
            }
        }
        return integers;
    }

    /**
     * @param charList  文本形式的字符串
     * @return 不含引号允许转义含义的字符串
     */
    public static String charList2string(String charList) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < charList.length(); i++) {
            if (charList.charAt(i) == '\\' && charList.charAt(i + 1) == 'n') {
                sb.append("\n");
                i++;
            } else {
                sb.append(charList.charAt(i));
            }
        }
        return sb.toString();
    }

    /**
     * @param str  不含引号字符串
     * @return 含引号可打印转义字符的字符串
     */
    public static String str2charList(String str) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < str.length(); i++) {
            if (str.charAt(i) == '\n') {
                sb.append("\\0A");
                i++;
            } else {
                sb.append(str.charAt(i));
            }
        }
        return sb.toString();
    }
}
