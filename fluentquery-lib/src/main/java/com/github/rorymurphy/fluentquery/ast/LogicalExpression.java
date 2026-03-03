package com.github.rorymurphy.fluentquery.ast;

public record LogicalExpression(
    String operator,
    PredicateExpression left,
    PredicateExpression right
) implements PredicateExpression {
}
