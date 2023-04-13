package cn.scewin.ormplus;

import androidx.room.ColumnInfo;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.Insert;
import androidx.room.PrimaryKey;
import androidx.room.Query;
import androidx.room.Update;

import com.google.auto.service.AutoService;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ArrayTypeName;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeVariableName;

import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.Alias;
import net.sf.jsqlparser.expression.CaseExpression;
import net.sf.jsqlparser.expression.DateValue;
import net.sf.jsqlparser.expression.DoubleValue;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.Function;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.MySQLGroupConcat;
import net.sf.jsqlparser.expression.StringValue;
import net.sf.jsqlparser.expression.TimeValue;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.create.table.ColumnDefinition;
import net.sf.jsqlparser.statement.create.table.CreateTable;
import net.sf.jsqlparser.statement.select.AllColumns;
import net.sf.jsqlparser.statement.select.AllTableColumns;
import net.sf.jsqlparser.statement.select.FromItem;
import net.sf.jsqlparser.statement.select.Join;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SelectBody;
import net.sf.jsqlparser.statement.select.SelectExpressionItem;
import net.sf.jsqlparser.statement.select.SelectItem;
import net.sf.jsqlparser.statement.select.SetOperationList;
import net.sf.jsqlparser.statement.select.SubSelect;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.MirroredTypeException;
import javax.tools.Diagnostic;

import cn.scewin.annotionsx.common.utils.StringUtil;
import cn.scewin.annotionsx.common.utils.TypeNameUtil;
import cn.scewin.common.compiler.BaseAnnotationProcessor;

