package com.ucar.datalink.worker.api.util.dialect.sqlserver;

import com.ucar.datalink.worker.api.util.dialect.AbstractSqlTemplate;
import com.ucar.datalink.worker.api.util.dialect.DbDialect;
import org.apache.ddlutils.model.Column;
import org.apache.ddlutils.model.Table;

/**
 * SqlServer数据库对应的Sql生成器.
 * Created by lubiao on 2017/3/15.
 */
public class SqlServerTemplate extends AbstractSqlTemplate {

    public SqlServerTemplate(DbDialect dbDialect) {
        super(dbDialect);
    }

    @Override
    public String getMergeSql(String schemaName, String tableName, String[] pkNames, String[] columnNames, String[] viewColumnNames) {
        final String aliasA = "a";
        final String aliasB = "b";
        StringBuilder sql = new StringBuilder();

        if (isAutoIncrement(schemaName, tableName)) {
            sql.append("set IDENTITY_INSERT ").append(tableName).append(" on ;");
        }

        sql.append("merge into ").append(getFullName(schemaName, tableName)).append(" ").append(aliasA);
        sql.append(" using (select ");

        int size = columnNames.length;
        // 构建 (select ? as col1, ? as col2 from dual)
        for (int i = 0; i < size; i++) {
            sql.append("? as " + columnNames[i]).append(" , ");
        }
        size = pkNames.length;
        for (int i = 0; i < size; i++) {
            sql.append("? as " + pkNames[i]).append((i + 1 < size) ? " , " : "");
        }
        sql.append(") ").append(aliasB);
        sql.append(" on (");

        size = pkNames.length;
        for (int i = 0; i < size; i++) {
            sql.append(aliasA + "." + pkNames[i]).append("=").append(aliasB + "." + pkNames[i]);
            sql.append((i + 1 < size) ? " and " : "");
        }

        sql.append(") when matched then update set ");

        size = columnNames.length;
        for (int i = 0; i < size; i++) {
            sql.append(columnNames[i]).append("=").append(aliasB + "." + columnNames[i]);
            sql.append((i + 1 < size) ? " , " : "");
        }

        if (isAutoIncrement(schemaName, tableName)) {
            sql.append(" when not matched then insert (");
            //sql.append(" when not matched then set IDENTITY_INSERT ").append(tableName).append(" on ").append("insert (");
        } else {
            sql.append(" when not matched then insert (");
        }

        size = columnNames.length;
        for (int i = 0; i < size; i++) {
            sql.append(columnNames[i]).append(" , ");
        }
        size = pkNames.length;
        for (int i = 0; i < size; i++) {
            sql.append(pkNames[i]).append((i + 1 < size) ? " , " : "");
        }

        sql.append(" ) values (");
        size = columnNames.length;
        for (int i = 0; i < size; i++) {
            sql.append(aliasB + "." + columnNames[i]).append(" , ");
        }
        size = pkNames.length;
        for (int i = 0; i < size; i++) {
            sql.append(aliasB + "." + pkNames[i]).append((i + 1 < size) ? " , " : "");
        }
        sql.append(" );");

        if (isAutoIncrement(schemaName, tableName)) {
            sql.append(" set IDENTITY_INSERT ").append(tableName).append(" off ;");
        }

        return sql.toString().intern(); // intern优化，避免出现大量相同的字符串
    }

    private boolean isAutoIncrement(String schemaName, String tableName) {
        Table table = getDbDialect().findTable(schemaName, tableName);
        Column[] pks = table.getPrimaryKeyColumns();
        return pks.length == 1 && pks[0].isAutoIncrement();
    }
}
