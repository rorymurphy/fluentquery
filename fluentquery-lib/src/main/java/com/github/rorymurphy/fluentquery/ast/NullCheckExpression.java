package com.github.rorymurphy.fluentquery.ast;

public record NullCheckExpression(Expression<?> expression, boolean isNullCheck) implements PredicateExpression {
}
