package com.github.rorymurphy.fluentquery.ast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public record SelectStatement(
    TableSource from,
    List<Expression<?>> projections,
    List<JoinClause> joins,
    PredicateExpression where,
    List<Expression<?>> groupBy,
    PredicateExpression having
) implements SqlStatement {
    public SelectStatement {
        projections = Collections.unmodifiableList(new ArrayList<>(projections));
        joins = Collections.unmodifiableList(new ArrayList<>(joins));
        groupBy = Collections.unmodifiableList(new ArrayList<>(groupBy));
    }

    public SelectStatement withWhere(PredicateExpression predicate) {
        return new SelectStatement(from, projections, joins, predicate, groupBy, having);
    }

    public SelectStatement withHaving(PredicateExpression predicate) {
        return new SelectStatement(from, projections, joins, where, groupBy, predicate);
    }

    public SelectStatement withProjections(List<Expression<?>> newProjections) {
        return new SelectStatement(from, newProjections, joins, where, groupBy, having);
    }

    public SelectStatement withGroupBy(List<Expression<?>> newGroupBy) {
        return new SelectStatement(from, projections, joins, where, newGroupBy, having);
    }

    public SelectStatement withJoin(JoinClause joinClause) {
        List<JoinClause> copy = new ArrayList<>(joins);
        copy.add(joinClause);
        return new SelectStatement(from, projections, copy, where, groupBy, having);
    }
}
