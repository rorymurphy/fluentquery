package com.github.rorymurphy.fluentquery.sql;

public record QueryParameter(int index, Object value, Class<?> javaType) {
}
