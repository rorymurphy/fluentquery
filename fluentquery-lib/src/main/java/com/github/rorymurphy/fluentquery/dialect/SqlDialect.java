package com.github.rorymurphy.fluentquery.dialect;

public interface SqlDialect {
    String quoteIdentifier(String identifier);

    String parameterPlaceholder(int parameterIndex);

    String toLiteral(Object value);
}
