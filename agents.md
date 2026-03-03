# Fluentquery

## Purpose

Fluentquery provides a set of tools that make it simple for developers to write complex queries against data using a fluent Java interface. Fluentquery provides compile-time type safety for queries, along with optimizing the generated SQL and accounting for any SQL dialect specific syntax. Fluentquery is similar in nature to [querydsl](), but strives to provide even greater functionality.

## How it works

1. Developers create the entities representing their data model as POJO classes
1. The POJO classes are then decorated with javax.persistence annotations, including `@Table`, `@Column`, `@UniqueConstraint` and `@Id`.
1. Using a Maven plugin, Fluentquery introspects the annotations applied to entities and generates metaclasses that mirror the structure of the entities & their fields.
1. A developer can then use the metaclasses to construct queries as shown in the code snippet below. Fluentquery builds up an expression tree representing the query, which can then be converted into a String.
```java
package somepackage;

import java.util.Date;
import lombok;
import fluentquery.QueryBuilder;
import static fluentquery.Operators; // Provides static methods for operations such as `and`, `or`.
import somepackage.queryentities.*; // Contains QProduct

@Data
@Table("product")
class Product {
    @Column("id")
    private int id;
    @Column("name", length=30)
    private String name;
    @Column("description", length=100)
    private String description;
    @Column("price")
    private float price;
}

class Purchase {
    @Column("id")
    private int id;

    @Column("product_id")
    private int product_id;

    @Column("quantity")
    private int quantity;

    @Column("purchase_timestamp")
    private Date purchase_timestamp;
}

class FluentQuerySyntaxDemo {
    public void queryProductsByPrice(float price) {
        // Construct a query for products whose name starts with "Nike" where the price is less than $5.00
        // Note, only fields of type String should support the "like" operator.
        var query = QueryBuilder.query(QProduct).where(product -> and(product.name.like("Nike%"), product.price.lt(5.00));

        // Using the default SQL dialect (Postgres)
        var sql = query.toSql();
        // To specify a differenct SQL dialect (MySQL)
        var mysql_sql = query.toSql("MySQL");

        // IMPORTANT: The statement below should return a new query object, distinct from `query`
        var query2 = query.where(product -> product.id > 1000);

        // Join query, of type Query<Product> with QPurchase. In the `where` clause, the row now has columns for both Product and Purchase so, in order to
        // write queries against the fields of either, we must first narrow the type to one or the other.
        var join_query = query.innerJoin(QueryBuilder.query(QPurchase), (product, purchase) -> product.id == purchase.product_id).where(row -> row.as<QPurchase>().quantity.eq(2));
    }
}
```

For the above block of code, the variable `sql` should contain syntactically valid SQL that would query all the columns from a table called product, selecting only the rows that start with "Nike" and where the price is less than 5.00.

### Types supported
The library should be able to generate queries for columns with a minimum of the following SQL types:

- INT
- DECIMAL
- FLOAT
- CHAR
- VARCHAR
- TEXT
- NCHAR
- NVARCHAR
- DATE
- TIME
- DATETIME
- TIMESTAMP
- BINARY
- VARBINARY
- BLOB
- BOOLEAN
- JSON
- UUID

## Additional Specifications

- **Immutability**: Once created, a query is immutable. Any additional operation performed on the query (e.g. another "where" call)
  should generate a new query. The expression tree for the new query may point to that of the original.
- **Query scope (MVP):** Include support for `SELECT`, `WHERE`, `JOIN`, `GROUP BY`, and `HAVING`.
- **Typed Methods**: Query is a parameterized type, with the parameter corresponding to a row entity type. However, certain operations
  will create rows that are not directly mappable to an entity (e.g. a JOIN). In such cases 
- **SQL rendering:** Support parameterized SQL (default) and the query must track the set of parameters used
- **Metaclass generation trigger:** Generation is performed by the Maven plugin only (no runtime fallback generation).
- **Initial SQL dialects:** Officially support PostgreSQL and MySQL in the first release.
- **Null semantics**: The library should support .eq(Null) for testing for a null value
- **Java version**: The library should support Java 21 and later
- **Metaclass naming**: Metaclasses are made up of a "Q" prefix + the name of the original entity. Metaclasses should reside in the same package as the original entity.
- **DML extensibility (non-MVP):** `INSERT` and `UPDATE` do not need to be implemented in MVP, but the AST, SQL rendering abstraction, and fluent API layering should be designed so these can be added later without breaking existing `SELECT` APIs.

## Clarifications
For MVP join typing, what should we implement first?
Untyped join rows
- Use a dynamic row model initially, type safety later
What is required for `SELECT` in MVP?
Custom projections too
- Support selecting specific columns/expressions in MVP
How should the Maven plugin discover entities in MVP?
Compiled classes scan
- Inspect compiled bytecode/annotations during build
Which annotation/API baseline should MVP target?
javax.persistence only
- Keep MVP narrow and stable

## Non-functional requirements

- The Maven plugin should provide `debug` level logging of the generation process

## Components

Fluentquery consists of two modules:

- **fluentquery-maven-plugin**: (artifactId: fluentquery-maven-plugin) Parses the annotations applied to entities and generates the appropriate meta classes.
- **fluentquery**: (artifactId: fluentquery) Provides library functions such as fluentQuery.QueryBuilder and fluentQuery.operators.

## Technology

- The Maven plugin should use slf4j logging
- The Maven plugin should support the latest stable version of Maven
- The project should avoid dependency on Hibernate or any similar ORM solutions

## Build

This project utilizes Maven for all builds. A parent POM should be leveraged to specify dependency versions.

The **groupID** for all modules is "com.github.rorymurphy.fluentquery"

## Structure

- `fluentquery-lib/` - contains all code for **fluentquery**
- `fluentquery-maven-plugin/` contains all code for **fluentquery-maven-plugin**
- `fluentquery-parent/` contains all code for the parent POM Maven project.

### Implementation context

- Prefer a dialect abstraction in `fluentquery` so PostgreSQL and MySQL differences are encapsulated cleanly.
- Ensure the API for SQL rendering clearly exposes both parameterized and literal output modes.
- Treat metaclass generation as a build-time concern owned by `fluentquery-maven-plugin`.
- Keep statement modeling extensible so future non-query statements (`INSERT`, `UPDATE`) can reuse parameter handling and dialect translation infrastructure.