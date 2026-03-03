package com.github.rorymurphy.fluentquery.api;

import com.github.rorymurphy.fluentquery.meta.QEntity;

public final class QueryBuilder {
    private QueryBuilder() {
    }

    public static <T extends QEntity<?>> Query<T> query(T entityAccessor) {
        return Query.forRoot(entityAccessor.tableName(), entityAccessor.alias(), entityAccessor);
    }
}
