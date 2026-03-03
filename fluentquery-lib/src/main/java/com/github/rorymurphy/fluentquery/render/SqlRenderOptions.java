package com.github.rorymurphy.fluentquery.render;

import com.github.rorymurphy.fluentquery.dialect.SqlDialect;

public record SqlRenderOptions(SqlDialect dialect, SqlRenderMode mode) {
}
