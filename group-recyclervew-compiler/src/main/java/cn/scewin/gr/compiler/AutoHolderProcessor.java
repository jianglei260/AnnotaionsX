package cn.scewin.gr.compiler;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeSpec;

import java.util.HashSet;
import java.util.Set;

import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;

import cn.scewin.common.compiler.BaseAnnotationProcessor;
import cn.scewin.gr.AutoHolder;
import cn.scewin.gr.HoldValue;

public class AutoHolderProcessor extends BaseAnnotationProcessor {

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        Set<? extends Element> entityElements = roundEnv.getElementsAnnotatedWith(AutoHolder.class);
        if (entityElements != null && entityElements.size() > 0) {
            String packageName = AutoHolder.class.getPackage().getName();
            String className = "AutoHolderHelper";
            TypeSpec.Builder typeSpecBuilder = TypeSpec.classBuilder(className).addModifiers(Modifier.PUBLIC);
            MethodSpec.Builder initMethod = MethodSpec.methodBuilder("init").addModifiers(Modifier.PUBLIC, Modifier.STATIC);
            initMethod.addParameter(ParameterSpec.builder(ClassName.bestGuess("cn.scewin.gr.android.AutoViewHolder"), "holder").build());
            for (Element element : entityElements) {
                MethodSpec.Builder initElementMethod = MethodSpec.methodBuilder("init" + element.getSimpleName().toString()).addModifiers(Modifier.PUBLIC, Modifier.STATIC);
                initElementMethod.addParameter(ParameterSpec.builder(ClassName.bestGuess(element.asType().toString()), "holder").build());
                for (Element variableElement : element.getEnclosedElements()) {
                    if ((variableElement instanceof VariableElement)) {
                        HoldValue holdValue = variableElement.getAnnotation(HoldValue.class);
                        if (holdValue != null) {
                            initElementMethod.addStatement("holder.$L=holder.itemView.findViewById($L)", variableElement.getSimpleName().toString(), holdValue.resId());
                        }
                    }
                }
                initMethod.beginControlFlow("if(holder instanceof $L)", element.asType().toString());
                initMethod.addStatement("init" + element.getSimpleName().toString() + "(($L)holder)", element.asType().toString());
                initMethod.endControlFlow();
                typeSpecBuilder.addMethod(initElementMethod.build());
            }
            typeSpecBuilder.addMethod(initMethod.build());
            buildCode(packageName, typeSpecBuilder);
        }
        return false;
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        Set<String> types = new HashSet<>();
        types.add(AutoHolder.class.getName());
        return types;
    }
}