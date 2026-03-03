package com.github.rorymurphy.fluentquery.dialect;

public final class MySqlDialect implements SqlDialect {
    @Override
    public String quoteIdentifier(String identifier) {
        return "`" + identifier + "`";
    }

    @Override
    public String parameterPlaceholder(int parameterIndex) {
        return "?";
    }

    @Override
    public String toLiteral(Object value) {
        if (value == null) {
            return "NULL";
        }
        if (value instanceof Number || value instanceof Boolean) {
            return value.toString();
        }
        return "'" + value.toString().replace("'", "''") + "'";
    }
}
