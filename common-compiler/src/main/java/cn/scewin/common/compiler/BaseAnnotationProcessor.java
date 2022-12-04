package cn.scewin.common.compiler;

import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.TypeSpec;

import java.io.File;
import java.io.IOException;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import javax.tools.FileObject;
import javax.tools.StandardLocation;

import cn.scewin.annotionsx.common.utils.FileUtil;
import cn.scewin.annotionsx.common.utils.StringUtil;

public abstract class BaseAnnotationProcessor extends AbstractProcessor {
    protected Types types;
    protected Messager messager;
    protected Filer filer;
    protected Elements elementsUtils;
    protected ProcessingEnvironment processingEnv;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        this.types = processingEnv.getTypeUtils();
        this.messager = processingEnv.getMessager();
        this.filer = processingEnv.getFiler();
        this.elementsUtils = processingEnv.getElementUtils();
        this.processingEnv = processingEnv;
    }

    public String buildCode(String packageName, TypeSpec typeSpec) {
        try {
            JavaFile.builder(packageName, typeSpec).build().writeTo(processingEnv.getFiler());
        } catch (IOException e) {
            e.printStackTrace();
        }
        String code = typeSpec.toString();
        return code;
    }

    public String getCommonPackage(Set<String> names) {
        String common = names.iterator().next();
        for (String name : names) {
            common = getCommonString(common, name);
            if (StringUtil.isEmpty(common)) {
                break;
            }
        }
        return common;
    }

    public String getCommonString(String s1, String s2) {
        String[] strs1 = s1.split("\\.");
        String[] strs2 = s2.split("\\.");
        int min = Math.min(strs1.length, strs2.length);
        StringBuilder commonBuilder = new StringBuilder();
        for (int i = 0; i < min; i++) {
            if (strs1[i].equals(strs2[i])) {
                commonBuilder.append(strs1[i]);
                commonBuilder.append(".");
            } else {
                break;
            }
        }
        if (commonBuilder.charAt(commonBuilder.length() - 1) == '.') {
            commonBuilder.deleteCharAt(commonBuilder.length() - 1);
        }
        String common = commonBuilder.toString();
        return common;
    }


    public String buildCode(String packageName, TypeSpec.Builder typeBuilder) {
        try {
            JavaFile.builder(packageName, typeBuilder.build()).build().writeTo(processingEnv.getFiler());
        } catch (IOException e) {
            e.printStackTrace();
        }
        String code = typeBuilder.build().toString();
        return code;
    }


    public String saveToFile(Object object, String name) throws IOException {
        String path = getRootPath(name) + "/app/src/main/assets/" + name + ".json";
        FileUtil.saveToFile(object, path);
        return path;
    }

    public String saveToFileWithTail(Object object, String name) throws IOException {
        String moduleRootPath = getModuleRootPath(name);
        if (StringUtil.isNotEmpty(moduleRootPath)) {
            String rootPath = moduleRootPath.substring(0, moduleRootPath.lastIndexOf(File.separator));
            String moduleName = moduleRootPath.substring(moduleRootPath.lastIndexOf(File.separator) + 1);
            String path = rootPath + "/app/src/main/assets/" + name + "_" + moduleName + ".json";
            FileUtil.saveToFile(object, path);
            return path;
        }
        return "";
    }

    public String getType(TypeMirror typeMirror) {
        return typeMirror.toString();
    }

    public String getRootPath(String name) throws IOException {
        try {
            FileObject fileObject = null;
            try {
                fileObject = filer.getResource(StandardLocation.SOURCE_PATH, "", name);
            } catch (IOException e) {
                fileObject = processingEnv.getFiler().createSourceFile(name);
            }

            String path = fileObject.getName();
            fileObject.delete();
            String temp = File.separator + "build" + File.separator + "generated";
            System.out.println(temp);
            String rootPath = path.substring(0, path.indexOf(temp));
            rootPath = rootPath.substring(0, rootPath.lastIndexOf(File.separator));
            return rootPath;
        } catch (Exception e) {
            return "";
        }

    }

    public String getModuleRootPath(String name) throws IOException {
        try {
            FileObject fileObject = processingEnv.getFiler().createSourceFile(name);
            String path = fileObject.getName();
            fileObject.delete();
            String temp = File.separator + "build" + File.separator + "generated";
            System.out.println(temp);
            String rootPath = path.substring(0, path.indexOf(temp));
            return rootPath;
        } catch (Exception e) {
            return "";
        }
    }

    protected void log(String msg) {
        if (processingEnv.getOptions().containsKey("debug")) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, msg);
        }
    }

    protected void error(String msg, Element element, AnnotationMirror annotation) {
        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, msg, element, annotation);
    }

    protected void fatalError(String msg) {
        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "FATAL ERROR: " + msg);
    }
}
