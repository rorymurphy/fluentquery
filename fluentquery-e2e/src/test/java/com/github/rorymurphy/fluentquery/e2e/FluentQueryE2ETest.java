package com.github.rorymurphy.fluentquery.e2e;

import com.github.rorymurphy.fluentquery.dialect.MySqlDialect;
import com.github.rorymurphy.fluentquery.e2e.entities.fluentquery.QEmployeeEntity;
import com.github.rorymurphy.fluentquery.sql.RenderedSql;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

import static com.github.rorymurphy.fluentquery.api.Operators.and;
import static com.github.rorymurphy.fluentquery.api.Operators.count;
import static com.github.rorymurphy.fluentquery.api.Operators.gt;
import static com.github.rorymurphy.fluentquery.api.QueryBuilder.query;
import static com.github.rorymurphy.fluentquery.e2e.entities.fluentquery.QEntities.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class FluentQueryE2ETest {
    @Test
    void generatedQClassBuildsAndRendersSql() {
        RenderedSql renderedSql = query(productEntity)
            .where(p -> and(p.name.like("Nike%"), p.price.lt(new BigDecimal("5.00"))))
            .toSql();

        assertEquals("SELECT \"p\".* FROM \"product\" \"p\" WHERE (\"p\".\"name\" LIKE $1 AND \"p\".\"price\" < $2)", renderedSql.sql());
        assertEquals(2, renderedSql.parameters().size());
        assertEquals("Nike%", renderedSql.parameters().get(0).value());
        assertEquals(new BigDecimal("5.00"), renderedSql.parameters().get(1).value());
    }

    @Test
    void groupByHavingAndMysqlDialectRenderCorrectly() {
        RenderedSql renderedSql = query(productEntity)
            .select(p -> p.name.toExpression(), p -> count(p.id))
            .groupBy(p -> p.name.toExpression())
            .having(p -> gt(count(p.id), 10L))
            .toSql(new MySqlDialect());

        assertEquals("SELECT `p`.`name`, COUNT(`p`.`id`) FROM `product` `p` GROUP BY `p`.`name` HAVING COUNT(`p`.`id`) > ?", renderedSql.sql());
        assertEquals(1, renderedSql.parameters().size());
        assertEquals(10L, renderedSql.parameters().get(0).value());
    }

    @Test
    void whereShortcutMethodsRenderCorrectly() {
        RenderedSql likeSql = query(productEntity)
            .whereLike(p -> p.name, "Ni%")
            .toSql();

        assertEquals("SELECT \"p\".* FROM \"product\" \"p\" WHERE \"p\".\"name\" LIKE $1", likeSql.sql());
        assertEquals("Ni%", likeSql.parameters().get(0).value());

        RenderedSql eqSql = query(productEntity)
            .whereEq(p -> p.id, 42)
            .toSql();

        assertEquals("SELECT \"p\".* FROM \"product\" \"p\" WHERE \"p\".\"id\" = $1", eqSql.sql());
        assertEquals(1, eqSql.parameters().size());
        assertEquals(42, eqSql.parameters().get(0).value());
    }

    @Test
    void selfJoinUsesDistinctAliasesAndAliasBasedJoinedRowAccess() {
        var employee = employeeEntity.as("employee");
        var manager = employeeEntity.as("manager");

        var employeeQuery = query(employee);
        var managerQuery = query(manager);

        var joined = employeeQuery.innerJoin(managerQuery, (e, m) -> e.managerId.eq(m.id));

        var row = joined.rowAccessor();
        assertSame(employee, row.as("employee", QEmployeeEntity.class));
        assertSame(manager, row.as("manager", QEmployeeEntity.class));
        assertSame(manager, row.as(manager));

        RenderedSql renderedSql = joined
            .where(r -> r.as("manager", QEmployeeEntity.class).name.like("A%"))
            .toSql();

        assertEquals("SELECT \"employee\".* FROM \"employee\" \"employee\" INNER JOIN \"employee\" \"manager\" ON \"employee\".\"manager_id\" = \"manager\".\"id\" WHERE \"manager\".\"name\" LIKE $1", renderedSql.sql());
        assertEquals(1, renderedSql.parameters().size());
        assertEquals("A%", renderedSql.parameters().get(0).value());
    }
}
