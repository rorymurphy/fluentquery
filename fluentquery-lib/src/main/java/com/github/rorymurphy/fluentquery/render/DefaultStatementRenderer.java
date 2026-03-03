package com.github.rorymurphy.fluentquery.render;

import com.github.rorymurphy.fluentquery.ast.ColumnRefExpression;
import com.github.rorymurphy.fluentquery.ast.ComparisonExpression;
import com.github.rorymurphy.fluentquery.ast.Expression;
import com.github.rorymurphy.fluentquery.ast.FunctionExpression;
import com.github.rorymurphy.fluentquery.ast.InExpression;
import com.github.rorymurphy.fluentquery.ast.JoinClause;
import com.github.rorymurphy.fluentquery.ast.LogicalExpression;
import com.github.rorymurphy.fluentquery.ast.NullCheckExpression;
import com.github.rorymurphy.fluentquery.ast.ParameterExpression;
import com.github.rorymurphy.fluentquery.ast.PredicateExpression;
import com.github.rorymurphy.fluentquery.ast.SelectStatement;
import com.github.rorymurphy.fluentquery.ast.SqlStatement;
import com.github.rorymurphy.fluentquery.sql.QueryParameter;
import com.github.rorymurphy.fluentquery.sql.RenderedSql;
import java.util.ArrayList;
import java.util.List;

public final class DefaultStatementRenderer implements StatementRenderer {
    @Override
    public RenderedSql render(SqlStatement statement, SqlRenderOptions options) {
        if (statement instanceof SelectStatement selectStatement) {
            return renderSelect(selectStatement, options);
        }
        throw new UnsupportedOperationException("Rendering not implemented for statement type: " + statement.getClass().getSimpleName());
    }

    private RenderedSql renderSelect(SelectStatement selectStatement, SqlRenderOptions options) {
        List<QueryParameter> parameters = new ArrayList<>();
        StringBuilder builder = new StringBuilder();

        builder.append("SELECT ");
        if (selectStatement.projections().isEmpty()) {
            builder.append(options.dialect().quoteIdentifier(selectStatement.from().alias())).append(".*");
        } else {
            builder.append(renderProjectionList(selectStatement.projections(), options, parameters));
        }

        builder.append(" FROM ")
            .append(options.dialect().quoteIdentifier(selectStatement.from().tableName()))
            .append(" ")
            .append(options.dialect().quoteIdentifier(selectStatement.from().alias()));

        for (JoinClause joinClause : selectStatement.joins()) {
            builder.append(" INNER JOIN ")
                .append(options.dialect().quoteIdentifier(joinClause.right().tableName()))
                .append(" ")
                .append(options.dialect().quoteIdentifier(joinClause.right().alias()))
                .append(" ON ")
                .append(renderPredicate(joinClause.on(), options, parameters));
        }

        if (selectStatement.where() != null) {
            builder.append(" WHERE ").append(renderPredicate(selectStatement.where(), options, parameters));
        }

        if (!selectStatement.groupBy().isEmpty()) {
            builder.append(" GROUP BY ")
                .append(renderProjectionList(selectStatement.groupBy(), options, parameters));
        }

        if (selectStatement.having() != null) {
            builder.append(" HAVING ").append(renderPredicate(selectStatement.having(), options, parameters));
        }

        return new RenderedSql(builder.toString(), parameters);
    }

    private String renderProjectionList(List<Expression<?>> expressions, SqlRenderOptions options, List<QueryParameter> parameters) {
        List<String> rendered = new ArrayList<>();
        for (Expression<?> expression : expressions) {
            rendered.add(renderExpression(expression, options, parameters));
        }
        return String.join(", ", rendered);
    }

    private String renderPredicate(PredicateExpression expression, SqlRenderOptions options, List<QueryParameter> parameters) {
        if (expression instanceof ComparisonExpression comparison) {
            return renderExpression(comparison.left(), options, parameters)
                + " " + comparison.operator() + " "
                + renderExpression(comparison.right(), options, parameters);
        }

        if (expression instanceof LogicalExpression logical) {
            return "(" + renderPredicate(logical.left(), options, parameters)
                + " " + logical.operator() + " "
                + renderPredicate(logical.right(), options, parameters) + ")";
        }

        if (expression instanceof NullCheckExpression nullCheck) {
            return renderExpression(nullCheck.expression(), options, parameters)
                + (nullCheck.isNullCheck() ? " IS NULL" : " IS NOT NULL");
        }

        if (expression instanceof InExpression inExpression) {
            List<String> values = new ArrayList<>(inExpression.values().size());
            for (Expression<?> value : inExpression.values()) {
                values.add(renderExpression(value, options, parameters));
            }
            return renderExpression(inExpression.left(), options, parameters)
                + " IN (" + String.join(", ", values) + ")";
        }

        throw new IllegalArgumentException("Unsupported predicate expression: " + expression.getClass().getSimpleName());
    }

    private String renderExpression(Expression<?> expression, SqlRenderOptions options, List<QueryParameter> parameters) {
        if (expression instanceof ColumnRefExpression<?> column) {
            return options.dialect().quoteIdentifier(column.alias()) + "." + options.dialect().quoteIdentifier(column.column());
        }

        if (expression instanceof ParameterExpression<?> parameterExpression) {
            if (options.mode() == SqlRenderMode.LITERAL) {
                return options.dialect().toLiteral(parameterExpression.value());
            }
            int index = parameters.size() + 1;
            parameters.add(new QueryParameter(index, parameterExpression.value(), parameterExpression.javaType()));
            return options.dialect().parameterPlaceholder(index);
        }

        if (expression instanceof FunctionExpression<?> functionExpression) {
            List<String> renderedArgs = new ArrayList<>(functionExpression.arguments().size());
            for (Expression<?> arg : functionExpression.arguments()) {
                renderedArgs.add(renderExpression(arg, options, parameters));
            }
            return functionExpression.name() + "(" + String.join(", ", renderedArgs) + ")";
        }

        throw new IllegalArgumentException("Unsupported expression: " + expression.getClass().getSimpleName());
    }
}
