package cn.scewin.annotionsx.common.utils;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import cn.scewin.annotionsx.common.MethodParam;


public class ObjectUtil {
    public static boolean isNotEmpty(Object object) {
        if (object != null) {
            if (object instanceof Collection) {
                return !((Collection<?>) object).isEmpty();
            }
            if (object instanceof String) {
                return ((String) object).length() > 0;
            }
            if (object instanceof Map) {
                return !((Map) object).isEmpty();
            }
            if (object.getClass().isArray()) {
                return Array.getLength(object) > 0;
            }
            return true;
        }
        return false;
    }

    public static Object getOne(Object object) {
        if (object != null) {
            if (object instanceof Collection) {
                Collection collection = (Collection) object;
                return collection.iterator().next();
            }
            if (object instanceof Map) {
                Map map = (Map) object;
                return map.values().iterator().next();
            }
            if (object.getClass().isArray()) {
                return Array.get(object, 0);
            }
        }
        return null;
    }

    public static boolean isList(Class type) {
        return isList(type.getName());
    }

    public static boolean isList(String type) {
        try {
            Class clazz = getClassType(type);
            boolean isCollection = Collection.class.isAssignableFrom(clazz);
            boolean isArray = clazz.isArray();
            return isArray || isCollection;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static Class boxType(String type) throws ClassNotFoundException {
        switch (type) {
            case "int":
                return Integer.class;
            case "boolean":
                return Boolean.class;
            case "long":
                return Long.class;
            case "double":
                return Double.class;
            case "float":
                return Float.class;
            case "byte":
                return Byte.class;
            case "char":
                return Character.class;
            case "short":
                return Short.class;
            case "void":
                return Void.class;
            default:
                if (type.contains("<")) {
                    return Class.forName(type.substring(0, type.indexOf('<')));
                } else {
                    return Class.forName(type);
                }
        }
    }

    public static Class getClassType(String type) throws ClassNotFoundException {
        switch (type) {
            case "int":
                return int.class;
            case "boolean":
                return boolean.class;
            case "long":
                return long.class;
            case "double":
                return double.class;
            case "float":
                return float.class;
            case "byte":
                return byte.class;
            case "char":
                return char.class;
            case "short":
                return short.class;
            case "void":
                return void.class;
            default:
                if (type.contains("<")) {
                    return Class.forName(type.substring(0, type.indexOf('<')));
                } else {
                    return Class.forName(type);
                }
        }
    }

    public static Object invokeMethodByJsonParam(Gson gson,Method method, String args) throws Exception {
        Object obj = method.getDeclaringClass().newInstance();
        return invokeMethodByJsonParam(gson,method, obj, args);
    }

    public static Object invokeMethodByDefaultArgAndJsonParam(Gson gson,Method method, Object obj, Object defaultArg, String argJson) throws Exception {
        Parameter[] parameters = method.getParameters();
        JsonParser parser = new JsonParser();
        Object result = null;
        if (parameters != null && parameters.length > 0) {
            JsonObject root = parser.parse(argJson).getAsJsonObject();
            Object args[] = new Object[parameters.length];
            for (int i = 0; i < parameters.length; i++) {
                Class paramType = parameters[i].getType();
                if (i == 0) {
                    if (paramType.equals(defaultArg.getClass())) {
                        args[i] = defaultArg;
                    }
                } else {
                    JsonElement jsonElement = root.get(parameters[i].getName());
                    args[i] = inflateArgs(gson,jsonElement, parameters[i], paramType);
                }
            }
            result = method.invoke(obj, args);
        } else {
            result = method.invoke(obj);
        }
        return result;

    }

    public static Object invokeMethodByJsonParam(Gson gson,Method method, Object obj, String argJson) throws Exception {
        Parameter[] parameters = method.getParameters();
        JsonParser parser = new JsonParser();
        Object result = null;
        if (parameters != null && parameters.length > 0) {
            JsonObject root = parser.parse(argJson).getAsJsonObject();
            Object args[] = new Object[parameters.length];
            for (int i = 0; i < parameters.length; i++) {
                Class paramType = parameters[i].getType();
                JsonElement jsonElement = root.get(parameters[i].getName());
                args[i] = inflateArgs(gson,jsonElement, parameters[i], paramType);
            }
            result = method.invoke(obj, args);
        } else {
            result = method.invoke(obj);
        }
        return result;
    }

    public static Object inflateArgs(Gson gson,JsonElement jsonElement, Parameter parameter, Class paramType) {
        Object result = null;
        if (paramType.isArray() || Collection.class.isAssignableFrom(paramType)) {
            JsonArray jsonArray = jsonElement.getAsJsonArray();
            MethodParam requestParam = parameter.getAnnotation(MethodParam.class);
            if (requestParam != null) {
                Class rawType = requestParam.rawType();
                if (rawType != null) {
                    result = new ArrayList<>();
                    for (JsonElement element : jsonArray) {
                        ((List) result).add(inflateArgs(gson,element, parameter, rawType));
                    }
                }
            }
        } else if (String.class.equals(paramType)) {
            result = jsonElement.getAsString();
        } else if (Integer.class.equals(paramType) || int.class.equals(paramType)) {
            result = jsonElement.getAsInt();
        } else if (Double.class.equals(paramType) || double.class.equals(paramType)) {
            result = jsonElement.getAsDouble();
        } else if (Boolean.class.equals(paramType) || boolean.class.equals(paramType)) {
            result = jsonElement.getAsBoolean();
        } else if (Float.class.equals(paramType) || boolean.class.equals(paramType)) {
            result = jsonElement.getAsFloat();
        } else if (Long.class.equals(paramType) || long.class.equals(paramType)) {
            result = jsonElement.getAsLong();
        } else if (jsonElement.isJsonObject()) {
            result = gson.fromJson(jsonElement.toString(), paramType);
        }
        return result;
    }


    public static Class getParameterizedType(String type) throws ClassNotFoundException {
        if (type.contains("<")) {
            String argType = type.substring(type.indexOf('<') + 1, type.lastIndexOf('>'));
            return getClassType(argType);
        }
        return Void.class;
    }

    public static TypeToken getTypeToken(String type) throws ClassNotFoundException {
        if (type.contains("<")) {
            String argType = type.substring(type.indexOf('<') + 1, type.lastIndexOf('>'));
            String[] args = argType.split(",");
            Type[] typeArgs = new Type[args.length];
            for (int i = 0; i < args.length; i++) {
                typeArgs[i] = getClassType(args[i]);
            }
            return TypeToken.getParameterized(getClassType(type), typeArgs);
        } else {
            return TypeToken.get(getClassType(type));
        }
    }

    public static class TypeItem {
        public TypeName typeName;
        public ClassName className;
        public List<TypeName> typeNames = new ArrayList<>();
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
