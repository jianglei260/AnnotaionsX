package cn.scewin.meta.compiler;

import com.google.auto.service.AutoService;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.sun.tools.javac.code.Attribute;
import com.sun.tools.javac.code.Type;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.MirroredTypesException;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;

import cn.scewin.annotionsx.common.utils.StringUtil;
import cn.scewin.common.compiler.BaseAnnotationProcessor;
import cn.scewin.meta.MetaAnnotation;
import cn.scewin.meta.MetaAnnotationManager;

@SupportedAnnotationTypes("*")
public class MetaAnnotationProcessor2 extends BaseAnnotationProcessor {
    private Types types;
    private Messager messager;
    private Filer filer;
    private Elements elementsUtils;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        types = processingEnv.getTypeUtils();
        messager = processingEnv.getMessager();
        filer = processingEnv.getFiler();
        elementsUtils = processingEnv.getElementUtils();
    }

    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {
        log("MetaAnnotationProcessor Start................");
        Set<? extends Element> elements = roundEnvironment.getElementsAnnotatedWith(MetaAnnotation.class);
        Map<MetaAnnotation, List<TypeElement>> fieldAnnotationMap = new HashMap<>();
        Map<MetaAnnotation, List<TypeElement>> methodAnnotationMap = new HashMap<>();
        Map<MetaAnnotation, Element> typeAnnotations = new HashMap<>();
        List<TypeElement> methodAnnotationElements = new ArrayList<>();
        List<TypeElement> filedAnnotationElements = new ArrayList<>();
        for (Element element : elements) {
            MetaAnnotation metaAnnotation = element.getAnnotation(MetaAnnotation.class);
            Target target = element.getAnnotation(Target.class);
            ElementType[] targetTypes = target.value();

            if (metaAnnotation.buildEntityClass()) {
                TypeSpec typeSpec = buildEntityClass(metaAnnotation, (TypeElement) element);
                String packageName = elementsUtils.getPackageOf(element).getQualifiedName().toString();
                buildCode(packageName, typeSpec);
                if (targetTypes[0].equals(ElementType.TYPE)) {
                    typeAnnotations.put(metaAnnotation, element);
                }
                if (targetTypes[0].equals(ElementType.FIELD)) {
                    filedAnnotationElements.add((TypeElement) element);
                    if (fieldAnnotationMap.containsKey(metaAnnotation)) {
                        fieldAnnotationMap.get(metaAnnotation).add((TypeElement) element);
                    } else {
                        List<TypeElement> list = new ArrayList<>();
                        list.add((TypeElement) element);
                        fieldAnnotationMap.put(metaAnnotation, list);
                    }
                }
                if (targetTypes[0].equals(ElementType.METHOD)) {
                    methodAnnotationElements.add((TypeElement) element);
                    if (methodAnnotationMap.containsKey(metaAnnotation)) {
                        methodAnnotationMap.get(metaAnnotation).add((TypeElement) element);
                    } else {
                        List<TypeElement> list = new ArrayList<>();
                        list.add((TypeElement) element);
                        methodAnnotationMap.put(metaAnnotation, list);
                    }
                }
            }
        }
        messager.printMessage(Diagnostic.Kind.NOTE, "metaAnnotation count:" + typeAnnotations.size());
        for (MetaAnnotation metaAnnotation : typeAnnotations.keySet()) {
            if (metaAnnotation.buildConstants()) {
                Element element = typeAnnotations.get(metaAnnotation);
                TypeSpec.Builder entityBuilder = TypeSpec.classBuilder(element.getSimpleName().toString() + "AnnotationInfos");
                entityBuilder.addModifiers(Modifier.PUBLIC);

                TypeSpec.Builder annotationsBuilder = TypeSpec.classBuilder(element.getSimpleName().toString() + "AnnotationClasses");
                annotationsBuilder.addModifiers(Modifier.PUBLIC);

                CodeBlock.Builder staticBuilder = CodeBlock.builder();
//                annotationsBuilder.addStaticBlock()

                MethodSpec.Builder initBuilder = MethodSpec.methodBuilder("init" + element.getSimpleName().toString());
                initBuilder.addModifiers(Modifier.PUBLIC);
                ClassName returnType = ClassName.bestGuess(((TypeElement) element).getQualifiedName().toString() + metaAnnotation.entitySuffix());
                initBuilder.returns(ParameterizedTypeName.get(ClassName.get(List.class), returnType));

                String typeName = element.getSimpleName().toString() + "Annotations";
                String packageName = elementsUtils.getPackageOf(element).getQualifiedName().toString();
                TypeSpec.Builder constantsBuilder = TypeSpec.classBuilder(typeName);
                constantsBuilder.addModifiers(Modifier.PUBLIC);
                Set<? extends Element> annotaionedElements = roundEnvironment.getElementsAnnotatedWith((TypeElement) element);
                String listName = StringUtil.lowerFirstChar(element.getSimpleName().toString()) + "s";
                initBuilder.addStatement("$T $N=new $T()", ParameterizedTypeName.get(ClassName.get(List.class), returnType), listName, ClassName.get(ArrayList.class));
                for (Element annotaionedElement : annotaionedElements) {
                    staticBuilder.addStatement("$T.getInstance().registe($T.class,$T.class)", ClassName.get(MetaAnnotationManager.class), TypeName.get(element.asType()), TypeName.get(annotaionedElement.asType()));
                    String returnName = StringUtil.lowerFirstChar(annotaionedElement.getSimpleName().toString());
                    initBuilder.addStatement("$T $N=new $T()", returnType, returnName, returnType);
                    inflateEntityValues(getElementAnnotationMirror(annotaionedElement, (TypeElement) element).getElementValues(), initBuilder, returnName);
                    initBuilder.addStatement("$N.add($N)", listName, returnName);
                    String innerClassBuilderName = annotaionedElement.getSimpleName().toString();
                    TypeSpec.Builder innerClassBuilder = TypeSpec.classBuilder(innerClassBuilderName);
                    innerClassBuilder.addModifiers(Modifier.PUBLIC, Modifier.STATIC);
                    String currentPath = typeName + "." + innerClassBuilderName;
                    for (TypeElement methodAnnotationElement : methodAnnotationElements) {
                        ClassName enclosedReturnType = ClassName.bestGuess((methodAnnotationElement).getQualifiedName().toString() + metaAnnotation.entitySuffix());
                        String enclosedListName = returnName + methodAnnotationElement.getSimpleName().toString() + "s";
                        initBuilder.addStatement("$T $N=new $T()", ParameterizedTypeName.get(ClassName.get(List.class), enclosedReturnType), enclosedListName, ClassName.get(ArrayList.class));
                        initBuilder.addStatement("$N.$L($N)", returnName, StringUtil.setMethodName(StringUtil.lowerFirstChar(methodAnnotationElement.getSimpleName().toString()) + "s"), enclosedListName);
                    }
                    for (TypeElement filedAnnotationElement : filedAnnotationElements) {
                        ClassName enclosedReturnType = ClassName.bestGuess((filedAnnotationElement).getQualifiedName().toString() + metaAnnotation.entitySuffix());
                        String enclosedListName = returnName + filedAnnotationElement.getSimpleName().toString() + "s";
                        initBuilder.addStatement("$T $N=new $T()", ParameterizedTypeName.get(ClassName.get(List.class), enclosedReturnType), enclosedListName, ClassName.get(ArrayList.class));
                        initBuilder.addStatement("$N.$L($N)", returnName, StringUtil.setMethodName(StringUtil.lowerFirstChar(filedAnnotationElement.getSimpleName().toString()) + "s"), enclosedListName);
                    }
                    for (Element enclosedElement : annotaionedElement.getEnclosedElements()) {
                        if (enclosedElement instanceof ExecutableElement) {
                            getAnnotation(methodAnnotationElements, enclosedElement, innerClassBuilder, currentPath);
                            inflateEnclosedAnnotation(methodAnnotationElements, enclosedElement, initBuilder, innerClassBuilderName, returnName);
                        } else if (enclosedElement instanceof VariableElement) {
                            getAnnotation(filedAnnotationElements, enclosedElement, innerClassBuilder, currentPath);
                            inflateEnclosedAnnotation(filedAnnotationElements, enclosedElement, initBuilder, innerClassBuilderName, returnName);
                        }
                    }
                    constantsBuilder.addType(innerClassBuilder.build());
                }
                initBuilder.addStatement("return $N", listName);
                entityBuilder.addMethod(initBuilder.build());
                annotationsBuilder.addStaticBlock(staticBuilder.build());
                buildCode(packageName, constantsBuilder);
                buildCode(packageName, entityBuilder);
                buildCode(packageName, annotationsBuilder);
            }
        }
        return false;
    }

    public AnnotationMirror getElementAnnotationMirror(Element element, TypeElement annotationElement) {
        for (AnnotationMirror annotationMirror : element.getAnnotationMirrors()) {
            if (annotationMirror.getAnnotationType().asElement().equals(annotationElement)) {
                return annotationMirror;
            }
        }
        return null;
    }

    public void inflateAnnotation(MetaAnnotation metaAnnotation, List<TypeElement> annotationTypeElements, Element typeElement, MethodSpec.Builder methodBuilder, String returnName, String varName) {
        List<? extends AnnotationMirror> annotationMirrors = typeElement.getAnnotationMirrors();
        for (AnnotationMirror annotationMirror : annotationMirrors) {
            if (annotationTypeElements.contains(annotationMirror.getAnnotationType().asElement())) {
                ClassName returnType = ClassName.bestGuess(((TypeElement) annotationMirror.getAnnotationType().asElement()).getQualifiedName().toString() + metaAnnotation.entitySuffix());
                methodBuilder.addStatement("$T $N=new $T()", returnType, returnName, returnType);
                Map<? extends ExecutableElement, ? extends AnnotationValue> elementValues = annotationMirror.getElementValues();
                inflateEntityValues(elementValues, methodBuilder, returnName);
                methodBuilder.addStatement("$N.add($N)", varName, returnName);
            }
        }
    }

    public void inflateEnclosedAnnotation(List<TypeElement> annotationTypeElements, Element enclosedElement, MethodSpec.Builder methodBuilder, String parentName, String varName) {
        List<? extends AnnotationMirror> annotationMirrors = enclosedElement.getAnnotationMirrors();
        for (AnnotationMirror annotationMirror : annotationMirrors) {
            if (annotationTypeElements.contains(annotationMirror.getAnnotationType().asElement())) {
                TypeElement annotationTypeElement = (TypeElement) annotationMirror.getAnnotationType().asElement();
                MetaAnnotation metaAnnotation = annotationTypeElement.getAnnotation(MetaAnnotation.class);
                ClassName returnType = ClassName.bestGuess(annotationTypeElement.getQualifiedName().toString() + metaAnnotation.entitySuffix());
                String currentVarName = StringUtil.lowerFirstChar(parentName) + StringUtil.upcaseFirstChar(enclosedElement.getSimpleName().toString());
                methodBuilder.addStatement("$T $N=new $T()", returnType, currentVarName, returnType);
                Map<? extends ExecutableElement, ? extends AnnotationValue> elementValues = annotationMirror.getElementValues();
                inflateEntityValues(elementValues, methodBuilder, currentVarName);
                String enclosedListName = varName + annotationTypeElement.getSimpleName().toString() + "s";
                methodBuilder.addStatement("$N.add($N)", enclosedListName, currentVarName);
            }
        }
    }

    public void inflateEntityValues(Map<? extends ExecutableElement, ? extends AnnotationValue> elementValues, MethodSpec.Builder methodBuilder, String varName) {
        for (ExecutableElement executableElement : elementValues.keySet()) {
            String callMethodName = StringUtil.setMethodName(executableElement.getSimpleName().toString());
            if (elementValues.get(executableElement) instanceof AnnotationMirror) {
                AnnotationMirror annotationMirror = (AnnotationMirror) elementValues.get(executableElement).getValue();
                String currentVarName = varName + executableElement.getSimpleName().toString();
                TypeElement annotationTypeElement = (TypeElement) annotationMirror.getAnnotationType().asElement();
                MetaAnnotation metaAnnotation = annotationTypeElement.getAnnotation(MetaAnnotation.class);
                String typeName = annotationTypeElement.getQualifiedName().toString() + metaAnnotation.entitySuffix();
                methodBuilder.addStatement("$T $N=new $T()", ClassName.bestGuess(typeName), currentVarName, ClassName.bestGuess(typeName));
                inflateEntityValues(annotationMirror.getElementValues(), methodBuilder, currentVarName);
                methodBuilder.addStatement("$N.$L($N)", varName, callMethodName, currentVarName);
            } else {
                Object value = elementValues.get(executableElement).getValue();
//                messager.printMessage(Diagnostic.Kind.NOTE,"------------find class type:"+value.getClass().getName());
                if (executableElement.getReturnType() instanceof ArrayType) {
                    List<Attribute> attributes = (List<Attribute>) value;
                    List values = new ArrayList();
                    StringBuilder codeBuilder = new StringBuilder();
                    for (int i = 0; i < attributes.size(); i++) {
                        Object obj = attributes.get(i).getValue();
                        values.add(obj);
                        codeBuilder.append(getVarType(obj));
                        if (i < attributes.size() - 1) {
                            codeBuilder.append(",");
                        }
                    }
                    values.add(0, varName);
                    values.add(1, callMethodName);
                    values.add(2, ClassName.get(Arrays.class));
                    methodBuilder.addStatement("$N.$L($T.asList(" + codeBuilder + "))", values.toArray());
                } else {
                    if (value instanceof Type.ClassType) {
                        value = ((Type.ClassType) value).toString();
                    }
                    methodBuilder.addStatement("$N.$L(" + getVarType(value) + ")", varName, callMethodName, value);
                }
            }
        }
    }


    public void getAnnotation(List<TypeElement> annotationTypeElements, Element enclosedElement, TypeSpec.Builder typeBuilder, String currentPath) {
        List<? extends AnnotationMirror> annotationMirrors = enclosedElement.getAnnotationMirrors();
        for (AnnotationMirror annotationMirror : annotationMirrors) {
            messager.printMessage(Diagnostic.Kind.NOTE, "getAnnotation:" + annotationTypeElements.size() + " -- " + annotationMirror.toString());
            if (annotationTypeElements.contains(annotationMirror.getAnnotationType().asElement())) {
                String enclosedBuilderName = enclosedElement.getSimpleName().toString();
                TypeSpec.Builder enclosedBuilder = TypeSpec.classBuilder(enclosedBuilderName);
                enclosedBuilder.addModifiers(Modifier.PUBLIC, Modifier.STATIC);

                String innerBuilderName = annotationMirror.getAnnotationType().asElement().getSimpleName().toString();
                TypeSpec.Builder innerBuilder = TypeSpec.classBuilder(innerBuilderName);
                innerBuilder.addModifiers(Modifier.PUBLIC, Modifier.STATIC);
                Map<? extends ExecutableElement, ? extends AnnotationValue> elementValues = annotationMirror.getElementValues();
                String myCurrentPath = currentPath + "." + enclosedBuilderName + "." + innerBuilderName;
                inflateElementValues(elementValues, innerBuilder, myCurrentPath);
                enclosedBuilder.addType(innerBuilder.build());
                typeBuilder.addType(enclosedBuilder.build());
            }
        }
    }

    public void inflateElementValues(Map<? extends ExecutableElement, ? extends AnnotationValue> elementValues, TypeSpec.Builder typeBuilder, String currentPath) {
        for (ExecutableElement executableElement : elementValues.keySet()) {
            if (elementValues.get(executableElement) instanceof AnnotationMirror) {
                String innerBuilderName = executableElement.getSimpleName().toString();
                TypeSpec.Builder innerBuilder = TypeSpec.classBuilder(innerBuilderName);
                innerBuilder.addModifiers(Modifier.PUBLIC, Modifier.STATIC);
                AnnotationMirror annotationMirror = (AnnotationMirror) elementValues.get(executableElement).getValue();
                String myCurrentPath = currentPath + "." + innerBuilderName;
                inflateElementValues(annotationMirror.getElementValues(), innerBuilder, myCurrentPath);
                typeBuilder.addType(innerBuilder.build());
            } else {
                String innerBuilderName = executableElement.getSimpleName().toString();
                TypeSpec.Builder innerBuilder = TypeSpec.classBuilder(innerBuilderName);
                innerBuilder.addModifiers(Modifier.PUBLIC, Modifier.STATIC);
                String myCurrentPath = currentPath + "." + innerBuilderName;
                Object value = elementValues.get(executableElement).getValue();
                inflateValue(executableElement, innerBuilder, myCurrentPath, value);
                typeBuilder.addType(innerBuilder.build());
            }
        }
    }

    public void inflateValue(ExecutableElement executableElement, TypeSpec.Builder innerBuilder, String currentPath, Object value) {
        FieldSpec.Builder fieldBuilder = FieldSpec.builder(TypeName.get(executableElement.getReturnType()), "VALUE", Modifier.PUBLIC, Modifier.STATIC);
        if (executableElement.getReturnType() instanceof ArrayType) {
            List<Attribute> attributes = (List<Attribute>) value;
            List values = new ArrayList();
            StringBuilder codeBuilder = new StringBuilder();
            for (int i = 0; i < attributes.size(); i++) {
                Object obj = attributes.get(i).getValue();
                values.add(obj);
                codeBuilder.append(getVarType(obj));
                if (i < attributes.size() - 1) {
                    codeBuilder.append(",");
                }
            }
            values.add(0, ClassName.get(values.get(0).getClass()));
            fieldBuilder.initializer("new $T[]{" + codeBuilder + "}", values.toArray());
        } else {
            fieldBuilder.initializer(getVarType(value), value);
        }
        innerBuilder.addField(fieldBuilder.build());
        FieldSpec.Builder fieldRefBuilder = FieldSpec.builder(TypeName.get(String.class), "REF", Modifier.PUBLIC, Modifier.STATIC);
        fieldRefBuilder.initializer("$S", currentPath);
        innerBuilder.addField(fieldRefBuilder.build());
    }

    public String getVarType(Object value) {
        if (value instanceof String) {
            return "$S";
        } else if (value instanceof Boolean) {
            return "$L";
        }
        return "$L";
    }

    public TypeSpec buildEntityClass(MetaAnnotation metaAnnotation, TypeElement element) {
        String typeName = element.getSimpleName().toString() + metaAnnotation.entitySuffix();
        messager.printMessage(Diagnostic.Kind.NOTE, "handle : " + typeName);
        TypeSpec.Builder typeBuilder = TypeSpec.classBuilder(typeName);
        typeBuilder.addModifiers(Modifier.PUBLIC);
        List<? extends TypeMirror> childrenMirrors = new ArrayList<>();
        try {
            metaAnnotation.children();
        } catch (MirroredTypesException e) {
            childrenMirrors = e.getTypeMirrors();
        }
        List<? extends Element> elements = element.getEnclosedElements();
        for (Element enclosedElement : elements) {
            if (enclosedElement instanceof ExecutableElement) {
                ExecutableElement executableElement = (ExecutableElement) enclosedElement;
                String name = executableElement.getSimpleName().toString();
                TypeMirror returnTypeMirror = executableElement.getReturnType();
                buildChildAnnotationType(returnTypeMirror, typeBuilder, name);
            }
        }
        for (TypeMirror childrenMirror : childrenMirrors) {
            buildChildAnnotationType(childrenMirror, typeBuilder, "");
        }
        return typeBuilder.build();
    }

    public void buildChildAnnotationType(TypeMirror childrenMirror, TypeSpec.Builder typeBuilder, String name) {
//        String name = "";
        TypeName returnTypeName = null;
        if (childrenMirror instanceof ArrayType) {
            String fullClassName = ((ArrayType) childrenMirror).getComponentType().toString();
            TypeElement childElement = elementsUtils.getTypeElement(fullClassName);
            if (childElement != null) {
                MetaAnnotation childMetaAnnotation = childElement.getAnnotation(MetaAnnotation.class);
                String simpleName = StringUtil.getClassSimpleName(fullClassName);
                if (StringUtil.isEmpty(name)) {
                    name = StringUtil.lowerFirstChar(simpleName) + "s";
                }
                String returnTypeClassName = "";
                if (childMetaAnnotation != null) {
                    returnTypeClassName = childElement.getQualifiedName().toString() + childMetaAnnotation.entitySuffix();
                } else {
                    returnTypeClassName = childElement.getQualifiedName().toString();
                }
                returnTypeName = ParameterizedTypeName.get(ClassName.get(List.class), ClassName.bestGuess(returnTypeClassName));
            } else {
                returnTypeName = ParameterizedTypeName.get(ClassName.get(List.class), TypeName.get(((ArrayType) childrenMirror).getComponentType()));
            }
        } else {
            String fullClassName = childrenMirror.toString();
            TypeElement childElement = elementsUtils.getTypeElement(fullClassName);
            if (childElement != null) {
                MetaAnnotation childMetaAnnotation = childElement.getAnnotation(MetaAnnotation.class);
                if (StringUtil.isEmpty(name)) {
                    name = StringUtil.lowerFirstChar(StringUtil.getClassSimpleName(fullClassName));
                }
                String returnTypeClassName = "";
                if (childMetaAnnotation != null) {
                    returnTypeClassName = childElement.getQualifiedName().toString() + childMetaAnnotation.entitySuffix();
                } else {
                    returnTypeClassName = childElement.getQualifiedName().toString();
                }
                returnTypeName = ClassName.bestGuess(returnTypeClassName);
            } else {
                returnTypeName = TypeName.get(childrenMirror);
            }
        }
        if (returnTypeName.equals(TypeName.get(Class.class))) {
            returnTypeName = TypeName.get(String.class);
        }
        FieldSpec.Builder fieldBuilder = FieldSpec.builder(returnTypeName, name, Modifier.PRIVATE);
        typeBuilder.addField(fieldBuilder.build());
        MethodSpec.Builder setMethodBuilder = MethodSpec.methodBuilder(StringUtil.setMethodName(name));
        setMethodBuilder.addParameter(ParameterSpec.builder(returnTypeName, name).build());
        setMethodBuilder.addStatement("this.$L=$L", name, name);
        setMethodBuilder.addModifiers(Modifier.PUBLIC);
        typeBuilder.addMethod(setMethodBuilder.build());

        MethodSpec.Builder getMethodBuilder = MethodSpec.methodBuilder(StringUtil.getMethodName(name, TypeName.BOOLEAN.equals(returnTypeName)));
        getMethodBuilder.addStatement("return this.$L", name);
        getMethodBuilder.returns(returnTypeName);
        getMethodBuilder.addModifiers(Modifier.PUBLIC);
        typeBuilder.addMethod(getMethodBuilder.build());
    }
}