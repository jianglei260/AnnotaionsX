package cn.scewin.annotionsx.common.utils;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;

import java.util.Stack;

import cn.scewin.annotionsx.common.TypeItem;

public class TypeNameUtil {
    public static TypeName getTypeName(String type) {
        TypeName typeName = null;
        if (type.contains(".")) {
            return ClassName.bestGuess(type);
        } else {
            switch (type) {
                case "void":
                    typeName = TypeName.VOID;
                    break;
                case "boolean":
                    typeName = TypeName.BOOLEAN;
                    break;
                case "byte":
                    typeName = TypeName.BYTE;
                    break;
                case "short":
                    typeName = TypeName.SHORT;
                    break;
                case "int":
                    typeName = TypeName.INT;
                    break;
                case "long":
                    typeName = TypeName.LONG;
                    break;
                case "char":
                    typeName = TypeName.CHAR;
                    break;
                case "float":
                    typeName = TypeName.FLOAT;
                    break;
                case "double":
                    typeName = TypeName.DOUBLE;
                    break;
                default:
                    typeName = TypeName.OBJECT;
                    break;
            }
            return typeName;
        }
    }


    public static TypeName getTypeNameFromString(String type) {
        if (!type.contains("<")) {
            return ClassName.bestGuess(type);
        }
        Stack<TypeItem> stack = new Stack<>();
        char[] chars = type.toCharArray();
        StringBuilder sb = new StringBuilder();
        TypeName typeName = null;
        for (int i = 0; i < chars.length; i++) {
            if (chars[i] == '<') {
                TypeItem item = new TypeItem();
                item.className = ClassName.bestGuess(sb.toString());
                sb = new StringBuilder();
                stack.push(item);
            } else if (chars[i] == ',') {
                TypeItem item = stack.peek();
                if (sb.length() > 0) {
                    item.typeNames.add(ClassName.bestGuess(sb.toString()));
                }
                sb = new StringBuilder();
            } else if (chars[i] == '>') {
                TypeItem item = stack.pop();
                if (sb.length() > 0) {
                    item.typeNames.add(ClassName.bestGuess(sb.toString()));
                }
                sb = new StringBuilder();
                if (item.typeNames.size() > 0) {
                    TypeName[] childTypeNames = new TypeName[item.typeNames.size()];
                    item.typeNames.toArray(childTypeNames);
                    item.typeName = ParameterizedTypeName.get(item.className, childTypeNames);
                } else {
                    item.typeName = item.className;
                }
                if (!stack.isEmpty()) {
                    TypeItem lastItem = stack.peek();
                    lastItem.typeNames.add(item.typeName);
                }
                typeName = item.typeName;
            } else {
                sb.append(chars[i]);
            }
        }
        return typeName;
    }

}
