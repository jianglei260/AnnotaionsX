package cn.scewin.ormplus;

import java.util.List;

public class EntitiyInfo {
    private String packageName;
    private String className;

    private String tableName;

    private List<EntityColumnInfo> columnInfos;

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public List<EntityColumnInfo> getColumnInfos() {
        return columnInfos;
    }

    public void setColumnInfos(List<EntityColumnInfo> columnInfos) {
        this.columnInfos = columnInfos;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

}