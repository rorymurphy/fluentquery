package com.github.rorymurphy.fluentquery.ast;

public record JoinClause(JoinType type, TableSource right, PredicateExpression on) {
}
