# Fluentquery

Fluentquery is a Java fluent SQL query library with build-time metaclass generation from `javax.persistence` entities.

## Modules

- `fluentquery-lib` - runtime fluent API, AST, SQL rendering, dialect support
- `fluentquery-maven-plugin` - generates `Q*` metaclasses from compiled entities
- `fluentquery-e2e-entities` - fixture entities for integration testing
- `fluentquery-e2e` - end-to-end tests validating generated classes + runtime
- `fluentquery-parent` - shared dependency/plugin management

## Current API conventions

- Query entrypoint is instance-based:
  - `QueryBuilder.query(qEntityInstance)`
- Generated metaclasses are emitted to:
  - `<entity package>.fluentquery`
- A generated `QEntities` facade is available per fluentquery package:
  - allows static import of Q singletons

## Build

```bash
mvn clean test
```

## Typical usage

```java
import static com.example.entities.fluentquery.QEntities.*;
import static com.github.rorymurphy.fluentquery.api.QueryBuilder.query;
import static com.github.rorymurphy.fluentquery.api.Operators.and;

class Demo {
    void run() {
        var rendered = query(productEntity)
            .where(p -> and(p.name.like("Nike%"), p.price.lt(new java.math.BigDecimal("5.00"))))
            .toSql();
    }
}
```

## WHERE IN

`IN` is modeled on `QColumn`, not on `Query`:

```java
import static com.example.entities.fluentquery.QEntities.*;
import static com.github.rorymurphy.fluentquery.api.QueryBuilder.query;

var rendered = query(productEntity)
    .where(p -> p.id.in(1, 2, 3))
    .toSql();
```

Static operator helper is also available:

```java
import static com.github.rorymurphy.fluentquery.api.Operators.in;

var rendered = query(productEntity)
    .where(p -> in(p.id, 1, 2, 3))
    .toSql();
```

## Self-join pattern

Use aliasable generated Q classes and alias-based `JoinedRow` access:

```java
import com.example.entities.fluentquery.QEmployeeEntity;
import static com.example.entities.fluentquery.QEntities.*;
import static com.github.rorymurphy.fluentquery.api.QueryBuilder.query;

var employee = employeeEntity.as("employee");
var manager = employeeEntity.as("manager");

var joined = query(employee)
    .innerJoin(query(manager), (e, m) -> e.managerId.eq(m.id));

var sql = joined
    .where(r -> r.as(manager).name.like("A%"))
    .toSql();
```

## Supported query features (MVP)

- `SELECT`
- `WHERE`
- `INNER JOIN`
- `GROUP BY`
- `HAVING`
- PostgreSQL and MySQL SQL rendering

## Notes

- `INSERT` and `UPDATE` are out of MVP functional scope.
- Statement model and renderer are structured for future DML extension.
