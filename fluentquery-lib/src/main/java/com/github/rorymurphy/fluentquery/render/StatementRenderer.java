package com.github.rorymurphy.fluentquery.render;

import com.github.rorymurphy.fluentquery.ast.SqlStatement;
import com.github.rorymurphy.fluentquery.sql.RenderedSql;

public interface StatementRenderer {
    RenderedSql render(SqlStatement statement, SqlRenderOptions options);
}
