package cn.scewin.ormplus;

public class ApiParameter {
    private String type;
    private String name;
    private String methodParamName;
    private boolean isPathVariable;
    private boolean isMuiltPart;
    private boolean isRequestBody;
    private String pathVariableName;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getMethodParamName() {
        return methodParamName;
    }

    public void setMethodParamName(String methodParamName) {
        this.methodParamName = methodParamName;
    }

    public boolean isPathVariable() {
        return isPathVariable;
    }

    public void setPathVariable(boolean pathVariable) {
        isPathVariable = pathVariable;
    }

    public boolean isMuiltPart() {
        return isMuiltPart;
    }

    public void setMuiltPart(boolean muiltPart) {
        isMuiltPart = muiltPart;
    }

    public String getPathVariableName() {
        return pathVariableName;
    }

    public void setPathVariableName(String pathVariableName) {
        this.pathVariableName = pathVariableName;
    }

    public boolean isRequestBody() {
        return isRequestBody;
    }

    public void setRequestBody(boolean requestBody) {
        isRequestBody = requestBody;
    }
}
