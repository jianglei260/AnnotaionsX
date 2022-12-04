package cn.scewin.ormplus;

public class EntityColumnInfo {
    private String fieldType;
    private String fieldName;
    private String columnName;
    private boolean primaryKey;
    private boolean autoGenerate;
    private boolean ignore;


    public String getFieldType() {
        return fieldType;
    }

    public void setFieldType(String fieldType) {
        this.fieldType = fieldType;
    }

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public String getColumnName() {
        return columnName;
    }

    public void setColumnName(String columnName) {
        this.columnName = columnName;
    }

    public boolean isPrimaryKey() {
        return primaryKey;
    }

    public void setPrimaryKey(boolean primaryKey) {
        this.primaryKey = primaryKey;
    }

    public boolean isAutoGenerate() {
        return autoGenerate;
    }

    public void setAutoGenerate(boolean autoGenerate) {
        this.autoGenerate = autoGenerate;
    }

    public boolean isIgnore() {
        return ignore;
    }

    public void setIgnore(boolean ignore) {
        this.ignore = ignore;
    }

    @Override
    public String toString() {
        return "EntityColumnInfo{" +
                "fieldType='" + fieldType + '\'' +
                ", fieldName='" + fieldName + '\'' +
                ", columnName='" + columnName + '\'' +
                ", primaryKey=" + primaryKey +
                ", autoGenerate=" + autoGenerate +
                ", ignore=" + ignore +
                '}';
    }
}
