package com.github.rorymurphy.fluentquery.ast;

public record LiteralExpression<T>(T value, Class<T> javaType) implements Expression<T> {
}
