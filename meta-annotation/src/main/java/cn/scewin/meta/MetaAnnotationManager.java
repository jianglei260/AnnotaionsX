package cn.scewin.meta;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class MetaAnnotationManager {
    private static MetaAnnotationManager instance;
    private Map<Class<? extends Annotation>, Set<Class>> map;

    public synchronized static MetaAnnotationManager getInstance() {
        if (instance == null) {
            instance = new MetaAnnotationManager();
        }
        return instance;
    }

    private MetaAnnotationManager() {
        map = new HashMap<>();
    }

    public void registe(Class<? extends Annotation> annotationClass, Class clazz) {
        if (map.containsKey(annotationClass)){
            map.get(annotationClass).add(clazz);
        }else {
            Set<Class> set=new HashSet<>();
            set.add(clazz);
            map.put(annotationClass,set);
        }
    }

    public Set<Class> getClass(Class<? extends Annotation> annotationClass) {
        Set<Class> result = map.get(annotationClass);
        if (result == null) {
            try {
                Class clazz = Class.forName(annotationClass.getName() + "AnnotationClasses");
                clazz.newInstance();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return map.get(annotationClass);
    }

}
