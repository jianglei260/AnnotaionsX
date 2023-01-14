package cn.scewin.bm.compiler;

import com.google.auto.service.AutoService;
import com.squareup.javapoet.ArrayTypeName;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import java.util.HashSet;
import java.util.Set;

import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;

import cn.scewin.annotionsx.common.utils.StringUtil;
import cn.scewin.bm.BroadcastReceiver;
import cn.scewin.bm.ReceiveMethod;
import cn.scewin.common.compiler.BaseAnnotationProcessor;

@AutoService(Processor.class)
public class BroadcastProcessor extends BaseAnnotationProcessor {
    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        Set<? extends Element> entityElements = roundEnv.getElementsAnnotatedWith(BroadcastReceiver.class);
        if (entityElements != null && entityElements.size() > 0) {
            Set<String> packageNameSet = new HashSet<>();
            String className = "BroadcastManageServiceHelper";
            TypeSpec.Builder typeSpecBuilder = TypeSpec.classBuilder(className).addModifiers(Modifier.PUBLIC);
            MethodSpec.Builder initMethod = MethodSpec.methodBuilder("init").addModifiers(Modifier.PUBLIC, Modifier.STATIC);
            for (Element element : entityElements) {
                packageNameSet.add(element.asType().toString());
                for (Element childelment : element.getEnclosedElements()) {
                    if ((childelment instanceof ExecutableElement)) {
                        ReceiveMethod receiveMethod = childelment.getAnnotation(ReceiveMethod.class);
                        if (receiveMethod != null) {
                            TypeSpec receiver = buildReceiverClass(element, (ExecutableElement) childelment, receiveMethod).build();
                            String receiverPackage = elementsUtils.getPackageOf(element).getQualifiedName().toString();
                            buildCode(receiverPackage, receiver);
                            initMethod.addStatement("$T.getInstance().registeReceiver($T.class)", ClassName.bestGuess("cn.scewin.bm.android.BroadcastManageService"), ClassName.bestGuess(receiverPackage + "." + receiver.name));
                        }
                    }
                }
            }
            typeSpecBuilder.addMethod(initMethod.build());
            String packageName = getCommonPackage(packageNameSet);
            buildCode(packageName, typeSpecBuilder);
        }
        return false;
    }


    public TypeSpec.Builder buildReceiverClass(Element typeElment, ExecutableElement executableElement, ReceiveMethod receiveMethod) {
        String action = receiveMethod.value();
        String simpleName = StringUtil.upcaseFirstChar(StringUtil.convertName(action)) + "BroadcastReceiver";
        TypeSpec.Builder typeSpecBuilder = TypeSpec.classBuilder(simpleName).addModifiers(Modifier.PUBLIC);
        typeSpecBuilder.superclass(ParameterizedTypeName.get(ClassName.bestGuess("cn.scewin.bm.android.BaseBroadcastReceiver"), TypeName.get(typeElment.asType())));

        MethodSpec.Builder invokeMethodBuilder = MethodSpec.methodBuilder("invoke");
        invokeMethodBuilder.addParameter(ParameterSpec.builder(ClassName.bestGuess("android.content.Intent"), "intent").build());
        invokeMethodBuilder.addParameter(ParameterSpec.builder(TypeName.get(typeElment.asType()), "t").build());
        invokeMethodBuilder.addModifiers(Modifier.PUBLIC);

        MethodSpec.Builder sendMethodBuilder = MethodSpec.methodBuilder("sendBroadcasr");
        sendMethodBuilder.addModifiers(Modifier.PUBLIC, Modifier.STATIC);
        sendMethodBuilder.addParameter(ParameterSpec.builder(ClassName.bestGuess("android.content.Context"), "context").build());

        StringBuilder sb = new StringBuilder();
        ClassName intentClassName = ClassName.bestGuess("android.content.Intent");
        sendMethodBuilder.addStatement("$T intent=new $T($S)", intentClassName, intentClassName, action);
        if (executableElement.getParameters().size() > 0) {
            int size = executableElement.getParameters().size();
            for (int i = 0; i < size; i++) {
                VariableElement parameter = executableElement.getParameters().get(i);
                TypeName typeName = TypeName.get(parameter.asType());
                String name = parameter.getSimpleName().toString();
                sb.append(name);
                if (i < size - 1) {
                    sb.append(",");
                }
                sendMethodBuilder.addParameter(ParameterSpec.builder(typeName, name).build());
                TypeElement paramElement = elementsUtils.getTypeElement(parameter.asType().toString());
                boolean isParcelable = false;
                for (TypeMirror anInterface : paramElement.getInterfaces()) {
                    if (anInterface.toString().equals("android.os.Parcelable")) {
                        isParcelable = true;
                        break;
                    }
                }

                if (typeName.isPrimitive() || typeName.isBoxedPrimitive() || typeName.equals(ClassName.get(String.class))) {
                    sendMethodBuilder.addStatement("intent.putExtra($S,$N)", name, name);
                    if (typeName.equals(TypeName.INT) || typeName.equals(TypeName.INT.box())) {
                        invokeMethodBuilder.addStatement("$T $N=intent.getIntExtra($S,0)", typeName, name, name);
                    } else if (typeName.equals(TypeName.LONG) || typeName.equals(TypeName.LONG.box())) {
                        invokeMethodBuilder.addStatement("$T $N=intent.getLongExtra($S,0l)", typeName, name, name);
                    } else if (typeName.equals(TypeName.FLOAT) || typeName.equals(TypeName.FLOAT.box())) {
                        invokeMethodBuilder.addStatement("$T $N=intent.getFloatExtra($S,0f)", typeName, name, name);
                    } else if (typeName.equals(TypeName.DOUBLE) || typeName.equals(TypeName.DOUBLE.box())) {
                        invokeMethodBuilder.addStatement("$T $N=intent.getDoubleExtra($S,0d)", typeName, name, name);
                    } else if (typeName.equals(TypeName.BOOLEAN) || typeName.equals(TypeName.BOOLEAN.box())) {
                        invokeMethodBuilder.addStatement("$T $N=intent.getBooleanExtra($S,0l)", typeName, name, name);
                    } else {
                        invokeMethodBuilder.addStatement("$T $N=intent.getStringExtra($S)", typeName, name, name);
                    }
                } else if (typeName instanceof ArrayTypeName) {
                    sendMethodBuilder.addStatement("intent.putExtra($S,$N)", name, name);
                    ArrayTypeName arrayTypeName = (ArrayTypeName) typeName;
                    if (arrayTypeName.componentType.equals(TypeName.INT) || arrayTypeName.componentType.equals(TypeName.INT.box())) {
                        invokeMethodBuilder.addStatement("$T $N=intent.getIntArrayExtra($S)", typeName, name, name);
                    } else if (arrayTypeName.componentType.equals(TypeName.LONG) || arrayTypeName.componentType.equals(TypeName.LONG.box())) {
                        invokeMethodBuilder.addStatement("$T $N=intent.getLongArrayExtra($S)", typeName, name, name);
                    } else if (arrayTypeName.componentType.equals(TypeName.FLOAT) || arrayTypeName.componentType.equals(TypeName.FLOAT.box())) {
                        invokeMethodBuilder.addStatement("$T $N=intent.getFloatArrayExtra($S)", typeName, name, name);
                    } else if (arrayTypeName.componentType.equals(TypeName.DOUBLE) || arrayTypeName.componentType.equals(TypeName.DOUBLE.box())) {
                        invokeMethodBuilder.addStatement("$T $N=intent.getDoubleArrayExtra($S)", typeName, name, name);
                    } else if (arrayTypeName.componentType.equals(TypeName.BOOLEAN) || arrayTypeName.componentType.equals(TypeName.BOOLEAN.box())) {
                        invokeMethodBuilder.addStatement("$T $N=intent.getBooleanArrayExtra($S)", typeName, name, name);
                    } else {
                        invokeMethodBuilder.addStatement("$T $N=intent.getStringArrayExtra($S)", typeName, name, name);
                    }
                } else if (isParcelable) {
                    sendMethodBuilder.addStatement("intent.putExtra($S,$N)", name, name);
                    invokeMethodBuilder.addStatement("$T $N=intent.getParcelableExtra($S)", typeName, name, name);
                } else {
                    sendMethodBuilder.addStatement("intent.putExtra($S,gson.toJson($N))", name, name);
                    invokeMethodBuilder.addStatement("$T $N=gson.fromJson(intent.getStringExtra($S),$T.class)", typeName, name, name, typeName);
                }
            }
        }
        sendMethodBuilder.addStatement("context.sendBroadcast(intent)");
        typeSpecBuilder.addMethod(sendMethodBuilder.build());
        invokeMethodBuilder.addStatement("$N.$L(" + sb + ")", "t", executableElement.getSimpleName().toString());
        typeSpecBuilder.addMethod(invokeMethodBuilder.build());

        FieldSpec.Builder fieldBuilder = FieldSpec.builder(String.class, "FILTER", Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL);
        fieldBuilder.initializer("$S", action);
        typeSpecBuilder.addField(fieldBuilder.build());

        MethodSpec.Builder getFilterMethodBuilder = MethodSpec.methodBuilder("getFilter");
        getFilterMethodBuilder.addModifiers(Modifier.PUBLIC);
        getFilterMethodBuilder.returns(String.class);
        getFilterMethodBuilder.addStatement("return $N", "FILTER");
        typeSpecBuilder.addMethod(getFilterMethodBuilder.build());

        return typeSpecBuilder;
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        Set<String> types = new HashSet<>();
        types.add(BroadcastReceiver.class.getName());
        return types;
    }
}