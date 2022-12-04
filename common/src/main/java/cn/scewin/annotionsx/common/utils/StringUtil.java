package cn.scewin.annotionsx.common.utils;

import java.util.List;


public class StringUtil {
    public static boolean isEmpty(String s) {
        return s == null || s.equals("");
    }

    public static boolean isNotEmpty(String s) {
        return !isEmpty(s);
    }

    public static String getMethodName(String fieldName, boolean isBool) {
        if (isBool) {
            return "is" + upcaseFirstChar(fieldName);
        } else {
            return "get" + upcaseFirstChar(fieldName);
        }
    }

    public static String setMethodName(String fieldName) {
        return "set" + upcaseFirstChar(fieldName);
    }

    public static String lowerFirstChar(String name) {
        char ch = name.charAt(0);
        return name.replaceFirst(ch + "", Character.toLowerCase(ch) + "");
    }

    public static String upcaseFirstChar(String name) {
        char ch = name.charAt(0);
        return name.replaceFirst(ch + "", Character.toUpperCase(ch) + "");
    }


    public static String getClassSimpleName(String name) {
        if (name.contains(".")) {
            return name.substring(name.lastIndexOf(".") + 1);
        } else {
            return name;
        }
    }

    public static String convertWith_(String name) {
        int len = name.length();
        StringBuilder nameBuilder = new StringBuilder();
        for (int i = 0; i < len; i++) {
            char ch = name.charAt(i);
            if (Character.isUpperCase(ch)) {
                if (i != 0) {
                    nameBuilder.append('_');
                }
                nameBuilder.append(Character.toLowerCase(ch));
            } else {
                nameBuilder.append(ch);
            }
        }
        return nameBuilder.toString();
    }

    public static String convertName(String colName) {
        int len = colName.length();
        StringBuilder nameBuilder = new StringBuilder();
        boolean upperNext = false;
        for (int i = 0; i < len; i++) {
            char ch = colName.charAt(i);
            if (ch == '_') {
                if (i == 0) {
                    upperNext = false;
                } else {
                    upperNext = true;
                }
                continue;
            } else {
                if (upperNext) {
                    nameBuilder.append(Character.toUpperCase(ch));
                } else {
                    nameBuilder.append(ch);
                }
                upperNext = false;
            }
        }
        return nameBuilder.toString();
    }

    public static String concat(String[] strings, char ch) {
        StringBuilder builder = new StringBuilder();
        for (String string : strings) {
            builder.append(string);
            builder.append(ch);
        }
        builder.deleteCharAt(builder.length() - 1);
        return builder.toString();
    }

    public static String concat(List<String> strings, char ch) {
        StringBuilder builder = new StringBuilder();
        for (String string : strings) {
            builder.append(string);
            builder.append(ch);
        }
        builder.deleteCharAt(builder.length() - 1);
        return builder.toString();
    }
}
