package com.github.rorymurphy.fluentquery.api;

import com.github.rorymurphy.fluentquery.ast.Expression;
import com.github.rorymurphy.fluentquery.ast.JoinClause;
import com.github.rorymurphy.fluentquery.ast.JoinType;
import com.github.rorymurphy.fluentquery.ast.PredicateExpression;
import com.github.rorymurphy.fluentquery.ast.SelectStatement;
import com.github.rorymurphy.fluentquery.ast.TableSource;
import com.github.rorymurphy.fluentquery.dialect.PostgreSqlDialect;
import com.github.rorymurphy.fluentquery.dialect.SqlDialect;
import com.github.rorymurphy.fluentquery.meta.QColumn;
import com.github.rorymurphy.fluentquery.render.DefaultStatementRenderer;
import com.github.rorymurphy.fluentquery.render.SqlRenderMode;
import com.github.rorymurphy.fluentquery.render.SqlRenderOptions;
import com.github.rorymurphy.fluentquery.sql.RenderedSql;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

public final class Query<R> {
    private final SelectStatement statement;
    private final R rowAccessor;

    Query(SelectStatement statement, R rowAccessor) {
        this.statement = statement;
        this.rowAccessor = rowAccessor;
    }

    public R rowAccessor() {
        return rowAccessor;
    }

    public SelectStatement statement() {
        return statement;
    }

    public Query<R> where(Function<R, PredicateExpression> predicateBuilder) {
        PredicateExpression predicate = predicateBuilder.apply(rowAccessor);
        return new Query<>(statement.withWhere(predicate), rowAccessor);
    }

    public <V> Query<R> whereEq(Function<R, QColumn<?, V>> columnSelector, V value) {
        return where(row -> columnSelector.apply(row).eq(value));
    }

    public Query<R> whereLike(Function<R, QColumn<?, String>> columnSelector, String pattern) {
        return where(row -> columnSelector.apply(row).like(pattern));
    }

    public Query<R> having(Function<R, PredicateExpression> predicateBuilder) {
        PredicateExpression predicate = predicateBuilder.apply(rowAccessor);
        return new Query<>(statement.withHaving(predicate), rowAccessor);
    }

    @SafeVarargs
    public final Query<R> select(Function<R, Expression<?>>... projectionBuilders) {
        List<Expression<?>> projections = new ArrayList<>(projectionBuilders.length);
        for (Function<R, Expression<?>> projectionBuilder : projectionBuilders) {
            projections.add(projectionBuilder.apply(rowAccessor));
        }
        return new Query<>(statement.withProjections(projections), rowAccessor);
    }

    @SafeVarargs
    public final Query<R> groupBy(Function<R, Expression<?>>... groupBuilders) {
        List<Expression<?>> groups = new ArrayList<>(groupBuilders.length);
        for (Function<R, Expression<?>> groupBuilder : groupBuilders) {
            groups.add(groupBuilder.apply(rowAccessor));
        }
        return new Query<>(statement.withGroupBy(groups), rowAccessor);
    }

    public <U> Query<JoinedRow> innerJoin(Query<U> right, BiFunction<R, U, PredicateExpression> onBuilder) {
        PredicateExpression on = onBuilder.apply(rowAccessor, right.rowAccessor);
        TableSource rightSource = right.statement.from();
        JoinClause joinClause = new JoinClause(JoinType.INNER, rightSource, on);
        SelectStatement withJoin = statement.withJoin(joinClause);

        Map<String, Object> accessors = new HashMap<>();
        accessors.putAll(JoinedRow.singletonAccessorMap(rowAccessor));
        accessors.putAll(JoinedRow.singletonAccessorMap(right.rowAccessor));

        JoinedRow joinedRow = new JoinedRow(accessors);
        return new Query<>(withJoin, joinedRow);
    }

    public RenderedSql toSql() {
        return render(new SqlRenderOptions(new PostgreSqlDialect(), SqlRenderMode.PARAMETERIZED));
    }

    public RenderedSql toSql(SqlDialect dialect) {
        return render(new SqlRenderOptions(dialect, SqlRenderMode.PARAMETERIZED));
    }

    public RenderedSql toSqlLiteral(SqlDialect dialect) {
        return render(new SqlRenderOptions(dialect, SqlRenderMode.LITERAL));
    }

    public RenderedSql render(SqlRenderOptions options) {
        return new DefaultStatementRenderer().render(statement, options);
    }

    public static <R> Query<R> forRoot(String tableName, String alias, R accessor) {
        SelectStatement statement = new SelectStatement(
            new TableSource(tableName, alias),
            new ArrayList<>(),
            new ArrayList<>(),
            null,
            new ArrayList<>(),
            null
        );
        return new Query<>(statement, accessor);
    }
}
