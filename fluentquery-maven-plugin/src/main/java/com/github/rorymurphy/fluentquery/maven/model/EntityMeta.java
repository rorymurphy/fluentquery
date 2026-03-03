package com.github.rorymurphy.fluentquery.maven.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public record EntityMeta(
    String packageName,
    String className,
    String tableName,
    List<ColumnMeta> columns
) {
    public EntityMeta {
        columns = Collections.unmodifiableList(new ArrayList<>(columns));
    }
}
