package com.github.rorymurphy.fluentquery.ast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public record FunctionExpression<T>(String name, List<Expression<?>> arguments, Class<T> resultType) implements Expression<T> {
    public FunctionExpression {
        arguments = Collections.unmodifiableList(new ArrayList<>(arguments));
    }
}
