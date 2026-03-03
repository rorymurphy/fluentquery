package com.github.rorymurphy.fluentquery.ast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public record InExpression(Expression<?> left, List<Expression<?>> values) implements PredicateExpression {
    public InExpression {
        values = Collections.unmodifiableList(new ArrayList<>(values));
    }
}
