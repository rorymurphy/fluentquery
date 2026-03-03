package com.github.rorymurphy.fluentquery.sql;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public record RenderedSql(String sql, List<QueryParameter> parameters) {
    public RenderedSql {
        parameters = Collections.unmodifiableList(new ArrayList<>(parameters));
    }
}
