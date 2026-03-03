package com.github.rorymurphy.fluentquery.ast;

public record ParameterExpression<T>(T value, Class<T> javaType) implements Expression<T> {
}
