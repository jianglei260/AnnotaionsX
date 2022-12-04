package cn.scewin.gr.android;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DataObjectManager {
    private static DataObjectManager instance;

    public static DataObjectManager getInstance() {
        if (instance == null) {
            instance = new DataObjectManager();
        }
        return instance;
    }

    public <K, T> Map<K, List<T>> mapObjects(List<T> objects, String keyField, Class<K> fieldType, Class<T> dataClass) {
        Map<K, List<T>> map = new HashMap();
        try {
            Field field = dataClass.getDeclaredField(keyField);
            field.setAccessible(true);
            if (field.getType().equals(fieldType)) {
                for (T object : objects) {
                    K key = (K) field.get(object);
                    if (map.containsKey(key)) {
                        List<T> list = map.get(key);
                        list.add(object);
                    } else {
                        List<T> list = new ArrayList<>();
                        map.put(key, list);
                        list.add(object);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return map;
    }
}
