package cn.scewin.ormplus.android.db;

import android.content.Context;
import android.database.Cursor;

import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteStatement;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import cn.scewin.annotionsx.common.utils.ObjectUtil;
import cn.scewin.annotionsx.common.utils.StringUtil;
import cn.scewin.ormplus.DaoInfo;
import cn.scewin.ormplus.EntitiyInfo;
import cn.scewin.ormplus.EntityColumnInfo;
import cn.scewin.ormplus.utils.SQLUtil;

public class OrmPlusDatabaseManager {
    private static OrmPlusDatabaseManager instance;
    private List<DaoInfo> daoInfos = new ArrayList<>();
    private List<EntitiyInfo> entitiyInfos = new ArrayList<>();
    private Map<String, EntitiyInfo> entitiyInfoMap = new HashMap<>();
    private Map<String, DaoInfo> daoInfoMap = new HashMap<>();
    private SupportSQLiteDatabase db;
    private Context context;

    public synchronized static OrmPlusDatabaseManager getInstance() {
        if (instance == null) {
            instance = new OrmPlusDatabaseManager();
        }
        return instance;
    }

    public void init(Context context, SupportSQLiteDatabase db) {
        this.db = db;
        this.context = context;
        Gson gson = new Gson();
        try {
            String[] files = context.getResources().getAssets().list("");
            for (String file : files) {
                if (file.startsWith("dao_")) {
                    InputStream daoIs = context.getResources().getAssets().open(file);
                    List<DaoInfo> infos = gson.fromJson(new InputStreamReader(daoIs), new TypeToken<List<DaoInfo>>() {
                    }.getType());
                    daoInfos.addAll(infos);
                }
                if (file.startsWith("entity_")) {
                    InputStream daoIs = context.getResources().getAssets().open(file);
                    List<EntitiyInfo> infos = gson.fromJson(new InputStreamReader(daoIs), new TypeToken<List<EntitiyInfo>>() {
                    }.getType());
                    entitiyInfos.addAll(infos);
                }
            }
            for (EntitiyInfo entitiyInfo : entitiyInfos) {
                entitiyInfoMap.put(getClassFullName(entitiyInfo), entitiyInfo);
            }
            for (DaoInfo daoInfo : daoInfos) {
                daoInfoMap.put(daoInfo.getDaoInterfaceName(), daoInfo);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public static String getClassFullName(EntitiyInfo entitiyInfo) {
        if (entitiyInfo.getClassName().contains(".")) {
            return entitiyInfo.getClassName();
        } else {
            return entitiyInfo.getPackageName() + "." + entitiyInfo.getClassName();
        }
    }

    public int deleteBySQL(String sql) {
        SupportSQLiteStatement statement = db.compileStatement(sql);
        return statement.executeUpdateDelete();
    }

    public <T> int delete(T... ts) throws Exception {
        if (!ObjectUtil.isNotEmpty(ts)) {
            return 0;
        }
        Class<T> entityClass = (Class<T>) ts[0].getClass();
        EntitiyInfo entitiyInfo = entitiyInfoMap.get(entityClass.getName());
        Field field = null;
        String idColName = "";
        if (entitiyInfo != null) {
            StringBuilder sqlBuilder = new StringBuilder();
            sqlBuilder.append("DELETE FROM ");
            sqlBuilder.append(entitiyInfo.getTableName());
            sqlBuilder.append(" WHERE ");
            for (EntityColumnInfo columnInfo : entitiyInfo.getColumnInfos()) {
                if (columnInfo.isPrimaryKey()) {
                    idColName = columnInfo.getColumnName();
                    field = entityClass.getDeclaredField(columnInfo.getFieldName());
                    field.setAccessible(true);
                    break;
                }
            }
            if (field != null) {
                sqlBuilder.append(idColName + " in (");
                for (int i = 0; i < ts.length; i++) {
                    Object id = field.get(ts[i]);
                    if (id instanceof String) {
                        sqlBuilder.append("'" + id + "'");
                    } else {
                        sqlBuilder.append(id);
                    }
                    if (i < ts.length - 1) {
                        sqlBuilder.append(",");
                    }
                }
                sqlBuilder.append(");");
                String sql = sqlBuilder.toString();
                SupportSQLiteStatement statement = db.compileStatement(sql);
                return statement.executeUpdateDelete();
            }
        }
        return 0;
    }

    public <T> List<T> query(String sql, Object[] args, Class<T> entityClass) {
        int lastIndex = 0;
        List argList = new ArrayList();
        for (int i = 0; i < args.length; i++) {
            try {
                lastIndex = sql.indexOf('?', lastIndex + 1);
                if (args[i] instanceof Iterable || args[i].getClass().isArray()) {
                    String arg = concatArgs(args[i], argList);
                    sql = sql.substring(0, lastIndex) + arg + sql.substring(lastIndex + 1);
                    lastIndex = lastIndex + arg.length();
                } else {
                    argList.add(args[i]);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        Object[] flatArgs = new Object[argList.size()];
        argList.toArray(flatArgs);
        Cursor cursor = db.query(sql, flatArgs);
        List<EntitiyInfo> sqlEntities = SQLUtil.getEntityInfo(sql, entitiyInfos);
        List<T> list = new ArrayList<>();
        while (cursor.moveToNext()) {
            T t = buildEntity(sqlEntities, cursor, entityClass);
            list.add(t);
        }
        return list;
    }


    public <T> String buildCreateTableSql(Class<T> tClass) {
        EntitiyInfo entitiyInfo = entitiyInfoMap.get(tClass.getName());
        if (entitiyInfo != null) {
            StringBuilder sb = new StringBuilder("CREATE TABLE IF NOT EXISTS ");
            if (StringUtil.isEmpty(entitiyInfo.getTableName())) {
                sb.append(StringUtil.convertWith_(StringUtil.getClassSimpleName(entitiyInfo.getClassName())));
            } else {
                sb.append(entitiyInfo.getTableName());
            }
            sb.append(" ( ");
            for (EntityColumnInfo columnInfo : entitiyInfo.getColumnInfos()) {
                if (!columnInfo.isIgnore()) {
                    sb.append(columnInfo.getColumnName());
                    String type = getColumnType(columnInfo.getFieldType());
                    sb.append(" ");
                    sb.append(type);
                    if (columnInfo.isPrimaryKey()) {
                        sb.append(" PRIMARY KEY ");
                    }
                    if (columnInfo.isAutoGenerate()) {
                        sb.append(" AUTOINCREMENT ");
                    }
                    sb.append(",");
                }
            }
            sb.deleteCharAt(sb.length() - 1);
            sb.append(");");
            return sb.toString();
        }
        return "";
    }

    public String getColumnType(String fieldType) {
        String type = "";
        String TYPE_TEXT = "TEXT";
        String TYPE_INTEGER = "INTEGER";
        String TYPE_DOUBLE = "DOUBLE";
        String TYPE_BLOB = "BLOB";
        switch (fieldType) {
            case "java.lang.String":
                type = TYPE_TEXT;
                break;
            case "java.lang.Byte[]":
            case "byte[]":
                type = TYPE_BLOB;
                break;
            case "java.lang.Long":
            case "long":
                type = TYPE_INTEGER;
                break;
            case "java.lang.Boolean":
            case "boolean":
                type = TYPE_INTEGER;
                break;
            case "java.lang.Integer":
            case "int":
                type = TYPE_INTEGER;
                break;
            case "java.lang.Short":
            case "short":
                type = TYPE_INTEGER;
                break;
            case "java.lang.Double":
            case "double":
                type = TYPE_DOUBLE;
                break;
            case "java.lang.Float":
            case "float":
                type = TYPE_DOUBLE;
                break;
            case "java.util.Date":
                type = TYPE_INTEGER;
                break;
            default:
                type = TYPE_TEXT;
                break;
        }
        return type;
    }

    public String concatArgs(Object args, List argList) {
        StringBuilder builder = new StringBuilder();
        if (args instanceof Iterable) {
            Iterator it = ((Iterable<?>) args).iterator();
            while (it.hasNext()) {
                Object next = it.next();
                argList.add(next);
                builder.append('?');
                if (it.hasNext()) {
                    builder.append(",");
                }
            }
        } else if (args.getClass().isArray()) {
            int length = Array.getLength(args);
            for (int i = 0; i < length; i++) {
                Object next = Array.get(args, i);
                argList.add(next);
                builder.append('?');
                if (i < length - 1) {
                    builder.append(",");
                }
            }
        }
        return builder.toString();
    }

    public <T> T buildEntity(List<EntitiyInfo> entitiyInfos, Cursor cursor, Class<T> tClass) {
        T t = null;
        try {
            t = tClass.newInstance();
            EntitiyInfo entitiyInfo = entitiyInfoMap.get(tClass.getName());
            for (EntityColumnInfo columnInfo : entitiyInfo.getColumnInfos()) {
                Field field = tClass.getDeclaredField(columnInfo.getFieldName());
                if (field.getType().getName().startsWith("cn.scewin.")) {
                    EntitiyInfo info = entitiyInfoMap.get(columnInfo.getFieldType());
                    if (entitiyInfos.contains(info)) {
                        entitiyInfos.remove(info);
                    }
                }
            }
            for (EntityColumnInfo columnInfo : entitiyInfo.getColumnInfos()) {
                Field field = tClass.getDeclaredField(columnInfo.getFieldName());
                field.setAccessible(true);
                Object value = null;
                if (field.getType().getName().startsWith("cn.scewin.")) {
                    Class clazz = Class.forName(columnInfo.getFieldType());
                    value = buildEntity(entitiyInfos, cursor, clazz);
                    field.set(t, value);
                    continue;
                }
                int colIndex = cursor.getColumnIndex(columnInfo.getColumnName());
                if (colIndex < 0) {
                    continue;
                }
                switch (columnInfo.getFieldType()) {
                    case "java.lang.String":
                        value = cursor.getString(colIndex);
                        break;
                    case "java.lang.Byte[]":
                    case "byte[]":
                        value = cursor.getBlob(colIndex);
                        break;
                    case "java.lang.Long":
                    case "long":
                        value = cursor.getLong(colIndex);
                        break;
                    case "java.lang.Boolean":
                    case "boolean":
                        value = cursor.getInt(colIndex) == 1 ? true : false;
                        break;
                    case "java.lang.Integer":
                    case "int":
                        value = cursor.getInt(colIndex);
                        break;
                    case "java.lang.Short":
                    case "short":
                        value = cursor.getInt(colIndex);
                        break;
                    case "java.lang.Double":
                    case "double":
                        value = cursor.getDouble(colIndex);
                        break;
                    case "java.lang.Float":
                    case "float":
                        value = cursor.getFloat(colIndex);
                        break;
                    case "java.util.Date":
                        value = new Date(cursor.getLong(colIndex));
                        break;
                    default:
                        Class clazz = Class.forName(columnInfo.getFieldType());
                        value = buildEntity(entitiyInfos, cursor, clazz);
                        break;
                }
                field.set(t, value);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return t;
    }

    public <T> void insertEntity(List<T> entitys) {
        for (T entity : entitys) {
            insertEntity(entity);
        }
    }

    public <T> long insertEntity(T entity) {
        if (entity == null) {
            return -1;
        }
        Class<T> entityClass = (Class<T>) entity.getClass();
        EntitiyInfo entitiyInfo = entitiyInfoMap.get(entityClass.getName());
        if (entitiyInfo != null) {
            StringBuilder sqlBuilder = new StringBuilder();
            StringBuilder valueBuilder = new StringBuilder();
            sqlBuilder.append("INSERT OR REPLACE INTO ");
            valueBuilder.append("VALUES (");
            sqlBuilder.append(entitiyInfo.getTableName());
            sqlBuilder.append("(");
            for (EntityColumnInfo columnInfo : entitiyInfo.getColumnInfos()) {
                if (columnInfo.isIgnore()) {
                    continue;
                }
                sqlBuilder.append(columnInfo.getColumnName());
                sqlBuilder.append(",");
                if (columnInfo.isPrimaryKey() && columnInfo.isAutoGenerate()) {
                    valueBuilder.append("nullif(?, 0),");
                } else {
                    valueBuilder.append("?,");
                }
            }
            if (sqlBuilder.charAt(sqlBuilder.length() - 1) == ',') {
                sqlBuilder.deleteCharAt(sqlBuilder.length() - 1);
            }
            if (valueBuilder.charAt(valueBuilder.length() - 1) == ',') {
                valueBuilder.deleteCharAt(valueBuilder.length() - 1);
            }
            sqlBuilder.append(") ");
            valueBuilder.append(");");
            sqlBuilder.append(valueBuilder.toString());
            String sql = sqlBuilder.toString();
            SupportSQLiteStatement statement = db.compileStatement(sql);
            int size = entitiyInfo.getColumnInfos().size();
            int index = 1;
            for (int i = 0; i < size; i++) {
                try {
                    EntityColumnInfo columnInfo = entitiyInfo.getColumnInfos().get(i);
                    if (columnInfo.isIgnore()) {
                        continue;
                    }
                    Field field = entityClass.getDeclaredField(columnInfo.getFieldName());
                    field.setAccessible(true);
                    Object value = field.get(entity);
                    switch (columnInfo.getFieldType()) {
                        case "java.lang.String":
                            if (value == null) {
                                statement.bindNull(index);
                            } else {
                                statement.bindString(index, (String) value);
                            }
                            break;
                        case "java.lang.Byte[]":
                        case "byte[]":
                            if (value == null) {
                                statement.bindNull(index);
                            } else {
                                statement.bindBlob(index, (byte[]) value);
                            }
                            break;
                        case "java.lang.Long":
                        case "long":
                        case "java.lang.Integer":
                        case "int":
                            if (value == null) {
                                statement.bindNull(index);
                            } else {
                                statement.bindLong(index, (long) value);
                            }
                            break;
                        case "java.lang.Double":
                        case "double":
                        case "java.lang.Float":
                        case "float":
                            if (value == null) {
                                statement.bindNull(index);
                            } else {
                                statement.bindDouble(index, (double) value);
                            }
                            break;
                        case "java.util.Date":
                            if (value == null) {
                                statement.bindNull(index);
                            } else {
                                statement.bindLong(index, ((Date) value).getTime());
                            }
                            break;
                        case "java.lang.Boolean":
                        case "boolean":
                            if (value == null) {
                                statement.bindNull(index);
                            } else {
                                statement.bindDouble(index, (boolean) value ? 1 : 0);
                            }
                            break;
                    }
                    index++;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            long id = statement.executeInsert();
            return id;
        }
        return -1;
    }


    public Map getFromCursor(Cursor cursor) {
        Map map = new HashMap();
        if (cursor.getColumnCount() >= 2) {
            inflateMap(cursor, map, 0);
        }
        cursor.close();
        return map;
    }

    public void inflateMap(Cursor cursor, Map map, int colIndex) {
        int colCount = cursor.getColumnCount();
        cursor.moveToFirst();
        if (colIndex < colCount - 1) {
            while (cursor.moveToNext()) {
                Object colValue = getColValue(cursor, colIndex);
                Object nextColValue = null;
                if (colIndex == (colCount - 2)) {
                    nextColValue = getColValue(cursor, colIndex + 1);
                } else {
                    nextColValue = new HashMap<>();
                }
                if (colIndex > 0 && colIndex < colCount - 1) {
                    Map currentMap = getCurrentMap(map, cursor, colIndex);
                    if (!currentMap.containsKey(colValue)) {
                        currentMap.put(colValue, nextColValue);
                    }
                } else {
                    if (!map.containsKey(colValue)) {
                        map.put(colValue, nextColValue);
                    }
                }
            }
            inflateMap(cursor, map, ++colIndex);
        }
    }

    public Map getCurrentMap(Map map, Cursor cursor, int colIndex) {
        Map currentMap = map;
        for (int i = 0; i < colIndex; i++) {
            currentMap = (Map) currentMap.get(getColValue(cursor, i));
        }
        return currentMap;
    }

    public Object getColValue(Cursor cursor, int colIndex) {
        Object colValue = null;
        switch (cursor.getType(colIndex)) {
            case Cursor.FIELD_TYPE_NULL:
                break;
            case Cursor.FIELD_TYPE_INTEGER:
                colValue = cursor.getInt(colIndex);
                break;
            case Cursor.FIELD_TYPE_FLOAT:
                colValue = cursor.getFloat(colIndex);
                break;
            case Cursor.FIELD_TYPE_STRING:
                colValue = cursor.getString(colIndex);
                break;
            case Cursor.FIELD_TYPE_BLOB:
                colValue = cursor.getBlob(colIndex);
                break;
        }
        return colValue;
    }
}
