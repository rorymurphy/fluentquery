package com.github.rorymurphy.fluentquery.api;

import com.github.rorymurphy.fluentquery.dialect.MySqlDialect;
import com.github.rorymurphy.fluentquery.meta.QColumn;
import com.github.rorymurphy.fluentquery.meta.QEntity;
import com.github.rorymurphy.fluentquery.sql.RenderedSql;
import com.github.rorymurphy.fluentquery.types.SqlType;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

import static com.github.rorymurphy.fluentquery.api.Operators.and;
import static com.github.rorymurphy.fluentquery.api.Operators.count;
import static com.github.rorymurphy.fluentquery.api.Operators.gt;
import static com.github.rorymurphy.fluentquery.api.Operators.in;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class QueryTest {
    @Test
    void whereReturnsNewQueryAndParameterizedSqlTracksParameters() {
        Query<TestQProduct> query = QueryBuilder.query(TestQProduct.product);

        Query<TestQProduct> filtered = query.where(p -> and(p.name.like("Nike%"), p.price.lt(5.00f)));

        assertNotSame(query, filtered);

        RenderedSql renderedSql = filtered.toSql();
        assertEquals("SELECT \"p\".* FROM \"product\" \"p\" WHERE (\"p\".\"name\" LIKE $1 AND \"p\".\"price\" < $2)", renderedSql.sql());
        assertEquals(2, renderedSql.parameters().size());
        assertEquals("Nike%", renderedSql.parameters().get(0).value());
        assertEquals(5.00f, renderedSql.parameters().get(1).value());
    }

    @Test
    void supportsMysqlRendering() {
        Query<TestQProduct> query = QueryBuilder.query(TestQProduct.product)
            .whereEq(p -> p.id, 10);

        RenderedSql renderedSql = query.toSql(new MySqlDialect());
        assertEquals("SELECT `p`.* FROM `product` `p` WHERE `p`.`id` = ?", renderedSql.sql());
        assertEquals(1, renderedSql.parameters().size());
    }

    @Test
    void eqNullUsesIsNullSemantics() {
        RenderedSql renderedSql = QueryBuilder.query(TestQProduct.product)
            .where(p -> p.description.isNull())
            .toSql();

        assertEquals("SELECT \"p\".* FROM \"product\" \"p\" WHERE \"p\".\"description\" IS NULL", renderedSql.sql());
        assertEquals(0, renderedSql.parameters().size());
    }

    @Test
    void instanceOverloadStillSupported() {
        TestQProduct product = new TestQProduct();

        RenderedSql renderedSql = QueryBuilder.query(product)
            .where(p -> p.id.eq(1))
            .toSql();

        assertEquals("SELECT \"p\".* FROM \"product\" \"p\" WHERE \"p\".\"id\" = $1", renderedSql.sql());
        assertEquals(1, renderedSql.parameters().size());
    }

    @Test
    void querySingletonIsSupported() {
        RenderedSql renderedSql = QueryBuilder.query(TestQProduct.product)
            .where(p -> p.id.eq(2))
            .toSql();

        assertEquals("SELECT \"p\".* FROM \"product\" \"p\" WHERE \"p\".\"id\" = $1", renderedSql.sql());
        assertEquals(1, renderedSql.parameters().size());
    }

    @Test
    void queryInstanceIsSupported() {
        RenderedSql renderedSql = QueryBuilder.query(new TestQProduct())
            .whereEq(p -> p.id, 3)
            .toSql();

        assertEquals("SELECT \"p\".* FROM \"product\" \"p\" WHERE \"p\".\"id\" = $1", renderedSql.sql());
        assertEquals(1, renderedSql.parameters().size());
    }

    @Test
    void whereLikeShortcutIsSupported() {
        RenderedSql renderedSql = QueryBuilder.query(TestQProduct.product)
            .whereLike(p -> p.name, "Ni%")
            .toSql();

        assertEquals("SELECT \"p\".* FROM \"product\" \"p\" WHERE \"p\".\"name\" LIKE $1", renderedSql.sql());
        assertEquals("Ni%", renderedSql.parameters().get(0).value());
    }

    @Test
    void whereInSupportsVarargsAndParameterizedRendering() {
        RenderedSql renderedSql = QueryBuilder.query(TestQProduct.product)
            .where(p -> p.id.in(1, 2, 3))
            .toSql();

        assertEquals("SELECT \"p\".* FROM \"product\" \"p\" WHERE \"p\".\"id\" IN ($1, $2, $3)", renderedSql.sql());
        assertEquals(3, renderedSql.parameters().size());
        assertEquals(1, renderedSql.parameters().get(0).value());
        assertEquals(2, renderedSql.parameters().get(1).value());
        assertEquals(3, renderedSql.parameters().get(2).value());
    }

    @Test
    void staticInOperatorHelperIsSupported() {
        RenderedSql renderedSql = QueryBuilder.query(TestQProduct.product)
            .where(p -> in(p.id, 7, 8))
            .toSql();

        assertEquals("SELECT \"p\".* FROM \"product\" \"p\" WHERE \"p\".\"id\" IN ($1, $2)", renderedSql.sql());
        assertEquals(2, renderedSql.parameters().size());
        assertEquals(7, renderedSql.parameters().get(0).value());
        assertEquals(8, renderedSql.parameters().get(1).value());
    }

    @Test
    void whereInSupportsCollectionValues() {
        RenderedSql renderedSql = QueryBuilder.query(TestQProduct.product)
            .where(p -> p.name.in(Arrays.asList("A", "B")))
            .toSql();

        assertEquals("SELECT \"p\".* FROM \"product\" \"p\" WHERE \"p\".\"name\" IN ($1, $2)", renderedSql.sql());
    }

    @Test
    void whereInRejectsEmptyValues() {
        assertThrows(IllegalArgumentException.class, () -> QueryBuilder.query(TestQProduct.product)
            .where(p -> p.id.in(List.<Integer>of())));
    }

    @Test
    void varargsSelectAndGroupByAreSupported() {
        RenderedSql renderedSql = QueryBuilder.query(TestQProduct.product)
            .select(p -> p.name.toExpression(), p -> p.price.toExpression())
            .groupBy(p -> p.name.toExpression(), p -> p.price.toExpression())
            .toSql();

        assertEquals("SELECT \"p\".\"name\", \"p\".\"price\" FROM \"product\" \"p\" GROUP BY \"p\".\"name\", \"p\".\"price\"", renderedSql.sql());
    }

    @Test
    void staticAggregateOperatorsRenderInHaving() {
        RenderedSql renderedSql = QueryBuilder.query(TestQProduct.product)
            .select(p -> p.name.toExpression(), p -> count(p.id))
            .groupBy(p -> p.name.toExpression())
            .having(p -> gt(count(p.id), 1L))
            .toSql();

        assertEquals("SELECT \"p\".\"name\", COUNT(\"p\".\"id\") FROM \"product\" \"p\" GROUP BY \"p\".\"name\" HAVING COUNT(\"p\".\"id\") > $1", renderedSql.sql());
        assertEquals(1L, renderedSql.parameters().get(0).value());
    }

    @Test
    void innerJoinRendersExpectedSqlAndParameterOrder() {
        Query<TestQProduct> products = QueryBuilder.query(TestQProduct.product);
        Query<TestQPurchase> purchases = QueryBuilder.query(TestQPurchase.purchase);

        RenderedSql renderedSql = products
            .innerJoin(purchases, (p, u) -> p.id.eq(u.productId))
            .where(row -> and(row.as(TestQProduct.class).name.like("Nike%"), row.as(TestQPurchase.class).quantity.gt(1)))
            .toSql();

        assertEquals("SELECT \"p\".* FROM \"product\" \"p\" INNER JOIN \"purchase\" \"u\" ON \"p\".\"id\" = \"u\".\"product_id\" WHERE (\"p\".\"name\" LIKE $1 AND \"u\".\"quantity\" > $2)", renderedSql.sql());
        assertEquals(2, renderedSql.parameters().size());
        assertEquals("Nike%", renderedSql.parameters().get(0).value());
        assertEquals(1, renderedSql.parameters().get(1).value());
    }

    @Test
    void joinedRowRequiresAliasForSelfJoinOfSameType() {
        TestQProduct employee = new TestQProduct("employee");
        TestQProduct manager = new TestQProduct("manager");

        Query<JoinedRow> joined = QueryBuilder.query(employee)
            .innerJoin(QueryBuilder.query(manager), (e, m) -> e.id.eq(m.id));

        JoinedRow row = joined.rowAccessor();
        assertSame(employee, row.as("employee", TestQProduct.class));
        assertSame(manager, row.as("manager", TestQProduct.class));
        assertThrows(IllegalArgumentException.class, () -> row.as(TestQProduct.class));
    }

    @Test
    void likeFailsForNonStringColumns() {
        TestQProduct product = new TestQProduct();
        assertThrows(IllegalArgumentException.class, () -> product.price.like("5%"));
    }

    static final class TestQProduct extends QEntity<TestProduct> {
        static final TestQProduct product = new TestQProduct();
        final QColumn<TestProduct, Integer> id;
        final QColumn<TestProduct, String> name;
        final QColumn<TestProduct, String> description;
        final QColumn<TestProduct, Float> price;
        final QColumn<TestProduct, BigDecimal> cost;

        TestQProduct() {
            this("p");
        }

        TestQProduct(String alias) {
            super(TestProduct.class, "product", alias, List.of());
            this.id = new QColumn<>(this, "id", Integer.class, SqlType.INT, false);
            this.name = new QColumn<>(this, "name", String.class, SqlType.VARCHAR, false);
            this.description = new QColumn<>(this, "description", String.class, SqlType.TEXT, true);
            this.price = new QColumn<>(this, "price", Float.class, SqlType.FLOAT, false);
            this.cost = new QColumn<>(this, "cost", BigDecimal.class, SqlType.DECIMAL, false);
        }
    }

    static final class TestQPurchase extends QEntity<TestPurchase> {
        static final TestQPurchase purchase = new TestQPurchase();
        final QColumn<TestPurchase, Integer> id;
        final QColumn<TestPurchase, Integer> productId;
        final QColumn<TestPurchase, Integer> quantity;

        TestQPurchase() {
            super(TestPurchase.class, "purchase", "u", List.of());
            this.id = new QColumn<>(this, "id", Integer.class, SqlType.INT, false);
            this.productId = new QColumn<>(this, "product_id", Integer.class, SqlType.INT, false);
            this.quantity = new QColumn<>(this, "quantity", Integer.class, SqlType.INT, false);
        }
    }

    private static final class TestProduct {
    }

    private static final class TestPurchase {
    }
}
