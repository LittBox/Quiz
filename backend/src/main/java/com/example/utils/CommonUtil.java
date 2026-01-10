package com.example.utils;

public class CommonUtil {
    public static String fullWidthToHalfWidth(String input) {
        if (input == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder(input.length());
        for (char c : input.toCharArray()) {
            // 全角空格转换为半角空格
            if (c == 12288) {
                sb.append((char) 32);
                continue;
            }
            // 其他全角字符转换为半角字符
            if (c >= 65281 && c <= 65374) {
                sb.append((char) (c - 65248));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
