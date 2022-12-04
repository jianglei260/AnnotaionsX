package cn.scewin.annotionsx.common;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;

import java.util.ArrayList;
import java.util.List;

public class TypeItem {

    public TypeName typeName;
    public ClassName className;
    public List<TypeName> typeNames = new ArrayList<>();

    public TypeName getTypeName() {
        return typeName;
    }

    public void setTypeName(TypeName typeName) {
        this.typeName = typeName;
    }

    public ClassName getClassName() {
        return className;
    }

    public void setClassName(ClassName className) {
        this.className = className;
    }

    public List<TypeName> getTypeNames() {
        return typeNames;
    }

    public void setTypeNames(List<TypeName> typeNames) {
        this.typeNames = typeNames;
    }
}
