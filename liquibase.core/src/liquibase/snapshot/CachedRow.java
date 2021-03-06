package liquibase.snapshot;

import java.util.Map;

public class CachedRow {
    private final Map row;

    public CachedRow(Map row) {
        this.row = row;
    }



    public Object get(String columnName) {
        return row.get(columnName);
    }

    public void set(String columnName, Object value) {
        row.put(columnName, value);
    }


    public boolean containsColumn(String columnName) {
        return row.containsKey(columnName);
    }

    public String getString(String columnName) {
        return (String) row.get(columnName);
    }

    public Integer getInt(String columnName) {
        Object o = row.get(columnName);
        if (o instanceof Number) {
            return Integer.valueOf(((Number) o).intValue());
        } else if (o instanceof String) {
            return Integer.valueOf((String) o);
        }
        return (Integer) o;
    }

    public Short getShort(String columnName) {
        Object o = row.get(columnName);
        if (o instanceof Number) {
            return Short.valueOf(((Number) o).shortValue());
        } else if (o instanceof String) {
            return Short.valueOf((String) o);
        }
        return (Short) o;
    }

    public Boolean getBoolean(String columnName) {
        Object o = row.get(columnName);
        if (o instanceof Number) {
            return ((Number) o).longValue() == 0 ? Boolean.FALSE : Boolean.TRUE;
        }
        if (o instanceof String) {
            return Boolean.valueOf(o.toString());
        }
        return (Boolean) o;
    }
}
