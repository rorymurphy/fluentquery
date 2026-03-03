package com.github.rorymurphy.fluentquery.api;

import com.github.rorymurphy.fluentquery.ast.ComparisonExpression;
import com.github.rorymurphy.fluentquery.ast.Expression;
import com.github.rorymurphy.fluentquery.ast.FunctionExpression;
import com.github.rorymurphy.fluentquery.ast.LogicalExpression;
import com.github.rorymurphy.fluentquery.ast.NullCheckExpression;
import com.github.rorymurphy.fluentquery.ast.ParameterExpression;
import com.github.rorymurphy.fluentquery.ast.PredicateExpression;
import com.github.rorymurphy.fluentquery.meta.QColumn;
import java.util.Collection;
import java.util.List;

public final class Operators {
    private Operators() {
    }

    public static PredicateExpression and(PredicateExpression left, PredicateExpression right) {
        return new LogicalExpression("AND", left, right);
    }

    public static PredicateExpression or(PredicateExpression left, PredicateExpression right) {
        return new LogicalExpression("OR", left, right);
    }

    public static <OWNER, V> PredicateExpression eq(QColumn<OWNER, V> column, V value) {
        return column.eq(value);
    }

    public static <OWNER, V> PredicateExpression lt(QColumn<OWNER, V> column, V value) {
        return column.lt(value);
    }

    public static <OWNER, V> PredicateExpression gt(QColumn<OWNER, V> column, V value) {
        return column.gt(value);
    }

    public static <OWNER> PredicateExpression like(QColumn<OWNER, String> column, String pattern) {
        return column.like(pattern);
    }

    public static <OWNER, V> PredicateExpression in(QColumn<OWNER, V> column, Collection<V> values) {
        return column.in(values);
    }

    @SafeVarargs
    public static <OWNER, V> PredicateExpression in(QColumn<OWNER, V> column, V... values) {
        return column.in(values);
    }

    public static <V> PredicateExpression eq(Expression<V> expression, V value) {
        if (value == null) {
            return new NullCheckExpression(expression, true);
        }
        return new ComparisonExpression("=", expression, new ParameterExpression<>(value, inferType(value)));
    }

    public static <V> PredicateExpression lt(Expression<V> expression, V value) {
        return new ComparisonExpression("<", expression, new ParameterExpression<>(value, inferType(value)));
    }

    public static <V> PredicateExpression gt(Expression<V> expression, V value) {
        return new ComparisonExpression(">", expression, new ParameterExpression<>(value, inferType(value)));
    }

    public static PredicateExpression like(Expression<String> expression, String pattern) {
        return new ComparisonExpression("LIKE", expression, new ParameterExpression<>(pattern, String.class));
    }

    public static Expression<Long> count(Expression<?> expression) {
        return new FunctionExpression<>("COUNT", List.of(expression), Long.class);
    }

    public static Expression<Long> count(QColumn<?, ?> column) {
        return count(column.toExpression());
    }

    public static <N extends Number> Expression<N> sum(Expression<N> expression) {
        return new FunctionExpression<>("SUM", List.of(expression), expressionType(expression));
    }

    public static <N extends Number> Expression<N> sum(QColumn<?, N> column) {
        return sum(column.toExpression());
    }

    public static <V> Expression<V> max(Expression<V> expression) {
        return new FunctionExpression<>("MAX", List.of(expression), expressionType(expression));
    }

    public static <V> Expression<V> max(QColumn<?, V> column) {
        return max(column.toExpression());
    }

    @SuppressWarnings("unchecked")
    private static <V> Class<V> inferType(V value) {
        return (Class<V>) value.getClass();
    }

    @SuppressWarnings("unchecked")
    private static <V> Class<V> expressionType(Expression<V> expression) {
        if (expression instanceof com.github.rorymurphy.fluentquery.ast.ColumnRefExpression<?> column) {
            return (Class<V>) column.javaType();
        }
        if (expression instanceof ParameterExpression<?> parameter) {
            return (Class<V>) parameter.javaType();
        }
        if (expression instanceof FunctionExpression<?> functionExpression) {
            return (Class<V>) functionExpression.resultType();
        }
        return (Class<V>) Object.class;
    }
}
