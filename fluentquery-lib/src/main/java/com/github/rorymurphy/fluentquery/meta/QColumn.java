package com.github.rorymurphy.fluentquery.meta;

import com.github.rorymurphy.fluentquery.ast.ColumnRefExpression;
import com.github.rorymurphy.fluentquery.ast.ComparisonExpression;
import com.github.rorymurphy.fluentquery.ast.Expression;
import com.github.rorymurphy.fluentquery.ast.InExpression;
import com.github.rorymurphy.fluentquery.ast.NullCheckExpression;
import com.github.rorymurphy.fluentquery.ast.ParameterExpression;
import com.github.rorymurphy.fluentquery.ast.PredicateExpression;
import com.github.rorymurphy.fluentquery.types.SqlType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class QColumn<OWNER, V> {
    private final QEntity<OWNER> owner;
    private final String columnName;
    private final Class<V> javaType;
    private final SqlType sqlType;
    private final boolean nullable;

    public QColumn(QEntity<OWNER> owner, String columnName, Class<V> javaType, SqlType sqlType, boolean nullable) {
        this.owner = owner;
        this.columnName = columnName;
        this.javaType = javaType;
        this.sqlType = sqlType;
        this.nullable = nullable;
    }

    public String columnName() {
        return columnName;
    }

    public Class<V> javaType() {
        return javaType;
    }

    public SqlType sqlType() {
        return sqlType;
    }

    public boolean nullable() {
        return nullable;
    }

    public String ownerAlias() {
        return owner.alias();
    }

    public ColumnRefExpression<V> toExpression() {
        return new ColumnRefExpression<>(owner.alias(), columnName, javaType);
    }

    public PredicateExpression eq(V value) {
        if (value == null) {
            return new NullCheckExpression(toExpression(), true);
        }
        return new ComparisonExpression("=", toExpression(), new ParameterExpression<>(value, javaType));
    }

    public PredicateExpression eq(QColumn<?, V> other) {
        return new ComparisonExpression("=", toExpression(), other.toExpression());
    }

    public PredicateExpression lt(V value) {
        return numericOrTemporalComparison("<", value);
    }

    public PredicateExpression lt(QColumn<?, V> other) {
        return new ComparisonExpression("<", toExpression(), other.toExpression());
    }

    public PredicateExpression lte(V value) {
        return numericOrTemporalComparison("<=", value);
    }

    public PredicateExpression lte(QColumn<?, V> other) {
        return new ComparisonExpression("<=", toExpression(), other.toExpression());
    }

    public PredicateExpression gt(V value) {
        return numericOrTemporalComparison(">", value);
    }

    public PredicateExpression gt(QColumn<?, V> other) {
        return new ComparisonExpression(">", toExpression(), other.toExpression());
    }

    public PredicateExpression gte(V value) {
        return numericOrTemporalComparison(">=", value);
    }

    public PredicateExpression gte(QColumn<?, V> other) {
        return new ComparisonExpression(">=", toExpression(), other.toExpression());
    }

    public PredicateExpression isNull() {
        return new NullCheckExpression(toExpression(), true);
    }

    public PredicateExpression isNotNull() {
        return new NullCheckExpression(toExpression(), false);
    }

    public PredicateExpression in(Collection<V> values) {
        if (values == null || values.isEmpty()) {
            throw new IllegalArgumentException("in() requires at least one value.");
        }

        List<Expression<?>> expressions = new ArrayList<>(values.size());
        for (V value : values) {
            if (value == null) {
                throw new IllegalArgumentException("in() does not support null values. Use isNull() or explicit OR predicates.");
            }
            expressions.add(new ParameterExpression<>(value, javaType));
        }

        return new InExpression(toExpression(), expressions);
    }

    @SafeVarargs
    public final PredicateExpression in(V... values) {
        return in(Arrays.asList(values));
    }

    public PredicateExpression like(String pattern) {
        if (!String.class.equals(javaType)) {
            throw new IllegalArgumentException("like() is only supported on String columns.");
        }
        return new ComparisonExpression("LIKE", toExpression(), new ParameterExpression<>(pattern, String.class));
    }

    private PredicateExpression numericOrTemporalComparison(String operator, V value) {
        if (value == null) {
            throw new IllegalArgumentException("Comparison value cannot be null for operator " + operator);
        }
        if (!(Number.class.isAssignableFrom(javaType)
            || javaType.getName().startsWith("java.time")
            || java.util.Date.class.isAssignableFrom(javaType))) {
            throw new IllegalArgumentException("Operator " + operator + " is only supported on numeric/temporal columns.");
        }
        Expression<V> right = new ParameterExpression<>(value, javaType);
        return new ComparisonExpression(operator, toExpression(), right);
    }
}
