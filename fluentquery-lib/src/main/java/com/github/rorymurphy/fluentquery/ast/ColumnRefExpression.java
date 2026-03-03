package com.github.rorymurphy.fluentquery.ast;

public record ColumnRefExpression<T>(String alias, String column, Class<T> javaType) implements Expression<T> {
}
