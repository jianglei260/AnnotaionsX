package cn.scewin.ormplus.utils;

import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.AllColumns;
import net.sf.jsqlparser.statement.select.AllTableColumns;
import net.sf.jsqlparser.statement.select.FromItem;
import net.sf.jsqlparser.statement.select.Join;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SelectBody;
import net.sf.jsqlparser.statement.select.SelectItem;
import net.sf.jsqlparser.statement.select.SetOperationList;
import net.sf.jsqlparser.statement.select.SubSelect;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cn.scewin.ormplus.EntitiyInfo;
import cn.scewin.ormplus.EntityColumnInfo;
import cn.scewin.ormplus.TableFinder;


public class SQLUtil {
    public static List<EntitiyInfo> getEntityInfo(String sql, List<EntitiyInfo> entitiyInfos) {
        List<EntitiyInfo> result = new ArrayList<>();
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
                    handleSelect(plainSelect, entitiyInfos, result, tableAliasMap);
                }
            } catch (JSQLParserException e) {
                e.printStackTrace();
            }
        }
        return result;
    }

    public static void handleSelect(PlainSelect plainSelect, List<EntitiyInfo> entitiyInfos, List<EntitiyInfo> result, Map<String, Table> tableAliasMap) {
        List<SelectItem> selectItems = plainSelect.getSelectItems();
        for (SelectItem selectItem : selectItems) {
            if (selectItem instanceof AllColumns) {
                handleAllColumns(plainSelect, (AllColumns) selectItem, entitiyInfos, result, tableAliasMap);
            } else if (selectItem instanceof AllTableColumns) {
                handleAllTableColumns(plainSelect, (AllTableColumns) selectItem, entitiyInfos, result, tableAliasMap);
            }
        }
    }

    public static void handleAllColumns(PlainSelect plainSelect, AllColumns allColumns, List<EntitiyInfo> entitiyInfos, List<EntitiyInfo> result, Map<String, Table> tableAliasMap) {
        FromItem fromItem = plainSelect.getFromItem();
        handleFromItem(fromItem, entitiyInfos, result, tableAliasMap);
        if (plainSelect.getJoins() != null) {
            for (Join join : plainSelect.getJoins()) {
                handleFromItem(join.getRightItem(), entitiyInfos, result, tableAliasMap);
            }
        }
    }

    public static void handleAllTableColumns(PlainSelect plainSelect, AllTableColumns allTableColumns, List<EntitiyInfo> entitiyInfos,List<EntitiyInfo> result, Map<String, Table> tableAliasMap) {
        String tableName = allTableColumns.getTable().getName();
        if (tableAliasMap.containsKey(tableName)) {
            tableName = tableAliasMap.get(tableName).getName();
        }
         result.add(findEntity(entitiyInfos, tableName));
    }

    public static void handleFromItem(FromItem fromItem, List<EntitiyInfo> entitiyInfos, List<EntitiyInfo> result, Map<String, Table> tableAliasMap) {
        if (fromItem instanceof Table) {
            Table table = (Table) fromItem;
            String tableName = table.getName();
            if (table.getAlias() != null) {
                tableAliasMap.put(table.getAlias().getName(), table);
            } else {
                tableAliasMap.put(table.getName(), table);
            }
             result.add(findEntity(entitiyInfos, tableName));
        } else if (fromItem instanceof SubSelect) {
             handleSelect((PlainSelect) fromItem, entitiyInfos, result, tableAliasMap);
        }
    }

    public static EntitiyInfo findEntity(List<EntitiyInfo> entitiyInfos, String tableName) {
        for (int i = 0; i < entitiyInfos.size(); i++) {
            EntitiyInfo entitiyInfo = entitiyInfos.get(i);
            if (entitiyInfo.getTableName().equals(tableName)) {
                return entitiyInfo;
            }
        }
        return null;
    }

}
