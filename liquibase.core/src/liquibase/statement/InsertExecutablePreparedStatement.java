package liquibase.statement;


import java.util.List;
import liquibase.change.ColumnConfig;
import liquibase.changelog.ChangeSet;
import liquibase.database.Database;

/**
 * Handles INSERT Execution
 */
public class InsertExecutablePreparedStatement extends ExecutablePreparedStatementBase {
	
	public InsertExecutablePreparedStatement(Database database, String catalogName, String schemaName, String tableName, List<ColumnConfig> columns, ChangeSet changeSet) {
		super(database, catalogName, schemaName, tableName, columns, changeSet);
	}

	@Override
	protected String generateSql(List<ColumnConfig> cols) {
		StringBuilder sql = new StringBuilder("INSERT INTO ");
	    StringBuilder params = new StringBuilder("VALUES(");
	    sql.append(database.escapeTableName(getCatalogName(), getSchemaName(), getTableName()));
	    sql.append("(");
	    for(ColumnConfig column : getColumns()) {
	        if (database.supportsAutoIncrement()
	            && Boolean.TRUE.equals(column.isAutoIncrement())) {
	            continue;
	        }
	        sql.append(database.escapeColumnName(getCatalogName(), getSchemaName(), getTableName(), column.getName()));
	        sql.append(", ");
	        params.append("?, ");
	        cols.add(column);
	    }
	    sql.deleteCharAt(sql.lastIndexOf(" "));
	    sql.deleteCharAt(sql.lastIndexOf(","));
	    params.deleteCharAt(params.lastIndexOf(" "));
	    params.deleteCharAt(params.lastIndexOf(","));
	    params.append(")");
	    sql.append(") ");
	    sql.append(params);
		return sql.toString();
	}
}
