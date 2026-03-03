package com.github.rorymurphy.fluentquery.meta;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class QEntity<T> {
    private final Class<T> entityClass;
    private final String tableName;
    private final String alias;
    private final List<QColumn<T, ?>> columns;

    public QEntity(Class<T> entityClass, String tableName, String alias, List<QColumn<T, ?>> columns) {
        this.entityClass = entityClass;
        this.tableName = tableName;
        this.alias = alias;
        this.columns = Collections.unmodifiableList(new ArrayList<>(columns));
    }

    public Class<T> entityClass() {
        return entityClass;
    }

    public String tableName() {
        return tableName;
    }

    public String alias() {
        return alias;
    }

    public List<QColumn<T, ?>> columns() {
        return columns;
    }
}
