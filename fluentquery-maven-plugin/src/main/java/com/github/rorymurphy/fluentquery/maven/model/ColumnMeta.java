package com.github.rorymurphy.fluentquery.maven.model;

public record ColumnMeta(
    String fieldName,
    String columnName,
    String javaType,
    boolean nullable
) {
}
