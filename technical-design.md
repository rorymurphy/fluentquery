# Fluentquery Technical Design (Current As-Built)

## 1) Scope

MVP currently implements:
- Select queries with where, inner join, group by, having.
- Parameterized SQL rendering (default) and literal mode.
- PostgreSQL and MySQL dialect rendering.
- Build-time metaclass generation from javax.persistence annotations.

Non-MVP but architecture-preserved:
- Insert and update statement extensibility via statement-root abstraction.

## 2) Repository Modules

- fluentquery-parent: shared dependency and plugin management.
- fluentquery-lib: runtime fluent API, AST, rendering, dialects.
- fluentquery-maven-plugin: entity scan + code generation.
- fluentquery-e2e-entities: fixture entities for integration generation tests.
- fluentquery-e2e: end-to-end tests using generated classes.

## 3) Runtime Library Design (fluentquery-lib)

### 3.1 API Entry and Query Immutability

- QueryBuilder exposes a single entrypoint:
  - QueryBuilder.query(T entityAccessor)
- Query is immutable: each fluent call returns a new Query instance.
- Current where and having behavior replaces previous clause state (does not auto-chain).

### 3.2 Query Fluent Surface

- where(Function<R, PredicateExpression>)
- whereEq(Function<R, QColumn<?, V>>, V)
- whereLike(Function<R, QColumn<?, String>>, String)
- select(Function<R, Expression<?>>...)
- groupBy(Function<R, Expression<?>>...)
- having(Function<R, PredicateExpression>)
- innerJoin(Query<U>, BiFunction<R, U, PredicateExpression>)
- toSql(), toSql(SqlDialect), toSqlLiteral(SqlDialect), render(SqlRenderOptions)

Design choice:
- `WHERE IN` is intentionally expressed via `QColumn.in(...)` inside `where(...)` (no `Query.whereIn(...)` shortcut).

### 3.3 Join Row Model and Self-Join Support

- JoinedRow stores accessors by alias, not by Java type.
- Access methods:
  - as(Class<T>): succeeds only when exactly one accessor of that type exists.
  - as(String alias, Class<T>): explicit disambiguation for self-joins.
- Query.innerJoin merges alias maps across existing joined rows.

This supports same-table multi-join scenarios when aliases are distinct.

### 3.4 Metamodel Runtime Types

- QEntity<T>: entityClass, tableName, alias, columns.
- QColumn<OWNER, V>: typed column descriptor and predicate builders.
- SqlType: normalized SQL type enum used by generated metaclasses.

QColumn currently supports:
- eq(V), eq(QColumn<?, V>)
- lt(V), lt(QColumn<?, V>)
- lte(V), lte(QColumn<?, V>)
- gt(V), gt(QColumn<?, V>)
- gte(V), gte(QColumn<?, V>)
- in(Collection<V>), in(V...)
- like(String) for String columns only
- isNull(), isNotNull()

## 4) AST and Rendering

### 4.1 Statement and Expressions

- SqlStatement is the root abstraction.
- SelectStatement is implemented for MVP.
- InsertStatement and UpdateStatement placeholders exist for extensibility.

Key expression nodes:
- ColumnRefExpression
- ParameterExpression
- ComparisonExpression
- InExpression
- LogicalExpression
- NullCheckExpression
- FunctionExpression

### 4.2 Renderer

- DefaultStatementRenderer implements StatementRenderer.
- Handles SELECT with JOIN, WHERE, GROUP BY, HAVING.
- Supports `IN (...)` predicate rendering through `InExpression`.
- FunctionExpression rendering supports aggregate functions such as COUNT, SUM, MAX.
- QueryParameter list preserves deterministic parameter order.

### 4.3 Dialects

- PostgreSqlDialect:
  - quoted identifiers via double quotes
  - parameter placeholders $1, $2, ...
- MySqlDialect:
  - quoted identifiers via backticks
  - parameter placeholder ?

## 5) Operator Utilities

Operators static helpers include:
- logical: and, or
- comparisons on QColumn: eq, lt, lte, gt, gte, like, in
- comparisons on Expression: eq, lt, gt, like
- aggregates: count, sum, max

This supports concise static-import usage in user query code.

## 6) Maven Plugin Design (fluentquery-maven-plugin)

### 6.1 Scan and Model

- Scans compiled classes.
- Parses javax.persistence annotations including Table and Column.
- Builds EntityMeta and ColumnMeta models.

### 6.2 Generated Package and Class Layout

For an entity in package some.package:
- Q class is generated to some.package.fluentquery.
- Q class name is Q + EntitySimpleName.

Generated Q class features:
- Static singleton field named as lower camel of entity class name.
  - Example: ProductEntity -> QProductEntity.productEntity
- Alias factories:
  - as(String alias)
  - withAlias(String alias)
- Constructors:
  - default constructor uses default alias
  - alias constructor for explicit aliasing
- Typed QColumn fields for each mapped column.

Per generated fluentquery package, plugin also generates:
- QEntities class containing static fields for each Q singleton.
  - Enables import static some.package.fluentquery.QEntities.*;

### 6.3 Mojo Flow

GenerateMetaClassesMojo:
1. Scan compiled classes.
2. Generate Q classes under .fluentquery subpackage.
3. Group entities by original package and generate one QEntities facade per package.
4. Write changed files only.
5. Register generated source root.

### 6.4 Build Lifecycle Detail

- Plugin goal generate remains defaultPhase process-classes.
- Plugin descriptor generation for the plugin artifact is explicitly bound to compile in fluentquery-maven-plugin module, ensuring reactor builds can execute the plugin during compile workflows.

## 7) Query Syntax Patterns (Current)

Recommended style with generated facade:

- import static some.package.fluentquery.QEntities.*;
- import static com.github.rorymurphy.fluentquery.api.QueryBuilder.query;

Example:
- query(productEntity).where(p -> p.price.lt(...))

Self-join pattern:
- var employee = employeeEntity.as("employee");
- var manager = employeeEntity.as("manager");
- query(employee).innerJoin(query(manager), (e, m) -> e.managerId.eq(m.id))
- use row.as("employee", QEmployeeEntity.class) and row.as("manager", QEmployeeEntity.class)

## 8) Testing Strategy (Implemented)

- fluentquery-lib unit tests cover:
  - immutability basics
  - SQL rendering snapshots
  - null semantics and type-restricted operators
  - convenience API methods
- fluentquery-maven-plugin unit tests cover:
  - scanner discovery
  - Q class source generation
  - QEntities facade generation
- fluentquery-e2e tests cover:
  - generated class usage via static imports
  - PostgreSQL and MySQL rendering
  - group by/having with aggregate helpers
  - self-join alias rendering and alias-based joined-row access

## 9) Known Constraints and Next Enhancements

- where and having currently replace existing clause state; a future enhancement can provide additive chaining semantics.
- Typed multi-join row modeling is intentionally deferred; alias-based JoinedRow is the current MVP strategy.
- Insert and update remain non-MVP; statement-root architecture is ready for extension.

## 10) Compatibility

- Java target: 21+
- Annotation baseline: javax.persistence
- Official dialects: PostgreSQL and MySQL
- No Hibernate dependency
