package cn.scewin.ormplus;

import java.util.List;

public class DaoMethodInfo {
    private String methodName;

    private String returnType;

    private String operation;

    private String sql;

    private List<DaoParameterInfo> parameterInfos;

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public String getReturnType() {
        return returnType;
    }

    public void setReturnType(String returnType) {
        this.returnType = returnType;
    }

    public String getOperation() {
        return operation;
    }

    public void setOperation(String operation) {
        this.operation = operation;
    }

    public String getSql() {
        return sql;
    }

    public void setSql(String sql) {
        this.sql = sql;
    }

    public List<DaoParameterInfo> getParameterInfos() {
        return parameterInfos;
    }

    public void setParameterInfos(List<DaoParameterInfo> parameterInfos) {
        this.parameterInfos = parameterInfos;
    }
}
