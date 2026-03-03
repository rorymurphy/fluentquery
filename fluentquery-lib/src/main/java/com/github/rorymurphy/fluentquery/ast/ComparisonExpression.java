package com.github.rorymurphy.fluentquery.ast;

public record ComparisonExpression(
    String operator,
    Expression<?> left,
    Expression<?> right
) implements PredicateExpression {
}