@AutoService(Processor.class)
public class OrmPlusProcessor extends BaseAnnotationProcessor {
    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        List<EntitiyInfo> entitiyInfos = new ArrayList<>();
        Set<? extends Element> entityElements = roundEnv.getElementsAnnotatedWith(EntityGenerate.class);
        Set<? extends Element> queryElements = roundEnv.getElementsAnnotatedWith(QueryDao.class);
        entitiyInfos.addAll(generateByEntity(entityElements));
        List<EntitiyInfo> infos = new ArrayList<>();
        infos.addAll(entitiyInfos);
        entitiyInfos.addAll(generateByQuery(infos, queryElements));
        try {
            for (EntitiyInfo entitiyInfo : entitiyInfos) {
                TypeSpec.Builder typeSpecBuilder = buildEntityClass(entitiyInfo);
                AnnotationSpec.Builder annotationBuilder = AnnotationSpec.builder(Entity.class);
                annotationBuilder.addMember("tableName", "$S", entitiyInfo.getTableName());
                typeSpecBuilder.addAnnotation(annotationBuilder.build());
                buildCode(entitiyInfo.getPackageName(), typeSpecBuilder);
            }
            messager.printMessage(Diagnostic.Kind.NOTE, "entity num" + entitiyInfos.size());
            Set<? extends Element> elements = roundEnv.getElementsAnnotatedWith(Entity.class);
            messager.printMessage(Diagnostic.Kind.NOTE, "elements num" + elements.size());
            List<EntitiyInfo> databaseEntitis = new ArrayList<>(elements.size());
            for (Element element : elements) {
                EntitiyInfo entitiyInfo = buildEntityInfo(element);
                databaseEntitis.add(entitiyInfo);
            }
            entitiyInfos.addAll(databaseEntitis);
            saveToFileWithTail(entitiyInfos, "entity");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    private void buildEntityAndDao(RoundEnvironment roundEnv) {
        Set<? extends Element> daoElements = roundEnv.getElementsAnnotatedWith(Dao.class);
        List<DaoInfo> daoInfos = new ArrayList<>(daoElements.size());
        for (Element element : daoElements) {
            DaoInfo daoInfo = new DaoInfo();
            daoInfo.setDaoInterfaceName(element.toString());
            List<? extends Element> childElements = element.getEnclosedElements();
            List<DaoMethodInfo> methodInfos = new ArrayList<>();
            for (Element childElement : childElements) {
                if (childElement instanceof ExecutableElement) {
                    DaoMethodInfo daoMethodInfo = buildMethodInfo((ExecutableElement) childElement);
                    methodInfos.add(daoMethodInfo);
                }
            }
            daoInfo.setMethodInfos(methodInfos);
            daoInfos.add(daoInfo);
        }
        try {
            saveToFileWithTail(daoInfos, "dao");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public EntitiyInfo buildEntityInfo(Element element) {
        List<? extends Element> childElements = element.getEnclosedElements();
        EntitiyInfo entitiyInfo = new EntitiyInfo();
        entitiyInfo.setClassName(element.toString());
        Entity entity = element.getAnnotation(Entity.class);
        entitiyInfo.setTableName(entity.tableName());
        List<EntityColumnInfo> columnInfos = new ArrayList<>();
        for (Element childElement : childElements) {
            if (childElement instanceof VariableElement) {
                EntityColumnInfo entityColumnInfo = new EntityColumnInfo();
                entityColumnInfo.setFieldName(((VariableElement) childElement).getSimpleName().toString());
                String fieldType = getType(childElement.asType());
//                if (!fieldType.contains(".")&&childElement.asType().getKind().isPrimitive()){
//                    fieldType="cn.scewin.visionmaster.domain."+fieldType;
//                }
                entityColumnInfo.setFieldType(fieldType);
                entityColumnInfo.setColumnName(entityColumnInfo.getFieldName());
                for (AnnotationMirror annotationMirror : childElement.getAnnotationMirrors()) {
                    String type = annotationMirror.getAnnotationType().toString();
                    if (ColumnInfo.class.getName().equals(type)) {
                        ColumnInfo columnInfo = childElement.getAnnotation(ColumnInfo.class);
                        if (columnInfo.name() != null && !columnInfo.name().equals("") && !columnInfo.name().equals(ColumnInfo.INHERIT_FIELD_NAME)) {
                            entityColumnInfo.setColumnName(columnInfo.name());
                        }
                    }
                    if (PrimaryKey.class.getName().equals(type)) {
                        PrimaryKey primaryKey = childElement.getAnnotation(PrimaryKey.class);
                        entityColumnInfo.setPrimaryKey(true);
                        if (primaryKey.autoGenerate()) {
                            entityColumnInfo.setAutoGenerate(true);
                        }
                    }
                    if (Ignore.class.getName().equals(type)) {
                        Ignore primaryKey = childElement.getAnnotation(Ignore.class);
                        entityColumnInfo.setIgnore(true);
                    }
                }
                columnInfos.add(entityColumnInfo);
            }
        }
        entitiyInfo.setColumnInfos(columnInfos);
        return entitiyInfo;
    }

    public DaoMethodInfo buildMethodInfo(ExecutableElement methodElement) {
        DaoMethodInfo methodInfo = new DaoMethodInfo();
        methodInfo.setMethodName(methodElement.getSimpleName().toString());
        methodInfo.setReturnType(getType(methodElement.getReturnType()));
        for (AnnotationMirror annotationMirror : methodElement.getAnnotationMirrors()) {
            String type = annotationMirror.getAnnotationType().toString();
            if (Query.class.getName().equals(type) || Insert.class.getName().equals(type) || Update.class.getName().equals(type) || Delete.class.getName().equals(type)) {
                methodInfo.setOperation(type);
                if (Query.class.getName().equals(type)) {
                    Query query = methodElement.getAnnotation(Query.class);
                    methodInfo.setSql(query.value());
                }
                List<DaoParameterInfo> parameterInfos = new ArrayList<>();
                for (VariableElement parameter : methodElement.getParameters()) {
                    DaoParameterInfo parameterInfo = buildParameterInfo(parameter);
                    parameterInfos.add(parameterInfo);
                }
                methodInfo.setParameterInfos(parameterInfos);
            }
        }
        return methodInfo;
    }

    public DaoParameterInfo buildParameterInfo(VariableElement parameter) {
        DaoParameterInfo parameterInfo = new DaoParameterInfo();
        parameterInfo.setName(parameter.getSimpleName().toString());
        parameterInfo.setTypeName(getType(parameter.asType()));
        return parameterInfo;
    }

    public List<EntitiyInfo> generateByQuery(List<EntitiyInfo> dbEntitiyInfos, Set<? extends Element> queryElements) {
        List<EntitiyInfo> entitiyInfos = new ArrayList<>();
        for (Element element : queryElements) {
            QueryDao queryDao = element.getAnnotation(QueryDao.class);
            String className = element.getSimpleName().toString();
            String packageName = elementsUtils.getPackageOf(element).getQualifiedName().toString();
            String implClassName = className + "_Impl";
            TypeSpec.Builder typeSpecBuilder = TypeSpec.classBuilder(implClassName).addModifiers(Modifier.PUBLIC);
            typeSpecBuilder.addSuperinterface(TypeName.get(element.asType()));
            for (Element enclosedElement : element.getEnclosedElements()) {
                if (enclosedElement instanceof ExecutableElement) {
                    ExecutableElement executableElement = (ExecutableElement) enclosedElement;
                    QueryMethod queryMethod = executableElement.getAnnotation(QueryMethod.class);
                    if (queryMethod != null) {
                        String sql = queryMethod.value();
                        sql = sql.replaceAll("\\?\\{[A-Za-z]+[0-9]*\\}", "1");
                        if (sql != null && !sql.equals("")) {
                            EntitiyInfo entitiyInfo = new EntitiyInfo();
                            List<EntityColumnInfo> entityColumnInfos = new ArrayList<>();
                            try {
                                Statement stmt = CCJSqlParserUtil.parse(sql);
                                if (stmt instanceof Select) {
                                    Map<String, Table> tableAliasMap = new HashMap<>();
                                    Select select = (Select) stmt;
                                    SelectBody selectBody = select.getSelectBody();
                                    PlainSelect plainSelect = null;
                                    if (selectBody instanceof SetOperationList) {
                                        plainSelect = (PlainSelect) ((SetOperationList) selectBody).getSelects().get(0);
                                    } else {
                                        plainSelect = (PlainSelect) selectBody;
                                    }
                                    TableFinder tableFinder = new TableFinder();
                                    List<Table> tables = tableFinder.getTableList(select);
                                    for (Table table : tables) {
                                        if (table.getAlias() != null) {
                                            tableAliasMap.put(table.getAlias().getName(), table);
                                        } else {
                                            tableAliasMap.put(table.getName(), table);
                                        }
                                    }
                                    if (!queryMethod.className().contains(".")) {
                                        entitiyInfo.setClassName(queryMethod.packageName() + "." + queryMethod.className());
                                    } else {
                                        entitiyInfo.setClassName(queryMethod.className());
                                    }
                                    boolean contained = false;
                                    for (EntitiyInfo info : dbEntitiyInfos) {
                                        if (info.getClassName() != null && entitiyInfo != null && info.getClassName().equals(entitiyInfo.getClassName())) {
                                            contained = true;
                                            break;
                                        }
                                    }
                                    if (!contained) {
                                        handleSelect(plainSelect, dbEntitiyInfos, entityColumnInfos, tableAliasMap);
                                        entitiyInfo.setColumnInfos(entityColumnInfos);
                                        entitiyInfo.setTableName(queryMethod.entityName());
                                        entitiyInfo.setPackageName(queryMethod.packageName());
                                        entitiyInfos.add(entitiyInfo);
                                    }
                                    String methodName = executableElement.getSimpleName().toString();
                                    MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder(methodName).addModifiers(Modifier.PUBLIC);
                                    methodBuilder.returns(TypeName.get(executableElement.getReturnType()));
                                    String originSql = queryMethod.value();
                                    String querySql = originSql.replaceAll("\\?\\{[A-Za-z]+[0-9]*\\}", "?");

                                    methodBuilder.addStatement("$T sql=$S", String.class, querySql);
                                    StringBuilder paramBuilder = new StringBuilder();

                                    Map<Integer, String> paramIndexMap = new HashMap<>();
                                    for (VariableElement parameter : executableElement.getParameters()) {
                                        String name = parameter.getSimpleName().toString();
                                        methodBuilder.addParameter(ParameterSpec.builder(TypeName.get(parameter.asType()), name).build());
                                        String paramName = "?{" + name + "}";
                                        int index = 0;
                                        do {
                                            index = originSql.indexOf(paramName, index);
                                            if (index > 0) {
                                                paramIndexMap.put(index, name);
                                                index = index + paramName.length();
                                            }
                                        } while (index > 0);
                                    }
                                    for (Map.Entry<Integer, String> integerStringEntry : paramIndexMap.entrySet()) {
                                        paramBuilder.append(integerStringEntry.getValue());
                                        paramBuilder.append(",");
                                    }
                                    if (paramBuilder.length() > 0) {
                                        paramBuilder.deleteCharAt(paramBuilder.length() - 1);
                                    }
                                    methodBuilder.addTypeVariable(TypeVariableName.get("T"));
                                    methodBuilder.addStatement("return (List<T>)$T.getInstance().getAppDatabaseManager().query(sql,new $T[]{" + paramBuilder.toString() + "},$T.class)", ClassName.bestGuess("cn.scewin.visionmaster.App"), TypeName.get(Object.class), ClassName.bestGuess(entitiyInfo.getClassName()));
                                    typeSpecBuilder.addMethod(methodBuilder.build());
                                }
                            } catch (JSQLParserException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }
            buildCode(packageName, typeSpecBuilder);
        }
        return entitiyInfos;
    }

    public void handleSelect(PlainSelect plainSelect, List<EntitiyInfo> entitiyInfos, List<EntityColumnInfo> entityColumnInfos, Map<String, Table> tableAliasMap) {
        List<SelectItem> selectItems = plainSelect.getSelectItems();
        for (SelectItem selectItem : selectItems) {
            if (selectItem instanceof AllColumns) {
                handleAllColumns(plainSelect, (AllColumns) selectItem, entitiyInfos, entityColumnInfos, tableAliasMap);
            } else if (selectItem instanceof AllTableColumns) {
                handleAllTableColumns(plainSelect, (AllTableColumns) selectItem, entitiyInfos, entityColumnInfos, tableAliasMap);
            } else if (selectItem instanceof SelectExpressionItem) {
                handleSelectExpressionItem(plainSelect, (SelectExpressionItem) selectItem, entitiyInfos, entityColumnInfos, tableAliasMap);
            }
        }
    }

    public void handleFromItem(FromItem fromItem, List<EntitiyInfo> entitiyInfos, List<EntityColumnInfo> entityColumnInfos, Map<String, Table> tableAliasMap) {
        if (fromItem instanceof Table) {
            Table table = (Table) fromItem;
            String tableName = table.getName();
            if (table.getAlias() != null) {
                tableAliasMap.put(table.getAlias().getName(), table);
            } else {
                tableAliasMap.put(table.getName(), table);
            }
            inflateByEntity(entityColumnInfos, entitiyInfos, tableName);
        } else if (fromItem instanceof SubSelect) {
            handleSelect((PlainSelect) fromItem, entitiyInfos, entityColumnInfos, tableAliasMap);
        }
    }

    public void handleAllColumns(PlainSelect plainSelect, AllColumns allColumns, List<EntitiyInfo> entitiyInfos, List<EntityColumnInfo> entityColumnInfos, Map<String, Table> tableAliasMap) {
        FromItem fromItem = plainSelect.getFromItem();
        handleFromItem(fromItem, entitiyInfos, entityColumnInfos, tableAliasMap);
        if (plainSelect.getJoins() != null) {
            for (Join join : plainSelect.getJoins()) {
                handleFromItem(join.getRightItem(), entitiyInfos, entityColumnInfos, tableAliasMap);
            }
        }
    }

    public void handleAllTableColumns(PlainSelect plainSelect, AllTableColumns allTableColumns, List<EntitiyInfo> entitiyInfos, List<EntityColumnInfo> entityColumnInfos, Map<String, Table> tableAliasMap) {
        String tableName = allTableColumns.getTable().getName();
        if (tableAliasMap.containsKey(tableName)) {
            tableName = tableAliasMap.get(tableName).getName();
        }
        inflateByEntity(entityColumnInfos, entitiyInfos, tableName);
    }

    public void handleExpression(SelectExpressionItem selectExpressionItem, Expression expression, List<EntitiyInfo> entitiyInfos, List<EntityColumnInfo> entityColumnInfos, Map<String, Table> tableAliasMap) {
        String alis = selectExpressionItem.getAlias().getName();
        if (expression instanceof Column) {
            Column column = (Column) expression;
            String colName = column.getColumnName();
            String tableName = column.getTable().getName();
            if (tableAliasMap.containsKey(tableName)) {
                tableName = tableAliasMap.get(tableName).getName();
            }
            inflateByEntity(entityColumnInfos, entitiyInfos, tableName, colName, alis);
        } else if (expression instanceof CaseExpression) {
            CaseExpression caseExpression = (CaseExpression) expression;
            Expression elseExpression = caseExpression.getElseExpression();
            handleExpression(selectExpressionItem, elseExpression, entitiyInfos, entityColumnInfos, tableAliasMap);
        } else if (expression instanceof Function) {
            System.out.println("func alis:" + alis);
            Function function = (Function) expression;
            String functionName = function.getName().toLowerCase();
            String fieldType = "";
            switch (functionName) {
                case "group_concat":
                case "lower":
                case "ltrim":
                case "rtrim":
                case "replace":
                case "substr":
                case "trim":
                case "upper":
                case "typeof":
                    fieldType = "java.lang.String";
                    break;
                case "date":
                case "time":
                case "datetime":
                case "strftime":
                    fieldType = "java.util.Date";
                    break;
                case "avg":
                case "total":
                    fieldType = "float";
                    break;
                case "count":
                case "abs":
                case "length":
                case "random":
                    fieldType = "int";
                    break;
                case "max":
                case "min":
                case "sum":
                case "coalesce":
                case "ifnull":
                case "nullif":
                case "round":
                    handleFirstFunciotnParamter(selectExpressionItem, function, entitiyInfos, entityColumnInfos, tableAliasMap);
                    break;
            }
            if (!StringUtil.isEmpty(fieldType)) {
                String columnName = function.getName();
                Alias alias = selectExpressionItem.getAlias();
                if (alias != null && !StringUtil.isEmpty(alias.getName())) {
                    columnName = alias.getName();
                }
                EntityColumnInfo entityColumnInfo = getColumnInfo(columnName, columnName, fieldType);
                entityColumnInfos.add(entityColumnInfo);
            }
        } else if (expression instanceof StringValue) {
            String fieldType = "java.lang.String";
            inflateByValueType(selectExpressionItem, fieldType, entityColumnInfos);
        } else if (expression instanceof LongValue) {
            String fieldType = "long";
            inflateByValueType(selectExpressionItem, fieldType, entityColumnInfos);
        } else if (expression instanceof DoubleValue) {
            String fieldType = "double";
            inflateByValueType(selectExpressionItem, fieldType, entityColumnInfos);
        } else if (expression instanceof DateValue || expression instanceof TimeValue) {
            String fieldType = "java.util.Date";
            inflateByValueType(selectExpressionItem, fieldType, entityColumnInfos);
        } else if (expression instanceof MySQLGroupConcat) {
            String columnName = "group_concat";
            Alias alias = selectExpressionItem.getAlias();
            if (alias != null && !StringUtil.isEmpty(alias.getName())) {
                columnName = alias.getName();
            }
            EntityColumnInfo entityColumnInfo = getColumnInfo(columnName, columnName, "java.lang.String");
            entityColumnInfos.add(entityColumnInfo);
        }
    }

    public void inflateByValueType(SelectExpressionItem selectExpressionItem, String fieldType, List<EntityColumnInfo> entityColumnInfos) {
        Alias alias = selectExpressionItem.getAlias();
        if (alias != null && !StringUtil.isEmpty(alias.getName())) {
            String columnName = alias.getName();
            EntityColumnInfo entityColumnInfo = getColumnInfo(columnName, columnName, fieldType);
            entityColumnInfos.add(entityColumnInfo);
        }
    }

    public EntityColumnInfo getColumnInfo(String columnName, String fieldName, String fieldType) {
        EntityColumnInfo columnInfo = new EntityColumnInfo();
        columnInfo.setColumnName(columnName);
        columnInfo.setFieldName(fieldName);
        columnInfo.setFieldType(fieldType);
        return columnInfo;
    }

    public void handleFirstFunciotnParamter(SelectExpressionItem selectExpressionItem, Function function, List<EntitiyInfo> entitiyInfos, List<EntityColumnInfo> entityColumnInfos, Map<String, Table> tableAliasMap) {
        ExpressionList expressionList = function.getParameters();
        List<Expression> expressions = expressionList.getExpressions();
        if (expressions.size() > 0) {
            handleExpression(selectExpressionItem, expressions.get(0), entitiyInfos, entityColumnInfos, tableAliasMap);
        }
    }

    public void inflateByEntity(List<EntityColumnInfo> entityColumnInfos, List<EntitiyInfo> entitiyInfos, String tableName, String columnName, String alias) {
        for (EntitiyInfo entitiyInfo : entitiyInfos) {
            if (entitiyInfo.getTableName().equals(tableName)) {
                for (EntityColumnInfo columnInfo : entitiyInfo.getColumnInfos()) {
                    if (columnName.equals(columnInfo.getColumnName())) {
                        if (StringUtil.isEmpty(alias)) {
                            entityColumnInfos.add(columnInfo);
                        } else {
                            EntityColumnInfo entityColumnInfo = new EntityColumnInfo();
                            entityColumnInfo.setColumnName(alias);
                            entityColumnInfo.setFieldName(convertName(alias));
                            entityColumnInfo.setFieldType(columnInfo.getFieldType());
                            entityColumnInfo.setPrimaryKey(columnInfo.isPrimaryKey());
                            entityColumnInfo.setIgnore(columnInfo.isIgnore());
                            entityColumnInfo.setAutoGenerate(columnInfo.isAutoGenerate());
                            entityColumnInfos.add(entityColumnInfo);
                        }
                    }
                }
            }
        }
    }

    public void handleSelectExpressionItem(PlainSelect plainSelect, SelectExpressionItem selectExpressionItem, List<EntitiyInfo> entitiyInfos, List<EntityColumnInfo> entityColumnInfos, Map<String, Table> tableAliasMap) {
        Expression expression = selectExpressionItem.getExpression();
        handleExpression(selectExpressionItem, expression, entitiyInfos, entityColumnInfos, tableAliasMap);
    }

    public void inflateByEntity(List<EntityColumnInfo> entityColumnInfos, List<EntitiyInfo> entitiyInfos, String tableName) {
        for (int i = 0; i < entitiyInfos.size(); i++) {
            EntitiyInfo entitiyInfo = entitiyInfos.get(i);
            if (entitiyInfo.getTableName().equals(tableName)) {
//                entityColumnInfos.addAll(entitiyInfo.getColumnInfos());
                String name = entitiyInfo.getTableName();
                EntityColumnInfo entityColumnInfo = getColumnInfo(name, name, entitiyInfo.getClassName());
                entityColumnInfos.add(entityColumnInfo);
            }
        }
    }

    public List<EntitiyInfo> generateByEntity(Set<? extends Element> entityElements) {
        List<EntitiyInfo> entitiyInfos = new ArrayList<>();
        for (Element element : entityElements) {
            EntityGenerate entityGenerate = element.getAnnotation(EntityGenerate.class);
            String entityName = entityGenerate.entityName();
            String packageName = entityGenerate.packageName();
            EntitiyInfo entitiyInfo = new EntitiyInfo();
            entitiyInfo.setClassName(packageName + "." + entityName);
            entitiyInfo.setPackageName(packageName);
            List<EntityColumnInfo> entityColumnInfos = new ArrayList<>();
            entitiyInfo.setColumnInfos(entityColumnInfos);
            List<String> excludes = Arrays.asList(entityGenerate.excludes());
            if (!entityGenerate.createSql().equals("")) {
                String sql = entityGenerate.createSql();
                try {
                    Statement stmt = CCJSqlParserUtil.parse(sql);
                    if (stmt instanceof CreateTable) {
                        CreateTable createTable = (CreateTable) stmt;
                        entitiyInfo.setTableName(createTable.getTable().getName());
                        for (ColumnDefinition columnDefinition : createTable.getColumnDefinitions()) {
                            EntityColumnInfo columnInfo = new EntityColumnInfo();
                            String colName = columnDefinition.getColumnName();
                            if (excludes.contains(colName)) {
                                continue;
                            }
                            String colType = columnDefinition.getColDataType().getDataType();
                            if (columnDefinition.getColumnSpecs() != null && (columnDefinition.getColumnSpecs().contains("PRIMARY") || columnDefinition.getColumnSpecs().contains("primary"))) {
                                columnInfo.setPrimaryKey(true);
                            }
                            if (columnDefinition.getColumnSpecs() != null && (columnDefinition.getColumnSpecs().contains("AUTOINCREMENT") || columnDefinition.getColumnSpecs().contains("autoincrement"))) {
                                columnInfo.setAutoGenerate(true);
                            }
                            columnInfo.setColumnName(colName);
                            switch (colType) {
                                case "INTEGER":
                                case "integer":
                                    columnInfo.setFieldType("java.lang.Long");
                                    break;
                                case "TEXT":
                                case "text":
                                    columnInfo.setFieldType("java.lang.String");
                                    break;
                                case "DOUBLE":
                                case "double":
                                    columnInfo.setFieldType("java.lang.Double");
                                    break;
                                case "BLOB":
                                case "blob":
                                    columnInfo.setFieldType("java.lang.Byte[]");
                                    break;
                            }
                            columnInfo.setFieldName(convertName(colName));
                            entityColumnInfos.add(columnInfo);
                        }

                    }
                } catch (JSQLParserException e) {
                    e.printStackTrace();
                }
            }
            for (FieldGenerate field : entityGenerate.fields()) {
                EntityColumnInfo columnInfo = new EntityColumnInfo();
                columnInfo.setColumnName(field.name());
                columnInfo.setFieldName(field.name());
                columnInfo.setIgnore(field.ignore());
                String typeName = "";
                try {
                    Class type = field.type();
                    typeName = type.getName();
                } catch (MirroredTypeException e) {
                    typeName = e.getTypeMirror().toString();
                }
                columnInfo.setFieldType(typeName);
                columnInfo.setAutoGenerate(field.autoGenerate());
                columnInfo.setPrimaryKey(field.primaryKey());
                entityColumnInfos.add(columnInfo);
            }
            entitiyInfos.add(entitiyInfo);
        }
        return entitiyInfos;
    }

    public String convertName(String colName) {
        int len = colName.length();
        StringBuilder nameBuilder = new StringBuilder();
        boolean upperNext = false;
        for (int i = 0; i < len; i++) {
            char ch = colName.charAt(i);
            if (ch == '_') {
                if (i == 0) {
                    upperNext = false;
                } else {
                    upperNext = true;
                }
                continue;
            } else {
                if (upperNext) {
                    nameBuilder.append(Character.toUpperCase(ch));
                } else {
                    nameBuilder.append(ch);
                }
                upperNext = false;
            }
        }
        return nameBuilder.toString();

    }

    public TypeSpec.Builder buildEntityClass(EntitiyInfo entitiyInfo) {
        TypeSpec.Builder typeBuilder = TypeSpec.classBuilder(ClassName.bestGuess(entitiyInfo.getClassName())).addModifiers(Modifier.PUBLIC);
        for (EntityColumnInfo columnInfo : entitiyInfo.getColumnInfos()) {
            TypeName typeName = null;
            if (columnInfo.getFieldType().contains("[]")) {
                typeName = ArrayTypeName.of(TypeNameUtil.getTypeName(columnInfo.getFieldType().replace("[]", "")));
            } else {
                typeName = TypeNameUtil.getTypeName(columnInfo.getFieldType());
            }
            FieldSpec.Builder fieldSpecBuilder = FieldSpec.builder(typeName, columnInfo.getFieldName(), Modifier.PRIVATE);
            if (columnInfo.isPrimaryKey()) {
                AnnotationSpec.Builder annoBuilder = AnnotationSpec.builder(PrimaryKey.class);
                if (columnInfo.isAutoGenerate()) {
                    annoBuilder.addMember("autoGenerate", "$L", columnInfo.isAutoGenerate());
                }
                fieldSpecBuilder.addAnnotation(annoBuilder.build());
            }
            if (columnInfo.isIgnore()) {
                AnnotationSpec.Builder annoBuilder = AnnotationSpec.builder(Ignore.class);
                fieldSpecBuilder.addAnnotation(annoBuilder.build());
            }
            AnnotationSpec.Builder colAnnoBuilder = AnnotationSpec.builder(ColumnInfo.class);
            colAnnoBuilder.addMember("name", "$S", columnInfo.getColumnName());
            fieldSpecBuilder.addAnnotation(colAnnoBuilder.build());
            FieldSpec fieldSpec = fieldSpecBuilder.build();
            typeBuilder.addField(fieldSpec);
            String uppedName = Character.toUpperCase(columnInfo.getFieldName().charAt(0)) + columnInfo.getFieldName().substring(1);
            String setName = "set" + uppedName;
            String getName = typeName.equals(TypeName.BOOLEAN) ? "is" : "get" + uppedName;
            MethodSpec mSetter = MethodSpec.methodBuilder(setName).addModifiers(Modifier.PUBLIC).addParameter(fieldSpec.type, columnInfo.getFieldName()).returns(TypeName.VOID).addStatement("this.$L=$N", columnInfo.getFieldName(), columnInfo.getFieldName()).build();
            MethodSpec mGetter = MethodSpec.methodBuilder(getName).returns(fieldSpec.type).addModifiers(Modifier.PUBLIC).addStatement("return this.$L", columnInfo.getFieldName()).build();
            typeBuilder.addMethod(mSetter);
            typeBuilder.addMethod(mGetter);
        }

        return typeBuilder;
    }


    @Override
    public Set<String> getSupportedAnnotationTypes() {
        Set<String> types = new HashSet<>();
        types.add(EntityGenerate.class.getName());
        types.add(QueryDao.class.getName());
        types.add(Entity.class.getName());
        types.add(Dao.class.getName());
        return types;
    }
}